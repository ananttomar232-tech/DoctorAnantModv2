package com.dranant.shop;

import com.dranant.clinic.ClinicBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Clinic-upgrade preview: a glowing green outline of the clinic's footprint, 7 blocks in front of the player,
 * that follows where the player looks for 6 seconds. Right-clicking the same pedestal again confirms the purchase.
 */
public final class PreviewManager {
    private static final int DURATION = 120;
    private static final DustParticleOptions GREEN = new DustParticleOptions(new Vector3f(0.2F, 1.0F, 0.3F), 1.4F);
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.85F, 0.1F), 1.6F);

    private record Pending(BlockPos pedestal, int tier, int ticksLeft) {}

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    public static synchronized void start(Player player, BlockPos pedestal, int tier) {
        PENDING.put(player.getUUID(), new Pending(pedestal.immutable(), tier, DURATION));
    }

    public static synchronized boolean isConfirming(Player player, BlockPos pedestal) {
        Pending p = PENDING.get(player.getUUID());
        return p != null && p.pedestal().equals(pedestal);
    }

    public static synchronized void clear(Player player) {
        PENDING.remove(player.getUUID());
    }

    public static synchronized void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Pending>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Pending> e = it.next();
            Pending p = e.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
            if (player == null || p.ticksLeft() <= 0) {
                it.remove();
                continue;
            }
            e.setValue(new Pending(p.pedestal(), p.tier(), p.ticksLeft() - 1));
            if (p.ticksLeft() % 5 == 0) drawOutline(player.serverLevel(), player, ClinicBuilder.plan(player.serverLevel(), player, p.tier()).footprint());
        }
    }

    private static void drawOutline(ServerLevel level, ServerPlayer player, BoundingBox box) {
        double y = box.minY() + 1.1;
        for (int x = box.minX(); x <= box.maxX() + 1; x++) {
            level.sendParticles(player, GREEN, true, x, y, box.minZ(), 1, 0, 0, 0, 0);
            level.sendParticles(player, GREEN, true, x, y, box.maxZ() + 1, 1, 0, 0, 0, 0);
        }
        for (int z = box.minZ(); z <= box.maxZ() + 1; z++) {
            level.sendParticles(player, GREEN, true, box.minX(), y, z, 1, 0, 0, 0, 0);
            level.sendParticles(player, GREEN, true, box.maxX() + 1, y, z, 1, 0, 0, 0, 0);
        }
        int[][] corners = {{box.minX(), box.minZ()}, {box.maxX() + 1, box.minZ()}, {box.minX(), box.maxZ() + 1}, {box.maxX() + 1, box.maxZ() + 1}};
        for (int[] c : corners) {
            for (int dy = 0; dy <= Math.min(12, box.getYSpan()); dy++) {
                level.sendParticles(player, GOLD, true, c[0], y + dy, c[1], 1, 0, 0, 0, 0);
            }
        }
    }

    private PreviewManager() {}
}
