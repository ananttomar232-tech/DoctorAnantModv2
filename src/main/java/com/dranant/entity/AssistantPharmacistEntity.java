package com.dranant.entity;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Stationed behind the medicine counter. The {@link com.dranant.clinic.ClinicQueueManager} calls
 * {@link #serveCustomer} when the villager at the front of Line 1 is ready.
 */
public class AssistantPharmacistEntity extends AssistantEntity {
    private int handClearTicks;

    public AssistantPharmacistEntity(EntityType<? extends AssistantPharmacistEntity> type, Level level) {
        super(type, level);
    }

    public void serveCustomer(LivingEntity customer, ItemStack medicine) {
        setItemSlot(EquipmentSlot.MAINHAND, medicine.copy());
        swing(InteractionHand.MAIN_HAND);
        getLookControl().setLookAt(customer, 30.0F, 30.0F);
        playSound(SoundEvents.VILLAGER_TRADE, 1.0F, 1.0F);
        handClearTicks = 30;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && handClearTicks > 0 && --handClearTicks == 0) {
            setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    @Override
    protected String greeting() {
        return "Namaste! Line 1 mein dawai milti hai. Dr. Anant ke rate sabse best!";
    }
}
