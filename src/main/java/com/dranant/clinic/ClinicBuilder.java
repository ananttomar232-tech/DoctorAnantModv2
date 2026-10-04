package com.dranant.clinic;

import com.dranant.DoctorAnantMod;
import com.dranant.block.DiamondCashRegisterBlock;
import com.dranant.block.ExaminationBedBlock;
import com.dranant.block.PosterBlock;
import com.dranant.block.StretcherBlock;
import com.dranant.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Builds clinic tiers 1-6, 7 blocks in front of the player with the entrance facing the player, using an animated
 * layer-by-layer construction. Tiers 1-3 are procedural; tiers 4-6 use the attached .nbt structures (re-ordered by size).
 * If a structure file is missing or empty, a procedural clinic is built instead so a summoner never fails.
 */
public final class ClinicBuilder {
    public static final int DISTANCE = 7;

    private static final Set<String> GROUND_BLOCKS = Set.of("minecraft:dirt", "minecraft:grass_block", "minecraft:coarse_dirt",
            "minecraft:podzol", "minecraft:stone", "minecraft:gravel", "minecraft:sand", "minecraft:mud", "minecraft:rooted_dirt",
            "minecraft:dirt_path", "minecraft:mycelium", "minecraft:deepslate", "minecraft:andesite", "minecraft:diorite", "minecraft:granite");

    private static final BlockState[] ACCENTS = {
            Blocks.RED_CONCRETE.defaultBlockState(), Blocks.LIME_CONCRETE.defaultBlockState(), Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(),
            Blocks.ORANGE_CONCRETE.defaultBlockState(), Blocks.PURPLE_CONCRETE.defaultBlockState(), Blocks.CYAN_CONCRETE.defaultBlockState()};

    private static final Map<Integer, StructureInfo> INFO_CACHE = new HashMap<>();

    public record StructureInfo(int sizeX, int sizeY, int sizeZ, int blockCount, int groundLayer) {}

    /** Everything needed to place (or preview) a clinic in front of a player. */
    public record Plan(int tier, Direction look, Rotation rotation, BlockPos center, int groundY, int width, int depth, int height,
                       @Nullable StructureTemplate template, @Nullable StructureInfo info) {
        public BoundingBox footprint() {
            Frame f = new Frame(new BlockPos(center.getX(), groundY - 1, center.getZ()), rotation);
            int hw = width / 2, hd = depth / 2;
            return BoundingBox.fromCorners(f.at(-hw, 0, -hd), f.at(width - hw - 1, height, depth - hd - 1));
        }
    }

    // ================================================================== entry points
    public static Plan plan(ServerLevel level, Player player, int tier) {
        Direction look = player.getDirection();
        Rotation rotation = Frame.rotationFacing(look.getOpposite());
        StructureInfo info = readInfo(level, tier);
        StructureTemplate template = null;
        int w, d, h;
        if (info != null && info.blockCount() > 0) {
            Optional<StructureTemplate> t = level.getStructureManager().get(DoctorAnantMod.id(ClinicTiers.structureName(tier)));
            if (t.isPresent()) template = t.get();
        }
        if (template != null) {
            Vec3i size = template.getSize();
            w = size.getX();
            d = size.getZ();
            h = size.getY();
        } else {
            int[] dims = proceduralDims(tier);
            w = dims[0];
            d = dims[1];
            h = dims[2] + 1;
        }
        int groundY = player.blockPosition().getY();
        BlockPos center = player.blockPosition().relative(look, DISTANCE + d / 2);
        return new Plan(tier, look, rotation, new BlockPos(center.getX(), groundY, center.getZ()), groundY, w, d, h, template, info);
    }

    /** Builds the clinic for {@code tier} 7 blocks in front of {@code player}. */
    public static boolean summonInFront(ServerLevel level, Player player, int tier) {
        Plan plan = plan(level, player, tier);
        if (plan.template() != null && plan.info() != null) {
            buildFromTemplate(level, plan);
        } else {
            buildProcedural(level, plan);
        }
        player.sendSystemMessage(Component.literal("Building Dr. Anant's Clinic - Level " + tier + " (" + ClinicTiers.name(tier) + ")...")
                .withStyle(ChatFormatting.GREEN));
        return true;
    }

