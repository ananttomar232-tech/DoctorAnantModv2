package com.dranant.registry;

import com.dranant.DoctorAnantMod;
import com.dranant.block.entity.DiamondCashRegisterBlockEntity;
import com.dranant.block.entity.ExaminationBedBlockEntity;
import com.dranant.block.entity.ItemDisplayPedestalBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DoctorAnantMod.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ItemDisplayPedestalBlockEntity>> ITEM_DISPLAY_PEDESTAL =
            BLOCK_ENTITIES.register("item_display_pedestal", () -> BlockEntityType.Builder.of(ItemDisplayPedestalBlockEntity::new,
                    ModBlocks.ITEM_DISPLAY_PEDESTAL.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ExaminationBedBlockEntity>> EXAMINATION_BED =
            BLOCK_ENTITIES.register("examination_bed", () -> BlockEntityType.Builder.of(ExaminationBedBlockEntity::new,
                    ModBlocks.EXAMINATION_BED.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DiamondCashRegisterBlockEntity>> DIAMOND_CASH_REGISTER =
            BLOCK_ENTITIES.register("diamond_cash_register", () -> BlockEntityType.Builder.of(DiamondCashRegisterBlockEntity::new,
                    ModBlocks.DIAMOND_CASH_REGISTER.get()).build(null));

    private ModBlockEntities() {}
}
