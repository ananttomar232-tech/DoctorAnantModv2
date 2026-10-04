package com.dranant.block;

import com.dranant.clinic.ClinicBuilder;
import com.dranant.clinic.ClinicTiers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Clinic Summoner block (tiers 1-6). Placed with LEFT-click it is just a decorative block;
 * right-clicking the placed block builds that tier's clinic 7 blocks in front of the player.
 * (Right-clicking with the item in hand builds immediately, see ClinicSummonerItem.)
 */
public class ClinicSummonerBlock extends Block {
    public static final MapCodec<ClinicSummonerBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("tier").forGetter(ClinicSummonerBlock::getTier),
            propertiesCodec()
    ).apply(instance, ClinicSummonerBlock::new));

    private final int tier;

    public ClinicSummonerBlock(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }

    public String getClinicName() {
        return ClinicTiers.name(tier);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel) {
            ClinicBuilder.summonInFront(serverLevel, player, tier);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
