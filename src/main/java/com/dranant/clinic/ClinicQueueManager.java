package com.dranant.clinic;

import com.dranant.block.DiamondCashRegisterBlock;
import com.dranant.block.ExaminationBedBlock;
import com.dranant.block.entity.DiamondCashRegisterBlockEntity;
import com.dranant.block.entity.ExaminationBedBlockEntity;
import com.dranant.config.DrAnantConfig;
import com.dranant.dialogue.Lines;
import com.dranant.dialogue.Speech;
import com.dranant.entity.AssistantPharmacistEntity;
import com.dranant.entity.AssistantScientistEntity;
import com.dranant.entity.PatientVillagerEntity;
import com.dranant.registry.ModBlocks;
import com.dranant.registry.ModEntities;
import com.dranant.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Dual-queue clinic AI (v2, built on {@link PatientVillagerEntity} so the lines always form properly).
 * <ul>
 *   <li>Villagers pick the CLOSEST clinic. Nearby vanilla villagers are converted into patients (and restored later).</li>
 *   <li>Line 1 (Counter): neat straight line in front of the register, 1.2 blocks apart. At the counter the patient
 *       explains the problem, haggles with the pharmacist, pays 1-5 diamonds and leaves healthy.</li>
 *   <li>Line 2 (Lab): severe patients lie on stretchers (or stand in line) until the Examination Bed is free.</li>
 * </ul>
 */
public final class ClinicQueueManager {
    public static final String INCOME_TAG = "dranant_income";
    private static final double SPACING = 1.2D;
    private static final double FIRST_SPOT = 1.4D;

    public static void tick(ServerLevel level, BlockPos pos, BlockState state, DiamondCashRegisterBlockEntity reg) {
        long now = level.getGameTime();
        if ((now + (pos.asLong() & 3L)) % 5L != 0L) return;
        Direction facing = state.getValue(DiamondCashRegisterBlock.FACING);

        if (now >= reg.nextFacilityScan) {
            reg.nextFacilityScan = now + 100L;
            ClinicRegistry.add(level, pos); // safety net in case onLoad was not fired for a freshly built register
            BlockPos bed = findBed(level, pos);
            reg.setBedHead(bed);
            reg.getStretchers().clear();
            if (bed != null) reg.getStretchers().addAll(findStretchers(level, bed));
        }
        BlockPos bedHead = reg.getBedHead();
        if (bedHead != null && !level.getBlockState(bedHead).is(ModBlocks.EXAMINATION_BED.get())) {
            reg.setBedHead(null);
            bedHead = null;
        }

        prune(level, reg.getLine1(), pos, 1);
        prune(level, reg.getLine2(), pos, 2);
        if (now % 40L < 5L) reattach(level, pos, reg, bedHead);
        if (now >= reg.nextConversion) {
            reg.nextConversion = now + 60L;
            convertNearbyVillager(level, pos, reg, bedHead);
        }
        if (DrAnantConfig.SPAWN_CUSTOMERS.get() && now >= reg.nextCustomerSpawn) {
            reg.nextCustomerSpawn = now + DrAnantConfig.CUSTOMER_SPAWN_INTERVAL.get();
            if (reg.getLine1().size() + reg.getLine2().size() < DrAnantConfig.MAX_QUEUE_LENGTH.get() * 2) {
                spawnCustomer(level, pos);
            }
        }

        // ---------------- Line 1
        Vec3 registerCenter = Vec3.atCenterOf(pos);
        List<UUID> line1 = reg.getLine1();
        for (int i = 0; i < line1.size(); i++) {
            PatientVillagerEntity p = patient(level, line1.get(i));
            if (p != null) p.setQueueTarget(line1Spot(level, pos, facing, i), registerCenter);
        }
        runCounter(level, pos, reg);

        // ---------------- Line 2
        if (bedHead != null) {
            runLab(level, pos, reg, bedHead);
        } else if (!reg.getLine2().isEmpty()) {
            for (UUID id : reg.getLine2()) {
                PatientVillagerEntity p = patient(level, id);
                if (p != null) {
                    p.setLine(1);
                    line1.add(id);
                }
            }
            reg.getLine2().clear();
        }
    }

