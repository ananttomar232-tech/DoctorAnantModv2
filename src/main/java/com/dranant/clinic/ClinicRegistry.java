package com.dranant.clinic;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Tracks every loaded clinic (Diamond Cash Register) so villagers can pick the CLOSEST clinic. */
public final class ClinicRegistry {
    private static final Map<ResourceKey<Level>, Set<BlockPos>> CLINICS = new HashMap<>();

    public static synchronized void add(Level level, BlockPos pos) {
        CLINICS.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(pos.immutable());
    }

    public static synchronized void remove(Level level, BlockPos pos) {
        Set<BlockPos> set = CLINICS.get(level.dimension());
        if (set != null) set.remove(pos);
    }

    @Nullable
    public static synchronized BlockPos nearest(Level level, BlockPos from, double maxDistance) {
        Set<BlockPos> set = CLINICS.get(level.dimension());
        if (set == null) return null;
        BlockPos best = null;
        double bestD = maxDistance * maxDistance;
        for (BlockPos p : set) {
            double d = p.distSqr(from);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    public static synchronized void clear() {
        CLINICS.clear();
    }

    private ClinicRegistry() {}
}
