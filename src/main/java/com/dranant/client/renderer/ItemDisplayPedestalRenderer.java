package com.dranant.client.renderer;

import com.dranant.block.entity.ItemDisplayPedestalBlockEntity;
import com.dranant.item.ClinicSummonerItem;
import com.dranant.clinic.ClinicTiers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Matrix4f;

/**
 * 360-degree floating display, 0.6 blocks above the pedestal, rotating at (gameTime + partialTick) * 3 degrees with a
 * sine bob. The pedestal you look at lifts up, spins faster and shows its full name + price card; the others only show a
 * tiny price tag, so labels never overlap.
 */
public class ItemDisplayPedestalRenderer implements BlockEntityRenderer<ItemDisplayPedestalBlockEntity> {
    private static final double PEDESTAL_TOP = 1.0D;
    private static final double HOVER = 0.6D;

    private final ItemRenderer itemRenderer;
    private final Font font;

    public ItemDisplayPedestalRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
        this.font = context.getFont();
    }

    public static String compact(long n) {
        if (n >= 1_000_000L) return trim(n / 1_000_000.0) + "M";
        if (n >= 10_000L) return trim(n / 1_000.0) + "K";
        return String.format("%,d", n);
    }

    private static String trim(double v) {
        String s = String.format("%.1f", v);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }

    @Override
    public void render(ItemDisplayPedestalBlockEntity pedestal, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        ItemStack stack = pedestal.getDisplayedItem();
        if (stack.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        Level level = pedestal.getLevel();
        HitResult hit = mc.hitResult;
        boolean targeted = hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK && bhr.getBlockPos().equals(pedestal.getBlockPos());

        pedestal.clientHover += ((targeted ? 1.0F : 0.0F) - pedestal.clientHover) * 0.12F;
        float hover = pedestal.clientHover;
        pedestal.clientSpin = (pedestal.clientSpin + hover * 4.0F) % 360.0F;

        float time = (level != null ? level.getGameTime() : 0L) + partialTick;
        float rotation = (time * 3.0F + pedestal.clientSpin) % 360.0F;
        float bob = Mth.sin(time * 0.1F) * 0.08F;
        float lift = hover * 0.18F;
        int light = level != null ? LevelRenderer.getLightColor(level, pedestal.getBlockPos().above()) : LightTexture.FULL_BRIGHT;
        boolean clinic = stack.getItem() instanceof ClinicSummonerItem;

        poseStack.pushPose();
        poseStack.translate(0.5D, PEDESTAL_TOP + HOVER + bob + lift, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        float scale = (clinic ? 0.6F : 0.7F) + hover * 0.15F;
        poseStack.scale(scale, scale, scale);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, poseStack, buffer, level,
                (int) pedestal.getBlockPos().asLong());
        poseStack.popPose();

        if (mc.player == null) return;
        double dist = Math.sqrt(mc.player.distanceToSqr(pedestal.getBlockPos().getCenter()));
        if (targeted && dist < 12.0D) {
            Component name = clinic
                    ? Component.literal("Clinic Lv." + ((ClinicSummonerItem) stack.getItem()).getTier() + " - "
                    + ClinicTiers.name(((ClinicSummonerItem) stack.getItem()).getTier())).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
                    : stack.getHoverName().copy().withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
            Component price = Component.literal("◆ ").withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(String.format("%,d Diamonds", pedestal.getDiamondPrice())).withStyle(ChatFormatting.GOLD));
            Component hint = Component.literal(clinic ? "Right-click: preview & build" : "Right-click to buy").withStyle(ChatFormatting.GRAY);
            float alpha = Mth.clamp(hover * 1.4F, 0.0F, 1.0F);
            double y = PEDESTAL_TOP + HOVER + 0.62D + lift;
            drawLine(poseStack, buffer, mc, name, y + 0.27D, 0.016F, alpha);
            drawLine(poseStack, buffer, mc, price, y + 0.08D, 0.016F, alpha);
            drawLine(poseStack, buffer, mc, hint, y - 0.08D, 0.011F, alpha);
        } else if (dist < 7.0D) {
            Component tag = Component.literal("◆ ").withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(compact(pedestal.getDiamondPrice())).withStyle(ChatFormatting.GOLD));
            drawLine(poseStack, buffer, mc, tag, PEDESTAL_TOP + HOVER + 0.48D + bob, 0.012F, 1.0F);
        }
    }

    private void drawLine(PoseStack poseStack, MultiBufferSource buffer, Minecraft mc, Component text, double y, float scale, float alpha) {
        poseStack.pushPose();
        poseStack.translate(0.5D, y, 0.5D);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(scale, -scale, scale);
        Matrix4f matrix = poseStack.last().pose();
        float x = -font.width(text) / 2.0F;
        int bgAlpha = (int) (mc.options.getBackgroundOpacity(0.45F) * 255.0F * alpha);
        int textAlpha = Math.max(8, (int) (255 * alpha));
        font.drawInBatch(text, x, 0.0F, (textAlpha << 24) | 0xFFFFFF, false, matrix, buffer, Font.DisplayMode.NORMAL, bgAlpha << 24, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    public AABB getRenderBoundingBox(ItemDisplayPedestalBlockEntity pedestal) {
        return new AABB(pedestal.getBlockPos()).expandTowards(0.0D, 3.0D, 0.0D).inflate(1.0D, 0.0D, 1.0D);
    }
}
