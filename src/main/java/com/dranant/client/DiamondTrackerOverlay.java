package com.dranant.client;

import com.dranant.counter.ClientDiamondData;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Top-right Diamond Counter HUD: [diamond] Diamonds: 1,234,567 with a smoothly rolling number, the live growth rate,
 * a pulse on every transaction and animated "+X / -X [diamond]" popups that float up and fade.
 * (Minecraft's font has no diamond emoji, so the real diamond item icon is drawn instead.)
 */
public class DiamondTrackerOverlay implements LayeredDraw.Layer {
    private static final ItemStack DIAMOND = new ItemStack(Items.DIAMOND);

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || !ClientDiamondData.isVisible()) return;
        Font font = mc.font;
        boolean running = ClientDiamondData.isRunning();
        long value = ClientDiamondData.displayValue();
        String label = String.format("Diamonds: %,d", value);
        double rate = ClientDiamondData.getRate();
        String rateText = running ? String.format("+%,d / sec", Math.round(rate)) : "(paused)";

        float pulse = ClientDiamondData.pulse();
        int right = graphics.guiWidth() - 6;
        int width = Math.max(font.width(label), font.width(rateText)) + 30;
        int left = right - width;
        int top = 6;

        graphics.fill(left - 1, top - 1, right + 1, top + 29, running ? 0xFF2EE6E6 : 0xFF666666);
        graphics.fill(left, top, right, top + 28, 0xD0101820);
        if (running) {
            int barWidth = (int) (width * ((Util.getMillis() % 2000L) / 2000.0F));
            graphics.fill(left, top + 26, left + barWidth, top + 28, 0xFF2EE6E6);
        }

        graphics.pose().pushPose();
        float iconScale = 1.0F + pulse * 0.35F;
        graphics.pose().translate(left + 11, top + 12, 0);
        graphics.pose().scale(iconScale, iconScale, 1.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(Mth.sin(Util.getMillis() / 300.0F) * 8.0F));
        graphics.renderItem(DIAMOND, -8, -8);
        graphics.pose().popPose();

        int textColor = pulse > 0 ? lerpColor(0xFF55FFFF, 0xFFFFFFFF, pulse) : (running ? 0xFF55FFFF : 0xFFAAAAAA);
        graphics.drawString(font, label, left + 24, top + 4, textColor, true);
        graphics.drawString(font, rateText, left + 24, top + 15, running ? 0xFF55FF55 : 0xFFAAAAAA, true);

        long now = Util.getMillis();
        List<ClientDiamondData.Popup> popups = ClientDiamondData.getActivePopups();
        for (int i = 0; i < popups.size(); i++) {
            ClientDiamondData.Popup popup = popups.get(i);
            float t = Mth.clamp((now - popup.startMillis()) / (float) ClientDiamondData.POPUP_MILLIS, 0.0F, 1.0F);
            int alpha = Mth.clamp((int) ((1.0F - t) * 255.0F), 8, 255);
            float scale = 1.0F + 0.4F * (1.0F - t);
            boolean gain = popup.amount() > 0;
            String text = String.format(gain ? "+%,d" : "-%,d", Math.abs(popup.amount()));
            float px = left + 16 + popup.xJitter();
            float py = top + 46 - t * 26.0F + i * 3;
            graphics.pose().pushPose();
            graphics.pose().translate(px, py, 200.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.drawString(font, text, 0, 0, (alpha << 24) | (gain ? 0x55FF55 : 0xFF5555), true);
            graphics.pose().translate(font.width(text) + 1.0F, -3.0F, 0.0F);
            graphics.pose().scale(0.75F, 0.75F, 1.0F);
            graphics.renderItem(DIAMOND, 0, 0);
            graphics.pose().popPose();
        }
    }

    private static int lerpColor(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return 0xFF000000 | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }
}
