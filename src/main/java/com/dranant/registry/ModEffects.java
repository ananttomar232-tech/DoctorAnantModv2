package com.dranant.registry;

import com.dranant.DoctorAnantMod;
import com.dranant.effect.ImmortalityEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, DoctorAnantMod.MODID);

    /** Granted by the Tier 10 Immortality Elixir: a one-time Totem-of-Undying style resurrection. */
    public static final DeferredHolder<MobEffect, ImmortalityEffect> IMMORTALITY = MOB_EFFECTS.register("immortality", ImmortalityEffect::new);

    private ModEffects() {}
}
