package com.dranant.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Thin wall poster showing "Anant Doctor karege aapke sabhi dukho ka ilaaj".
 * FACING = the direction the printed side looks at. The wall sits behind it.
 * Wide posters extend toward FACING.getClockWise() (the viewer's left) and tall ones extend upward.
 */
public abstract class PosterBlock extends HorizontalDirectionalBlock {
    private static final VoxelShape NORTH = Block.box(0, 0, 15, 16, 16, 16);
    private static final VoxelShape SOUTH = Block.box(0, 0, 0, 16, 16, 1);
    private static final VoxelShape EAST = Block.box(0, 0, 0, 1, 16, 16);
    private static final VoxelShape WEST = Block.box(15, 0, 0, 16, 16, 16);

    private final int widthBlocks;
    private final int heightBlocks;

    protected PosterBlock(Properties properties, int widthBlocks, int heightBlocks) {
        super(properties);
        this.widthBlocks = widthBlocks;
        this.heightBlocks = heightBlocks;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public int getWidthBlocks() {
        return widthBlocks;
    }

    public int getHeightBlocks() {
        return heightBlocks;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        Direction facing = face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> NORTH;
        };
    }
}