    // ================================================================== structure path (tiers 4-6)
    @Nullable
    private static StructureInfo readInfo(ServerLevel level, int tier) {
        if (INFO_CACHE.containsKey(tier)) return INFO_CACHE.get(tier);
        StructureInfo result = null;
        ResourceLocation file = DoctorAnantMod.id("structure/" + ClinicTiers.structureName(tier) + ".nbt");
        Optional<Resource> resource = level.getServer().getResourceManager().getResource(file);
        if (resource.isPresent()) {
            try (InputStream in = resource.get().open()) {
                CompoundTag tag = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
                ListTag size = tag.getList("size", Tag.TAG_INT);
                int sx = size.size() == 3 ? size.getInt(0) : 0;
                int sy = size.size() == 3 ? size.getInt(1) : 0;
                int sz = size.size() == 3 ? size.getInt(2) : 0;
                ListTag palette = tag.contains("palette", Tag.TAG_LIST) ? tag.getList("palette", Tag.TAG_COMPOUND)
                        : tag.contains("palettes", Tag.TAG_LIST) && !tag.getList("palettes", Tag.TAG_LIST).isEmpty()
                        ? tag.getList("palettes", Tag.TAG_LIST).getList(0) : new ListTag();
                ListTag blocks = tag.getList("blocks", Tag.TAG_COMPOUND);
                int[] groundPerLayer = new int[Math.max(1, sy)];
                for (int i = 0; i < blocks.size(); i++) {
                    CompoundTag b = blocks.getCompound(i);
                    int state = b.getInt("state");
                    ListTag p = b.getList("pos", Tag.TAG_INT);
                    if (p.size() != 3 || state < 0 || state >= palette.size()) continue;
                    int y = p.getInt(1);
                    if (y >= 0 && y < groundPerLayer.length && GROUND_BLOCKS.contains(palette.getCompound(state).getString("Name"))) {
                        groundPerLayer[y]++;
                    }
                }
                int area = Math.max(1, sx * sz);
                int ground = -1;
                for (int y = 0; y < groundPerLayer.length; y++) if (groundPerLayer[y] >= area * 0.3) ground = y;
                result = new StructureInfo(sx, sy, sz, blocks.size(), ground);
            } catch (Exception e) {
                DoctorAnantMod.LOGGER.error("Could not read {}", file, e);
            }
        }
        INFO_CACHE.put(tier, result);
        return result;
    }

    private static void buildFromTemplate(ServerLevel level, Plan plan) {
        StructureTemplate template = plan.template();
        StructureInfo info = plan.info();
        Vec3i size = template.getSize();
        int baseY = info.groundLayer() >= 0 ? plan.groundY() - 1 - info.groundLayer() : plan.groundY() - 1;
        BlockPos origin = new BlockPos(plan.center().getX() - size.getX() / 2, baseY, plan.center().getZ() - size.getZ() / 2);
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(plan.rotation())
                .setRotationPivot(new BlockPos(size.getX() / 2, 0, size.getZ() / 2))
                .setIgnoreEntities(true);
        BoundingBox box = template.getBoundingBox(settings, origin);
        if (box.maxY() >= plan.groundY()) {
            BuildHelper.clear(level, new BoundingBox(box.minX(), plan.groundY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()));
        }
        List<Consumer<ServerLevel>> after = new ArrayList<>();
        after.add(l -> installFacilities(l, box, plan));
        ConstructionManager.buildTemplate(level, template, settings, origin, box, after, plan.center());
    }

