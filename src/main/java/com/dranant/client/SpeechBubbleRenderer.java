package com.dranant.client;

import com.dranant.DoctorAnantMod;
import com.dranant.dialogue.Speech;
import com.dranant.network.ClientSpeechData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.joml.Matrix4f;

import java.util.List;

/** Draws animated speech bubbles (pop-in, gentle bob, fade-out) above talking villagers, staff and the boss. */
@EventBusSubscriber(modid = DoctorAnantMod.MODID, value = Dist.CLIENT)
public final class SpeechBubbleRenderer {
    private static final int MAX_WIDTH = 140;

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        ClientSpeechData.Bubble bubble = ClientSpeechData.get(entity.getId());
        if (bubble == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.distanceToSqr(entity) > 32 * 32) return;

        long age = Util.getMillis() - bubble.startMillis();
        float t = Mth.clamp(age / 180.0F, 0.0F, 1.0F);
        float pop = easeOutBack(t);
        long remaining = bubble.durationMillis() - age;
        float alpha = remaining < 400 ? Mth.clamp(remaining / 400.0F, 0.0F, 1.0F) : 1.0F;
        float bob = Mth.sin(age / 300.0F) * 0.04F;

        Font font = mc.font;
        List<FormattedCharSequence> lines = font.split(Component.literal(bubble.text()), MAX_WIDTH);
        int textColor = switch (bubble.style()) {
            case Speech.STYLE_STAFF -> 0x7CFF7C;
            case Speech.STYLE_RUDE -> 0xFF6B6B;
            case Speech.STYLE_FRIENDLY -> 0xFFD866;
            case Speech.STYLE_BOSS -> 0xFF2020;
            case Speech.STYLE_HAPPY -> 0x66FFFF;
            default -> 0xFFFFFF;
        };
        int bgColor = bubble.style() == Speech.STYLE_BOSS ? 0x400000 : 0x000000;

        PoseStack pose = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        float height = Math.max(entity.getBbHeight(), 0.9F);
        boolean hasName = entity.shouldShowName();
        pose.pushPose();
        pose.translate(0.0D, height + (hasName ? 0.75D : 0.5D) + bob + lines.size() * 0.12D, 0.0D);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        float s = 0.025F * pop;
        pose.scale(s, -s, s);
        Matrix4f matrix = pose.last().pose();
        int textAlpha = Math.max(8, (int) (255 * alpha));
        int bgAlpha = (int) (0xB0 * alpha);
        int lineHeight = 10;
        int top = -lines.size() * lineHeight;
        for (int i = 0; i < lines.size(); i++) {
            FormattedCharSequence line = lines.get(i);
            float x = -font.width(line) / 2.0F;
            font.drawInBatch(line, x, top + i * lineHeight, (textAlpha << 24) | textColor, false, matrix, buffer,
                    Font.DisplayMode.NORMAL, (bgAlpha << 24) | bgColor, LightTexture.FULL_BRIGHT);
        }
        // little tail under the bubble
        font.drawInBatch(Component.literal("▼").getVisualOrderText(), -font.width("▼") / 2.0F, 0, (Math.max(8, bgAlpha) << 24),
                false, matrix, buffer, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        pose.popPose();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getGameTime() % 40 == 0) {
            ClientSpeechData.cleanup();
        }
    }

    private static float easeOutBack(float x) {
        float c1 = 1.70158F, c3 = c1 + 1.0F;
        return 1.0F + c3 * (float) Math.pow(x - 1.0F, 3) + c1 * (float) Math.pow(x - 1.0F, 2);
    }

    private SpeechBubbleRenderer() {}
}
