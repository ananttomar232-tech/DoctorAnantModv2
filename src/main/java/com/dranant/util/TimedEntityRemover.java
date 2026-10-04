package com.dranant.util;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Removes temporary entities (e.g. the X-Ray Tonic glowing ore outlines) after a delay. */
public final class TimedEntityRemover {
    public static final String XRAY_TAG = "dranant_xray";

    private record Entry(UUID id, long expireTick) {}

    private static final Map<ResourceKey<Level>, List<Entry>> PENDING = new HashMap<>();

    public static synchronized void schedule(ServerLevel level, Entity entity, int ticks) {
        PENDING.computeIfAbsent(level.dimension(), k -> new ArrayList<>()).add(new Entry(entity.getUUID(), level.getGameTime() + ticks));
    }

    public static synchronized boolean isTracked(ServerLevel level, UUID id) {
        List<Entry> list = PENDING.get(level.dimension());
        if (list == null) return false;
        for (Entry e : list) if (e.id().equals(id)) return true;
        return false;
    }

    public static synchronized void tick(ServerLevel level) {
        List<Entry> list = PENDING.get(level.dimension());
        if (list == null || list.isEmpty()) return;
        long now = level.getGameTime();
        Iterator<Entry> it = list.iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            if (now >= e.expireTick()) {
                Entity entity = level.getEntity(e.id());
                if (entity != null) entity.discard();
                it.remove();
            }
        }
    }

    public static synchronized void clear() {
        PENDING.clear();
    }

    private TimedEntityRemover() {}
}
