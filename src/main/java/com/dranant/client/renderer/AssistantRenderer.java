package com.dranant.client.renderer;

import com.dranant.entity.AssistantEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Native humanoid (player) model + doctor/scientist texture overlay - no external 3D files needed. */
public class AssistantRenderer<T extends AssistantEntity> extends HumanoidMobRenderer<T, PlayerModel<T>> {
    private final ResourceLocation texture;

    public AssistantRenderer(EntityRendererProvider.Context context, ResourceLocation texture) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.texture = texture;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
