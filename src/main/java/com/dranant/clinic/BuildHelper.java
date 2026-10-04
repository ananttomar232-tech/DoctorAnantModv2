package com.dranant.clinic;

import com.dranant.block.DiamondCashRegisterBlock;
import com.dranant.block.PosterBlock;
import com.dranant.block.entity.ItemDisplayPedestalBlockEntity;
import com.dranant.entity.AssistantEntity;
import com.dranant.entity.AssistantPharmacistEntity;
import com.dranant.entity.AssistantScientistEntity;
import com.dranant.entity.ShopkeeperEntity;
import com.dranant.registry.ModBlocks;
import com.dranant.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

/** Block-placing + NPC-spawning toolkit shared by the shop and clinic builders. */
public final class BuildHelper {
    public static final int FLAGS = Block.UPDATE_CLIENTS;

    private static final String[] PHARMACIST_NAMES = {"Raju", "Pappu", "Chintu", "Golu", "Bunty", "Monu", "Sonu", "Tinku"};
    private static final String[] SCIENTIST_NAMES = {"Dr. Vikram", "Dr. Sneha", "Dr. Kabir", "Dr. Ananya", "Dr. Rohan", "Dr. Pooja"};
    private static final String[] SELLER_NAMES = {"Seth Dhaniram", "Lala Kanjoosmal", "Seth Paisewala", "Munim Ji"};

    public static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, FLAGS);
    }

    /** Clears a volume to air (top-down, no drops) and removes loose items inside it. */
    public static void clear(ServerLevel level, BoundingBox box) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int y = box.maxY(); y >= box.minY(); y--)
            for (int x = box.minX(); x <= box.maxX(); x++)
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    m.set(x, y, z);
                    if (!level.getBlockState(m).isAir()) level.setBlock(m, air, FLAGS | Block.UPDATE_SUPPRESS_DROPS);
                }
        AABB aabb = new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1);
        for (Entity e : level.getEntitiesOfClass(ItemEntity.class, aabb)) e.discard();
        level.sendParticles(ParticleTypes.POOF, (box.minX() + box.maxX()) / 2.0, box.minY() + 2.0, (box.minZ() + box.maxZ()) / 2.0,
                60, box.getXSpan() / 3.0, 1.5, box.getZSpan() / 3.0, 0.02);
    }

    public static void placeRegister(ServerLevel level, BlockPos pos, Direction facing) {
        set(level, pos, ModBlocks.DIAMOND_CASH_REGISTER.get().defaultBlockState().setValue(DiamondCashRegisterBlock.FACING, facing));
    }

    /** Loads a pedestal that already exists in the world (shop stock is locked for survival players). */
    public static void stockPedestal(ServerLevel level, BlockPos pos, ItemStack display, long price, boolean shopStock) {
        if (level.getBlockEntity(pos) instanceof ItemDisplayPedestalBlockEntity pedestal) {
            pedestal.setDisplayedItem(display);
            pedestal.setDiamondPrice(price);
            pedestal.setShopStock(shopStock);
        }
    }

    /** Places a poster whose printed side faces {@code facing}; returns false if the needed cells are occupied. */
    public static boolean placePoster(ServerLevel level, BlockPos pos, Direction facing, PosterBlock poster, boolean force) {
        Direction side = facing.getClockWise();
        if (!force) {
            for (int w = 0; w < poster.getWidthBlocks(); w++)
                for (int h = 0; h < poster.getHeightBlocks(); h++) {
                    BlockPos cell = pos.relative(side, w).above(h);
                    if (!level.getBlockState(cell).isAir()) return false;
                    BlockPos wall = cell.relative(facing.getOpposite());
                    if (!level.getBlockState(wall).isSolidRender(level, wall)) return false;
                }
        }
        set(level, pos, poster.defaultBlockState().setValue(PosterBlock.FACING, facing));
        return true;
    }

    public static AssistantPharmacistEntity spawnPharmacist(ServerLevel level, BlockPos pos, float yaw) {
        AssistantPharmacistEntity e = ModEntities.ASSISTANT_PHARMACIST.get().create(level);
        if (e == null) return null;
        setup(level, e, pos, yaw, Component.literal(pick(PHARMACIST_NAMES, level.random) + " (Pharmacist)").withStyle(ChatFormatting.AQUA));
        return e;
    }

    public static AssistantScientistEntity spawnScientist(ServerLevel level, BlockPos pos, float yaw) {
        AssistantScientistEntity e = ModEntities.ASSISTANT_SCIENTIST.get().create(level);
        if (e == null) return null;
        setup(level, e, pos, yaw, Component.literal(pick(SCIENTIST_NAMES, level.random) + " (Scientist)").withStyle(ChatFormatting.GREEN));
        return e;
    }

    public static ShopkeeperEntity spawnShopkeeper(ServerLevel level, BlockPos pos, float yaw) {
        ShopkeeperEntity e = ModEntities.SHOPKEEPER.get().create(level);
        if (e == null) return null;
        setup(level, e, pos, yaw, Component.literal(pick(SELLER_NAMES, level.random) + " (Seller)").withStyle(ChatFormatting.GOLD));
        return e;
    }

    private static String pick(String[] names, RandomSource random) {
        return names[random.nextInt(names.length)];
    }

    private static void setup(ServerLevel level, AssistantEntity e, BlockPos pos, float yaw, Component name) {
        e.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, 0.0F);
        e.setYHeadRot(yaw);
        e.yBodyRot = yaw;
        e.setPost(pos);
        e.setCustomName(name);
        e.setCustomNameVisible(true);
        level.addFreshEntity(e);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 15, 0.3, 0.6, 0.3, 0.05);
    }

    private BuildHelper() {}
}
