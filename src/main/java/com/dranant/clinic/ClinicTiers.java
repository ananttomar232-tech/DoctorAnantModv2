package com.dranant.clinic;

/**
 * The six clinic tiers, ordered from smallest to biggest building.
 * Tiers 1-3 are generated in code; tiers 4-6 load the attached structures
 * (data/dranant/structure/clinic_level_4/5/6.nbt = original level 4, 6 and 5 files, re-ordered by size).
 */
public final class ClinicTiers {
    public static final int MAX_TIER = 6;

    private static final String[] NAMES = {
            "Village Dispensary", "Community Clinic", "Health Centre",
            "City Hospital", "Medical College", "Mega Hospital"
    };
    /** Shop prices in diamonds - all below one million. */
    private static final long[] PRICES = {5_000L, 20_000L, 60_000L, 150_000L, 350_000L, 750_000L};

    public static String name(int tier) {
        return NAMES[clamp(tier) - 1];
    }

    public static long price(int tier) {
        return PRICES[clamp(tier) - 1];
    }

    public static String structureName(int tier) {
        return "clinic_level_" + clamp(tier);
    }

    private static int clamp(int tier) {
        return Math.max(1, Math.min(MAX_TIER, tier));
    }

    private ClinicTiers() {}
}
