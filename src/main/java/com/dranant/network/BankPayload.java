package com.dranant.network;

import com.dranant.DoctorAnantMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client Diamond Bank snapshot.
 * @param balance   current spendable diamonds (the counter value)
 * @param delta     a real earning (+) or purchase (-) to show as a floating popup, 0 for a plain sync
 * @param ratePerSec passive growth rate used by the HUD to keep counting smoothly between packets
 */
public record BankPayload(long balance, long delta, double ratePerSec, boolean running, boolean visible) implements CustomPacketPayload {
    public static final Type<BankPayload> TYPE = new Type<>(DoctorAnantMod.id("diamond_bank"));

    public static final StreamCodec<ByteBuf, BankPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, BankPayload::balance,
            ByteBufCodecs.VAR_LONG, BankPayload::delta,
            ByteBufCodecs.DOUBLE, BankPayload::ratePerSec,
            ByteBufCodecs.BOOL, BankPayload::running,
            ByteBufCodecs.BOOL, BankPayload::visible,
            BankPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
