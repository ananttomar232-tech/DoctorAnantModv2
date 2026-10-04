package com.dranant;

import com.dranant.config.DrAnantConfig;
import com.dranant.counter.ClientDiamondData;
import com.dranant.entity.AssistantEntity;
import com.dranant.entity.MutantBloodBeastEntity;
import com.dranant.entity.PatientVillagerEntity;
import com.dranant.network.BankPayload;
import com.dranant.network.ClientSpeechData;
import com.dranant.network.SpeechPayload;
import com.dranant.registry.ModArmorMaterials;
import com.dranant.registry.ModBlockEntities;
import com.dranant.registry.ModBlocks;
import com.dranant.registry.ModCreativeTabs;
import com.dranant.registry.ModEffects;
import com.dranant.registry.ModEntities;
import com.dranant.registry.ModItems;
import com.dranant.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

/**
 * Doctor Anant's Mega Clinic & The Expired Mutant Outbreak (v2).
 * Main mod entry point: wires every DeferredRegister, attributes, config and networking onto the mod event bus.
 */
@Mod(DoctorAnantMod.MODID)
public class DoctorAnantMod {
    public static final String MODID = "dranant";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DoctorAnantMod(IEventBus modBus, ModContainer container) {
        ModSounds.SOUNDS.register(modBus);
        ModEffects.MOB_EFFECTS.register(modBus);
        ModArmorMaterials.ARMOR_MATERIALS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);

        modBus.addListener(this::onEntityAttributes);
        modBus.addListener(this::onRegisterPayloads);

        container.registerConfig(ModConfig.Type.COMMON, DrAnantConfig.SPEC);
        LOGGER.info("Doctor Anant's Mega Clinic is open for business!");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private void onEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.MUTANT_BLOOD_BEAST.get(), MutantBloodBeastEntity.createAttributes().build());
        event.put(ModEntities.ASSISTANT_PHARMACIST.get(), AssistantEntity.createAttributes().build());
        event.put(ModEntities.ASSISTANT_SCIENTIST.get(), AssistantEntity.createAttributes().build());
        event.put(ModEntities.SHOPKEEPER.get(), AssistantEntity.createAttributes().build());
        event.put(ModEntities.PATIENT_VILLAGER.get(), PatientVillagerEntity.createAttributes().build());
    }

    private void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
        registrar.playToClient(BankPayload.TYPE, BankPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientDiamondData.update(payload)));
        registrar.playToClient(SpeechPayload.TYPE, SpeechPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientSpeechData.add(payload)));
    }
}
