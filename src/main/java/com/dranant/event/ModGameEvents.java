package com.dranant.event;

import com.dranant.DoctorAnantMod;
import com.dranant.block.entity.ItemDisplayPedestalBlockEntity;
import com.dranant.clinic.ClinicBuilder;
import com.dranant.clinic.ClinicRegistry;
import com.dranant.clinic.ConstructionManager;
import com.dranant.config.DrAnantConfig;
import com.dranant.counter.DiamondBank;
import com.dranant.counter.DiamondFx;
import com.dranant.dialogue.Lines;
import com.dranant.dialogue.Speech;
import com.dranant.entity.AssistantScientistEntity;
import com.dranant.entity.MutantBloodBeastEntity;
import com.dranant.entity.PatientVillagerEntity;
import com.dranant.item.ClinicSummonerItem;
import com.dranant.registry.ModBlocks;
import com.dranant.registry.ModEffects;
import com.dranant.registry.ModEntities;
import com.dranant.registry.ModItems;
import com.dranant.shop.PreviewManager;
import com.dranant.util.TimedEntityRemover;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Game-bus handlers: counter commands, bank deposits, summoner left-click, cure tools, outbreak, coat, immortality. */
@EventBusSubscriber(modid = DoctorAnantMod.MODID)
public final class ModGameEvents {
    private static final Map<UUID, Long> LEFT_CLICK_COOLDOWN = new HashMap<>();

