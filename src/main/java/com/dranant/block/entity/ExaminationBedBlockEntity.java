package com.dranant.block.entity;

import com.dranant.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Remembers which villager is currently lying on the bed for an experimental treatment. */
public class ExaminationBedBlockEntity extends BlockEntity {
    @Nullable
    private UUID patient;

    public ExaminationBedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXAMINATION_BED.get(), pos, state);
    }

    @Nullable
    public UUID getPatient() {
        return patient;
    }

    public void setPatient(@Nullable UUID patient) {
        this.patient = patient;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (patient != null) tag.putUUID("patient", patient);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        patient = tag.hasUUID("patient") ? tag.getUUID("patient") : null;
    }
}
