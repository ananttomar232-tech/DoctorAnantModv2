package com.dranant.clinic;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Animated construction: buildings rise layer by layer over ~2-4 seconds with block sounds and particles,
 * then finish with fireworks. Ticked from {@link com.dranant.event.ModGameEvents#onServerTick}.
 */
public final class ConstructionManager {
    private interface Task {
        /** @return true when finished */
        boolean tick(ServerLevel level);

        ResourceKey<Level> dimension();
    }

    private static final List<Task> TASKS = new ArrayList<>();

    public static synchronized void tick(MinecraftServer server) {
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            ServerLevel level = server.getLevel(t.dimension());
            if (level == null || t.tick(level)) it.remove();
        }
    }

    public static synchronized void clearAll() {
        TASKS.clear();
    }

    /** Animated placement of a blueprint (bottom-up). */
    public static synchronized void build(ServerLevel level, Blueprint blueprint, BlockPos fxCenter) {
        if (blueprint.clearBox() != null) BuildHelper.clear(level, blueprint.clearBox());
        List<Map.Entry<BlockPos, net.minecraft.world.level.block.state.BlockState>> entries = new ArrayList<>(blueprint.blocks().entrySet());
        entries.sort(Comparator.comparingInt(e -> e.getKey().getY()));
        TASKS.add(new BlueprintTask(level.dimension(), entries, blueprint.afterBuild(), fxCenter));
        level.playSound(null, fxCenter, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.2F);
    }

    /** Animated placement of a structure template, one horizontal slice at a time. */
    public static synchronized void buildTemplate(ServerLevel level, StructureTemplate template, StructurePlaceSettings settings,
                                                  BlockPos origin, BoundingBox box, List<Consumer<ServerLevel>> after, BlockPos fxCenter) {
        TASKS.add(new TemplateTask(level.dimension(), template, settings, origin, box, after, fxCenter));
        level.playSound(null, fxCenter, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.2F);
    }

    // ------------------------------------------------------------------ tasks
    private static final class BlueprintTask implements Task {
        private final ResourceKey<Level> dim;
        private final List<Map.Entry<BlockPos, BlockState>> entries;
        private final List<Consumer<ServerLevel>> after;
        private final BlockPos center;
        private final int perTick;
        private int index;
        private int ticks;

        BlueprintTask(ResourceKey<Level> dim, List<Map.Entry<BlockPos, BlockState>> entries, List<Consumer<ServerLevel>> after, BlockPos center) {
            this.dim = dim;
            this.entries = entries;
            this.after = after;
            this.center = center;
            this.perTick = Math.max(30, entries.size() / 60); // ~3 seconds
        }

        @Override
        public ResourceKey<Level> dimension() {
            return dim;
        }

        @Override
        public boolean tick(ServerLevel level) {
            ticks++;
            int end = Math.min(entries.size(), index + perTick);
            for (int i = index; i < end; i++) {
                Map.Entry<BlockPos, BlockState> e = entries.get(i);
                level.setBlock(e.getKey(), e.getValue(), Block.UPDATE_CLIENTS);
            }
            if (end > index) {
                BlockPos fx = entries.get(index + level.random.nextInt(end - index)).getKey();
                level.sendParticles(ParticleTypes.CLOUD, fx.getX() + 0.5, fx.getY() + 1.0, fx.getZ() + 0.5, 3, 0.4, 0.2, 0.4, 0.01);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, fx.getX() + 0.5, fx.getY() + 1.0, fx.getZ() + 0.5, 2, 0.5, 0.3, 0.5, 0.0);
                if (ticks % 3 == 0) {
                    float progress = end / (float) entries.size();
                    level.playSound(null, fx, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.8F, 0.7F + progress * 0.9F);
                }
            }
            index = end;
            if (index >= entries.size()) {
                for (Consumer<ServerLevel> a : after) a.accept(level);
                finale(level, center);
                return true;
            }
            return false;
        }
    }

    private static final class TemplateTask implements Task {
        private final ResourceKey<Level> dim;
        private final StructureTemplate template;
        private final StructurePlaceSettings settings;
        private final BlockPos origin;
        private final BoundingBox box;
        private final List<Consumer<ServerLevel>> after;
        private final BlockPos center;
        private final int interval;
        private int y;
        private int ticks;

        TemplateTask(ResourceKey<Level> dim, StructureTemplate template, StructurePlaceSettings settings, BlockPos origin,
                     BoundingBox box, List<Consumer<ServerLevel>> after, BlockPos center) {
            this.dim = dim;
            this.template = template;
            this.settings = settings;
            this.origin = origin;
            this.box = box;
            this.after = after;
            this.center = center;
            this.y = box.minY();
            this.interval = box.getYSpan() > 30 ? 1 : 3;
        }

        @Override
        public ResourceKey<Level> dimension() {
            return dim;
        }

        @Override
        public boolean tick(ServerLevel level) {
            if (++ticks % interval != 0) return false;
            settings.setBoundingBox(new BoundingBox(box.minX(), y, box.minZ(), box.maxX(), y, box.maxZ()));
            template.placeInWorld(level, origin, origin, settings, level.getRandom(), Block.UPDATE_CLIENTS);
            for (int i = 0; i < 6; i++) {
                double px = box.minX() + level.random.nextDouble() * box.getXSpan();
                double pz = box.minZ() + level.random.nextDouble() * box.getZSpan();
                level.sendParticles(ParticleTypes.CLOUD, px, y + 1.0, pz, 2, 0.3, 0.1, 0.3, 0.01);
            }
            float progress = (y - box.minY()) / (float) Math.max(1, box.getYSpan());
            level.playSound(null, new BlockPos(center.getX(), y, center.getZ()), SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 0.7F + progress * 0.9F);
            y++;
            if (y > box.maxY()) {
                settings.setBoundingBox(null);
                for (Consumer<ServerLevel> a : after) a.accept(level);
                finale(level, center);
                return true;
            }
            return false;
        }
    }

    private static void finale(ServerLevel level, BlockPos center) {
        level.playSound(null, center, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.2F, 0.9F);
        level.playSound(null, center, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.playSound(null, center, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 0.8F, 1.0F);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, center.getX() + 0.5, center.getY() + 4.0, center.getZ() + 0.5, 200, 6.0, 3.0, 6.0, 0.4);
        int[][] colors = {{0xFF3030, 0xFFD700}, {0x30FF80, 0xFFFFFF}, {0x30C0FF, 0xFF60FF}};
        for (int i = 0; i < 3; i++) {
            ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
            FireworkExplosion explosion = new FireworkExplosion(FireworkExplosion.Shape.LARGE_BALL,
                    IntList.of(colors[i][0], colors[i][1]), IntList.of(0xFFFFFF), true, true);
            rocket.set(DataComponents.FIREWORKS, new Fireworks(1, List.of(explosion)));
            double ox = (i - 1) * 4.0;
            FireworkRocketEntity firework = new FireworkRocketEntity(level, center.getX() + 0.5 + ox, center.getY() + 2.0, center.getZ() + 0.5, rocket);
            level.addFreshEntity(firework);
        }
    }

    private ConstructionManager() {}
}
