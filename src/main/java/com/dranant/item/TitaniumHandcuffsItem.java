package com.dranant.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Step 1 of the cure (model from low_poly_handcuffs.zip). Logic lives in ModGameEvents + MutantBloodBeastEntity#applyHandcuffs. */
public class TitaniumHandcuffsItem extends Item {
    public TitaniumHandcuffsItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Step 1: Use on the Mutant Blood Beast below 50% HP").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Immobilises it for 30 seconds").withStyle(ChatFormatting.DARK_AQUA));
    }
}
