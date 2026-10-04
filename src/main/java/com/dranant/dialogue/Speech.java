package com.dranant.dialogue;

import com.dranant.network.SpeechPayload;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server helper: makes an entity "say" something with a floating speech bubble. */
public final class Speech {
    public static final int STYLE_PATIENT = 0;
    public static final int STYLE_STAFF = 1;
    public static final int STYLE_RUDE = 2;
    public static final int STYLE_FRIENDLY = 3;
    public static final int STYLE_BOSS = 4;
    public static final int STYLE_HAPPY = 5;

    public static void say(Entity entity, String text, int style) {
        say(entity, text, style, Math.max(50, Math.min(140, 30 + text.length() * 2)));
    }

    public static void say(Entity entity, String text, int style, int durationTicks) {
        if (entity.level().isClientSide) return;
        PacketDistributor.sendToPlayersTrackingEntity(entity, new SpeechPayload(entity.getId(), text, durationTicks, style));
    }

    private Speech() {}
}
