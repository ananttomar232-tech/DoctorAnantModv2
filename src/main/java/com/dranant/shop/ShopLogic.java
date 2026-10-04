package com.dranant.shop;

import com.dranant.block.entity.ItemDisplayPedestalBlockEntity;
import com.dranant.clinic.ClinicBuilder;
import com.dranant.clinic.ClinicTiers;
import com.dranant.counter.DiamondBank;
import com.dranant.dialogue.Lines;
import com.dranant.entity.ShopkeeperEntity;
import com.dranant.item.ClinicSummonerItem;
import com.dranant.item.MedicineItem;
import com.dranant.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Shop purchases. The price is paid from the Diamond Bank (counter) first, then from diamonds in the inventory.
 * Clinic upgrades show a live preview outline first; right-click again within 6 seconds to buy & build.
 */
public final class ShopLogic {

    public static long defaultPrice(ItemStack stack) {
        if (stack.getItem() instanceof ClinicSummonerItem summoner) return ClinicTiers.price(summoner.getTier());
        if (stack.getItem() instanceof MedicineItem medicine) return medicine.getTier() * 100L;
        if (stack.is(ModItems.TITANIUM_HANDCUFFS.get())) return 2_500L;
        if (stack.is(ModItems.UNIVERSAL_CURE_SYRINGE.get())) return 5_000L;
        if (stack.is(ModItems.EXPIRED_EXPERIMENTAL_SERUM.get())) return 1_000L;
        if (stack.is(ModItems.DR_ANANT_COAT.get())) return 3_000L;
        return 250L;
    }

    public static long funds(Player player) {
        MinecraftServer server = player.getServer();
        long bank = server != null ? DiamondBank.get(server).getBalance() : 0L;
        return bank + countDiamonds(player.getInventory());
    }

    /** Pays {@code price}: bank first, then inventory diamonds. @return false if the player cannot afford it */
    public static boolean pay(Player player, long price) {
        if (player.getAbilities().instabuild || price <= 0) return true;
        MinecraftServer server = player.getServer();
        if (server == null || funds(player) < price) return false;
        long fromBank = DiamondBank.get(server).withdraw(server, price);
        long rest = price - fromBank;
        if (rest > 0) removeDiamonds(player.getInventory(), (int) Math.min(Integer.MAX_VALUE, rest));
        return true;
    }

    public static void purchase(ServerLevel level, BlockPos pos, Player player, ItemDisplayPedestalBlockEntity pedestal) {
        ItemStack display = pedestal.getDisplayedItem();
        if (display.isEmpty()) {
            player.displayClientMessage(Component.literal("This pedestal is empty.").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        long price = pedestal.getDiamondPrice();
        if (display.getItem() instanceof ClinicSummonerItem summoner) {
            purchaseClinic(level, pos, player, summoner.getTier(), price);
            return;
        }
        if (!pay(player, price)) {
            notEnough(level, pos, player, price);
            return;
        }
        ItemStack bought = display.copyWithCount(1);
        if (!player.getInventory().add(bought)) player.drop(bought, false);
        celebrate(level, pos);
        player.displayClientMessage(Component.literal("Purchased ").append(display.getHoverName())
                .append(String.format(" for %,d Diamonds!", price)).withStyle(ChatFormatting.GREEN), true);
        sellerThanks(level, pos);
    }

    private static void purchaseClinic(ServerLevel level, BlockPos pos, Player player, int tier, long price) {
        if (!PreviewManager.isConfirming(player, pos)) {
            if (!player.getAbilities().instabuild && funds(player) < price) {
                notEnough(level, pos, player, price);
                return;
            }
            PreviewManager.start(player, pos, tier);
            player.displayClientMessage(Component.literal(String.format("PREVIEW: Level %d %s (%,d Diamonds) - right-click again to BUY & BUILD",
                    tier, ClinicTiers.name(tier), price)).withStyle(ChatFormatting.YELLOW), true);
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.8F, 1.2F);
            return;
        }
        PreviewManager.clear(player);
        if (!pay(player, price)) {
            notEnough(level, pos, player, price);
            return;
        }
        celebrate(level, pos);
        sellerThanks(level, pos);
        ClinicBuilder.summonInFront(level, player, tier);
    }

    private static void notEnough(ServerLevel level, BlockPos pos, Player player, long price) {
        player.displayClientMessage(Component.literal(String.format("You need %,d Diamonds (you have %,d)", price, funds(player)))
                .withStyle(ChatFormatting.RED), true);
        level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 1.0F, 1.0F);
        List<ShopkeeperEntity> sellers = level.getEntitiesOfClass(ShopkeeperEntity.class, new AABB(pos).inflate(16.0D));
        if (!sellers.isEmpty()) sellers.get(0).sayLine(Lines.pick(Lines.SELLER_BROKE, level.random), true);
    }

    private static void celebrate(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.VILLAGER_CELEBRATE, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.4F);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.6, pos.getZ() + 0.5, 25, 0.45, 0.45, 0.45, 0.1);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.getX() + 0.5, pos.getY() + 1.6, pos.getZ() + 0.5, 20, 0.2, 0.3, 0.2, 0.25);
    }

    private static void sellerThanks(ServerLevel level, BlockPos pos) {
        List<ShopkeeperEntity> sellers = level.getEntitiesOfClass(ShopkeeperEntity.class, new AABB(pos).inflate(16.0D));
        if (!sellers.isEmpty()) sellers.get(0).sayLine(Lines.pick(Lines.SELLER_THANKS, level.random), false);
    }

    public static int countDiamonds(Inventory inventory) {
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack s = inventory.getItem(i);
            if (s.is(Items.DIAMOND)) total += s.getCount();
        }
        return total;
    }

    public static void removeDiamonds(Inventory inventory, int amount) {
        int remaining = amount;
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack s = inventory.getItem(i);
            if (s.is(Items.DIAMOND)) {
                int take = Math.min(remaining, s.getCount());
                s.shrink(take);
                remaining -= take;
            }
        }
        inventory.setChanged();
    }

    private ShopLogic() {}
}