    /** Finds indoor floor space inside the placed structure and installs register, lab bed, stretchers, staff and posters. */
    private static void installFacilities(ServerLevel level, BoundingBox box, Plan plan) {
        Set<Long> floor = new HashSet<>();
        List<BlockPos> candidates = new ArrayList<>();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = box.minX() + 1; x < box.maxX(); x++)
            for (int z = box.minZ() + 1; z < box.maxZ(); z++)
                for (int y = Math.max(box.minY(), plan.groundY() - 2); y < box.maxY() - 1; y++) {
                    m.set(x, y, z);
                    if (isIndoorFloor(level, m, box.maxY())) {
                        BlockPos p = m.immutable();
                        floor.add(p.asLong());
                        candidates.add(p);
                    }
                }
        BlockPos c = plan.center();
        int lowestY = candidates.stream().mapToInt(BlockPos::getY).min().orElse(plan.groundY());
        candidates.sort(Comparator.comparingDouble(p -> Math.hypot(p.getX() - c.getX(), p.getZ() - c.getZ()) + (p.getY() - lowestY) * 3.0));

        Direction toPlayer = plan.look().getOpposite();
        BlockPos registerPos = null;
        Direction registerFacing = toPlayer;
        for (BlockPos cand : candidates) {
            Direction[] order = {toPlayer, toPlayer.getClockWise(), toPlayer.getCounterClockWise(), toPlayer.getOpposite()};
            for (Direction d : order) {
                if (run(floor, cand, d, 6) >= 5 && floor.contains(cand.relative(d.getOpposite()).asLong())) {
                    registerPos = cand;
                    registerFacing = d;
                    break;
                }
            }
            if (registerPos != null) break;
        }
        if (registerPos == null) {
            registerPos = new BlockPos(c.getX(), plan.groundY(), c.getZ()).relative(toPlayer, plan.depth() / 2 + 2);
            registerFacing = toPlayer;
        }
        BuildHelper.placeRegister(level, registerPos, registerFacing);
        BuildHelper.spawnPharmacist(level, registerPos.relative(registerFacing.getOpposite()), registerFacing.toYRot());

