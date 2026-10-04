package com.dranant.client.renderer;

import com.dranant.DoctorAnantMod;
import com.dranant.client.model.MutantBloodBeastModel;
import com.dranant.entity.MutantBloodBeastEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Renders the boss 3.5 blocks tall with glowing red eyes; smoothly shrinks during the cure. */
public class MutantBloodBeastRenderer extends MobRenderer<MutantBloodBeastEntity, MutantBloodBeastModel> {
    private static final ResourceLocation TEXTURE = DoctorAnantMod.id("textures/entity/mutant_blood_beast.png");
    private static final RenderType EYES = RenderType.eyes(DoctorAnantMod.id("textures/entity/mutant_blood_beast_eyes.png"));
    /** Raw model is ~2.46 blocks tall -> scale to 3.5 blocks. */
    private static final float MODEL_SCALE = 1.42F;

    public MutantBloodBeastRenderer(EntityRendererProvider.Context context) {
        super(context, new MutantBloodBeastModel(context.bakeLayer(MutantBloodBeastModel.LAYER)), 1.4F);
        addLayer(new EyesLayer<MutantBloodBeastEntity, MutantBloodBeastModel>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    @Override
    protected void scale(MutantBloodBeastEntity entity, PoseStack poseStack, float partialTick) {
        float s = MODEL_SCALE * entity.getCureScale(partialTick);
        poseStack.scale(s, s, s);
        this.shadowRadius = 1.4F * entity.getCureScale(partialTick);
    }

    @Override
    public ResourceLocation getTextureLocation(MutantBloodBeastEntity entity) {
        return TEXTURE;
    }
}
