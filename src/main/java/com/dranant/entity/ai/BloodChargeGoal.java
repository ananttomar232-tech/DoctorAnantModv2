package com.dranant.entity.ai;

import com.dranant.entity.MutantBloodBeastEntity;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Telegraphed bull charge: the beast scrapes the ground for 0.8 s, then sprints in a straight line hitting everything in
 * its path. If it slams into a wall it is STUNNED for 2.5 s - the perfect moment to attack!
 */
public class BloodChargeGoal extends Goal {
    private static final int WINDUP = 16;
    private static final int MAX_CHARGE = 30;
    private final MutantBloodBeastEntity beast;
    private int phase;
    private int timer;
    private Vec3 direction = Vec3.ZERO;
    private final Set<UUID> hit = new HashSet<>();

    public BloodChargeGoal(MutantBloodBeastEntity beast) {
        this.beast = beast;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = beast.getTarget();
        if (target == null || !target.isAlive() || !beast.onGround() || !beast.canUseSpecial("charge")) return false;
        double d = beast.distanceTo(target);
        return d >= 6.0D && d <= 22.0D && beast.hasLineOfSight(target);
    }

    @Override
    public boolean canContinueToUse() {
        return phase < 2 && beast.isAlive() && !beast.isHandcuffed() && !beast.isBeingCured() && !beast.isStunned();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        phase = 0;
        timer = 0;
        hit.clear();
        beast.getNavigation().stop();
        beast.setActionState(MutantBloodBeastEntity.ACTION_CHARGE_WINDUP);
        beast.playSound(SoundEvents.RAVAGER_STEP, 2.0F, 0.6F);
    }

    @Override
    public void stop() {
        if (beast.getActionState() == MutantBloodBeastEntity.ACTION_CHARGE_WINDUP || beast.getActionState() == MutantBloodBeastEntity.ACTION_CHARGING) {
            beast.setActionState(MutantBloodBeastEntity.ACTION_NONE);
        }
        beast.markSpecialUsed("charge", beast.isEnraged() ? 140 : 220);
    }

    @Override
    public void tick() {
        LivingEntity target = beast.getTarget();
        timer++;
        if (!(beast.level() instanceof ServerLevel level)) return;
        if (phase == 0) {
            if (target != null) beast.getLookControl().setLookAt(target, 30.0F, 30.0F);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(beast.blockPosition().below())),
                    beast.getX(), beast.getY() + 0.1, beast.getZ(), 6, 0.6, 0.05, 0.6, 0.1);
            if (timer >= WINDUP) {
                Vec3 to = target != null ? target.position().subtract(beast.position()) : beast.getLookAngle();
                direction = new Vec3(to.x, 0.0D, to.z).normalize();
                float yaw = (float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0F;
                beast.setYRot(yaw);
                beast.yBodyRot = yaw;
                beast.yHeadRot = yaw;
                beast.setActionState(MutantBloodBeastEntity.ACTION_CHARGING);
                level.playSound(null, beast.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.0F, 1.1F);
                phase = 1;
                timer = 0;
            }
            return;
        }
        double speed = beast.isEnraged() ? 1.05D : 0.85D;
        beast.setDeltaMovement(direction.x * speed, beast.getDeltaMovement().y, direction.z * speed);
        level.sendParticles(ParticleTypes.CLOUD, beast.getX(), beast.getY() + 0.2, beast.getZ(), 2, 0.4, 0.1, 0.4, 0.01);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, beast.getBoundingBox().inflate(0.8D), e -> e != beast && e.isAlive())) {
            if (hit.add(victim.getUUID()) && victim.hurt(beast.damageSources().mobAttack(beast), 10.0F)) {
                victim.push(direction.x * 2.0D, 0.6D, direction.z * 2.0D);
                victim.hurtMarked = true;
                level.playSound(null, victim.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.HOSTILE, 1.2F, 0.7F);
            }
        }
        if (beast.horizontalCollision && timer > 3) {
            level.playSound(null, beast.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.5F, 0.5F);
            level.sendParticles(ParticleTypes.EXPLOSION, beast.getX() + direction.x, beast.getY() + 1.5, beast.getZ() + direction.z, 2, 0.3, 0.3, 0.3, 0);
            beast.stun(50);
            phase = 2;
        } else if (timer >= MAX_CHARGE) {
            phase = 2;
        }
    }
}
