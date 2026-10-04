package com.dranant.clinic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** A building described block-by-block in local coordinates, placed later by an animated {@link ConstructionManager} task. */
public class Blueprint {
    private final Frame frame;
    private final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
    private final List<Consumer<ServerLevel>> afterBuild = new ArrayList<>();
    private BoundingBox clearBox;

    public Blueprint(Frame frame) {
        this.frame = frame;
    }

    public Frame frame() {
        return frame;
    }

    public void set(int x, int y, int z, BlockState state) {
        blocks.put(frame.at(x, y, z), frame.state(state));
    }

    public void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++)
                    set(x, y, z, state);
    }

    /** World-space volume cleared (instantly) before the animated build starts. Local corners. */
    public void clearLocal(int x0, int y0, int z0, int x1, int y1, int z1) {
        clearBox = BoundingBox.fromCorners(frame.at(x0, y0, z0), frame.at(x1, y1, z1));
    }

    public BoundingBox clearBox() {
        return clearBox;
    }

    public void after(Consumer<ServerLevel> action) {
        afterBuild.add(action);
    }

    public Map<BlockPos, BlockState> blocks() {
        return blocks;
    }

    public List<Consumer<ServerLevel>> afterBuild() {
        return afterBuild;
    }
}
