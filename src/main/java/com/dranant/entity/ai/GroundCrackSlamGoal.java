package com.dranant.entity.ai;

import com.dranant.entity.MutantBloodBeastEntity;
import com.dranant.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Every 12-15 seconds the beast crouches, leaps ~8 blocks into the air toward its target and slams the ground:
 * explosion sound, a ring of block-breaking particles, 15 damage + Slowness III + knockback to everything within 6 blocks.
 */
public class GroundCrackSlamGoal extends Goal {
    private static final int WINDUP_TICKS = 12;
    private static final int MAX_AIR_TICKS = 80;
    private static final double LEAP_VELOCITY_Y = 1.25D; // ~8 blocks apex with vanilla gravity + drag
    private static final double SLAM_RADIUS = 6.0D;
    private static final float SLAM_DAMAGE = 15.0F;

    private final MutantBloodBeastEntity beast;
    private long nextSlamTime;
    private int phase; // 0 windup, 1 airborne, 2 finished
    private int timer;

    public GroundCrackSlamGoal(MutantBloodBeastEntity beast) {
        this.beast = beast;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    private void scheduleNext() {
        // 12-15 s normally, 7-9 s when enraged
        nextSlamTime = beast.level().getGameTime() + (beast.isEnraged() ? 140 + beast.getRandom().nextInt(41) : 240 + beast.getRandom().nextInt(61));
    }

    @Override
    public boolean canUse() {
        if (nextSlamTime == 0L) scheduleNext();
        LivingEntity target = beast.getTarget();
        if (target == null || !target.isAlive() || beast.isHandcuffed() || beast.isBeingCured() || beast.isStunned()) return false;
        if (!beast.canUseSpecial("slam")) return false;
        if (!beast.onGround() || beast.level().getGameTime() < nextSlamTime) return false;
        double dist = beast.distanceTo(target);
        return dist >= 3.0D && dist <= 28.0D;
    }

    @Override
    public boolean canContinueToUse() {
        return phase != 2 && beast.isAlive() && !beast.isHandcuffed() && !beast.isBeingCured();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        phase = 0;
        timer = 0;
        beast.getNavigation().stop();
        beast.setActionState(MutantBloodBeastEntity.ACTION_SLAM_WINDUP);
    }

    @Override
    public void stop() {
        beast.setActionState(MutantBloodBeastEntity.ACTION_NONE);
        beast.markSpecialUsed("slam", 0);
        scheduleNext();
    }

    @Override
    public void tick() {
        LivingEntity target = beast.getTarget();
        timer++;
        if (phase == 0) {
            if (target != null) beast.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (timer >= WINDUP_TICKS) launch(target);
        } else if (phase == 1) {
            if ((timer > 5 && beast.onGround()) || timer > MAX_AIR_TICKS) {
                slam();
                phase = 2;
            }
        }
    }

    private void launch(LivingEntity target) {
        Vec3 horizontal = Vec3.ZERO;
        if (target != null) {
            Vec3 delta = target.position().subtract(beast.position());
            double dist = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            if (dist > 0.01D) {
                // airborne ~30 ticks with 0.91 air drag -> total horizontal travel ~= v * 10.5
                double speed = Mth.clamp(dist / 10.5D, 0.0D, 2.2D);
                horizontal = new Vec3(delta.x / dist * speed, 0.0D, delta.z / dist * speed);
            }
            float yaw = (float) (Mth.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - 90.0F;
            beast.setYRot(yaw);
            beast.yBodyRot = yaw;
            beast.yHeadRot = yaw;
        }
        beast.setDeltaMovement(horizontal.x, LEAP_VELOCITY_Y, horizontal.z);
        beast.hasImpulse = true;
        beast.setActionState(MutantBloodBeastEntity.ACTION_AIRBORNE);
        beast.playSound(net.minecraft.sounds.SoundEvents.RAVAGER_ROAR, 2.0F, 0.7F);
        phase = 1;
        timer = 0;
    }

    private void slam() {
        if (!(beast.level() instanceof ServerLevel level)) return;
        double x = beast.getX(), y = beast.getY(), z = beast.getZ();
        level.playSound(null, x, y, z, ModSounds.GROUND_SLAM.get(), SoundSource.HOSTILE, 3.0F, 0.75F);

        BlockPos below = beast.blockPosition().below();
        BlockState ground = level.getBlockState(below);
        if (ground.isAir()) ground = Blocks.DIRT.defaultBlockState();
        BlockParticleOption crack = new BlockParticleOption(ParticleTypes.BLOCK, ground);
        for (int ring = 1; ring <= (int) SLAM_RADIUS; ring++) {
            int points = ring * 10;
            for (int i = 0; i < points; i++) {
                double a = (Math.PI * 2.0D * i) / points;
                level.sendParticles(crack, x + Math.cos(a) * ring, y + 0.15D, z + Math.sin(a) * ring, 3, 0.15D, 0.05D, 0.15D, 0.15D);
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION, x, y + 0.5D, z, 4, 1.2D, 0.2D, 1.2D, 0.0D);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.2D, z, 20, 2.5D, 0.1D, 2.5D, 0.02D);

        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, beast.getBoundingBox().inflate(SLAM_RADIUS, 3.0D, SLAM_RADIUS),
                e -> e != beast && e.isAlive() && e.distanceTo(beast) <= SLAM_RADIUS + 1.0D);
        for (LivingEntity victim : victims) {
            if (victim.hurt(beast.damageSources().mobAttack(beast), SLAM_DAMAGE)) {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 5, 2));
            }
            Vec3 push = victim.position().subtract(beast.position());
            double len = Math.sqrt(push.x * push.x + push.z * push.z);
            if (len < 0.01D) push = new Vec3(1.0D, 0.0D, 0.0D); else push = new Vec3(push.x / len, 0.0D, push.z / len);
            double strength = 1.8D * (1.0D - victim.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE) * 0.5D);
            victim.push(push.x * strength, 0.7D, push.z * strength);
            victim.hurtMarked = true;
        }
    }
}
