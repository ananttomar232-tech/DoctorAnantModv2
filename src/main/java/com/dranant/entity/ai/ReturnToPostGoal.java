package com.dranant.entity.ai;

import com.dranant.entity.AssistantEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Keeps an assistant stationed at its counter / ward post. */
public class ReturnToPostGoal extends Goal {
    private final AssistantEntity mob;

    public ReturnToPostGoal(AssistantEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    private double distSqrToPost() {
        BlockPos post = mob.getPost();
        if (post == null) return 0.0D;
        return mob.distanceToSqr(post.getX() + 0.5D, post.getY(), post.getZ() + 0.5D);
    }

    @Override
    public boolean canUse() {
        return mob.getPost() != null && distSqrToPost() > 1.0D;
    }

    @Override
    public boolean canContinueToUse() {
        return mob.getPost() != null && !mob.getNavigation().isDone() && distSqrToPost() > 0.3D;
    }

    @Override
    public void start() {
        BlockPos post = mob.getPost();
        if (post == null) return;
        if (distSqrToPost() > 32.0D * 32.0D) {
            mob.teleportTo(post.getX() + 0.5D, post.getY(), post.getZ() + 0.5D);
            return;
        }
        mob.getNavigation().moveTo(post.getX() + 0.5D, post.getY(), post.getZ() + 0.5D, 0.9D);
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }
}
