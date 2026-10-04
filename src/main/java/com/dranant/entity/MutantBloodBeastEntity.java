package com.dranant.entity;

import com.dranant.DoctorAnantMod;
import com.dranant.clinic.ClinicQueueManager;
import com.dranant.entity.ai.BloodChargeGoal;
import com.dranant.entity.ai.BloodGeyserGoal;
import com.dranant.entity.ai.BloodRoarGoal;
import com.dranant.entity.ai.GroundCrackSlamGoal;
import com.dranant.dialogue.Lines;
import com.dranant.dialogue.Speech;
import com.dranant.registry.ModItems;
import com.dranant.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The Mutant Blood Beast boss. 500 HP, 3.5 blocks tall, dripping blood, ground-crack leap slam.
 * Two-step cure: Titanium Handcuffs below 50% HP (30 s immobilised), then the Universal Cure Syringe.
 */
public class MutantBloodBeastEntity extends Monster {
    private static final EntityDataAccessor<Boolean> DATA_HANDCUFFED = SynchedEntityData.defineId(MutantBloodBeastEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_CURE_TICKS = SynchedEntityData.defineId(MutantBloodBeastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ACTION = SynchedEntityData.defineId(MutantBloodBeastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_ENRAGED = SynchedEntityData.defineId(MutantBloodBeastEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int HANDCUFF_DURATION = 20 * 30;
    public static final int CURE_DURATION = 60;
    public static final int ACTION_NONE = 0, ACTION_SLAM_WINDUP = 1, ACTION_AIRBORNE = 2, ACTION_ROAR = 3,
            ACTION_CHARGE_WINDUP = 4, ACTION_CHARGING = 5, ACTION_STUNNED = 6, ACTION_CAST = 7;
    /** Minimum gap between two special attacks so they never chain instantly. */
    private static final int GLOBAL_SPECIAL_GAP = 50;
    private static final ResourceLocation ENRAGE_SPEED_ID = DoctorAnantMod.id("enraged_speed");
    private static final ResourceLocation HANDCUFF_SPEED_ID = DoctorAnantMod.id("handcuffed_immobile");

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.literal("Mutant Blood Beast").withStyle(ChatFormatting.DARK_RED),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    private int handcuffTicks;
    private int stunTicks;
    private boolean minionsSpawned;
    private long nextAnySpecial;
    private final java.util.Map<String, Long> specialCooldowns = new java.util.HashMap<>();
    @Nullable
    private Player curer;

    public MutantBloodBeastEntity(EntityType<? extends MutantBloodBeastEntity> type, Level level) {
        super(type, level);
        this.xpReward = 250;
        setPersistenceRequired();
        bossEvent.setDarkenScreen(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9D)
                .add(Attributes.MOVEMENT_SPEED, 0.42D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.STEP_HEIGHT, 1.5D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HANDCUFFED, false);
        builder.define(DATA_CURE_TICKS, 0);
        builder.define(DATA_ACTION, ACTION_NONE);
        builder.define(DATA_ENRAGED, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new GroundCrackSlamGoal(this));
        goalSelector.addGoal(2, new BloodChargeGoal(this));
        goalSelector.addGoal(2, new BloodRoarGoal(this));
        goalSelector.addGoal(3, new BloodGeyserGoal(this));
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
    }

    // ------------------------------------------------------------------ state accessors
    public boolean isHandcuffed() {
        return entityData.get(DATA_HANDCUFFED);
    }

    public int getCureTicks() {
        return entityData.get(DATA_CURE_TICKS);
    }

    public boolean isBeingCured() {
        return getCureTicks() > 0;
    }

    public int getActionState() {
        return entityData.get(DATA_ACTION);
    }

    public void setActionState(int state) {
        entityData.set(DATA_ACTION, state);
    }

    public boolean isEnraged() {
        return entityData.get(DATA_ENRAGED);
    }

    public boolean isStunned() {
        return stunTicks > 0;
    }

    public void stun(int ticks) {
        stunTicks = ticks;
        getNavigation().stop();
        setActionState(ACTION_STUNNED);
    }

    /** True when the named special attack is off cooldown and no other special was used in the last 2.5 s. */
    public boolean canUseSpecial(String key) {
        if (isHandcuffed() || isBeingCured() || isStunned() || getActionState() != ACTION_NONE) return false;
        long now = level().getGameTime();
        if (now < nextAnySpecial) return false;
        Long ready = specialCooldowns.get(key);
        if (ready == null) {
            specialCooldowns.put(key, now + 60 + random.nextInt(140)); // stagger the first use of each attack
            return false;
        }
        return now >= ready;
    }

    public void markSpecialUsed(String key, int cooldown) {
        long now = level().getGameTime();
        specialCooldowns.put(key, now + cooldown);
        nextAnySpecial = now + GLOBAL_SPECIAL_GAP;
    }

    /** Smooth size multiplier used by the renderer while the cure shrinks the beast. */
    public float getCureScale(float partialTick) {
        int t = getCureTicks();
        if (t <= 0) return 1.0F;
        float progress = Mth.clamp((t + partialTick) / CURE_DURATION, 0.0F, 1.0F);
        return 1.0F - 0.75F * progress;
    }

    // ------------------------------------------------------------------ cure mechanic
    public boolean canBeHandcuffed() {
        return !isHandcuffed() && !isBeingCured() && getHealth() < getMaxHealth() * 0.5F;
    }

    public void applyHandcuffs(Player by) {
        entityData.set(DATA_HANDCUFFED, true);
        handcuffTicks = HANDCUFF_DURATION;
        getNavigation().stop();
        setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
        setActionState(ACTION_NONE);
        stunTicks = 0;
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(HANDCUFF_SPEED_ID, -1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        bossEvent.setColor(BossEvent.BossBarColor.YELLOW);
        bossEvent.setName(Component.literal("Mutant Blood Beast - HANDCUFFED").withStyle(ChatFormatting.YELLOW));
        level().playSound(null, blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 1.5F, 0.8F);
        level().playSound(null, blockPosition(), ModSounds.HANDCUFF_RATTLE.get(), SoundSource.HOSTILE, 1.5F, 1.0F);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CRIT, getX(), getY() + 2.0, getZ(), 30, 0.8, 0.8, 0.8, 0.2);
        }
    }

    private void releaseHandcuffs() {
        entityData.set(DATA_HANDCUFFED, false);
        handcuffTicks = 0;
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(HANDCUFF_SPEED_ID);
        bossEvent.setColor(BossEvent.BossBarColor.RED);
        bossEvent.setName(Component.literal("Mutant Blood Beast").withStyle(ChatFormatting.DARK_RED));
        level().playSound(null, blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.HOSTILE, 1.5F, 0.6F);
        level().playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.0F, 0.6F);
    }

    public void startCure(Player by) {
        if (!isHandcuffed() || isBeingCured()) return;
        curer = by;
        entityData.set(DATA_CURE_TICKS, 1);
        level().playSound(null, blockPosition(), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 1.5F, 1.0F);
        level().playSound(null, blockPosition(), ModSounds.SYRINGE_INJECTION.get(), SoundSource.PLAYERS, 1.5F, 1.0F);
    }

    private void finishCure(ServerLevel level) {
        Vec3 pos = position();
        Villager villager = EntityType.VILLAGER.create(level);
        if (villager != null) {
            villager.moveTo(pos.x, pos.y, pos.z, getYRot(), 0.0F);
            villager.setVillagerData(villager.getVillagerData().setProfession(VillagerProfession.CLERIC).setLevel(5));
            villager.setCustomName(Component.literal("Cured Patient").withStyle(ChatFormatting.GREEN));
            villager.setPersistenceRequired();
            level.addFreshEntity(villager);
        }
        level.playSound(null, blockPosition(), SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1.5F, 1.0F);
        level.playSound(null, blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.playSound(null, blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.HEART, pos.x, pos.y + 1.5, pos.z, 40, 1.0, 1.0, 1.0, 0.2);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.x, pos.y + 1.0, pos.z, 120, 0.8, 1.2, 0.8, 0.5);
        showerRewards(level, pos);
        level.players().forEach(p -> {
            if (p.distanceToSqr(this) < 64 * 64) {
                p.sendSystemMessage(Component.literal("<Cured Patient> Dhanyavaad Dr. Anant! You saved my life! Please take these 128 Diamonds!")
                        .withStyle(ChatFormatting.GREEN));
            }
        });
        bossEvent.removeAllPlayers();
        discard();
    }

    /** Showers 128 diamonds (16 bursts of 8) and rare items toward the doctor who cured the beast. */
    private void showerRewards(ServerLevel level, Vec3 from) {
        Vec3 target = curer != null ? curer.position() : from.add(0, 0, 2);
        Vec3 dir = target.subtract(from);
        double len = Math.max(0.001D, dir.length());
        Vec3 norm = dir.scale(1.0D / len);
        for (int i = 0; i < 16; i++) {
            spawnReward(level, from, norm, new ItemStack(Items.DIAMOND, 8));
        }
        spawnReward(level, from, norm, new ItemStack(Items.TOTEM_OF_UNDYING));
        spawnReward(level, from, norm, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 2));
        spawnReward(level, from, norm, new ItemStack(Items.NETHERITE_INGOT, 2));
        spawnReward(level, from, norm, new ItemStack(Items.EMERALD, 32));
        spawnReward(level, from, norm, new ItemStack(ModItems.IMMORTALITY_ELIXIR.get()));
        spawnReward(level, from, norm, new ItemStack(ModItems.UNIVERSAL_CURE_SYRINGE.get()));
    }

    private void spawnReward(ServerLevel level, Vec3 from, Vec3 dir, ItemStack stack) {
        ItemEntity item = new ItemEntity(level, from.x, from.y + 1.5, from.z, stack);
        double spread = 0.15D;
        item.setDeltaMovement(dir.x * 0.35D + (random.nextDouble() - 0.5D) * spread, 0.35D + random.nextDouble() * 0.25D,
                dir.z * 0.35D + (random.nextDouble() - 0.5D) * spread);
        if (stack.is(Items.DIAMOND)) {
            item.getPersistentData().putBoolean(ClinicQueueManager.INCOME_TAG, true);
        }
        item.setPickUpDelay(15);
        level.addFreshEntity(item);
    }

    // ------------------------------------------------------------------ ticking
    @Override
    public void tick() {
        super.tick();
        Level level = level();
        if (level.isClientSide) {
            spawnClientParticles();
            return;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        int cure = getCureTicks();
        if (cure > 0) {
            setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
            entityData.set(DATA_CURE_TICKS, cure + 1);
            if (cure % 4 == 0) {
                serverLevel.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() * getCureScale(0) * 0.6, getZ(), 6, 0.9, 0.9, 0.9, 0.1);
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.0, getZ(), 8, 0.9, 1.2, 0.9, 0.1);
            }
            if (cure >= CURE_DURATION) {
                finishCure(serverLevel);
            }
            return;
        }
        if (stunTicks > 0) {
            setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
            if (stunTicks % 5 == 0) serverLevel.sendParticles(ParticleTypes.CRIT, getX(), getY() + getBbHeight() + 0.3, getZ(), 6, 0.5, 0.1, 0.5, 0.05);
            if (--stunTicks <= 0 && getActionState() == ACTION_STUNNED) setActionState(ACTION_NONE);
        }
        tickPhases(serverLevel);
        if (isHandcuffed()) {
            setDeltaMovement(0.0D, getDeltaMovement().y, 0.0D);
            if (--handcuffTicks <= 0) {
                releaseHandcuffs();
            } else if (handcuffTicks % 20 == 0) {
                serverLevel.playSound(null, blockPosition(), ModSounds.HANDCUFF_RATTLE.get(), SoundSource.HOSTILE, 0.5F, 1.2F);
            }
        }
        bossEvent.setProgress(getHealth() / getMaxHealth());
    }

