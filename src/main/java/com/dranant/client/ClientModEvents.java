package com.dranant.client;

import com.dranant.DoctorAnantMod;
import com.dranant.client.model.MutantBloodBeastModel;
import com.dranant.client.renderer.AssistantRenderer;
import com.dranant.client.renderer.ItemDisplayPedestalRenderer;
import com.dranant.client.renderer.MutantBloodBeastRenderer;
import com.dranant.client.renderer.PatientVillagerRenderer;
import com.dranant.registry.ModBlockEntities;
import com.dranant.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** Client-only registration: entity + block-entity renderers, model layers and the Diamond Counter HUD. */
@EventBusSubscriber(modid = DoctorAnantMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MUTANT_BLOOD_BEAST.get(), MutantBloodBeastRenderer::new);
        event.registerEntityRenderer(ModEntities.ASSISTANT_PHARMACIST.get(),
                ctx -> new AssistantRenderer<>(ctx, DoctorAnantMod.id("textures/entity/assistant_pharmacist.png")));
        event.registerEntityRenderer(ModEntities.ASSISTANT_SCIENTIST.get(),
                ctx -> new AssistantRenderer<>(ctx, DoctorAnantMod.id("textures/entity/assistant_scientist.png")));
        event.registerEntityRenderer(ModEntities.SHOPKEEPER.get(),
                ctx -> new AssistantRenderer<>(ctx, DoctorAnantMod.id("textures/entity/shopkeeper.png")));
        event.registerEntityRenderer(ModEntities.PATIENT_VILLAGER.get(), PatientVillagerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ITEM_DISPLAY_PEDESTAL.get(), ItemDisplayPedestalRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MutantBloodBeastModel.LAYER, MutantBloodBeastModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(DoctorAnantMod.id("diamond_tracker"), new DiamondTrackerOverlay());
    }

    private ClientModEvents() {}
}
