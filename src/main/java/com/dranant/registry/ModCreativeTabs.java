package com.dranant.registry;

import com.dranant.DoctorAnantMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DoctorAnantMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MEGA_CLINIC = TABS.register("mega_clinic", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.dranant"))
            .icon(() -> new ItemStack(ModItems.UNIVERSAL_CURE_SYRINGE.get()))
            .displayItems((params, output) -> {
                for (DeferredHolder<Item, ? extends Item> holder : ModItems.ITEMS.getEntries()) {
                    output.accept(holder.get());
                }
            })
            .build());

    private ModCreativeTabs() {}
}
