package com.dranant.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Step 2 of the cure (model from medical_syringe.zip). Only works while the beast is handcuffed. */
public class UniversalCureSyringeItem extends Item {
    public UniversalCureSyringeItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Step 2: Inject the HANDCUFFED beast to cure it").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Reward: 128 Diamonds + rare loot").withStyle(ChatFormatting.AQUA));
    }
}
