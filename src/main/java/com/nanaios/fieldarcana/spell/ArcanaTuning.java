package com.nanaios.fieldarcana.spell;

import com.nanaios.fieldtheory.api.FieldType;
import com.nanaios.fieldtheory.api.FieldTypes;

/** Balance numbers in one place. */
public final class ArcanaTuning {
    public static final int MAX_RESONANCE = 1000;
    /** Resonance gained per second per unit of |ambient potential| at the holder's position. */
    public static final double REGEN_PER_AMBIENT = 6.0;

    public static final int EMIT_DURATION = 200;
    public static final int IMBUE_DURATION = 300;
    public static final int REGION_DURATION = 300;
    public static final double LENS_RADIUS = 5.0;
    public static final double LENS_GAIN = 3.0;
    public static final double SHIELD_RADIUS = 3.5;
    public static final double IMBUE_RANGE = 16.0;

    private ArcanaTuning() {
    }

    /** Charge of a source placed by the staff. */
    public static double emitCharge(FieldType type) {
        return pick(type, 24.0, 12.0, 8.0, 8.0, 10.0);
    }

    /** Charge induced on a creature by the staff. */
    public static double imbueCharge(FieldType type) {
        return pick(type, 20.0, 4.0, 3.0, 3.0, 4.0);
    }

    /** Charge of a Field Emitter per level of redstone signal. */
    public static double emitterChargePerSignal(FieldType type) {
        return pick(type, 2.0, 1.0, 0.6, 0.6, 1.0);
    }

    private static double pick(FieldType type, double gravis, double fulgur, double nexus, double mutatio, double other) {
        if (type == FieldTypes.GRAVIS.get()) {
            return gravis;
        } else if (type == FieldTypes.FULGUR.get()) {
            return fulgur;
        } else if (type == FieldTypes.NEXUS.get()) {
            return nexus;
        } else if (type == FieldTypes.MUTATIO.get()) {
            return mutatio;
        }
        return other;
    }
}