        Set<Long> reserved = new HashSet<>();
        for (int i = -1; i <= 8; i++) reserved.add(registerPos.relative(registerFacing, i).asLong());
        BlockPos bedFoot = null;
        Direction bedFacing = Direction.NORTH;
        for (BlockPos cand : candidates) {
            double dist = Math.sqrt(cand.distSqr(registerPos));
            if (dist < 5 || dist > 24 || reserved.contains(cand.asLong())) continue;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos head = cand.relative(d);
                if (!floor.contains(head.asLong()) || reserved.contains(head.asLong())) continue;
                if (run(floor, cand, d.getOpposite(), 4) >= 4 && floor.contains(head.relative(d.getClockWise()).asLong())) {
                    bedFoot = cand;
                    bedFacing = d;
                    break;
                }
            }
            if (bedFoot != null) break;
        }
        if (bedFoot == null) {
            bedFoot = registerPos.relative(registerFacing.getClockWise(), 6);
            bedFacing = registerFacing.getOpposite();
            BuildHelper.set(level, bedFoot.below(), Blocks.SMOOTH_QUARTZ.defaultBlockState());
            BuildHelper.set(level, bedFoot.relative(bedFacing).below(), Blocks.SMOOTH_QUARTZ.defaultBlockState());
        }
        placeBed(level, bedFoot, bedFacing);
        BlockPos head = bedFoot.relative(bedFacing);
        for (int i = 0; i < 6; i++) reserved.add(bedFoot.relative(bedFacing.getOpposite(), i).asLong());
        reserved.add(head.asLong());
        reserved.add(bedFoot.asLong());
        BlockPos scientistPos = head.relative(bedFacing.getClockWise());
        reserved.add(scientistPos.asLong());
        BuildHelper.spawnScientist(level, scientistPos, bedFacing.getCounterClockWise().toYRot());

        // two stretchers near the bed for waiting Line 2 patients
        int placed = 0;
        for (BlockPos cand : candidates) {
            if (placed >= 2) break;
            if (reserved.contains(cand.asLong()) || reserved.contains(cand.relative(bedFacing).asLong())) continue;
            if (cand.distSqr(head) > 64 || cand.distSqr(head) < 4) continue;
            if (!floor.contains(cand.relative(bedFacing).asLong())) continue;
            BuildHelper.set(level, cand, ModBlocks.STRETCHER.get().defaultBlockState().setValue(StretcherBlock.FACING, bedFacing));
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 2; dz++) reserved.add(cand.offset(dx, 0, dz).asLong());
            placed++;
        }
        placeOutsidePosters(level, box, plan.groundY());
    }

    private static void placeBed(ServerLevel level, BlockPos foot, Direction facing) {
        BlockState state = ModBlocks.EXAMINATION_BED.get().defaultBlockState().setValue(ExaminationBedBlock.FACING, facing);
        BuildHelper.set(level, foot, state.setValue(ExaminationBedBlock.PART, BedPart.FOOT));
        BuildHelper.set(level, foot.relative(facing), state.setValue(ExaminationBedBlock.PART, BedPart.HEAD));
    }

    private static boolean isIndoorFloor(ServerLevel level, BlockPos p, int maxY) {
        if (!level.getBlockState(p).isAir() || !level.getBlockState(p.above()).isAir()) return false;
        if (!level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) return false;
        for (int y = p.getY() + 2; y <= Math.min(maxY, p.getY() + 12); y++) {
            if (!level.getBlockState(new BlockPos(p.getX(), y, p.getZ())).isAir()) return true;
        }
        return false;
    }

    private static int run(Set<Long> floor, BlockPos start, Direction d, int max) {
        int n = 0;
        BlockPos p = start;
        for (int i = 0; i < max; i++) {
            p = p.relative(d);
            if (!floor.contains(p.asLong())) break;
            n++;
        }
        return n;
    }

    private static void placeOutsidePosters(ServerLevel level, BoundingBox box, int groundY) {
        PosterBlock[] posters = {ModBlocks.POSTER_LANDSCAPE.get(), ModBlocks.POSTER_SQUARE.get(), ModBlocks.POSTER_BILLBOARD.get(), ModBlocks.POSTER_LANDSCAPE.get()};
        Direction[] sides = {Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.NORTH};
        for (int s = 0; s < sides.length; s++) {
            Direction out = sides[s];
            int mid = out.getAxis() == Direction.Axis.Z ? (box.minX() + box.maxX()) / 2 : (box.minZ() + box.maxZ()) / 2;
            int[] offsets = {0, -3, 3, -6, 6, -9, 9};
            placed:
            for (int off : offsets) {
                for (int dy = 1; dy <= 3; dy++) {
                    int y = groundY + dy;
                    BlockPos start = switch (out) {
                        case SOUTH -> new BlockPos(mid + off, y, box.maxZ() + 1);
                        case NORTH -> new BlockPos(mid + off, y, box.minZ() - 1);
                        case EAST -> new BlockPos(box.maxX() + 1, y, mid + off);
                        default -> new BlockPos(box.minX() - 1, y, mid + off);
                    };
                    BlockPos cell = start;
                    int depth = (out.getAxis() == Direction.Axis.Z ? box.getZSpan() : box.getXSpan()) / 2;
                    for (int i = 0; i < depth; i++) {
                        BlockPos next = cell.relative(out.getOpposite());
                        if (level.getBlockState(next).isSolidRender(level, next)) {
                            if (BuildHelper.placePoster(level, cell, out, posters[s], false)) break placed;
                            break;
                        }
                        if (!level.getBlockState(next).isAir()) break;
                        cell = next;
                    }
                }
            }
        }
    }

    // ================================================================== procedural tiers 1-3
    /** width, depth, wall height (index by tier; tiers above 3 without a structure file reuse bigger sizes). */
    private static int[] proceduralDims(int tier) {
        return switch (tier) {
            case 1 -> new int[]{15, 13, 6};
            case 2 -> new int[]{21, 17, 7};
            case 3 -> new int[]{27, 21, 8};
            default -> new int[]{27 + (tier - 3) * 4, 21 + (tier - 3) * 2, 8};
        };
    }

    private static void buildProcedural(ServerLevel level, Plan plan) {
        int tier = plan.tier();
        int[] dims = proceduralDims(tier);
        int w = dims[0], d = dims[1], h = dims[2];
        int hw = w / 2, hd = d / 2;
        int wardWidth = tier <= 1 ? 7 : tier == 2 ? 8 : 10;
        int xWall = hw - wardWidth;
        int doorX = (-hw + xWall) / 2;
        int counterHalf = tier <= 1 ? 2 : 3;
        Frame f = new Frame(new BlockPos(plan.center().getX(), plan.groundY() - 1, plan.center().getZ()), plan.rotation());
        Blueprint bp = new Blueprint(f);
        bp.clearLocal(-hw - 2, 1, -hd - 2, hw + 2, h + 2, hd + 2);

        BlockState accent = ACCENTS[(tier - 1) % ACCENTS.length];
        BlockState white = Blocks.WHITE_CONCRETE.defaultBlockState();
        BlockState quartz = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        BlockState pillar = Blocks.QUARTZ_PILLAR.defaultBlockState();
        BlockState glass = Blocks.GLASS.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState wardFloor = Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState();

        // floor
        for (int x = -hw; x <= hw; x++)
            for (int z = -hd; z <= hd; z++) {
                boolean border = x == -hw || x == hw || z == -hd || z == hd;
                BlockState fl = border ? quartz : x > xWall ? (((x + z) & 1) == 0 ? wardFloor : white) : (((x + z) & 1) == 0 ? accent : white);
                bp.set(x, 0, z, fl);
            }
        // walls
        for (int y = 1; y < h; y++)
            for (int x = -hw; x <= hw; x++)
                for (int z = -hd; z <= hd; z++) {
                    boolean ex = x == -hw || x == hw, ez = z == -hd || z == hd;
                    if (!ex && !ez) continue;
                    boolean pil = (ex && ez) || (ez && (x + hw) % 4 == 0) || (ex && (z + hd) % 4 == 0);
                    bp.set(x, y, z, pil ? pillar : y == 1 ? accent : (y == 2 || y == 3) ? glass : white);
                }
        // roof with lights (+ parapet for bigger tiers)
        for (int x = -hw; x <= hw; x++)
            for (int z = -hd; z <= hd; z++) {
                boolean border = x == -hw || x == hw || z == -hd || z == hd;
                boolean light = !border && (x + hw) % 4 == 2 && (z + hd) % 4 == 2;
                bp.set(x, h, z, border ? accent : light ? Blocks.SEA_LANTERN.defaultBlockState() : quartz);
                if (tier >= 2 && border) bp.set(x, h + 1, z, Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState());
            }
        // main entrance + awning
        for (int x = doorX - 1; x <= doorX + 1; x++)
            for (int y = 1; y <= 3; y++) bp.set(x, y, hd, air);
        if (tier >= 2) {
            for (int x = doorX - 2; x <= doorX + 2; x++) bp.set(x, 4, hd + 1, ((x & 1) == 0 ? Blocks.RED_WOOL : Blocks.WHITE_WOOL).defaultBlockState());
        }
        // glass partition with a doorway near the front
        for (int z = -hd + 1; z < hd; z++)
            for (int y = 1; y < h; y++) {
                boolean doorway = z >= hd - 3 && z <= hd - 2 && y <= 3;
                bp.set(xWall, y, z, doorway ? air : y == 1 ? quartz : glass);
            }

        // reception counter + register (patients queue toward the entrance)
        int counterZ = -hd + 4;
        for (int x = doorX - counterHalf; x <= doorX + counterHalf; x++) {
            boolean end = x == doorX - counterHalf || x == doorX + counterHalf;
            bp.set(x, 1, counterZ, end ? pillar : Blocks.POLISHED_DIORITE.defaultBlockState());
        }
        bp.set(doorX, 2, counterZ, ModBlocks.DIAMOND_CASH_REGISTER.get().defaultBlockState().setValue(DiamondCashRegisterBlock.FACING, Direction.SOUTH));
        bp.set(doorX - 2, 2, counterZ, Blocks.BREWING_STAND.defaultBlockState());
        bp.set(doorX + 2, 2, counterZ, Blocks.POTTED_RED_TULIP.defaultBlockState());
        for (int x = -hw + 1; x < xWall; x++) {
            bp.set(x, 1, -hd + 1, Blocks.BOOKSHELF.defaultBlockState());
            bp.set(x, 2, -hd + 1, tier >= 2 && (x & 1) == 0 ? Blocks.CHISELED_BOOKSHELF.defaultBlockState() : Blocks.BOOKSHELF.defaultBlockState());
        }
        // waiting benches + plants
        BlockState bench = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
        for (int z = counterZ + 3; z < hd - 1; z += 2) bp.set(-hw + 1, 1, z, bench);
        bp.set(-hw + 1, 1, -hd + 2, Blocks.POTTED_FERN.defaultBlockState());
        bp.set(xWall - 1, 1, hd - 1, Blocks.POTTED_BAMBOO.defaultBlockState());

        // experimental ward: exam bed (head against the back wall), stretchers along the side wall
        int bedX = xWall + 2;
        BlockState bed = ModBlocks.EXAMINATION_BED.get().defaultBlockState().setValue(ExaminationBedBlock.FACING, Direction.NORTH);
        bp.set(bedX, 1, -hd + 1, bed.setValue(ExaminationBedBlock.PART, BedPart.HEAD));
        bp.set(bedX, 1, -hd + 2, bed.setValue(ExaminationBedBlock.PART, BedPart.FOOT));
        bp.set(xWall + 1, 1, -hd + 1, Blocks.BREWING_STAND.defaultBlockState());
        if (xWall + 4 < hw) bp.set(xWall + 4, 1, -hd + 1, Blocks.CAULDRON.defaultBlockState());
        for (int i = 0; i < Math.min(3, tier); i++) {
            int sz = -hd + 3 + i * 3;
            if (sz + 1 >= hd - 1) break;
            bp.set(hw - 1, 1, sz, ModBlocks.STRETCHER.get().defaultBlockState().setValue(StretcherBlock.FACING, Direction.SOUTH));
        }
        if (tier >= 3) {
            bp.set(xWall + 1, 1, hd - 1, Blocks.ENCHANTING_TABLE.defaultBlockState());
            for (int x = -hw + 2; x < xWall - 1; x += 3) bp.set(x, h - 1, 0, Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
        }
        // garden in front for bigger tiers
        if (tier >= 2) {
            for (int x = -hw; x <= hw; x++) {
                if (Math.abs(x - doorX) <= 2) continue;
                bp.set(x, 1, hd + 1, ((x & 1) == 0 ? Blocks.POPPY : Blocks.DANDELION).defaultBlockState());
            }
        }
        if (tier >= 3) {
            for (int side = -1; side <= 1; side += 2) {
                bp.set(doorX + side * 2, 1, hd + 2, Blocks.OAK_FENCE.defaultBlockState());
                bp.set(doorX + side * 2, 2, hd + 2, Blocks.LANTERN.defaultBlockState());
            }
        }
        // posters on the outer walls
        bp.set(doorX - 3, 3, hd + 1, ModBlocks.POSTER_LANDSCAPE.get().defaultBlockState().setValue(PosterBlock.FACING, Direction.SOUTH));
        bp.set(doorX + 2, 3, hd + 1, ModBlocks.POSTER_SQUARE.get().defaultBlockState().setValue(PosterBlock.FACING, Direction.SOUTH));
        bp.set(-hw - 1, 2, 0, ModBlocks.POSTER_BILLBOARD.get().defaultBlockState().setValue(PosterBlock.FACING, Direction.WEST));
        bp.set(hw + 1, 2, 0, ModBlocks.POSTER_BILLBOARD.get().defaultBlockState().setValue(PosterBlock.FACING, Direction.EAST));

        // staff arrive once the building is finished
        final BlockPos pharmacistPos = f.at(doorX, 1, -hd + 2);
        final BlockPos scientistPos = f.at(xWall + 3, 1, -hd + 2);
        final float pharmacistYaw = f.yaw(Direction.SOUTH);
        final float scientistYaw = f.yaw(Direction.WEST);
        bp.after(l -> BuildHelper.spawnPharmacist(l, pharmacistPos, pharmacistYaw));
        bp.after(l -> BuildHelper.spawnScientist(l, scientistPos, scientistYaw));
        ConstructionManager.build(level, bp, plan.center());
    }

    public static void clearCache() {
        INFO_CACHE.clear();
    }

    private ClinicBuilder() {}
}
