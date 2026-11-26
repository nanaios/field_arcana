package com.nanaios.fieldarcana;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server config ({@code serverconfig/field_arcana-server.toml}), synchronised to clients. */
public final class ArcanaConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.DoubleValue COST_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue REGEN_MULTIPLIER;
    private static final ModConfigSpec.DoubleValue CAST_RANGE;
    private static final ModConfigSpec.BooleanValue BINDING;
    private static final ModConfigSpec.BooleanValue BREAKDOWN;
    private static final ModConfigSpec.DoubleValue BREAKDOWN_THRESHOLD;
    private static final ModConfigSpec.BooleanValue TRANSMUTATION;
    private static final ModConfigSpec.IntValue TRANSMUTATION_ATTEMPTS;
    private static final ModConfigSpec.BooleanValue RESONANCE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.translation("config.field_arcana.staff").push("staff");
        COST_MULTIPLIER = b.comment("Multiplier of the resonance cost of every staff mode.")
                .translation("config.field_arcana.costMultiplier")
                .defineInRange("costMultiplier", 1.0, 0.0, 10.0);
        REGEN_MULTIPLIER = b.comment("Multiplier of the resonance the staff absorbs from ambient fields.")
                .translation("config.field_arcana.regenMultiplier")
                .defineInRange("regenMultiplier", 1.0, 0.0, 100.0);
        CAST_RANGE = b.comment("How far (blocks) the staff reaches when placing sources and lenses.")
                .translation("config.field_arcana.castRange")
                .defineInRange("castRange", 24.0, 4.0, 64.0);
        b.pop();
        b.translation("config.field_arcana.phenomena").push("phenomena");
        BINDING = b.comment("Nexus fields make space viscous.")
                .translation("config.field_arcana.binding")
                .define("binding", true);
        BREAKDOWN = b.comment("Strong Fulgur fields discharge through living bodies.")
                .translation("config.field_arcana.breakdown")
                .define("breakdown", true);
        BREAKDOWN_THRESHOLD = b.comment("Fulgur stress |E|·(1+|q|) at which dielectric breakdown happens.")
                .translation("config.field_arcana.breakdownThreshold")
                .defineInRange("breakdownThreshold", 6.0, 0.5, 1000.0);
        TRANSMUTATION = b.comment("Mutatio sources transmute nearby blocks.")
                .translation("config.field_arcana.transmutation")
                .define("transmutation", true);
        TRANSMUTATION_ATTEMPTS = b.comment("Blocks probed per Mutatio source every 10 ticks.")
                .translation("config.field_arcana.transmutationAttempts")
                .defineInRange("transmutationAttempts", 2, 0, 64);
        RESONANCE = b.comment("Amethyst sings where oscillating fields are strong.")
                .translation("config.field_arcana.resonance")
                .define("resonance", true);
        b.pop();
        SPEC = b.build();
    }

    private ArcanaConfig() {
    }

    private static double get(ModConfigSpec.DoubleValue v) {
        return SPEC.isLoaded() ? v.get() : v.getDefault();
    }

    private static boolean get(ModConfigSpec.BooleanValue v) {
        return SPEC.isLoaded() ? v.get() : v.getDefault();
    }

    private static int get(ModConfigSpec.IntValue v) {
        return SPEC.isLoaded() ? v.get() : v.getDefault();
    }

    public static double costMultiplier() {
        return get(COST_MULTIPLIER);
    }

    public static double regenMultiplier() {
        return get(REGEN_MULTIPLIER);
    }

    public static double castRange() {
        return get(CAST_RANGE);
    }

    public static boolean binding() {
        return get(BINDING);
    }

    public static boolean breakdown() {
        return get(BREAKDOWN);
    }

    public static double breakdownThreshold() {
        return get(BREAKDOWN_THRESHOLD);
    }

    public static boolean transmutation() {
        return get(TRANSMUTATION);
    }

    public static boolean resonance() {
        return get(RESONANCE);
    }

    public static int transmutationAttempts() {
        return get(TRANSMUTATION_ATTEMPTS);
    }
}
