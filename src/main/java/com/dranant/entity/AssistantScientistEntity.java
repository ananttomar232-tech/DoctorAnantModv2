package com.dranant.entity;

import com.dranant.block.entity.ExaminationBedBlockEntity;
import com.dranant.clinic.ClinicQueueManager;
import com.dranant.dialogue.Lines;
import com.dranant.dialogue.Speech;
import com.dranant.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Runs the Experimental Ward: calls the next Line 2 patient from the stretchers to the Examination Bed, treats them for
 * 5 seconds with brewing sounds, magic particles and dialogue, then the patient stands up, drops exactly 64 diamonds
 * and leaves happily.
 */
public class AssistantScientistEntity extends AssistantEntity {
    public static final int TREATMENT_TICKS = 100; // 5 seconds
    private static final int ARRIVAL_TIMEOUT = 300;

    @Nullable private UUID patient;
    @Nullable private BlockPos bedHead;
    @Nullable private BlockPos clinicPos;
    private int treatTicks;
    private int waitTicks;

    public AssistantScientistEntity(EntityType<? extends AssistantScientistEntity> type, Level level) {
        super(type, level);
    }

    public boolean isBusy() {
        return patient != null;
    }

    @Nullable
    public UUID getPatient() {
        return patient;
    }

    public void beginTreatment(PatientVillagerEntity p, BlockPos bed, BlockPos clinic) {
        patient = p.getUUID();
        bedHead = bed;
        clinicPos = clinic;
        treatTicks = 0;
        waitTicks = 0;
        p.callToBed(bed);
        if (level().getBlockEntity(bed) instanceof ExaminationBedBlockEntity bedEntity) bedEntity.setPatient(p.getUUID());
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.UNIVERSAL_CURE_SYRINGE.get()));
        Speech.say(this, "Next patient! Bed pe aa jao.", Speech.STYLE_STAFF);
    }

    public void abortTreatment() {
        if (bedHead != null && level().getBlockEntity(bedHead) instanceof ExaminationBedBlockEntity bedEntity) bedEntity.setPatient(null);
        patient = null;
        treatTicks = 0;
        waitTicks = 0;
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && patient != null) tickTreatment((ServerLevel) level());
    }

    private void tickTreatment(ServerLevel level) {
        Entity entity = level.getEntity(patient);
        if (!(entity instanceof PatientVillagerEntity p) || !p.isAlive() || bedHead == null) {
            abortTreatment();
            return;
        }
        getLookControl().setLookAt(p, 30.0F, 30.0F);
        if (!p.isOnBed()) {
            if (++waitTicks > ARRIVAL_TIMEOUT) p.forceOnBed(bedHead);
            return;
        }
        treatTicks++;
        double x = p.getX(), y = p.getY() + 0.4D, z = p.getZ();
        if (treatTicks == 1) Speech.say(this, Lines.pick(Lines.SCIENTIST_TREAT, random), Speech.STYLE_STAFF);
        if (treatTicks == 40) Speech.say(p, Lines.pick(Lines.PATIENT_ON_BED, random), Speech.STYLE_PATIENT);
        if (treatTicks % 10 == 0) {
            level.playSound(null, bedHead, SoundEvents.BREWING_STAND_BREW, SoundSource.NEUTRAL, 0.8F, 0.85F + random.nextFloat() * 0.3F);
        }
        if (treatTicks % 3 == 0) {
            level.sendParticles(ParticleTypes.WITCH, x, y, z, 5, 0.5, 0.3, 0.5, 0.05);
            level.sendParticles(ParticleTypes.ENCHANT, x, y + 0.5, z, 12, 0.6, 0.4, 0.6, 0.6);
            level.sendParticles(ParticleTypes.EFFECT, x, y + 0.2, z, 4, 0.4, 0.2, 0.4, 0.02);
        }
        if (treatTicks % 20 == 0) swing(InteractionHand.MAIN_HAND);
        if (treatTicks >= TREATMENT_TICKS) completeTreatment(level, p);
    }

    private void completeTreatment(ServerLevel level, PatientVillagerEntity p) {
        p.standUp();
        ItemEntity diamonds = new ItemEntity(level, p.getX(), p.getY() + 0.6, p.getZ(), new ItemStack(Items.DIAMOND, 64));
        diamonds.getPersistentData().putBoolean(ClinicQueueManager.INCOME_TAG, true);
        diamonds.setDefaultPickUpDelay();
        level.addFreshEntity(diamonds);
        Speech.say(p, Lines.pick(Lines.CURED_SEVERE, random), Speech.STYLE_HAPPY);
        level.playSound(null, p.blockPosition(), SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.7F, 1.4F);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1.0, p.getZ(), 30, 0.6, 0.6, 0.6, 0.1);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.5, 0.8, 0.5, 0.3);
        p.leave(true);
        abortTreatment();
    }

    @Override
    protected String greeting() {
        return "Experimental Ward mein swagat hai. Line 2 patients 64 diamonds dete hain... results guaranteed!";
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (bedHead != null) tag.putLong("BedHead", bedHead.asLong());
        if (clinicPos != null) tag.putLong("Clinic", clinicPos.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        bedHead = tag.contains("BedHead") ? BlockPos.of(tag.getLong("BedHead")) : null;
        clinicPos = tag.contains("Clinic") ? BlockPos.of(tag.getLong("Clinic")) : null;
        patient = null; // treatments restart cleanly after a reload
    }
}
