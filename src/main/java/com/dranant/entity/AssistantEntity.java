package com.dranant.entity;

import com.dranant.entity.ai.ReturnToPostGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Shared base for the two clinic assistants. Rendered with the native humanoid (player) model plus a doctor texture,
 * so no external 3D file is required. Invulnerable, never despawns, and always walks back to its post.
 */
public abstract class AssistantEntity extends PathfinderMob {
    @Nullable
    private BlockPos post;

    protected AssistantEntity(EntityType<? extends AssistantEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setInvulnerable(true);
        setCanPickUpLoot(false);
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        setDropChance(EquipmentSlot.OFFHAND, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new ReturnToPostGoal(this));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Villager.class, 6.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Nullable
    public BlockPos getPost() {
        return post;
    }

    public void setPost(@Nullable BlockPos post) {
        this.post = post;
    }

    protected abstract String greeting();

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide && hand == InteractionHand.MAIN_HAND) {
            String line = greeting();
            player.sendSystemMessage(Component.literal("<" + getName().getString() + "> " + line));
            com.dranant.dialogue.Speech.say(this, line, com.dranant.dialogue.Speech.STYLE_STAFF);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (post != null) tag.putLong("Post", post.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        post = tag.contains("Post") ? BlockPos.of(tag.getLong("Post")) : null;
        setInvulnerable(true);
    }
}
