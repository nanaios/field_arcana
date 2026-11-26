package com.nanaios.fieldarcana.phenomena;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nanaios.fieldarcana.registry.ArcanaRecipes;
import com.nanaios.fieldtheory.api.FieldType;
import com.nanaios.fieldtheory.api.FieldTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 転成 — a block that turns into another when the Mutatio potential reaches {@code catalyst}
 * and every {@link Condition} on the other fields holds. Mutatio only opens the door; the other
 * fields decide what comes through it.
 *
 * <p>A vanilla recipe type ({@code field_arcana:transmutation}), so it is synchronised to clients
 * and visible to recipe viewers. Recipes live in {@code data/<ns>/recipe/}:</p>
 *
 * <pre>{@code
 * { "type": "field_arcana:transmutation",
 *   "input": "minecraft:sand", "output": "minecraft:glass", "catalyst": 1.5,
 *   "conditions": [ { "field": "field_theory:fulgur", "min": 3.0 } ] }
 * }</pre>
 *
 * Potentials are total potentials (ambient included), so a thunderstorm's Fulgur counts.
 */
public record Transmutation(BlockMatcher input, Block output, double catalyst, List<Condition> conditions)
        implements Recipe<SingleRecipeInput> {
    public static final MapCodec<Transmutation> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockMatcher.CODEC.fieldOf("input").forGetter(Transmutation::input),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("output").forGetter(Transmutation::output),
            Codec.DOUBLE.optionalFieldOf("catalyst", 1.5).forGetter(Transmutation::catalyst),
            Condition.CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(Transmutation::conditions)
    ).apply(i, Transmutation::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Transmutation> STREAM_CODEC = StreamCodec.of(
            (buf, t) -> {
                buf.writeUtf(t.input.toString());
                buf.writeResourceLocation(BuiltInRegistries.BLOCK.getKey(t.output));
                buf.writeDouble(t.catalyst);
                buf.writeVarInt(t.conditions.size());
                for (Condition c : t.conditions) {
                    FieldType.STREAM_CODEC.encode(buf, c.field());
                    buf.writeDouble(c.min());
                    buf.writeDouble(c.max());
                }
            },
            buf -> {
                BlockMatcher input = BlockMatcher.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,
                        new com.google.gson.JsonPrimitive(buf.readUtf())).getOrThrow();
                Block output = BuiltInRegistries.BLOCK.get(buf.readResourceLocation());
                double catalyst = buf.readDouble();
                int n = buf.readVarInt();
                List<Condition> conditions = new ArrayList<>(n);
                for (int k = 0; k < n; k++) {
                    conditions.add(new Condition(FieldType.STREAM_CODEC.decode(buf), buf.readDouble(), buf.readDouble()));
                }
                return new Transmutation(input, output, catalyst, conditions);
            });

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

    // ------------------------------------------------------------ Recipe (used for sync and viewers)

    /** Transmutations happen in the world, never in a crafting grid. */
    @Override
    public boolean matches(SingleRecipeInput recipeInput, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(SingleRecipeInput recipeInput, HolderLookup.Provider registries) {
        return new ItemStack(output);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(output);
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ArcanaRecipes.TRANSMUTATION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ArcanaRecipes.TRANSMUTATION.get();
    }

    public record Condition(FieldType field, double min, double max) {
        public static final Codec<Condition> CODEC = RecordCodecBuilder.create(i -> i.group(
                FieldType.CODEC.fieldOf("field").forGetter(Condition::field),
                Codec.DOUBLE.optionalFieldOf("min", Double.NEGATIVE_INFINITY).forGetter(Condition::min),
                Codec.DOUBLE.optionalFieldOf("max", Double.POSITIVE_INFINITY).forGetter(Condition::max)
        ).apply(i, Condition::new));
    }
}
