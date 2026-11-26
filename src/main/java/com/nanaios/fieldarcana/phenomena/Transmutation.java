package com.nanaios.fieldarcana.phenomena;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nanaios.fieldtheory.api.FieldType;
import com.nanaios.fieldtheory.api.FieldTypes;
import java.util.List;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 転成 — a block that turns into another when the Mutatio potential reaches {@code catalyst}
 * and every {@link Condition} on the other fields holds. Mutatio only opens the door; the other
 * fields decide what comes through it. Loaded from
 * {@code data/<ns>/field_arcana/transmutation/*.json}:
 *
 * <pre>{@code
 * { "input": "minecraft:sand", "output": "minecraft:glass", "catalyst": 1.5,
 *   "conditions": [ { "field": "field_theory:fulgur", "min": 3.0 } ] }
 * }</pre>
 *
 * Potentials are total potentials (ambient included), so a thunderstorm's Fulgur counts.
 */
public record Transmutation(BlockMatcher input, Block output, double catalyst, List<Condition> conditions) {
    public static final Codec<Transmutation> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockMatcher.CODEC.fieldOf("input").forGetter(Transmutation::input),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("output").forGetter(Transmutation::output),
            Codec.DOUBLE.optionalFieldOf("catalyst", 1.5).forGetter(Transmutation::catalyst),
            Condition.CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(Transmutation::conditions)
    ).apply(i, Transmutation::new));

    public boolean matches(BlockState state, ToDoubleFunction<FieldType> potential) {
        if (!input.test(state) || potential.applyAsDouble(FieldTypes.MUTATIO.get()) < catalyst) {
            return false;
        }
        for (Condition condition : conditions) {
            double phi = potential.applyAsDouble(condition.field());
            if (phi < condition.min() || phi > condition.max()) {
                return false;
            }
        }
        return true;
    }

    public record Condition(FieldType field, double min, double max) {
        public static final Codec<Condition> CODEC = RecordCodecBuilder.create(i -> i.group(
                FieldType.CODEC.fieldOf("field").forGetter(Condition::field),
                Codec.DOUBLE.optionalFieldOf("min", Double.NEGATIVE_INFINITY).forGetter(Condition::min),
                Codec.DOUBLE.optionalFieldOf("max", Double.POSITIVE_INFINITY).forGetter(Condition::max)
        ).apply(i, Condition::new));
    }
}
