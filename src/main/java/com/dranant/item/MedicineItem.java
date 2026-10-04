package com.dranant.item;

import com.dranant.registry.ModEffects;
import com.dranant.util.TimedEntityRemover;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The ten consumable medicine tiers sold in Dr. Anant's clinic. */
public class MedicineItem extends Item {
    private final int tier;

    public MedicineItem(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }

    /** Default shop price (diamonds) used by the Central Shop pedestals. */
    public int getShopPrice() {
        return tier * 2;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return tier == 5 ? 20 : 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return tier == 3 ? UseAnim.EAT : UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            applyMedicine(level, entity);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.4F, 1.6F);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, entity.getX(), entity.getY() + 1.0, entity.getZ(), 15, 0.4, 0.6, 0.4, 0.05);
            }
        }
        if (!(entity instanceof Player p && p.getAbilities().instabuild)) {
            stack.shrink(1);
        }
        return stack;
    }

    private void applyMedicine(Level level, LivingEntity e) {
        switch (tier) {
            case 1 -> { // Fever Syrup: cure all negative effects, +2 hearts
                List<MobEffectInstance> active = new ArrayList<>(e.getActiveEffects());
                for (MobEffectInstance inst : active) {
                    if (inst.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                        e.removeEffect(inst.getEffect());
                    }
                }
                e.heal(4.0F);
            }
            case 2 -> { // Speed Vitamin Drops
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 60, 1));
                e.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 20 * 60, 0));
            }
            case 3 -> { // Iron Bone Capsule
                e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 60, 1));
                e.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 60, 1));
            }
            case 4 -> applyXray(level, e); // X-Ray Vision Tonic
            case 5 -> { // Vitality Heart Injection: 4 permanent yellow hearts (re-filled by ModGameEvents)
                e.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, MobEffectInstance.INFINITE_DURATION, 1, false, true, true));
                e.setAbsorptionAmount(Math.max(e.getAbsorptionAmount(), 8.0F));
            }
            case 6 -> e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 45, 2)); // Regeneration III
            case 7 -> e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 90, 2));  // Strength III
            case 8 -> e.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 300, 0)); // Fire Resistance
            case 9 -> { // Omega Super Serum: all advanced serums at once
                e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 60, 2));
                e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 120, 2));
                e.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 300, 0));
                e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 120, 1));
            }
            case 10 -> { // Immortality Elixir: full heal + one-time resurrection buff
                e.setHealth(e.getMaxHealth());
                e.addEffect(new MobEffectInstance(ModEffects.IMMORTALITY, 20 * 60 * 20, 0, false, true, true));
                e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 10, 1));
                if (e instanceof Player p) {
                    p.getFoodData().eat(20, 1.0F);
                }
            }
            default -> {
            }
        }
    }

    /** Glowing hostile mobs + glowing outlines (temporary block displays) around nearby ores for 45 seconds. */
    private static void applyXray(Level level, LivingEntity user) {
        int duration = 20 * 45;
        for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, user.getBoundingBox().inflate(48), m -> m instanceof Enemy)) {
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0));
        }
        user.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, false, false, true));
        if (!(level instanceof ServerLevel sl)) return;
        BlockPos center = user.blockPosition();
        int spawned = 0;
        int radius = 12;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dy = -radius; dy <= radius && spawned < 96; dy++) {
            for (int dx = -radius; dx <= radius && spawned < 96; dx++) {
                for (int dz = -radius; dz <= radius && spawned < 96; dz++) {
                    m.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = sl.getBlockState(m);
                    if (!state.is(Tags.Blocks.ORES)) continue;
                    if (spawnGlowingOre(sl, m, state, duration)) spawned++;
                }
            }
        }
        if (user instanceof Player p) {
            p.displayClientMessage(Component.literal("X-Ray Vision: " + spawned + " ores revealed!").withStyle(ChatFormatting.GREEN), true);
        }
    }

    private static boolean spawnGlowingOre(ServerLevel level, BlockPos pos, BlockState state, int duration) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:block_display");
        tag.put("block_state", NbtUtils.writeBlockState(state));
        tag.putBoolean("Glowing", true);
        tag.putInt("glow_color_override", 0xFFD700);
        ListTag position = new ListTag();
        position.add(DoubleTag.valueOf(pos.getX()));
        position.add(DoubleTag.valueOf(pos.getY()));
        position.add(DoubleTag.valueOf(pos.getZ()));
        tag.put("Pos", position);
        ListTag tags = new ListTag();
        tags.add(StringTag.valueOf(TimedEntityRemover.XRAY_TAG));
        tag.put("Tags", tags);
        Optional<Entity> display = EntityType.create(tag, level);
        if (display.isPresent()) {
            Entity entity = display.get();
            entity.moveTo(pos.getX(), pos.getY(), pos.getZ(), 0.0F, 0.0F);
            if (level.addFreshEntity(entity)) {
                TimedEntityRemover.schedule(level, entity, duration);
                return true;
            }
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Medicine Tier " + tier).withStyle(ChatFormatting.GOLD));
        String desc = switch (tier) {
            case 1 -> "Removes negative effects, restores 2 hearts";
            case 2 -> "Speed II + Haste I (60s)";
            case 3 -> "Resistance II + Absorption (60s)";
            case 4 -> "Reveals hostile mobs and ores (45s)";
            case 5 -> "+4 permanent yellow hearts";
            case 6 -> "Regeneration III (45s)";
            case 7 -> "Strength III (90s)";
            case 8 -> "Fire Resistance (5 min)";
            case 9 -> "Regen III + Strength III + Fire Res + Resistance II";
            case 10 -> "Full heal + Totem resurrection buff (20 min)";
            default -> "";
        };
        tooltip.add(Component.literal(desc).withStyle(ChatFormatting.GRAY));
    }
}
