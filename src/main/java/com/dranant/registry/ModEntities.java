package com.dranant.registry;

import com.dranant.DoctorAnantMod;
import com.dranant.entity.AssistantPharmacistEntity;
import com.dranant.entity.AssistantScientistEntity;
import com.dranant.entity.MutantBloodBeastEntity;
import com.dranant.entity.PatientVillagerEntity;
import com.dranant.entity.ShopkeeperEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, DoctorAnantMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<AssistantPharmacistEntity>> ASSISTANT_PHARMACIST = ENTITIES.register("assistant_pharmacist",
            () -> EntityType.Builder.of(AssistantPharmacistEntity::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10).build("assistant_pharmacist"));

    public static final DeferredHolder<EntityType<?>, EntityType<AssistantScientistEntity>> ASSISTANT_SCIENTIST = ENTITIES.register("assistant_scientist",
            () -> EntityType.Builder.of(AssistantScientistEntity::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10).build("assistant_scientist"));

    public static final DeferredHolder<EntityType<?>, EntityType<ShopkeeperEntity>> SHOPKEEPER = ENTITIES.register("shopkeeper",
            () -> EntityType.Builder.of(ShopkeeperEntity::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10).build("shopkeeper"));

    /** Clinic customer: looks like a villager, fully controlled by the clinic queue AI. */
    public static final DeferredHolder<EntityType<?>, EntityType<PatientVillagerEntity>> PATIENT_VILLAGER = ENTITIES.register("patient_villager",
            () -> EntityType.Builder.of(PatientVillagerEntity::new, MobCategory.CREATURE).sized(0.6F, 1.95F).clientTrackingRange(10).build("patient_villager"));

    /** 3.5 blocks tall boss. Model converted 1:1 from minecraft_mutant_zombie_download_free.zip. */
    public static final DeferredHolder<EntityType<?>, EntityType<MutantBloodBeastEntity>> MUTANT_BLOOD_BEAST = ENTITIES.register("mutant_blood_beast",
            () -> EntityType.Builder.of(MutantBloodBeastEntity::new, MobCategory.MONSTER).sized(1.9F, 3.5F).fireImmune().clientTrackingRange(12).build("mutant_blood_beast"));

    private ModEntities() {}
}
