package com.dranant.block.entity;

import com.dranant.clinic.ClinicQueueManager;
import com.dranant.clinic.ClinicRegistry;
import com.dranant.counter.DiamondBank;
import com.dranant.counter.DiamondFx;
import com.dranant.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The clinic's heart. Every diamond the Assistant Pharmacist earns goes straight into the Diamond Bank (the counter),
 * with a red + yellow particle burst. It also owns the clinic's two patient queues.
 */
public class DiamondCashRegisterBlockEntity extends BlockEntity {
    private long lifetimeEarnings;
    private final List<UUID> line1 = new ArrayList<>();
    private final List<UUID> line2 = new ArrayList<>();
    @Nullable private BlockPos bedHead;
    private final List<BlockPos> stretchers = new ArrayList<>();
    // runtime state (not saved)
    public long nextFacilityScan;
    public long nextCustomerSpawn;
    public long nextConversion;
    @Nullable public UUID counterPatient;
    public int counterTicks;
    public long lastNoStaffComplaint;

    public DiamondCashRegisterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIAMOND_CASH_REGISTER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DiamondCashRegisterBlockEntity register) {
        if (level instanceof ServerLevel serverLevel) {
            ClinicQueueManager.tick(serverLevel, pos, state, register);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) ClinicRegistry.add(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) ClinicRegistry.remove(level, worldPosition);
    }

    /** Pharmacist income: burst of particles + sound, deposited straight into the Diamond Bank counter. */
    public void addDiamonds(int amount) {
        if (amount <= 0 || !(level instanceof ServerLevel serverLevel)) return;
        lifetimeEarnings += amount;
        setChanged();
        DiamondFx.burst(serverLevel, worldPosition.getX() + 0.5, worldPosition.getY() + 0.8, worldPosition.getZ() + 0.5);
        DiamondBank.get(serverLevel.getServer()).earn(serverLevel.getServer(), amount);
    }

    public long getLifetimeEarnings() {
        return lifetimeEarnings;
    }

    public List<UUID> getLine1() {
        return line1;
    }

    public List<UUID> getLine2() {
        return line2;
    }

    @Nullable
    public BlockPos getBedHead() {
        return bedHead;
    }

    public void setBedHead(@Nullable BlockPos bedHead) {
        this.bedHead = bedHead;
        setChanged();
    }

    public List<BlockPos> getStretchers() {
        return stretchers;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("lifetimeEarnings", lifetimeEarnings);
        tag.put("line1", writeUuids(line1));
        tag.put("line2", writeUuids(line2));
        if (bedHead != null) tag.putLong("bedHead", bedHead.asLong());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        lifetimeEarnings = tag.getLong("lifetimeEarnings");
        readUuids(tag.getList("line1", Tag.TAG_INT_ARRAY), line1);
        readUuids(tag.getList("line2", Tag.TAG_INT_ARRAY), line2);
        bedHead = tag.contains("bedHead") ? BlockPos.of(tag.getLong("bedHead")) : null;
    }

    private static ListTag writeUuids(List<UUID> ids) {
        ListTag list = new ListTag();
        for (UUID id : ids) list.add(NbtUtils.createUUID(id));
        return list;
    }

    private static void readUuids(ListTag list, List<UUID> out) {
        out.clear();
        for (Tag t : list) out.add(NbtUtils.loadUUID(t));
    }
}
