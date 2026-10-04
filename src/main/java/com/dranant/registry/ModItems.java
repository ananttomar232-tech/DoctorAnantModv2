package com.dranant.registry;

import com.dranant.DoctorAnantMod;
import com.dranant.block.ClinicSummonerBlock;
import com.dranant.item.ClinicSummonerItem;
import com.dranant.item.DrAnantCoatItem;
import com.dranant.item.ExpiredExperimentalSerumItem;
import com.dranant.item.MedicineItem;
import com.dranant.item.TitaniumHandcuffsItem;
import com.dranant.item.UniversalCureSyringeItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DoctorAnantMod.MODID);

    // ---------------- Medicines, tiers 1-10 ----------------
    public static final DeferredItem<MedicineItem> FEVER_SYRUP = medicine("fever_syrup", 1);
    public static final DeferredItem<MedicineItem> SPEED_VITAMIN_DROPS = medicine("speed_vitamin_drops", 2);
    public static final DeferredItem<MedicineItem> IRON_BONE_CAPSULE = medicine("iron_bone_capsule", 3);
    public static final DeferredItem<MedicineItem> XRAY_VISION_TONIC = medicine("xray_vision_tonic", 4);
    public static final DeferredItem<MedicineItem> VITALITY_HEART_INJECTION = medicine("vitality_heart_injection", 5);
    public static final DeferredItem<MedicineItem> REGENERATION_SERUM = medicine("regeneration_serum", 6);
    public static final DeferredItem<MedicineItem> TITAN_STRENGTH_SERUM = medicine("titan_strength_serum", 7);
    public static final DeferredItem<MedicineItem> FIRE_SHIELD_SERUM = medicine("fire_shield_serum", 8);
    public static final DeferredItem<MedicineItem> OMEGA_SUPER_SERUM = medicine("omega_super_serum", 9);
    public static final DeferredItem<MedicineItem> IMMORTALITY_ELIXIR = medicine("immortality_elixir", 10);

    public static final List<DeferredItem<MedicineItem>> MEDICINES = List.of(FEVER_SYRUP, SPEED_VITAMIN_DROPS, IRON_BONE_CAPSULE,
            XRAY_VISION_TONIC, VITALITY_HEART_INJECTION, REGENERATION_SERUM, TITAN_STRENGTH_SERUM, FIRE_SHIELD_SERUM,
            OMEGA_SUPER_SERUM, IMMORTALITY_ELIXIR);

    // ---------------- Tools & story items ----------------
    public static final DeferredItem<ExpiredExperimentalSerumItem> EXPIRED_EXPERIMENTAL_SERUM = ITEMS.register("expired_experimental_serum",
            () -> new ExpiredExperimentalSerumItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));
    public static final DeferredItem<TitaniumHandcuffsItem> TITANIUM_HANDCUFFS = ITEMS.register("titanium_handcuffs",
            () -> new TitaniumHandcuffsItem(new Item.Properties().durability(16).rarity(Rarity.RARE)));
    public static final DeferredItem<UniversalCureSyringeItem> UNIVERSAL_CURE_SYRINGE = ITEMS.register("universal_cure_syringe",
            () -> new UniversalCureSyringeItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));
    public static final DeferredItem<DrAnantCoatItem> DR_ANANT_COAT = ITEMS.register("dr_anant_coat",
            () -> new DrAnantCoatItem(ModArmorMaterials.DR_ANANT, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(33)).rarity(Rarity.EPIC)));

    // ---------------- Block items ----------------
    /** Right-click = build the whole clinic 7 blocks in front of you. Left-click = place only the block. */
    public static final List<DeferredItem<ClinicSummonerItem>> CLINIC_SUMMONERS;

    static {
        List<DeferredItem<ClinicSummonerItem>> list = new java.util.ArrayList<>();
        for (int i = 0; i < ModBlocks.CLINIC_SUMMONERS.size(); i++) {
            final int tier = i + 1;
            final DeferredBlock<ClinicSummonerBlock> block = ModBlocks.CLINIC_SUMMONERS.get(i);
            list.add(ITEMS.register("clinic_summoner_" + tier, () -> new ClinicSummonerItem(block.get(), tier,
                    new Item.Properties().rarity(tier >= 5 ? Rarity.EPIC : tier >= 3 ? Rarity.RARE : Rarity.UNCOMMON))));
        }
        CLINIC_SUMMONERS = java.util.Collections.unmodifiableList(list);
    }
    public static final DeferredItem<?> CENTRAL_SHOP_SPAWNER = blockItem(ModBlocks.CENTRAL_SHOP_SPAWNER);
    public static final DeferredItem<?> ITEM_DISPLAY_PEDESTAL = blockItem(ModBlocks.ITEM_DISPLAY_PEDESTAL);
    public static final DeferredItem<?> EXAMINATION_BED = blockItem(ModBlocks.EXAMINATION_BED);
    public static final DeferredItem<?> DIAMOND_CASH_REGISTER = blockItem(ModBlocks.DIAMOND_CASH_REGISTER);
    public static final DeferredItem<?> POSTER_LANDSCAPE = blockItem(ModBlocks.POSTER_LANDSCAPE);
    public static final DeferredItem<?> POSTER_SQUARE = blockItem(ModBlocks.POSTER_SQUARE);
    public static final DeferredItem<?> POSTER_BILLBOARD = blockItem(ModBlocks.POSTER_BILLBOARD);
    public static final DeferredItem<?> STRETCHER = blockItem(ModBlocks.STRETCHER);

    // ---------------- Spawn eggs ----------------
    public static final DeferredItem<DeferredSpawnEggItem> MUTANT_BLOOD_BEAST_SPAWN_EGG = ITEMS.register("mutant_blood_beast_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.MUTANT_BLOOD_BEAST, 0x4A0000, 0xFF2A2A, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> ASSISTANT_PHARMACIST_SPAWN_EGG = ITEMS.register("assistant_pharmacist_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ASSISTANT_PHARMACIST, 0xF5F5F5, 0xD62E3A, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> SHOPKEEPER_SPAWN_EGG = ITEMS.register("shopkeeper_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.SHOPKEEPER, 0xFAA032, 0xC82828, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> PATIENT_VILLAGER_SPAWN_EGG = ITEMS.register("patient_villager_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.PATIENT_VILLAGER, 0x563C33, 0xE04848, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> ASSISTANT_SCIENTIST_SPAWN_EGG = ITEMS.register("assistant_scientist_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ASSISTANT_SCIENTIST, 0xD2F5D7, 0x28A050, new Item.Properties()));

    private static DeferredItem<MedicineItem> medicine(String name, int tier) {
        return ITEMS.register(name, () -> new MedicineItem(tier, new Item.Properties().stacksTo(16)
                .rarity(tier >= 10 ? Rarity.EPIC : tier >= 6 ? Rarity.RARE : tier >= 4 ? Rarity.UNCOMMON : Rarity.COMMON)));
    }

    private static DeferredItem<?> blockItem(DeferredBlock<? extends Block> block) {
        return ITEMS.registerSimpleBlockItem(block);
    }

    private ModItems() {}
}
