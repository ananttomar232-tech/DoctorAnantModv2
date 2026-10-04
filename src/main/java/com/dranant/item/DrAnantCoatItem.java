package com.dranant.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Dr. Anant's lab coat chestplate. While worn it continuously grants Resistance I and Saturation
 * (applied every second in ModGameEvents#onPlayerTick).
 */
public class DrAnantCoatItem extends ArmorItem {
    public DrAnantCoatItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Worn by the great Dr. Anant").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Resistance I + Saturation while worn").withStyle(ChatFormatting.BLUE));
    }
}
