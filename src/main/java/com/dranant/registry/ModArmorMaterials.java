package com.dranant.registry;

import com.dranant.DoctorAnantMod;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, DoctorAnantMod.MODID);

    /** Texture: assets/dranant/textures/models/armor/dr_anant_layer_1.png (built from male_lab_coat_and_pants.zip). */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> DR_ANANT = ARMOR_MATERIALS.register("dr_anant", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 2);
                map.put(ArmorItem.Type.LEGGINGS, 5);
                map.put(ArmorItem.Type.CHESTPLATE, 7);
                map.put(ArmorItem.Type.HELMET, 2);
                map.put(ArmorItem.Type.BODY, 7);
            }),
            25,
            SoundEvents.ARMOR_EQUIP_LEATHER,
            () -> Ingredient.of(Items.WHITE_WOOL),
            List.of(new ArmorMaterial.Layer(DoctorAnantMod.id("dr_anant"))),
            1.0F,
            0.0F));

    private ModArmorMaterials() {}
}
