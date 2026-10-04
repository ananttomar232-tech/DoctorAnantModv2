package com.dranant.block;

import com.dranant.block.entity.ItemDisplayPedestalBlockEntity;
import com.dranant.shop.ShopLogic;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Floating 360-degree shop pedestal.
 * <ul>
 *   <li>Right-click: buy the displayed item (paid from the Diamond Bank / inventory). Clinic upgrades: preview, then confirm.</li>
 *   <li>Shift + right-click with an item: put it on the pedestal (survival takes it from your hand; price is automatic).</li>
 *   <li>Shift + right-click with an empty hand: take the item back.</li>
 *   <li>Shop stock placed by the Central Shop can only be edited in creative. Use /pedestalprice to change a price.</li>
 * </ul>
 */
public class ItemDisplayPedestalBlock extends BaseEntityBlock {
    public static final MapCodec<ItemDisplayPedestalBlock> CODEC = simpleCodec(ItemDisplayPedestalBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 3, 16),
            Block.box(3, 3, 3, 13, 13, 13),
            Block.box(1, 13, 1, 15, 16, 15));

    public ItemDisplayPedestalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemDisplayPedestalBlockEntity(pos, state);
    }

    private static boolean canEdit(Player player, ItemDisplayPedestalBlockEntity pedestal) {
        return player.getAbilities().instabuild || !pedestal.isShopStock();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ItemDisplayPedestalBlockEntity pedestal)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                if (!canEdit(player, pedestal)) {
                    player.displayClientMessage(Component.literal("That's shop stock - right-click to buy it!").withStyle(ChatFormatting.RED), true);
                } else {
                    if (!player.getAbilities().instabuild) {
                        ItemStack old = pedestal.getDisplayedItem();
                        if (!old.isEmpty() && !player.getInventory().add(old.copy())) player.drop(old.copy(), false);
                        pedestal.setDisplayedItem(stack.copyWithCount(1));
                        stack.shrink(1);
                    } else {
                        pedestal.setDisplayedItem(stack.copyWithCount(1));
                    }
                    pedestal.setDiamondPrice(ShopLogic.defaultPrice(pedestal.getDisplayedItem()));
                    player.displayClientMessage(Component.literal("Now displaying: ").append(pedestal.getDisplayedItem().getHoverName())
                            .append(String.format(" (%,d Diamonds)", pedestal.getDiamondPrice())).withStyle(ChatFormatting.AQUA), true);
                    level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
                    ((ServerLevel) level).sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.6, pos.getZ() + 0.5, 12, 0.2, 0.3, 0.2, 0.03);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) ShopLogic.purchase((ServerLevel) level, pos, player, pedestal);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ItemDisplayPedestalBlockEntity pedestal)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                if (!canEdit(player, pedestal)) {
                    player.displayClientMessage(Component.literal("That's shop stock - right-click to buy it!").withStyle(ChatFormatting.RED), true);
                } else if (!pedestal.getDisplayedItem().isEmpty()) {
                    ItemStack old = pedestal.getDisplayedItem().copy();
                    pedestal.setDisplayedItem(ItemStack.EMPTY);
                    if (!player.getAbilities().instabuild && !player.getInventory().add(old)) player.drop(old, false);
                    level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            } else {
                ShopLogic.purchase((ServerLevel) level, pos, player, pedestal);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ItemDisplayPedestalBlockEntity pedestal
                && !pedestal.isShopStock() && !pedestal.getDisplayedItem().isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, pedestal.getDisplayedItem().copy());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
