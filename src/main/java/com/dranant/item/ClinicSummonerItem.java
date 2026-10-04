package com.dranant.item;

import com.dranant.clinic.ClinicBuilder;
import com.dranant.clinic.ClinicTiers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * RIGHT-click (on a block or in the air): builds the whole clinic 7 blocks in front of you, entrance facing you.
 * LEFT-click on a block: places just the summoner block (handled in ModGameEvents#onLeftClickBlock).
 */
public class ClinicSummonerItem extends BlockItem {
    private final int tier;

    public ClinicSummonerItem(Block block, int tier, Properties properties) {
        super(block, properties);
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        summon(context.getLevel(), player, context.getItemInHand());
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        summon(level, player, stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private void summon(Level level, Player player, ItemStack stack) {
        if (level instanceof ServerLevel serverLevel) {
            if (ClinicBuilder.summonInFront(serverLevel, player, tier) && !player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Level " + tier + " - " + ClinicTiers.name(tier)).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Right-click: build the clinic 7 blocks in front of you").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("Left-click: place only the block").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(String.format("Shop price: %,d Diamonds", ClinicTiers.price(tier))).withStyle(ChatFormatting.GOLD));
    }
}
