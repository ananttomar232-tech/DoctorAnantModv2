package com.dranant.entity;

import com.dranant.dialogue.Lines;
import com.dranant.dialogue.Speech;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The Central Shop's seller. Every 15-35 seconds he talks to the nearest player -
 * sometimes friendly, sometimes downright rude.
 */
public class ShopkeeperEntity extends AssistantEntity {
    private int talkCooldown = 200;

    public ShopkeeperEntity(EntityType<? extends ShopkeeperEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || --talkCooldown > 0) return;
        talkCooldown = 300 + random.nextInt(400);
        Player player = level().getNearestPlayer(this, 10.0D);
        if (player != null) {
            getLookControl().setLookAt(player, 30.0F, 30.0F);
            speakTo(player, random.nextFloat() < 0.4F);
        }
    }

    public void speakTo(Player player, boolean rude) {
        String line = Lines.pick(rude ? Lines.SELLER_RUDE : Lines.SELLER_FRIENDLY, random);
        Speech.say(this, line, rude ? Speech.STYLE_RUDE : Speech.STYLE_FRIENDLY);
        player.sendSystemMessage(Component.literal("<" + getName().getString() + "> " + line)
                .withStyle(rude ? ChatFormatting.RED : ChatFormatting.GOLD));
        playSound(rude ? SoundEvents.VILLAGER_NO : SoundEvents.VILLAGER_YES, 1.0F, 0.9F);
    }

    public void sayLine(String line, boolean rude) {
        Speech.say(this, line, rude ? Speech.STYLE_RUDE : Speech.STYLE_FRIENDLY);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide && hand == InteractionHand.MAIN_HAND) {
            speakTo(player, random.nextFloat() < 0.5F);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    protected String greeting() {
        return Lines.pick(Lines.SELLER_FRIENDLY, random);
    }
}