    // ------------------------------------------------------------------ counter (Line 1)
    private static void runCounter(ServerLevel level, BlockPos pos, DiamondCashRegisterBlockEntity reg) {
        List<UUID> line1 = reg.getLine1();
        if (line1.isEmpty()) {
            reg.counterPatient = null;
            return;
        }
        PatientVillagerEntity front = patient(level, line1.get(0));
        if (front == null) return;
        if (reg.counterPatient == null || !reg.counterPatient.equals(front.getUUID())) {
            if (!front.isAtTarget()) return;
            AssistantPharmacistEntity pharmacist = pharmacist(level, pos);
            if (pharmacist == null) {
                if (level.getGameTime() - reg.lastNoStaffComplaint > 200L) {
                    reg.lastNoStaffComplaint = level.getGameTime();
                    Speech.say(front, Lines.pick(Lines.NO_PHARMACIST, level.random), Speech.STYLE_PATIENT);
                }
                return;
            }
            reg.counterPatient = front.getUUID();
            reg.counterTicks = 0;
            front.startCounter();
        }
        AssistantPharmacistEntity pharmacist = pharmacist(level, pos);
        if (pharmacist == null) return;
        pharmacist.getLookControl().setLookAt(front, 30.0F, 30.0F);
        reg.counterTicks += 5;
        int t = reg.counterTicks;
        if (t == 5) {
            Speech.say(front, front.getProblemText(), Speech.STYLE_PATIENT);
        } else if (t == 45) {
            Speech.say(pharmacist, Lines.pick(Lines.PHARMACIST_ADVICE, level.random), Speech.STYLE_STAFF);
        } else if (t == 85) {
            Speech.say(front, Lines.pick(Lines.HAGGLE, level.random), Speech.STYLE_PATIENT);
            front.playSound(SoundEvents.VILLAGER_AMBIENT, 1.0F, 1.1F);
        } else if (t == 125) {
            Speech.say(pharmacist, Lines.pick(Lines.PHARMACIST_REPLY, level.random), Speech.STYLE_STAFF);
        } else if (t == 165) {
            Speech.say(front, Lines.pick(Lines.DEAL, level.random), Speech.STYLE_PATIENT);
            ItemStack medicine = new ItemStack(ModItems.MEDICINES.get(level.random.nextInt(ModItems.MEDICINES.size())).get());
            pharmacist.serveCustomer(front, medicine);
            front.setItemSlot(EquipmentSlot.MAINHAND, medicine.copy());
            reg.addDiamonds(1 + level.random.nextInt(5)); // 1-5 diamonds straight into the register / bank
            level.playSound(null, front.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
        } else if (t >= 190) {
            Speech.say(front, Lines.pick(Lines.CURED_MILD, level.random), Speech.STYLE_HAPPY);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, front.getX(), front.getY() + 1.9D, front.getZ(), 12, 0.3D, 0.3D, 0.3D, 0.05D);
            line1.remove(0);
            front.leave(true);
            reg.counterPatient = null;
            reg.counterTicks = 0;
            reg.setChanged();
        }
    }

    // ------------------------------------------------------------------ lab (Line 2)
    private static void runLab(ServerLevel level, BlockPos pos, DiamondCashRegisterBlockEntity reg, BlockPos bedHead) {
        List<UUID> line2 = reg.getLine2();
        if (line2.isEmpty()) return;
        Direction bedFacing = level.getBlockState(bedHead).getValue(ExaminationBedBlock.FACING);

        // call the longest-waiting patient to the bed when bed + scientist are free
        boolean bedFree = true;
        if (level.getBlockEntity(bedHead) instanceof ExaminationBedBlockEntity bed && bed.getPatient() != null) {
            Entity current = level.getEntity(bed.getPatient());
            if (current != null && current.isAlive()) bedFree = false;
            else bed.setPatient(null);
        }
        if (bedFree) {
            PatientVillagerEntity first = patient(level, line2.get(0));
            AssistantScientistEntity scientist = freeScientist(level, bedHead);
            if (first != null && scientist != null && first.isAtTarget()) {
                line2.remove(0);
                scientist.beginTreatment(first, bedHead, pos);
                reg.setChanged();
            }
        }

        // stretchers are kept by whoever is lying on them; free ones go to the next standing patients
        List<BlockPos> stretchers = new ArrayList<>(reg.getStretchers());
        stretchers.removeIf(s -> !level.getBlockState(s).is(ModBlocks.STRETCHER.get()));
        Set<BlockPos> taken = new HashSet<>();
        Map<UUID, BlockPos> assigned = new HashMap<>();
        for (UUID id : line2) {
            PatientVillagerEntity p = patient(level, id);
            if (p == null) continue;
            BlockPos s = p.getStretcher();
            if (s != null && stretchers.contains(s) && !taken.contains(s)) {
                taken.add(s);
                assigned.put(id, s);
            }
        }
        int standing = 0;
        for (UUID id : line2) {
            PatientVillagerEntity p = patient(level, id);
            if (p == null) continue;
            BlockPos s = assigned.get(id);
            if (s == null) {
                for (BlockPos free : stretchers) {
                    if (!taken.contains(free)) {
                        s = free;
                        taken.add(free);
                        break;
                    }
                }
            }
            if (s != null) {
                p.setStretcherTarget(s);
            } else {
                p.setQueueTarget(line2Spot(level, bedHead, bedFacing, standing++), Vec3.atCenterOf(bedHead));
            }
        }
    }

