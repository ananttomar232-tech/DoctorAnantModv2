package com.dranant.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Marker effect. The resurrection itself is handled in {@link com.dranant.event.ModGameEvents#onLivingDeath}. */
public class ImmortalityEffect extends MobEffect {
    public ImmortalityEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFD700);
    }
}
