package com.dranant.clinic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Local building coordinates -> world coordinates. Designs are written with the entrance on the local +Z (south) side;
 * the rotation turns that entrance to face the player.
 */
public record Frame(BlockPos origin, Rotation rotation) {

    public BlockPos at(int x, int y, int z) {
        int wx, wz;
        switch (rotation) {
            case CLOCKWISE_90 -> { wx = -z; wz = x; }
            case CLOCKWISE_180 -> { wx = -x; wz = -z; }
            case COUNTERCLOCKWISE_90 -> { wx = z; wz = -x; }
            default -> { wx = x; wz = z; }
        }
        return origin.offset(wx, y, wz);
    }

    public Direction dir(Direction local) {
        return local.getAxis().isHorizontal() ? rotation.rotate(local) : local;
    }

    public BlockState state(BlockState state) {
        return state.rotate(rotation);
    }

    public float yaw(Direction local) {
        return dir(local).toYRot();
    }

    /** Rotation that turns the local SOUTH side (entrance) toward {@code worldDirection}. */
    public static Rotation rotationFacing(Direction worldDirection) {
        for (Rotation r : Rotation.values()) {
            if (r.rotate(Direction.SOUTH) == worldDirection) return r;
        }
        return Rotation.NONE;
    }
}
