package com.dranant.entity.ai;

import com.dranant.entity.MutantBloodBeastEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Blood Geysers: the beast punches the ground, red warning circles appear under and around its target, and 1.5 s later
 * blood erupts from them (8 damage + launch). Dodge out of the circles! The beast can keep walking while they charge.
 */
public class BloodGeyserGoal extends Goal {
    private static final int CAST = 10;
    private static final int WARNING = 30;
    private static final double GEYSER_RADIUS = 2.2D;
    private static final DustParticleOptions WARN = new DustParticleOptions(new Vector3f(1.0F, 0.15F, 0.1F), 1.2F);
    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.6F, 0.0F, 0.0F), 2.5F);
    private final MutantBloodBeastEntity beast;
    private final List<Vec3> spots = new ArrayList<>();
    private int timer;

    public BloodGeyserGoal(MutantBloodBeastEntity beast) {
        this.beast = beast;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = beast.getTarget();
        return target != null && target.isAlive() && beast.distanceTo(target) < 24.0D && beast.canUseSpecial("geyser");
    }

    @Override
    public boolean canContinueToUse() {
        return timer < CAST + WARNING + 6 && beast.isAlive() && !beast.isBeingCured();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        timer = 0;
        spots.clear();
        beast.setActionState(MutantBloodBeastEntity.ACTION_CAST);
        LivingEntity target = beast.getTarget();
        if (target == null) return;
        int count = beast.isEnraged() ? 5 : 3;
        spots.add(target.position());
        for (int i = 1; i < count; i++) {
            double a = beast.getRandom().nextDouble() * Math.PI * 2;
            double r = 2.5D + beast.getRandom().nextDouble() * 3.0D;
            double x = target.getX() + Math.cos(a) * r, z = target.getZ() + Math.sin(a) * r;
            int y = beast.level().getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
            spots.add(new Vec3(x, Math.abs(y - target.getY()) < 4 ? y : target.getY(), z));
        }
    }

    @Override
    public void stop() {
        if (beast.getActionState() == MutantBloodBeastEntity.ACTION_CAST) beast.setActionState(MutantBloodBeastEntity.ACTION_NONE);
        beast.markSpecialUsed("geyser", beast.isEnraged() ? 160 : 240);
    }

    @Override
    public void tick() {
        timer++;
        if (!(beast.level() instanceof ServerLevel level)) return;
        if (timer == CAST) {
            level.playSound(null, beast.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.HOSTILE, 2.0F, 0.5F);
            beast.setActionState(MutantBloodBeastEntity.ACTION_NONE);
        }
        if (timer > CAST && timer < CAST + WARNING && timer % 3 == 0) {
            for (Vec3 s : spots) {
                for (int i = 0; i < 20; i++) {
                    double a = Math.PI * 2 * i / 20;
                    level.sendParticles(WARN, s.x + Math.cos(a) * GEYSER_RADIUS, s.y + 0.15, s.z + Math.sin(a) * GEYSER_RADIUS, 1, 0, 0, 0, 0);
                }
            }
        }
        if (timer == CAST + WARNING) {
            BlockParticleOption redstone = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState());
            for (Vec3 s : spots) {
                level.playSound(null, BlockPos.containing(s), com.dranant.registry.ModSounds.GROUND_SLAM.get(), SoundSource.HOSTILE, 1.2F, 1.4F);
                for (int h = 0; h < 6; h++) {
                    level.sendParticles(BLOOD, s.x, s.y + h * 0.6, s.z, 12, 0.5, 0.3, 0.5, 0.05);
                }
                level.sendParticles(redstone, s.x, s.y + 0.5, s.z, 40, 0.8, 0.8, 0.8, 0.3);
                AABB area = new AABB(s.x - GEYSER_RADIUS, s.y - 1, s.z - GEYSER_RADIUS, s.x + GEYSER_RADIUS, s.y + 3, s.z + GEYSER_RADIUS);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area, x -> x != beast && x.isAlive())) {
                    if (e.hurt(beast.damageSources().mobAttack(beast), 8.0F)) {
                        e.push(0, 0.9, 0);
                        e.hurtMarked = true;
                    }
                }
            }
        }
    }
}