    // ------------------------------------------------------------------ server tick: bank, construction, previews
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        DiamondBank.get(server).tick(server);
        ConstructionManager.tick(server);
        PreviewManager.tick(server);
    }

    // ------------------------------------------------------------------ Diamond Counter chat commands
    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        String msg = event.getRawText().trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        ServerPlayer player = event.getPlayer();
        MinecraftServer server = player.getServer();
        if (server == null) return;
        DiamondBank bank = DiamondBank.get(server);
        switch (msg) {
            case "#run diamond counter" -> {
                event.setCanceled(true);
                bank.setRunning(server, true);
                server.getPlayerList().broadcastSystemMessage(Component.literal("[Dr. Anant] Diamond Counter RUNNING - earnings are auto-deposited!").withStyle(ChatFormatting.AQUA), false);
            }
            case "#stop diamond counter" -> {
                event.setCanceled(true);
                bank.setRunning(server, false);
                server.getPlayerList().broadcastSystemMessage(Component.literal("[Dr. Anant] Diamond Counter PAUSED").withStyle(ChatFormatting.GRAY), false);
            }
            case "#reset diamond counter" -> {
                event.setCanceled(true);
                bank.reset(server);
                server.getPlayerList().broadcastSystemMessage(Component.literal("[Dr. Anant] Diamond Counter reset to 0").withStyle(ChatFormatting.YELLOW), false);
            }
            default -> {
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("diamondcounter")
                .then(Commands.literal("run").executes(ctx -> {
                    MinecraftServer s = ctx.getSource().getServer();
                    DiamondBank.get(s).setRunning(s, true);
                    ctx.getSource().sendSuccess(() -> Component.literal("Diamond Counter running"), true);
                    return 1;
                }))
                .then(Commands.literal("stop").executes(ctx -> {
                    MinecraftServer s = ctx.getSource().getServer();
                    DiamondBank.get(s).setRunning(s, false);
                    ctx.getSource().sendSuccess(() -> Component.literal("Diamond Counter paused"), true);
                    return 1;
                }))
                .then(Commands.literal("reset").requires(src -> src.hasPermission(2)).executes(ctx -> {
                    MinecraftServer s = ctx.getSource().getServer();
                    DiamondBank.get(s).reset(s);
                    ctx.getSource().sendSuccess(() -> Component.literal("Diamond Counter reset"), true);
                    return 1;
                }))
                .then(Commands.literal("set").requires(src -> src.hasPermission(2))
                        .then(Commands.argument("amount", LongArgumentType.longArg(0L)).executes(ctx -> {
                            MinecraftServer s = ctx.getSource().getServer();
                            long amount = LongArgumentType.getLong(ctx, "amount");
                            DiamondBank.get(s).set(s, amount);
                            ctx.getSource().sendSuccess(() -> Component.literal(String.format("Diamond Counter set to %,d", amount)), true);
                            return 1;
                        }))));
        dispatcher.register(Commands.literal("pedestalprice").requires(src -> src.hasPermission(2))
                .then(Commands.argument("amount", LongArgumentType.longArg(0L)).executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    HitResult hit = player.pick(6.0D, 1.0F, false);
                    if (hit instanceof BlockHitResult bhr && player.level().getBlockEntity(bhr.getBlockPos()) instanceof ItemDisplayPedestalBlockEntity pedestal) {
                        long amount = LongArgumentType.getLong(ctx, "amount");
                        pedestal.setDiamondPrice(amount);
                        ctx.getSource().sendSuccess(() -> Component.literal(String.format("Pedestal price set to %,d Diamonds", amount)), false);
                        return 1;
                    }
                    ctx.getSource().sendFailure(Component.literal("Look at an Item Display Pedestal first."));
                    return 0;
                })));
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && sp.getServer() != null) {
            DiamondBank.get(sp.getServer()).sendTo(sp);
        }
    }

    /** While the counter runs, picked-up diamonds vanish from the inventory and are counted in the Diamond Bank. */
    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Post event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide || !DrAnantConfig.AUTO_DEPOSIT.get()) return;
        ItemStack original = event.getOriginalStack();
        if (!original.is(Items.DIAMOND)) return;
        int picked = original.getCount() - event.getCurrentStack().getCount();
        if (picked <= 0) return;
        ServerLevel level = (ServerLevel) player.level();
        DiamondBank bank = DiamondBank.get(level.getServer());
        if (!bank.isRunning()) return;
        int removed = player.getInventory().clearOrCountMatchingItems(s -> s.is(Items.DIAMOND), picked, player.inventoryMenu.getCraftSlots());
        if (removed > 0) {
            bank.earn(level.getServer(), removed);
            DiamondFx.burst(level, player.getX(), player.getY() + 1.0, player.getZ());
        }
    }

    // ------------------------------------------------------------------ summoner: left-click = place block only
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ClinicSummonerItem summoner)) return;
        event.setCanceled(true);
        Level level = event.getLevel();
        Player player = event.getEntity();
        if (level.isClientSide || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        long now = level.getGameTime();
        Long last = LEFT_CLICK_COOLDOWN.get(player.getUUID());
        if (last != null && now - last < 6) return;
        LEFT_CLICK_COOLDOWN.put(player.getUUID(), now);
        Direction face = event.getFace() != null ? event.getFace() : Direction.UP;
        BlockPos target = event.getPos().relative(face);
        BlockState existing = level.getBlockState(target);
        if (!existing.canBeReplaced()) return;
        level.setBlock(target, summoner.getBlock().defaultBlockState(), 3);
        level.playSound(null, target, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.displayClientMessage(Component.literal("Placed the Level " + summoner.getTier() + " block. Right-click it to build the clinic!")
                .withStyle(ChatFormatting.GRAY), true);
    }

    // ------------------------------------------------------------------ cure tools + expired serum
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        Entity target = event.getTarget();
        Player player = event.getEntity();
        Level level = event.getLevel();

        if (target instanceof MutantBloodBeastEntity beast) {
            if (stack.is(ModItems.TITANIUM_HANDCUFFS.get())) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
                if (level.isClientSide) return;
                if (beast.isHandcuffed()) {
                    player.displayClientMessage(Component.literal("Already handcuffed! Use the Universal Cure Syringe now!").withStyle(ChatFormatting.YELLOW), true);
                } else if (!beast.canBeHandcuffed()) {
                    int pct = Math.round(beast.getHealth() / beast.getMaxHealth() * 100.0F);
                    player.displayClientMessage(Component.literal("Too strong (" + pct + "% HP)! Weaken it below 50% first.").withStyle(ChatFormatting.RED), true);
                    level.playSound(null, beast.blockPosition(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 1.0F, 0.6F);
                } else {
                    beast.applyHandcuffs(player);
                    stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(event.getHand()));
                    player.displayClientMessage(Component.literal("HANDCUFFED for 30 seconds! Inject the cure!").withStyle(ChatFormatting.GOLD), true);
                }
                return;
            }
            if (stack.is(ModItems.UNIVERSAL_CURE_SYRINGE.get())) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
                if (level.isClientSide) return;
                if (!beast.isHandcuffed()) {
                    player.displayClientMessage(Component.literal("It's too dangerous! Handcuff the beast first.").withStyle(ChatFormatting.RED), true);
                } else if (!beast.isBeingCured()) {
                    beast.startCure(player);
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    player.displayClientMessage(Component.literal("Universal Cure injected!").withStyle(ChatFormatting.GREEN), true);
                }
                return;
            }
        }

        if ((target instanceof Villager || target instanceof PatientVillagerEntity) && stack.is(ModItems.EXPIRED_EXPERIMENTAL_SERUM.get())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
            if (level.isClientSide) return;
            ServerLevel serverLevel = (ServerLevel) level;
            LivingEntity victim = (LivingEntity) target;
            if (!isLabPatient(serverLevel, victim)) {
                player.displayClientMessage(Component.literal("Give it to a patient on the Examination Bed or a stretcher...").withStyle(ChatFormatting.DARK_RED), true);
                return;
            }
            if (!player.getAbilities().instabuild) stack.shrink(1);
            triggerOutbreak(serverLevel, victim, player);
        }
    }

    private static boolean isLabPatient(ServerLevel level, LivingEntity v) {
        if (v instanceof PatientVillagerEntity p && (p.isOnBed() || p.getStretcher() != null || p.isSevere())) return true;
        BlockPos c = v.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-3, -1, -3), c.offset(3, 1, 3))) {
            BlockState s = level.getBlockState(p);
            if (s.is(ModBlocks.EXAMINATION_BED.get()) || s.is(ModBlocks.STRETCHER.get())) return true;
        }
        return false;
    }

    private static void triggerOutbreak(ServerLevel level, LivingEntity victim, Player player) {
        Vec3 pos = victim.position();
        for (AssistantScientistEntity s : level.getEntitiesOfClass(AssistantScientistEntity.class, victim.getBoundingBox().inflate(24.0D))) {
            if (victim.getUUID().equals(s.getPatient())) s.abortTreatment();
        }
        Speech.say(victim, "Ye... ye kaisi dawai hai?! AAAAHHH!", Speech.STYLE_RUDE, 40);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(pos);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        level.sendParticles(DustParticleOptions.REDSTONE, pos.x, pos.y + 1.0, pos.z, 200, 1.0, 1.2, 1.0, 0.0);
        level.sendParticles(new DustParticleOptions(new org.joml.Vector3f(0.45F, 0.0F, 0.0F), 2.0F), pos.x, pos.y + 1.0, pos.z, 120, 1.5, 1.5, 1.5, 0.0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y + 1.0, pos.z, 40, 0.8, 1.0, 0.8, 0.02);
        level.playSound(null, victim.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.HOSTILE, 1.5F, 0.5F);
        level.playSound(null, victim.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.2F);
        victim.discard();

        MutantBloodBeastEntity beast = ModEntities.MUTANT_BLOOD_BEAST.get().create(level);
        if (beast != null) {
            beast.moveTo(pos.x, pos.y, pos.z, victim.getYRot(), 0.0F);
            beast.setTarget(player);
            level.addFreshEntity(beast);
            Speech.say(beast, Lines.pick(Lines.BOSS, level.random), Speech.STYLE_BOSS);
        }
        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("THE EXPIRED SERUM MUTATED THE PATIENT! The Mutant Blood Beast has awakened!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
    }

    // ------------------------------------------------------------------ Dr. Anant coat + vitality hearts
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % 20 != 0) return;
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.DR_ANANT_COAT.get())) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 2, 0, true, false, true));
        }
        MobEffectInstance absorption = player.getEffect(MobEffects.ABSORPTION);
        if (absorption != null && absorption.isInfiniteDuration() && player.tickCount % 200 == 0) {
            float max = Math.min(player.getMaxAbsorption(), 8.0F);
            if (player.getAbsorptionAmount() < max) player.setAbsorptionAmount(Math.min(max, player.getAbsorptionAmount() + 2.0F));
        }
    }

    // ------------------------------------------------------------------ Immortality Elixir resurrection
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !entity.hasEffect(ModEffects.IMMORTALITY)) return;
        event.setCanceled(true);
        entity.removeEffect(ModEffects.IMMORTALITY);
        entity.setHealth(entity.getMaxHealth() * 0.5F);
        entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        entity.level().broadcastEntityEvent(entity, (byte) 35);
        if (entity instanceof Player p) {
            p.displayClientMessage(Component.literal("The Immortality Elixir saved you!").withStyle(ChatFormatting.GOLD), true);
        }
    }

    // ------------------------------------------------------------------ housekeeping
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) TimedEntityRemover.tick(serverLevel);
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Entity entity = event.getEntity();
        if (entity instanceof Display && entity.getTags().contains(TimedEntityRemover.XRAY_TAG)
                && !TimedEntityRemover.isTracked(level, entity.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TimedEntityRemover.clear();
        ConstructionManager.clearAll();
        ClinicRegistry.clear();
        ClinicBuilder.clearCache();
        LEFT_CLICK_COOLDOWN.clear();
    }

    private ModGameEvents() {}
}
