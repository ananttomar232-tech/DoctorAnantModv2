package com.dranant.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** config/dranant-common.toml */
public final class DrAnantConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue DETECTION_RADIUS = BUILDER
            .comment("Radius (blocks) in which villagers notice a clinic's Diamond Cash Register and join a queue.")
            .defineInRange("villagerDetectionRadius", 48, 8, 128);
    public static final ModConfigSpec.DoubleValue COUNTER_QUEUE_CHANCE = BUILDER
            .comment("Chance that an arriving villager joins Line 1 (counter). The rest join Line 2 (lab).")
            .defineInRange("counterQueueChance", 0.7D, 0.0D, 1.0D);
    public static final ModConfigSpec.IntValue MAX_QUEUE_LENGTH = BUILDER
            .comment("Maximum villagers per queue line.")
            .defineInRange("maxQueueLength", 8, 1, 20);
    public static final ModConfigSpec.BooleanValue SPAWN_CUSTOMERS = BUILDER
            .comment("If true, clinics periodically attract brand-new customer villagers (great for recording, works without a nearby village).")
            .define("spawnCustomerVillagers", true);
    public static final ModConfigSpec.IntValue CUSTOMER_SPAWN_INTERVAL = BUILDER
            .comment("Ticks between customer villager arrivals (20 ticks = 1 second).")
            .defineInRange("customerSpawnIntervalTicks", 160, 20, 24000);
    public static final ModConfigSpec.IntValue SERVED_COOLDOWN = BUILDER
            .comment("Ticks before a served village resident queues again.")
            .defineInRange("servedVillagerCooldownTicks", 2400, 0, 72000);

    public static final ModConfigSpec.BooleanValue PASSIVE_INCOME = BUILDER
            .comment("While the Diamond Counter is running it grows non-stop on an accelerating curve.")
            .define("passiveCounterGrowth", true);
    public static final ModConfigSpec.LongValue PASSIVE_TARGET = BUILDER
            .comment("Diamonds the passive growth reaches after 'passiveTargetSeconds'.")
            .defineInRange("passiveTargetDiamonds", 1_000_000L, 1L, 1_000_000_000_000L);
    public static final ModConfigSpec.IntValue PASSIVE_TARGET_SECONDS = BUILDER
            .comment("Seconds of running counter needed to reach 'passiveTargetDiamonds' (300 = 5 minutes).")
            .defineInRange("passiveTargetSeconds", 300, 10, 86400);
    public static final ModConfigSpec.BooleanValue AUTO_DEPOSIT = BUILDER
            .comment("While the counter runs, picked-up diamonds disappear from the inventory and are added to the counter.")
            .define("autoDepositPickedUpDiamonds", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private DrAnantConfig() {}
}
