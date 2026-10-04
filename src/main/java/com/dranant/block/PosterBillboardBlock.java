package com.dranant.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/** 2x2 billboard poster with Dr. Anant's slogan. */
public class PosterBillboardBlock extends PosterBlock {
    public static final MapCodec<PosterBillboardBlock> CODEC = simpleCodec(PosterBillboardBlock::new);

    public PosterBillboardBlock(Properties properties) {
        super(properties, 2, 2);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}
