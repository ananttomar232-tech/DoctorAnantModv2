package com.dranant.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/** 1x1 square poster with Dr. Anant's slogan. */
public class PosterSquareBlock extends PosterBlock {
    public static final MapCodec<PosterSquareBlock> CODEC = simpleCodec(PosterSquareBlock::new);

    public PosterSquareBlock(Properties properties) {
        super(properties, 1, 1);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}
