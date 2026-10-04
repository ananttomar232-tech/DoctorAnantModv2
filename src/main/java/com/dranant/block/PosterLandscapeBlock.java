package com.dranant.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/** 2x1 landscape poster with Dr. Anant's slogan. */
public class PosterLandscapeBlock extends PosterBlock {
    public static final MapCodec<PosterLandscapeBlock> CODEC = simpleCodec(PosterLandscapeBlock::new);

    public PosterLandscapeBlock(Properties properties) {
        super(properties, 2, 1);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}
