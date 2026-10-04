package com.dranant.counter;

import com.dranant.registry.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.joml.Vector3f;

/** Red + yellow radial particle burst and crisp collection sound used whenever diamonds are earned. */
public final class DiamondFx {
    public static final DustParticleOptions BRIGHT_RED = new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 0.9F);
    public static final DustParticleOptions VIVID_YELLOW = new DustParticleOptions(new Vector3f(1.0F, 0.9F, 0.0F), 0.9F);

    public static void burst(ServerLevel level, double x, double y, double z) {
        int rays = 28;
        for (int i = 0; i < rays; i++) {
            double angle = (Math.PI * 2.0 * i) / rays;
            double dx = Math.cos(angle) * 1.6;
            double dz = Math.sin(angle) * 1.6;
            double dy = 0.4 + level.random.nextDouble() * 0.8;
            // count = 0 makes (dx, dy, dz) the particle velocity -> particles radiate outward from the counter
            level.sendParticles(i % 2 == 0 ? BRIGHT_RED : VIVID_YELLOW, x, y, z, 0, dx, dy, dz, 1.0);
        }
        level.sendParticles(BRIGHT_RED, x, y + 0.2, z, 12, 0.35, 0.25, 0.35, 0.02);
        level.sendParticles(VIVID_YELLOW, x, y + 0.2, z, 12, 0.35, 0.25, 0.35, 0.02);
        level.playSound(null, x, y, z, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.9F, 1.1F + level.random.nextFloat() * 0.3F);
        level.playSound(null, x, y, z, ModSounds.CASH_REGISTER_CHIME.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
        level.playSound(null, x, y, z, ModSounds.DIAMOND_POP.get(), SoundSource.BLOCKS, 0.7F, 1.3F);
    }

    private DiamondFx() {}
}
