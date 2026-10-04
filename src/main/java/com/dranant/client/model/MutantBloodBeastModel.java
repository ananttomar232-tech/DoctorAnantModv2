package com.dranant.client.model;

import com.dranant.DoctorAnantMod;
import com.dranant.entity.MutantBloodBeastEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Mutant Blood Beast model, converted cube-for-cube from the Blockbench export inside
 * minecraft_mutant_zombie_download_free.zip (bone pivots, rotations and 128x128 box-UV offsets are exact).
 * Extra parts: titanium handcuffs + chain links on both wrists, only visible while handcuffed.
 */
public class MutantBloodBeastModel extends HierarchicalModel<MutantBloodBeastEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(DoctorAnantMod.id("mutant_blood_beast"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart topBody;
    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart leftArm2;
    private final ModelPart rightArm;
    private final ModelPart rightArm2;
    private final ModelPart leftLeg;
    private final ModelPart leftLeg2;
    private final ModelPart rightLeg;
    private final ModelPart rightLeg2;
    private final ModelPart leftCuff;
    private final ModelPart rightCuff;

    public MutantBloodBeastModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.topBody = body.getChild("topbody");
        this.head = topBody.getChild("head");
        this.leftArm = topBody.getChild("leftarm");
        this.leftArm2 = leftArm.getChild("leftarm2");
        this.rightArm = topBody.getChild("rightarm");
        this.rightArm2 = rightArm.getChild("rightarm2");
        this.leftLeg = root.getChild("leftleg");
        this.leftLeg2 = leftLeg.getChild("leftleg2");
        this.rightLeg = root.getChild("rightleg");
        this.rightLeg2 = rightLeg.getChild("rightleg2");
        this.leftCuff = leftArm2.getChild("left_cuff");
        this.rightCuff = rightArm2.getChild("right_cuff");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 44)
                .addBox(-7.0F, -15.0F, -6.0F, 14.0F, 16.0F, 12.0F), PartPose.offsetAndRotation(0.0F, 10.0F, 5.5F, 0.2618F, 0.0F, 0.0F));
        PartDefinition topBody = body.addOrReplaceChild("topbody", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-12.0F, -12.0F, -8.0F, 24.0F, 12.0F, 16.0F), PartPose.offsetAndRotation(0.0F, -11.0F, 0.0F, 0.61087F, 0.0F, 0.0F));
        topBody.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
                        .texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F)),
                PartPose.offsetAndRotation(0.0F, -12.0F, -7.0F, -0.87266F, 0.0F, 0.0F));

        PartDefinition leftArm = topBody.addOrReplaceChild("leftarm", CubeListBuilder.create().texOffs(104, 0).mirror()
                .addBox(-3.0F, -3.0F, -3.0F, 6.0F, 15.0F, 6.0F), PartPose.offsetAndRotation(13.0F, -6.5F, 0.5F, -0.7854F, 0.0F, -0.43633F));
        PartDefinition leftArm2 = leftArm.addOrReplaceChild("leftarm2", CubeListBuilder.create().texOffs(104, 22).mirror()
                .addBox(-3.0F, 0.0F, -3.0F, 6.0F, 15.0F, 6.0F, new CubeDeformation(0.01F)), PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, -0.87266F, 0.0F, 0.0F));
        PartDefinition rightArm = topBody.addOrReplaceChild("rightarm", CubeListBuilder.create().texOffs(104, 0)
                .addBox(-3.0F, -3.0F, -3.0F, 6.0F, 15.0F, 6.0F), PartPose.offsetAndRotation(-13.0F, -6.5F, 0.5F, -0.7854F, 0.0F, 0.43633F));
        PartDefinition rightArm2 = rightArm.addOrReplaceChild("rightarm2", CubeListBuilder.create().texOffs(104, 22)
                .addBox(-3.0F, 0.0F, -3.0F, 6.0F, 15.0F, 6.0F, new CubeDeformation(0.01F)), PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, -0.87266F, 0.0F, 0.0F));

        // handcuff rings + dangling chain links (metal texture region at 0,80 and 0,96)
        CubeListBuilder cuff = CubeListBuilder.create()
                .texOffs(0, 80).addBox(-3.5F, 10.5F, -3.5F, 7.0F, 3.0F, 7.0F)
                .texOffs(0, 96).addBox(-1.0F, 13.5F, -1.0F, 2.0F, 3.0F, 2.0F)
                .texOffs(0, 96).addBox(-1.0F, 16.5F, -1.0F, 2.0F, 3.0F, 2.0F);
        leftArm2.addOrReplaceChild("left_cuff", cuff, PartPose.ZERO);
        rightArm2.addOrReplaceChild("right_cuff", cuff, PartPose.ZERO);

        PartDefinition rightLeg = root.addOrReplaceChild("rightleg", CubeListBuilder.create().texOffs(80, 0)
                .addBox(-3.0F, -1.0F, -3.0F, 6.0F, 10.0F, 6.0F), PartPose.offsetAndRotation(-5.5F, 9.0F, 6.0F, -0.69813F, 0.0F, 0.0F));
        rightLeg.addOrReplaceChild("rightleg2", CubeListBuilder.create().texOffs(80, 18)
                .addBox(-3.0F, -1.0F, -3.0F, 6.0F, 10.0F, 6.0F, new CubeDeformation(0.01F)), PartPose.offsetAndRotation(0.0F, 9.0F, 0.0F, 0.69813F, 0.0F, 0.0F));
        PartDefinition leftLeg = root.addOrReplaceChild("leftleg", CubeListBuilder.create().texOffs(80, 0).mirror()
                .addBox(-3.0F, -1.0F, -3.0F, 6.0F, 10.0F, 6.0F), PartPose.offsetAndRotation(5.5F, 9.0F, 6.0F, -0.69813F, 0.0F, 0.0F));
        leftLeg.addOrReplaceChild("leftleg2", CubeListBuilder.create().texOffs(80, 18).mirror()
                .addBox(-3.0F, -1.0F, -3.0F, 6.0F, 10.0F, 6.0F, new CubeDeformation(0.01F)), PartPose.offsetAndRotation(0.0F, 9.0F, 0.0F, 0.69813F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(MutantBloodBeastEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);

        boolean cuffed = entity.isHandcuffed();
        leftCuff.visible = cuffed;
        rightCuff.visible = cuffed;

        // breathing
        float breathe = Mth.sin(ageInTicks * 0.08F) * 0.03F;
        topBody.xRot += breathe;

        if (cuffed || entity.isBeingCured()) {
            // subdued: head drooped, wrists pulled together in front by the titanium handcuffs
            head.xRot += 0.45F;
            leftArm.yRot += 0.5F;
            leftArm.zRot = 0.15F;
            rightArm.yRot -= 0.5F;
            rightArm.zRot = -0.15F;
            leftArm2.xRot -= 0.3F;
            rightArm2.xRot -= 0.3F;
            float shake = Mth.sin(ageInTicks * 0.9F) * 0.04F;
            leftArm.xRot += shake;
            rightArm.xRot -= shake;
            return;
        }

        head.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.7F;
        head.xRot += headPitch * Mth.DEG_TO_RAD * 0.5F;

        float walk = limbSwing * 0.6662F;
        float amount = Math.min(limbSwingAmount, 1.0F);
        rightLeg.xRot += Mth.cos(walk) * 1.1F * amount;
        leftLeg.xRot += Mth.cos(walk + Mth.PI) * 1.1F * amount;
        rightArm.xRot += Mth.cos(walk + Mth.PI) * 0.7F * amount;
        leftArm.xRot += Mth.cos(walk) * 0.7F * amount;
        body.zRot += Mth.cos(walk) * 0.05F * amount;

        float shakeAmt = entity.isEnraged() ? Mth.sin(ageInTicks * 1.7F) * 0.04F : 0.0F;
        topBody.zRot += shakeAmt;
        head.zRot += shakeAmt * 1.5F;
        switch (entity.getActionState()) {
            case MutantBloodBeastEntity.ACTION_SLAM_WINDUP -> {
                leftArm.xRot -= 1.6F;
                rightArm.xRot -= 1.6F;
                topBody.xRot -= 0.25F;
            }
            case MutantBloodBeastEntity.ACTION_AIRBORNE -> {
                leftArm.xRot -= 2.4F;
                rightArm.xRot -= 2.4F;
                leftArm2.xRot += 0.6F;
                rightArm2.xRot += 0.6F;
                topBody.xRot -= 0.35F;
            }
            case MutantBloodBeastEntity.ACTION_ROAR -> {
                // chest out, head thrown back, arms spread wide
                topBody.xRot -= 0.55F;
                head.xRot -= 0.7F;
                leftArm.zRot -= 0.9F;
                rightArm.zRot += 0.9F;
                leftArm.xRot -= 0.6F;
                rightArm.xRot -= 0.6F;
                float tremble = Mth.sin(ageInTicks * 2.5F) * 0.06F;
                leftArm.yRot += tremble;
                rightArm.yRot -= tremble;
            }
            case MutantBloodBeastEntity.ACTION_CHARGE_WINDUP -> {
                // crouch and scrape like a bull
                body.xRot += 0.35F;
                topBody.xRot += 0.2F;
                head.xRot -= 0.3F;
                rightArm.xRot += Mth.sin(ageInTicks * 1.2F) * 0.6F;
                rightLeg.xRot -= 0.3F;
                leftLeg.xRot -= 0.3F;
            }
            case MutantBloodBeastEntity.ACTION_CHARGING -> {
                body.xRot += 0.5F;
                head.xRot -= 0.45F;
                leftArm.xRot += 0.9F;
                rightArm.xRot += 0.9F;
                float run = ageInTicks * 1.6F;
                rightLeg.xRot += Mth.cos(run) * 1.3F;
                leftLeg.xRot += Mth.cos(run + Mth.PI) * 1.3F;
            }
            case MutantBloodBeastEntity.ACTION_STUNNED -> {
                head.xRot += 0.5F;
                head.zRot += Mth.sin(ageInTicks * 0.3F) * 0.35F;
                topBody.zRot += Mth.sin(ageInTicks * 0.3F) * 0.12F;
                leftArm.xRot += 0.4F;
                rightArm.xRot += 0.4F;
            }
            case MutantBloodBeastEntity.ACTION_CAST -> {
                // both fists raised, about to punch the ground
                leftArm.xRot -= 2.0F;
                rightArm.xRot -= 2.0F;
                leftArm.zRot += 0.3F;
                rightArm.zRot -= 0.3F;
            }
            default -> {
            }
        }

        // two-handed ground pound when attacking
        if (attackTime > 0.0F) {
            float swing = Mth.sin(attackTime * Mth.PI);
            leftArm.xRot -= swing * 1.8F;
            rightArm.xRot -= swing * 1.8F;
            topBody.xRot += swing * 0.3F;
        }
    }
}
