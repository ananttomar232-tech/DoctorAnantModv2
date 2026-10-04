package com.dranant.entity.ai;

import com.dranant.dialogue.Lines;
import com.dranant.dialogue.Speech;
import com.dranant.entity.MutantBloodBeastEntity;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;

/** Blood Roar: an expanding ring of blood that pushes everyone back and inflicts Slowness II + Weakness. */
public class BloodRoarGoal extends Goal {
    private static final int DURATION = 34;
    private static final double RADIUS = 10.0D;
    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.75F, 0.0F, 0.0F), 2.0F);
    private final MutantBloodBeastEntity beast;
    private int timer;

    public BloodRoarGoal(MutantBloodBeastEntity beast) {
        this.beast = beast;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = beast.getTarget();
        return target != null && target.isAlive() && beast.distanceTo(target) < 14.0D && beast.canUseSpecial("roar");
    }

    @Override
    public boolean canContinueToUse() {
        return timer < DURATION && beast.isAlive() && !beast.isHandcuffed() && !beast.isBeingCured() && !beast.isStunned();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        timer = 0;
        beast.getNavigation().stop();
        beast.setActionState(MutantBloodBeastEntity.ACTION_ROAR);
        Speech.say(beast, Lines.pick(Lines.BOSS, beast.getRandom()), Speech.STYLE_BOSS, 50);
    }

    @Override
    public void stop() {
        if (beast.getActionState() == MutantBloodBeastEntity.ACTION_ROAR) beast.setActionState(MutantBloodBeastEntity.ACTION_NONE);
        beast.markSpecialUsed("roar", beast.isEnraged() ? 280 : 440);
    }

    @Override
    public void tick() {
        timer++;
        if (!(beast.level() instanceof ServerLevel level)) return;
        if (timer == 10) {
            level.playSound(null, beast.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 3.0F, 0.5F);
            level.playSound(null, beast.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2.0F, 0.8F);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, beast.getBoundingBox().inflate(RADIUS), x -> x != beast && x.isAlive())) {
                if (e.distanceTo(beast) > RADIUS) continue;
                Vec3 push = e.position().subtract(beast.position()).multiply(1, 0, 1).normalize();
                e.push(push.x * 1.4D, 0.45D, push.z * 1.4D);
                e.hurtMarked = true;
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
                e.hurt(beast.damageSources().mobAttack(beast), 4.0F);
            }
        }
        if (timer >= 10 && timer <= 24) {
            double r = (timer - 9) * (RADIUS / 15.0D);
            int points = (int) (r * 12);
            for (int i = 0; i < points; i++) {
                double a = Math.PI * 2 * i / points;
                level.sendParticles(BLOOD, beast.getX() + Math.cos(a) * r, beast.getY() + 0.3, beast.getZ() + Math.sin(a) * r, 1, 0, 0.05, 0, 0);
            }
        }
    }
}
