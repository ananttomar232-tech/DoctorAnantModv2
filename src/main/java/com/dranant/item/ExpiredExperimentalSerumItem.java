package com.dranant.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * "The Expired Medicine" (model/texture from blood_potion.zip). Give it to a lab patient on the Examination Bed
 * to trigger the outbreak. The interaction is handled in {@link com.dranant.event.ModGameEvents#onEntityInteract}.
 */
public class ExpiredExperimentalSerumItem extends Item {
    public ExpiredExperimentalSerumItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        // dark bubbling crimson drip while held
        if (level.isClientSide && isSelected && level.random.nextInt(4) == 0) {
            level.addParticle(DustParticleOptions.REDSTONE, entity.getX() + (level.random.nextDouble() - 0.5) * 0.6,
                    entity.getY() + 1.0, entity.getZ() + (level.random.nextDouble() - 0.5) * 0.6, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("EXPIRED - DO NOT ADMINISTER").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("Use on a patient lying on the Examination Bed...").withStyle(ChatFormatting.GRAY));
    }
}
