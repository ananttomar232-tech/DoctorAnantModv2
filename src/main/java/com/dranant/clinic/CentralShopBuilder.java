package com.dranant.clinic;

import com.dranant.block.DiamondCashRegisterBlock;
import com.dranant.block.PosterBlock;
import com.dranant.item.MedicineItem;
import com.dranant.registry.ModBlocks;
import com.dranant.registry.ModItems;
import com.dranant.shop.ShopLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Procedurally constructs the Central Shop (pure Java, no schematic) with an animated build:
 * 19 x 7 x 19 centred on the spawner, entrance facing the player, quartz/terracotta floor, glass walls, lit ceiling,
 * customer counter with the Diamond Cash Register, 14 medicine/tool pedestals, a 6-pedestal "Clinic Upgrades" gallery,
 * the Assistant Pharmacist, the (sometimes rude) Seller and Dr. Anant posters outside.
 */
public final class CentralShopBuilder {
    private static final int HALF = 9;
    private static final int TOP = 6;

    public static void build(ServerLevel level, BlockPos origin, @Nullable Player player) {
        Rotation rotation = player != null ? Frame.rotationFacing(player.getDirection().getOpposite()) : Rotation.NONE;
        Frame f = new Frame(origin.below(), rotation);
        Blueprint bp = new Blueprint(f);
        bp.clearLocal(-HALF - 1, 1, -HALF - 1, HALF + 1, TOP + 1, HALF + 1);

        BlockState quartz = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        BlockState pillar = Blocks.QUARTZ_PILLAR.defaultBlockState();
        BlockState bricks = Blocks.QUARTZ_BRICKS.defaultBlockState();
        BlockState glass = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();

        for (int x = -HALF; x <= HALF; x++)
            for (int z = -HALF; z <= HALF; z++) {
                boolean border = Math.abs(x) == HALF || Math.abs(z) == HALF;
                bp.set(x, 0, z, border ? quartz : (((x + z) & 1) == 0 ? Blocks.LIGHT_BLUE_TERRACOTTA : Blocks.WHITE_TERRACOTTA).defaultBlockState());
            }
        for (int y = 1; y < TOP; y++)
            for (int x = -HALF; x <= HALF; x++)
                for (int z = -HALF; z <= HALF; z++) {
                    boolean ex = Math.abs(x) == HALF, ez = Math.abs(z) == HALF;
                    if (!ex && !ez) continue;
                    boolean pil = (ex && ez) || (ez && (x + HALF) % 4 == 0) || (ex && (z + HALF) % 4 == 0);
                    bp.set(x, y, z, pil ? pillar : (y == 1 || y == TOP - 1) ? bricks : glass);
                }
        for (int x = -HALF; x <= HALF; x++)
            for (int z = -HALF; z <= HALF; z++) {
                boolean border = Math.abs(x) == HALF || Math.abs(z) == HALF;
                boolean light = !border && (x + HALF) % 3 == 0 && (z + HALF) % 3 == 0;
                bp.set(x, TOP, z, border ? bricks : light ? Blocks.SEA_LANTERN.defaultBlockState() : quartz);
            }
        // entrance + welcome mat
        for (int x = -1; x <= 1; x++) {
            for (int y = 1; y <= 3; y++) bp.set(x, y, HALF, Blocks.AIR.defaultBlockState());
            bp.set(x, 0, HALF, Blocks.CYAN_TERRACOTTA.defaultBlockState());
        }
        // counter + register
        int counterZ = -4;
        for (int x = -4; x <= 4; x++) bp.set(x, 1, counterZ, Math.abs(x) == 4 ? pillar : Blocks.POLISHED_DIORITE.defaultBlockState());
        bp.set(0, 2, counterZ, ModBlocks.DIAMOND_CASH_REGISTER.get().defaultBlockState().setValue(DiamondCashRegisterBlock.FACING, Direction.SOUTH));
        bp.set(-3, 2, counterZ, Blocks.BREWING_STAND.defaultBlockState());
        bp.set(3, 2, counterZ, Blocks.POTTED_RED_TULIP.defaultBlockState());
        for (int x = -7; x <= 7; x++) {
            bp.set(x, 1, -8, Blocks.BOOKSHELF.defaultBlockState());
            bp.set(x, 2, -8, ((x & 1) == 0 ? Blocks.BOOKSHELF : Blocks.CHISELED_BOOKSHELF).defaultBlockState());
        }
        bp.set(-7, 1, -7, Blocks.CAULDRON.defaultBlockState());
        bp.set(7, 1, -7, Blocks.BREWING_STAND.defaultBlockState());
        BlockState benchW = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
        BlockState benchE = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
        bp.set(-8, 1, 8, benchW);
        bp.set(8, 1, 8, benchE);

        // pedestals
        int[][] medicineSlots = {{-7, -2}, {-7, 0}, {-7, 2}, {-7, 4}, {-7, 6}, {7, -2}, {7, 0}, {7, 2}, {7, 4}, {7, 6}, {-4, 1}, {4, 1}, {-4, 4}, {4, 4}};
        int[][] clinicSlots = {{-7, 8}, {-5, 8}, {-3, 8}, {3, 8}, {5, 8}, {7, 8}};
        BlockState pedestal = ModBlocks.ITEM_DISPLAY_PEDESTAL.get().defaultBlockState();
        for (int[] s : medicineSlots) bp.set(s[0], 1, s[1], pedestal);
        for (int[] s : clinicSlots) {
            bp.set(s[0], 1, s[1], pedestal);
            bp.set(s[0], 0, s[1], Blocks.GOLD_BLOCK.defaultBlockState());
        }

        List<ItemStack> goods = new ArrayList<>();
        for (DeferredItem<MedicineItem> med : ModItems.MEDICINES) goods.add(new ItemStack(med.get()));
        goods.add(new ItemStack(ModItems.TITANIUM_HANDCUFFS.get()));
        goods.add(new ItemStack(ModItems.UNIVERSAL_CURE_SYRINGE.get()));
        goods.add(new ItemStack(ModItems.EXPIRED_EXPERIMENTAL_SERUM.get()));
        goods.add(new ItemStack(ModItems.DR_ANANT_COAT.get()));
        for (int i = 0; i < medicineSlots.length && i < goods.size(); i++) {
            final BlockPos p = f.at(medicineSlots[i][0], 1, medicineSlots[i][1]);
            final ItemStack stack = goods.get(i);
            bp.after(l -> BuildHelper.stockPedestal(l, p, stack, ShopLogic.defaultPrice(stack), true));
        }
        for (int i = 0; i < clinicSlots.length; i++) {
            final BlockPos p = f.at(clinicSlots[i][0], 1, clinicSlots[i][1]);
            final ItemStack stack = new ItemStack(ModItems.CLINIC_SUMMONERS.get(i).get());
            bp.after(l -> BuildHelper.stockPedestal(l, p, stack, ShopLogic.defaultPrice(stack), true));
        }

        // posters outside
        bp.set(-3, 3, HALF + 1, ModBlocks.POSTER_LANDSCAPE.get().defaultBlockState().setValue(PosterBlock.FACING, Direction.SOUTH));
        bp.set(3, 3, HALF + 1, ModBlocks.POSTER_SQUARE.get().defaultBlockState().setValue(PosterBlock.FACING, Direction.SOUTH));
        bp.set(-HALF - 1, 2, 0, ModBlocks.POSTER_BILLBOARD.get().defaultBlockState().setValue(PosterBlock.FACING, Direction.WEST));
        bp.set(HALF + 1, 2, 0, ModBlocks.POSTER_BILLBOARD.get().defaultBlockState().setValue(PosterBlock.FACING, Direction.EAST));

        final BlockPos pharmacistPos = f.at(0, 1, -6);
        final BlockPos sellerPos = f.at(-5, 1, 6);
        final float pharmacistYaw = f.yaw(Direction.SOUTH);
        final float sellerYaw = f.yaw(Direction.EAST);
        bp.after(l -> BuildHelper.spawnPharmacist(l, pharmacistPos, pharmacistYaw));
        bp.after(l -> BuildHelper.spawnShopkeeper(l, sellerPos, sellerYaw));

        ConstructionManager.build(level, bp, origin);
        if (player != null) {
            player.sendSystemMessage(Component.literal("Building Dr. Anant's Central Shop... medicines + Clinic Upgrades inside!").withStyle(ChatFormatting.GREEN));
        }
    }

    private CentralShopBuilder() {}
}
