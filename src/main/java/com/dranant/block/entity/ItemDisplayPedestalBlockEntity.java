package com.dranant.block.entity;

import com.dranant.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/** One-slot pedestal inventory + diamond price, synced to clients for the floating 360-degree renderer. */
public class ItemDisplayPedestalBlockEntity extends BlockEntity {
    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            sync();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };
    private long diamondPrice = 250;
    private boolean shopStock;
    /** Client-only animation state (hover lift / spin boost when the player looks at the pedestal). */
    public float clientHover;
    public float clientSpin;

    public ItemDisplayPedestalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ITEM_DISPLAY_PEDESTAL.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public ItemStack getDisplayedItem() {
        return inventory.getStackInSlot(0);
    }

    public void setDisplayedItem(ItemStack stack) {
        inventory.setStackInSlot(0, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
    }

    public long getDiamondPrice() {
        return diamondPrice;
    }

    public void setDiamondPrice(long price) {
        this.diamondPrice = Math.max(0L, price);
        setChanged();
        sync();
    }

    /** Shop stock can only be changed by creative players; player-placed pedestals are free to edit. */
    public boolean isShopStock() {
        return shopStock;
    }

    public void setShopStock(boolean shopStock) {
        this.shopStock = shopStock;
        setChanged();
        sync();
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.putLong("diamondPrice", diamondPrice);
        tag.putBoolean("shopStock", shopStock);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        diamondPrice = tag.contains("diamondPrice") ? tag.getLong("diamondPrice") : 250L;
        shopStock = tag.getBoolean("shopStock");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
