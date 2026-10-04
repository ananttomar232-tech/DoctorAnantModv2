package com.dranant.network;

import com.dranant.DoctorAnantMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: show an animated speech bubble above an entity. */
public record SpeechPayload(int entityId, String text, int durationTicks, int style) implements CustomPacketPayload {
    public static final Type<SpeechPayload> TYPE = new Type<>(DoctorAnantMod.id("speech"));

    public static final StreamCodec<ByteBuf, SpeechPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SpeechPayload::entityId,
            ByteBufCodecs.STRING_UTF8, SpeechPayload::text,
            ByteBufCodecs.VAR_INT, SpeechPayload::durationTicks,
            ByteBufCodecs.VAR_INT, SpeechPayload::style,
            SpeechPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
