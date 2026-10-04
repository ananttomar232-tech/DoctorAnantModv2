package com.dranant.registry;

import com.dranant.DoctorAnantMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Custom sound events. Each one is defined in assets/dranant/sounds.json (layered on vanilla sound events, so no .ogg files are required). */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, DoctorAnantMod.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> HANDCUFF_RATTLE = register("handcuff_rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> SYRINGE_INJECTION = register("syringe_injection");
    public static final DeferredHolder<SoundEvent, SoundEvent> GROUND_SLAM = register("ground_slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> CASH_REGISTER_CHIME = register("cash_register_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> DIAMOND_POP = register("diamond_pop");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(DoctorAnantMod.id(name)));
    }

    private ModSounds() {}
}
