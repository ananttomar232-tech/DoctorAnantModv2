package com.dranant.network;

import net.minecraft.Util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Client-side store of active speech bubbles (plain Java: safe to reference from common packet handlers). */
public final class ClientSpeechData {
    public record Bubble(String text, long startMillis, long durationMillis, int style) {}

    private static final Map<Integer, Bubble> BUBBLES = new HashMap<>();

    public static synchronized void add(SpeechPayload payload) {
        BUBBLES.put(payload.entityId(), new Bubble(payload.text(), Util.getMillis(), payload.durationTicks() * 50L, payload.style()));
    }

    public static synchronized Bubble get(int entityId) {
        Bubble b = BUBBLES.get(entityId);
        if (b != null && Util.getMillis() - b.startMillis() > b.durationMillis()) {
            BUBBLES.remove(entityId);
            return null;
        }
        return b;
    }

    public static synchronized void cleanup() {
        long now = Util.getMillis();
        Iterator<Map.Entry<Integer, Bubble>> it = BUBBLES.entrySet().iterator();
        while (it.hasNext()) {
            Bubble b = it.next().getValue();
            if (now - b.startMillis() > b.durationMillis()) it.remove();
        }
    }

    private ClientSpeechData() {}
}