    // ------------------------------------------------------------------ geometry
    public static Vec3 line1Spot(ServerLevel level, BlockPos register, Direction facing, int index) {
        double dist = FIRST_SPOT + SPACING * index;
        double x = register.getX() + 0.5D + facing.getStepX() * dist;
        double z = register.getZ() + 0.5D + facing.getStepZ() * dist;
        return new Vec3(x, standY(level, x, z, register.getY()), z);
    }

    public static Vec3 line2Spot(ServerLevel level, BlockPos bedHead, Direction bedFacing, int index) {
        Direction out = bedFacing.getOpposite();
        BlockPos foot = bedHead.relative(out);
        double dist = FIRST_SPOT - 0.2D + SPACING * index;
        double x = foot.getX() + 0.5D + out.getStepX() * dist;
        double z = foot.getZ() + 0.5D + out.getStepZ() * dist;
        return new Vec3(x, standY(level, x, z, bedHead.getY()), z);
    }

    private static double standY(ServerLevel level, double x, double z, int startY) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int bx = Mth.floor(x), bz = Mth.floor(z);
        for (int y = startY + 2; y >= startY - 4; y--) {
            m.set(bx, y, bz);
            BlockPos above = m.above();
            BlockPos below = m.below();
            if (level.getBlockState(m).getCollisionShape(level, m).isEmpty()
                    && level.getBlockState(above).getCollisionShape(level, above).isEmpty()
                    && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                return y;
            }
        }
        return startY;
    }

    // ------------------------------------------------------------------ bookkeeping
    private static void prune(ServerLevel level, List<UUID> line, BlockPos clinic, int lineId) {
        Iterator<UUID> it = line.iterator();
        while (it.hasNext()) {
            PatientVillagerEntity p = patient(level, it.next());
            if (p == null || !p.isAlive() || !p.isQueued() || p.getLine() != lineId || !clinic.equals(p.getClinic())) {
                it.remove();
            }
        }
    }

    /** Re-links queued patients that are not in a list (e.g. after a world reload). */
    private static void reattach(ServerLevel level, BlockPos pos, DiamondCashRegisterBlockEntity reg, @Nullable BlockPos bedHead) {
        Set<UUID> known = new HashSet<>(reg.getLine1());
        known.addAll(reg.getLine2());
        for (PatientVillagerEntity p : level.getEntitiesOfClass(PatientVillagerEntity.class, new AABB(pos).inflate(64.0D),
                e -> pos.equals(e.getClinic()) && e.isQueued())) {
            if (known.contains(p.getUUID())) continue;
            if (p.getLine() == 2 && bedHead != null) reg.getLine2().add(p.getUUID());
            else {
                p.setLine(1);
                reg.getLine1().add(p.getUUID());
            }
        }
    }

    private static boolean addPatient(DiamondCashRegisterBlockEntity reg, PatientVillagerEntity p, BlockPos pos, @Nullable BlockPos bedHead) {
        int max = DrAnantConfig.MAX_QUEUE_LENGTH.get();
        boolean severe = bedHead != null && p.getRandom().nextDouble() >= DrAnantConfig.COUNTER_QUEUE_CHANCE.get();
        List<UUID> line = severe ? reg.getLine2() : reg.getLine1();
        if (line.size() >= max) {
            severe = !severe && bedHead != null;
            line = severe ? reg.getLine2() : reg.getLine1();
            if (line.size() >= max) return false;
        }
        p.assign(pos, severe ? 2 : 1, severe);
        line.add(p.getUUID());
        reg.setChanged();
        return true;
    }

    /** Converts ONE nearby vanilla villager (whose closest clinic is this one) into a patient. */
    private static void convertNearbyVillager(ServerLevel level, BlockPos pos, DiamondCashRegisterBlockEntity reg, @Nullable BlockPos bedHead) {
        int max = DrAnantConfig.MAX_QUEUE_LENGTH.get();
        if (reg.getLine1().size() >= max && (bedHead == null || reg.getLine2().size() >= max)) return;
        int radius = DrAnantConfig.DETECTION_RADIUS.get();
        for (Villager v : level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(radius),
                v -> v.isAlive() && !v.isBaby() && !v.isSleeping() && !v.isTrading() && !v.isNoAi()
                        && !v.getPersistentData().contains("dranant_visited"))) {
            if (level.getGameTime() < v.getPersistentData().getLong("dranant_cooldown")) continue;
            BlockPos nearest = ClinicRegistry.nearest(level, v.blockPosition(), radius);
            if (nearest == null || !nearest.equals(pos)) continue;
            PatientVillagerEntity p = ModEntities.PATIENT_VILLAGER.get().create(level);
            if (p == null) return;
            v.getPersistentData().putLong("dranant_cooldown", level.getGameTime() + DrAnantConfig.SERVED_COOLDOWN.get());
            p.copyFrom(v);
            v.discard();
            level.addFreshEntity(p);
            if (!addPatient(reg, p, pos, bedHead)) p.leave(false);
            return;
        }
    }

    /** A brand-new patient walks in from 18-26 blocks away and goes to whichever clinic is CLOSEST to it. */
    private static void spawnCustomer(ServerLevel level, BlockPos clinic) {
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double dist = 18.0D + level.random.nextDouble() * 8.0D;
            int x = Mth.floor(clinic.getX() + Math.cos(angle) * dist);
            int z = Mth.floor(clinic.getZ() + Math.sin(angle) * dist);
            if (!level.isLoaded(new BlockPos(x, clinic.getY(), z))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (Math.abs(y - clinic.getY()) > 12) continue;
            BlockPos spawn = new BlockPos(x, y, z);
            if (!level.getFluidState(spawn.below()).isEmpty() || !level.getBlockState(spawn).getCollisionShape(level, spawn).isEmpty()) continue;
            BlockPos target = ClinicRegistry.nearest(level, spawn, 96.0D);
            if (target == null) target = clinic;
            if (!(level.getBlockEntity(target) instanceof DiamondCashRegisterBlockEntity reg)) return;
            PatientVillagerEntity p = ModEntities.PATIENT_VILLAGER.get().create(level);
            if (p == null) return;
            p.moveTo(x + 0.5D, y, z + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
            VillagerType type = VillagerType.byBiome(level.getBiome(spawn));
            p.randomizeLook(BuiltInRegistries.VILLAGER_TYPE.getKey(type).getPath());
            level.addFreshEntity(p);
            if (!addPatient(reg, p, target, reg.getBedHead())) p.leave(false);
            return;
        }
    }

    /** Clears every patient belonging to a destroyed register. */
    public static void releaseAll(ServerLevel level, BlockPos clinic) {
        for (PatientVillagerEntity p : level.getEntitiesOfClass(PatientVillagerEntity.class, new AABB(clinic).inflate(96.0D),
                e -> clinic.equals(e.getClinic()))) {
            p.leave(false);
        }
    }

    // ------------------------------------------------------------------ lookups
    @Nullable
    private static AssistantPharmacistEntity pharmacist(ServerLevel level, BlockPos pos) {
        List<AssistantPharmacistEntity> list = level.getEntitiesOfClass(AssistantPharmacistEntity.class, new AABB(pos).inflate(10.0D));
        return list.isEmpty() ? null : list.get(0);
    }

    @Nullable
    private static AssistantScientistEntity freeScientist(ServerLevel level, BlockPos bedHead) {
        for (AssistantScientistEntity s : level.getEntitiesOfClass(AssistantScientistEntity.class, new AABB(bedHead).inflate(14.0D))) {
            if (!s.isBusy()) return s;
        }
        return null;
    }

    @Nullable
    public static BlockPos findBed(ServerLevel level, BlockPos center) {
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -28; dx <= 28; dx++) {
            for (int dz = -28; dz <= 28; dz++) {
                for (int dy = -6; dy <= 6; dy++) {
                    m.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState s = level.getBlockState(m);
                    if (!s.is(ModBlocks.EXAMINATION_BED.get()) || s.getValue(ExaminationBedBlock.PART) != BedPart.HEAD) continue;
                    double score = m.distSqr(center);
                    if (!level.getEntitiesOfClass(AssistantScientistEntity.class, new AABB(m).inflate(6.0D)).isEmpty()) score -= 10000.0D;
                    if (score < bestScore) {
                        bestScore = score;
                        best = m.immutable();
                    }
                }
            }
        }
        return best;
    }

    private static List<BlockPos> findStretchers(ServerLevel level, BlockPos bedHead) {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(bedHead.offset(-14, -3, -14), bedHead.offset(14, 3, 14))) {
            if (level.getBlockState(p).is(ModBlocks.STRETCHER.get())) out.add(p.immutable());
        }
        out.sort((a, b) -> Double.compare(a.distSqr(bedHead), b.distSqr(bedHead)));
        return out;
    }

    @Nullable
    private static PatientVillagerEntity patient(ServerLevel level, UUID id) {
        Entity e = level.getEntity(id);
        return e instanceof PatientVillagerEntity p ? p : null;
    }

    private ClinicQueueManager() {}
}
