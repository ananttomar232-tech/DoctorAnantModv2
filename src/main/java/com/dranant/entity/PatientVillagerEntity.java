package com.dranant.entity;

import com.dranant.block.DiamondCashRegisterBlock;
import com.dranant.block.ExaminationBedBlock;
import com.dranant.block.entity.DiamondCashRegisterBlockEntity;
import com.dranant.dialogue.Lines;
import com.dranant.dialogue.Speech;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A clinic customer that looks exactly like a villager (vanilla villager model + biome/profession textures)
 * but is driven 100% by the clinic queue AI, so it always walks into the clinic and stands in a neat line.
 * Every patient has its own problem which it tells out loud. Mild problems go to Line 1 (counter medicine),
 * severe ones to Line 2 (stretcher -> examination bed). Nearby vanilla villagers are converted into patients
 * and turned back into the very same villager (trades kept) when they leave.
 */
public class PatientVillagerEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> DATA_TYPE = SynchedEntityData.defineId(PatientVillagerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PROFESSION = SynchedEntityData.defineId(PatientVillagerEntity.class, EntityDataSerializers.INT);

    public static final String[] TYPES = {"plains", "desert", "jungle", "savanna", "snow", "swamp", "taiga"};
    public static final String[] PROFESSIONS = {"none", "farmer", "fisherman", "shepherd", "fletcher", "librarian", "cartographer",
            "cleric", "armorer", "weaponsmith", "toolsmith", "butcher", "leatherworker", "mason", "nitwit"};

    public static final int STATE_IDLE = 0;
    public static final int STATE_QUEUED = 1;
    public static final int STATE_AT_COUNTER = 2;
    public static final int STATE_BED_WALK = 3;
    public static final int STATE_ON_BED = 4;
    public static final int STATE_LEAVING = 5;

    private static final double WALK_SPEED = 0.7D;

    private int state = STATE_IDLE;
    @Nullable private BlockPos clinic;
    private int line;
    private boolean severe;
    private int problem;
    @Nullable private CompoundTag originalVillager;

    // runtime (not saved: recomputed by the clinic every few ticks)
    @Nullable private Vec3 moveTarget;
    @Nullable private Vec3 lookTarget;
    @Nullable private BlockPos lieTarget;
    private boolean lieIsBed;
    @Nullable private BlockPos lyingAt;
    private boolean lyingOnBed;
    @Nullable private Vec3 exitTarget;
    private int leaveTicks;
    private int repathCooldown;
    private int stuckSeconds;
    @Nullable private Vec3 lastPos;
    private int chatterCooldown = 200;
    private boolean pendingStandUp;

    public PatientVillagerEntity(EntityType<? extends PatientVillagerEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        if (getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
            nav.setCanFloat(true);
        }
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TYPE, 0);
        builder.define(DATA_PROFESSION, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new OpenDoorGoal(this, true));
    }

    // ------------------------------------------------------------------ appearance
    public String getVillagerTypeName() {
        return TYPES[Mth.clamp(entityData.get(DATA_TYPE), 0, TYPES.length - 1)];
    }

    public String getProfessionName() {
        return PROFESSIONS[Mth.clamp(entityData.get(DATA_PROFESSION), 0, PROFESSIONS.length - 1)];
    }

    public void setLook(int typeIndex, int professionIndex) {
        entityData.set(DATA_TYPE, Mth.clamp(typeIndex, 0, TYPES.length - 1));
        entityData.set(DATA_PROFESSION, Mth.clamp(professionIndex, 0, PROFESSIONS.length - 1));
    }

    public void randomizeLook(String biomeType) {
        int t = 0;
        for (int i = 0; i < TYPES.length; i++) if (TYPES[i].equals(biomeType)) t = i;
        setLook(t, random.nextInt(PROFESSIONS.length));
    }

    /** Takes over a vanilla villager: copies its look and remembers it so it can be restored later. */
    public void copyFrom(Villager villager) {
        String type = BuiltInRegistries.VILLAGER_TYPE.getKey(villager.getVillagerData().getType()).getPath();
        String prof = BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession()).getPath();
        int t = 0, p = 0;
        for (int i = 0; i < TYPES.length; i++) if (TYPES[i].equals(type)) t = i;
        for (int i = 0; i < PROFESSIONS.length; i++) if (PROFESSIONS[i].equals(prof)) p = i;
        setLook(t, p);
        originalVillager = villager.saveWithoutId(new CompoundTag());
        if (villager.hasCustomName()) setCustomName(villager.getCustomName());
        moveTo(villager.getX(), villager.getY(), villager.getZ(), villager.getYRot(), villager.getXRot());
    }

    // ------------------------------------------------------------------ clinic API
    public void assign(BlockPos clinicPos, int lineId, boolean isSevere) {
        clinic = clinicPos;
        line = lineId;
        severe = isSevere;
        state = STATE_QUEUED;
        problem = random.nextInt(isSevere ? Lines.SEVERE_PROBLEMS.length : Lines.MILD_PROBLEMS.length);
        Speech.say(this, getProblemText(), Speech.STYLE_PATIENT);
        chatterCooldown = 300 + random.nextInt(300);
    }

    public String getProblemText() {
        String[] pool = severe ? Lines.SEVERE_PROBLEMS : Lines.MILD_PROBLEMS;
        return pool[Mth.clamp(problem, 0, pool.length - 1)];
    }

    @Nullable
    public BlockPos getClinic() {
        return clinic;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int lineId) {
        this.line = lineId;
    }

    public boolean isSevere() {
        return severe;
    }

    public int getState() {
        return state;
    }

    public boolean isQueued() {
        return state == STATE_QUEUED || state == STATE_AT_COUNTER;
    }

    public void setQueueTarget(Vec3 spot, Vec3 look) {
        if (lyingAt != null) standUp();
        moveTarget = spot;
        lookTarget = look;
        lieTarget = null;
    }

    public void setStretcherTarget(BlockPos stretcher) {
        if (lyingAt != null && lyingAt.equals(stretcher)) return;
        moveTarget = Vec3.atBottomCenterOf(stretcher);
        lookTarget = null;
        lieTarget = stretcher;
        lieIsBed = false;
    }

    @Nullable
    public BlockPos getStretcher() {
        if (lyingAt != null && !lyingOnBed) return lyingAt;
        return lieTarget != null && !lieIsBed ? lieTarget : null;
    }

    public boolean isAtTarget() {
        if (lyingAt != null) return true;
        return moveTarget != null && horizontalDistTo(moveTarget) < 0.6D && Math.abs(getY() - moveTarget.y) < 1.5D;
    }

    public void startCounter() {
        state = STATE_AT_COUNTER;
    }

    public void callToBed(BlockPos bedHead) {
        if (lyingAt != null) standUp();
        state = STATE_BED_WALK;
        Direction facing = bedFacing(bedHead);
        BlockPos foot = bedHead.relative(facing.getOpposite());
        moveTarget = Vec3.atBottomCenterOf(foot.relative(facing.getOpposite()));
        lookTarget = Vec3.atCenterOf(bedHead);
        lieTarget = bedHead;
        lieIsBed = true;
    }

    public boolean isOnBed() {
        return lyingAt != null && lyingOnBed;
    }

    public void forceOnBed(BlockPos bedHead) {
        lieDown(bedHead, true);
    }

    /** Leaves the clinic; restores the original villager (or vanishes) once far enough away. */
    public void leave(boolean happy) {
        if (lyingAt != null) standUp();
        state = STATE_LEAVING;
        leaveTicks = 220;
        Vec3 origin = clinic != null ? Vec3.atBottomCenterOf(clinic) : position();
        Direction away = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        if (clinic != null && level().getBlockState(clinic).hasProperty(DiamondCashRegisterBlock.FACING)) {
            away = level().getBlockState(clinic).getValue(DiamondCashRegisterBlock.FACING);
        }
        exitTarget = origin.add(away.getStepX() * 26.0D + (random.nextDouble() - 0.5D) * 10.0D, 0.0D,
                away.getStepZ() * 26.0D + (random.nextDouble() - 0.5D) * 10.0D);
        moveTarget = exitTarget;
        lookTarget = null;
        lieTarget = null;
        if (happy && level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1.8D, getZ(), 8, 0.3D, 0.3D, 0.3D, 0.05D);
        }
    }

    // ------------------------------------------------------------------ lying down
    private static Direction bedFacing(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.hasProperty(ExaminationBedBlock.FACING) ? state.getValue(ExaminationBedBlock.FACING) : Direction.NORTH;
    }

    private Direction bedFacing(BlockPos pos) {
        return bedFacing(level(), pos);
    }

    private static float sleepYaw(Direction headDirection) {
        return switch (headDirection) {
            case SOUTH -> 90.0F;
            case WEST -> 0.0F;
            case EAST -> 180.0F;
            default -> 270.0F;
        };
    }

    private Vec3 lieSpot(BlockPos pos, boolean bed) {
        Direction facing = bedFacing(pos);
        if (bed) {
            BlockPos foot = pos.relative(facing.getOpposite());
            return Vec3.atBottomCenterOf(foot).add(-facing.getStepX() * 0.35D, 0.62D + 0.25D, -facing.getStepZ() * 0.35D);
        }
        return Vec3.atBottomCenterOf(pos).add(-facing.getStepX() * 0.45D, 0.44D + 0.25D, -facing.getStepZ() * 0.45D);
    }

    private void lieDown(BlockPos pos, boolean bed) {
        getNavigation().stop();
        lyingAt = pos;
        lyingOnBed = bed;
        if (bed) state = STATE_ON_BED;
        setPose(Pose.SLEEPING);
        setNoGravity(true);
        Vec3 spot = lieSpot(pos, bed);
        teleportTo(spot.x, spot.y, spot.z);
        applySleepRotation();
        playSound(SoundEvents.WOOL_PLACE, 0.6F, 1.0F);
    }

    private void applySleepRotation() {
        if (lyingAt == null) return;
        float yaw = sleepYaw(bedFacing(lyingAt));
        setYRot(yaw);
        setYHeadRot(yaw);
        yBodyRot = yaw;
        yBodyRotO = yaw;
        yHeadRotO = yaw;
    }

    public void standUp() {
        if (lyingAt == null) return;
        BlockPos pos = lyingAt;
        boolean bed = lyingOnBed;
        lyingAt = null;
        lyingOnBed = false;
        setPose(Pose.STANDING);
        setNoGravity(false);
        Direction facing = bedFacing(pos);
        BlockPos stand = bed ? pos.relative(facing.getOpposite(), 2) : pos.relative(facing.getClockWise());
        if (!level().getBlockState(stand).getCollisionShape(level(), stand).isEmpty()) stand = pos.above();
        teleportTo(stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D);
    }

    // ------------------------------------------------------------------ AI
    private double horizontalDistTo(Vec3 v) {
        double dx = getX() - v.x, dz = getZ() - v.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel serverLevel)) return;
        if (pendingStandUp) {
            pendingStandUp = false;
            standUp();
        }

        if (lyingAt != null) {
            if (getPose() != Pose.SLEEPING) setPose(Pose.SLEEPING);
            Vec3 spot = lieSpot(lyingAt, lyingOnBed);
            if (position().distanceToSqr(spot) > 0.01D) teleportTo(spot.x, spot.y, spot.z);
            setDeltaMovement(Vec3.ZERO);
            applySleepRotation();
            if (severe && tickCount % 40 == 0) {
                serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, getX(), getY() + 0.5D, getZ(), 1, 0.3D, 0.1D, 0.3D, 0.0D);
            }
            chatter();
            return;
        }

        if (tickCount % 100 == 0 && clinic != null && state != STATE_LEAVING && serverLevel.isLoaded(clinic)
                && !(serverLevel.getBlockEntity(clinic) instanceof DiamondCashRegisterBlockEntity)) {
            leave(false); // clinic was destroyed
        }

        if (state == STATE_LEAVING) {
            if (--leaveTicks <= 0 || (exitTarget != null && horizontalDistTo(exitTarget) < 2.0D)) {
                finishVisit(serverLevel);
                return;
            }
        }

        if (moveTarget == null) return;
        double dist = horizontalDistTo(moveTarget);
        if (dist > 0.55D) {
            if (--repathCooldown <= 0 || getNavigation().isDone()) {
                repathCooldown = 20;
                boolean ok = getNavigation().moveTo(moveTarget.x, moveTarget.y, moveTarget.z, WALK_SPEED);
                if (!ok) {
                    Vec3 step = LandRandomPos.getPosTowards(this, 16, 7, moveTarget);
                    if (step != null) getNavigation().moveTo(step.x, step.y, step.z, WALK_SPEED);
                }
            }
            if (tickCount % 20 == 0) {
                if (lastPos != null && position().distanceTo(lastPos) < 0.25D) stuckSeconds++;
                else stuckSeconds = 0;
                lastPos = position();
            }
            if (lieTarget != null && !lieIsBed && dist < 1.4D) {
                lieDown(lieTarget, false);
                return;
            }
            if (stuckSeconds >= 4 && state != STATE_LEAVING) {
                stuckSeconds = 0;
                if (dist < 12.0D) hopTo(moveTarget);
            }
        } else {
            getNavigation().stop();
            if (dist > 0.08D) {
                Vec3 to = new Vec3(moveTarget.x, getY(), moveTarget.z);
                Vec3 step = to.subtract(position());
                double len = step.length();
                setPos(position().add(step.scale(Math.min(1.0D, 0.15D / Math.max(0.001D, len)))));
            }
            if (lieTarget != null) {
                if (lieIsBed && state == STATE_BED_WALK) {
                    lieDown(lieTarget, true);
                } else if (!lieIsBed) {
                    lieDown(lieTarget, false);
                }
                return;
            }
            if (lookTarget != null) {
                float yaw = (float) (Mth.atan2(lookTarget.z - getZ(), lookTarget.x - getX()) * Mth.RAD_TO_DEG) - 90.0F;
                setYRot(yaw);
                yBodyRot = yaw;
                getLookControl().setLookAt(lookTarget.x, lookTarget.y + 0.5D, lookTarget.z);
            }
            chatter();
        }
        // bed walkers that cannot quite reach the bed side lie down once close
        if (state == STATE_BED_WALK && lieTarget != null && position().distanceTo(Vec3.atCenterOf(lieTarget)) < 2.6D) {
            lieDown(lieTarget, true);
        }
    }

    private void hopTo(Vec3 target) {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.5D, getZ(), 6, 0.2D, 0.3D, 0.2D, 0.02D);
            teleportTo(target.x, target.y, target.z);
            sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.5D, getZ(), 6, 0.2D, 0.3D, 0.2D, 0.02D);
        }
    }

    private void chatter() {
        if (state != STATE_QUEUED || --chatterCooldown > 0) return;
        chatterCooldown = 400 + random.nextInt(500);
        if (random.nextFloat() < 0.45F) {
            Speech.say(this, random.nextFloat() < 0.4F ? getProblemText() : Lines.pick(Lines.WAITING, random), Speech.STYLE_PATIENT);
        }
    }

    private void finishVisit(ServerLevel level) {
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        if (originalVillager != null) {
            CompoundTag tag = originalVillager.copy();
            tag.putString("id", "minecraft:villager");
            tag.remove("Pos");
            tag.remove("Motion");
            Entity restored = EntityType.loadEntityRecursive(tag, level, e -> {
                e.moveTo(getX(), getY(), getZ(), getYRot(), 0.0F);
                return e;
            });
            if (restored != null) {
                discard();
                level.addFreshEntity(restored);
                return;
            }
        }
        level.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.0D, getZ(), 10, 0.3D, 0.5D, 0.3D, 0.02D);
        discard();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide && hand == InteractionHand.MAIN_HAND) {
            Speech.say(this, state == STATE_LEAVING ? Lines.pick(Lines.CURED_MILD, random) : getProblemText(), Speech.STYLE_PATIENT);
            playSound(SoundEvents.VILLAGER_AMBIENT, 1.0F, 1.0F);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && severe && random.nextInt(30) == 0) {
            level().addParticle(ParticleTypes.SNEEZE, getX(), getEyeY(), getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return lyingAt == null && state != STATE_AT_COUNTER;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return lyingAt != null ? null : SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    // ------------------------------------------------------------------ save data
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("PatientState", state);
        if (clinic != null) tag.putLong("Clinic", clinic.asLong());
        tag.putInt("Line", line);
        tag.putBoolean("Severe", severe);
        tag.putInt("Problem", problem);
        tag.putInt("LookType", entityData.get(DATA_TYPE));
        tag.putInt("LookProfession", entityData.get(DATA_PROFESSION));
        if (originalVillager != null) tag.put("OriginalVillager", originalVillager);
        if (lyingAt != null) {
            tag.putLong("LyingAt", lyingAt.asLong());
            tag.putBoolean("LyingOnBed", lyingOnBed);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        state = tag.getInt("PatientState");
        clinic = tag.contains("Clinic") ? BlockPos.of(tag.getLong("Clinic")) : null;
        line = tag.getInt("Line");
        severe = tag.getBoolean("Severe");
        problem = tag.getInt("Problem");
        setLook(tag.getInt("LookType"), tag.getInt("LookProfession"));
        originalVillager = tag.contains("OriginalVillager") ? tag.getCompound("OriginalVillager") : null;
        if (tag.contains("LyingAt")) {
            lyingAt = BlockPos.of(tag.getLong("LyingAt"));
            lyingOnBed = tag.getBoolean("LyingOnBed");
            setNoGravity(true);
        }
        if (state == STATE_LEAVING) leave(false);
        if (state == STATE_BED_WALK || state == STATE_ON_BED) {
            // treatment does not survive a reload: stand up and rejoin the clinic queue
            state = STATE_QUEUED;
            if (lyingOnBed) pendingStandUp = true;
        }
    }
}