    /** Phase 2 (below 50% HP): enraged - faster, shorter cooldowns. Below 30% HP: summons Infected Patients once. */
    private void tickPhases(ServerLevel level) {
        if (!isEnraged() && getHealth() < getMaxHealth() * 0.5F) {
            entityData.set(DATA_ENRAGED, true);
            AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.addOrUpdateTransientModifier(new AttributeModifier(ENRAGE_SPEED_ID, 0.3D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            level.playSound(null, blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 3.0F, 0.6F);
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getY() + 3.0, getZ(), 20, 1.0, 1.0, 1.0, 0.1);
            Speech.say(this, "ENOUGH!!! RAAAAAGH!", Speech.STYLE_BOSS, 60);
            bossEvent.setName(Component.literal("Mutant Blood Beast - ENRAGED").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            level.players().forEach(p -> {
                if (p.distanceToSqr(this) < 64 * 64) p.displayClientMessage(Component.literal("The Mutant Blood Beast is ENRAGED! Handcuff it now!").withStyle(ChatFormatting.RED), true);
            });
        }
        if (!minionsSpawned && getHealth() < getMaxHealth() * 0.3F) {
            minionsSpawned = true;
            for (int i = 0; i < 3; i++) {
                net.minecraft.world.entity.monster.Zombie zombie = EntityType.ZOMBIE.create(level);
                if (zombie == null) continue;
                double a = Math.PI * 2 * i / 3;
                zombie.moveTo(getX() + Math.cos(a) * 3, getY(), getZ() + Math.sin(a) * 3, random.nextFloat() * 360, 0);
                zombie.setCustomName(Component.literal("Infected Patient").withStyle(ChatFormatting.DARK_RED));
                ItemStack shirt = new ItemStack(Items.LEATHER_CHESTPLATE);
                shirt.set(net.minecraft.core.component.DataComponents.DYED_COLOR, new net.minecraft.world.item.component.DyedItemColor(0x8B0000, false));
                zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, shirt);
                zombie.setDropChance(net.minecraft.world.entity.EquipmentSlot.CHEST, 0.0F);
                if (getTarget() != null) zombie.setTarget(getTarget());
                level.addFreshEntity(zombie);
                level.sendParticles(DustParticleOptions.REDSTONE, zombie.getX(), zombie.getY() + 1, zombie.getZ(), 30, 0.4, 0.8, 0.4, 0);
            }
            Speech.say(this, "MY PATIENTS... ATTACK!", Speech.STYLE_BOSS, 50);
        }
        if (tickCount % 400 == 0 && getTarget() != null && random.nextFloat() < 0.5F) {
            Speech.say(this, Lines.pick(Lines.BOSS, random), Speech.STYLE_BOSS, 40);
        }
    }

    private void spawnClientParticles() {
        Level level = level();
        // continuous dripping blood
        for (int i = 0; i < 2; i++) {
            level.addParticle(DustParticleOptions.REDSTONE, getRandomX(0.7D), getY() + random.nextDouble() * getBbHeight() * getCureScale(0),
                    getRandomZ(0.7D), 0.0D, -0.08D, 0.0D);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.DRIPPING_LAVA, getRandomX(0.5D), getY() + getBbHeight() * 0.7D, getRandomZ(0.5D), 0.0D, 0.0D, 0.0D);
        }
        if (isEnraged() && random.nextInt(2) == 0) {
            level.addParticle(new DustParticleOptions(new org.joml.Vector3f(1.0F, 0.0F, 0.0F), 1.5F), getRandomX(1.0D), getY() + random.nextDouble() * 3.0D, getRandomZ(1.0D), 0.0D, 0.05D, 0.0D);
        }
        if (isHandcuffed() && random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.CRIT, getRandomX(1.0D), getY() + 1.2D, getRandomZ(1.0D), 0.0D, 0.05D, 0.0D);
        }
    }

    /** Freezes AI (goals, navigation, look) while handcuffed or being cured. */
    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || isHandcuffed() || isBeingCured() || isStunned();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isBeingCured()) return false;
        if (isHandcuffed() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return super.hurt(source, amount);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return !isHandcuffed() && !isBeingCured();
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        spawnAtLocation(new ItemStack(Items.DIAMOND, 16));
        spawnAtLocation(new ItemStack(ModItems.EXPIRED_EXPERIMENTAL_SERUM.get()));
    }

    // ------------------------------------------------------------------ boss bar
    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    // ------------------------------------------------------------------ sounds
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.55F;
    }

    // ------------------------------------------------------------------ save data
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Handcuffed", isHandcuffed());
        tag.putInt("HandcuffTicks", handcuffTicks);
        tag.putBoolean("Enraged", isEnraged());
        tag.putBoolean("MinionsSpawned", minionsSpawned);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        minionsSpawned = tag.getBoolean("MinionsSpawned");
        if (tag.getBoolean("Enraged")) entityData.set(DATA_ENRAGED, true);
        if (tag.getBoolean("Handcuffed") && tag.getInt("HandcuffTicks") > 0) {
            entityData.set(DATA_HANDCUFFED, true);
            handcuffTicks = tag.getInt("HandcuffTicks");
        }
    }
}
