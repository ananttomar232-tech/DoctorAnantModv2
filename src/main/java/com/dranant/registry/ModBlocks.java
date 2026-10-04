package com.dranant.registry;

import com.dranant.DoctorAnantMod;
import com.dranant.block.CentralShopSpawnerBlock;
import com.dranant.block.ClinicSummonerBlock;
import com.dranant.block.DiamondCashRegisterBlock;
import com.dranant.block.ExaminationBedBlock;
import com.dranant.block.ItemDisplayPedestalBlock;
import com.dranant.block.PosterBillboardBlock;
import com.dranant.block.PosterLandscapeBlock;
import com.dranant.block.PosterSquareBlock;
import com.dranant.block.StretcherBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(DoctorAnantMod.MODID);

    public static final int CLINIC_TIERS = 6;

    /** clinic_summoner_1 ... clinic_summoner_6 (index 0 = tier 1). */
    public static final List<DeferredBlock<ClinicSummonerBlock>> CLINIC_SUMMONERS;

    static {
        List<DeferredBlock<ClinicSummonerBlock>> list = new ArrayList<>();
        for (int i = 1; i <= CLINIC_TIERS; i++) {
            final int tier = i;
            list.add(BLOCKS.register("clinic_summoner_" + tier, () -> new ClinicSummonerBlock(tier,
                    BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(1.5F).sound(SoundType.METAL).lightLevel(s -> 7))));
        }
        CLINIC_SUMMONERS = Collections.unmodifiableList(list);
    }

    public static final DeferredBlock<CentralShopSpawnerBlock> CENTRAL_SHOP_SPAWNER = BLOCKS.register("central_shop_spawner",
            () -> new CentralShopSpawnerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(1.5F).sound(SoundType.METAL).lightLevel(s -> 7)));

    public static final DeferredBlock<ItemDisplayPedestalBlock> ITEM_DISPLAY_PEDESTAL = BLOCKS.register("item_display_pedestal",
            () -> new ItemDisplayPedestalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0F, 6.0F).sound(SoundType.STONE).noOcclusion()));

    public static final DeferredBlock<ExaminationBedBlock> EXAMINATION_BED = BLOCKS.register("examination_bed",
            () -> new ExaminationBedBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(1.0F).sound(SoundType.METAL).noOcclusion()));

    public static final DeferredBlock<DiamondCashRegisterBlock> DIAMOND_CASH_REGISTER = BLOCKS.register("diamond_cash_register",
            () -> new DiamondCashRegisterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND).strength(2.0F, 6.0F).sound(SoundType.METAL).noOcclusion().lightLevel(s -> 5)));

    public static final DeferredBlock<PosterLandscapeBlock> POSTER_LANDSCAPE = BLOCKS.register("poster_landscape",
            () -> new PosterLandscapeBlock(posterProps()));
    public static final DeferredBlock<PosterSquareBlock> POSTER_SQUARE = BLOCKS.register("poster_square",
            () -> new PosterSquareBlock(posterProps()));
    public static final DeferredBlock<PosterBillboardBlock> POSTER_BILLBOARD = BLOCKS.register("poster_billboard",
            () -> new PosterBillboardBlock(posterProps()));

    public static final DeferredBlock<StretcherBlock> STRETCHER = BLOCKS.register("stretcher",
            () -> new StretcherBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.8F).sound(SoundType.WOOL).noOcclusion()));

    private static BlockBehaviour.Properties posterProps() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.5F).sound(SoundType.WOOL).noOcclusion().noCollission();
    }

    private ModBlocks() {}
}
