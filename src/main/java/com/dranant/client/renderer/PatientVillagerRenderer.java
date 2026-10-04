package com.dranant.client.renderer;

import com.dranant.entity.PatientVillagerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/** Renders patients with the vanilla villager model + biome type and profession overlays (just like real villagers). */
public class PatientVillagerRenderer extends MobRenderer<PatientVillagerEntity, VillagerModel<PatientVillagerEntity>> {
    private static final ResourceLocation BASE = ResourceLocation.withDefaultNamespace("textures/entity/villager/villager.png");

    public PatientVillagerRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
        addLayer(new LookLayer(this));
        addLayer(new CrossedArmsItemLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(PatientVillagerEntity entity) {
        return BASE;
    }

    @Override
    protected void scale(PatientVillagerEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }

    private static final class LookLayer extends RenderLayer<PatientVillagerEntity, VillagerModel<PatientVillagerEntity>> {
        LookLayer(RenderLayerParent<PatientVillagerEntity, VillagerModel<PatientVillagerEntity>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, PatientVillagerEntity entity,
                           float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (entity.isInvisible()) return;
            ResourceLocation type = ResourceLocation.withDefaultNamespace("textures/entity/villager/type/" + entity.getVillagerTypeName() + ".png");
            renderColoredCutoutModel(getParentModel(), type, poseStack, buffer, packedLight, entity, -1);
            String profession = entity.getProfessionName();
            if (!"none".equals(profession)) {
                ResourceLocation prof = ResourceLocation.withDefaultNamespace("textures/entity/villager/profession/" + profession + ".png");
                renderColoredCutoutModel(getParentModel(), prof, poseStack, buffer, packedLight, entity, -1);
            }
        }
    }
}
