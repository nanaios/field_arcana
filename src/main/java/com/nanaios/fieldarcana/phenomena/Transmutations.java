package com.nanaios.fieldarcana.phenomena;

import com.nanaios.fieldarcana.ArcanaConfig;
import com.nanaios.fieldarcana.advancement.WitnessPhenomenonTrigger;
import com.nanaios.fieldarcana.registry.ArcanaRecipes;
import com.nanaios.fieldarcana.registry.ArcanaTriggers;
import com.nanaios.fieldarcana.spell.ArcanaEffects;
import com.nanaios.fieldtheory.api.FieldSource;
import com.nanaios.fieldtheory.api.FieldTheoryAPI;
import com.nanaios.fieldtheory.api.FieldType;
import com.nanaios.fieldtheory.api.FieldTypes;
import com.nanaios.fieldtheory.api.FieldWorld;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Lets every Mutatio source act on the blocks around it, following the transmutation recipes. */
public final class Transmutations {
    /** How far from a Mutatio source blocks are probed (≈ 2σ of the Gaussian). */
    public static final int REACH = 4;
    public static final int INTERVAL = 10;

    /** A recipe together with its id. */
    public record Entry(ResourceLocation id, Transmutation recipe) {
    }

    private Transmutations() {
    }

    /** All transmutations, most specific first (more conditions win, then by id). */
    public static List<Entry> all(RecipeManager manager) {
        List<Entry> entries = new ArrayList<>();
        for (RecipeHolder<Transmutation> holder : manager.getAllRecipesFor(ArcanaRecipes.TRANSMUTATION.get())) {
            entries.add(new Entry(holder.id(), holder.value()));
        }
        entries.sort(Comparator.<Entry>comparingInt(e -> -e.recipe().conditions().size()).thenComparing(Entry::id));
        return entries;
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % INTERVAL != 0) {
            return;
        }
        int attempts = ArcanaConfig.transmutationAttempts();
        if (!ArcanaConfig.transmutation() || attempts == 0) {
            return;
        }
        FieldWorld world = FieldTheoryAPI.get(level);
        FieldType mutatio = FieldTypes.MUTATIO.get();
        if (!world.hasContent(mutatio)) {
            return;
        }
        List<Entry> recipes = all(level.getRecipeManager());
        if (recipes.isEmpty()) {
            return;
        }
        List<Vec3> seeds = new ArrayList<>();
        for (FieldSource source : world.sources()) {
            if (source.type() == mutatio && source.charge() > 0) {
                seeds.add(source.position());
            }
        }
        for (FieldWorld.Emitter emitter : world.emitters(mutatio)) {
            if (emitter.charge() > 0) {
                seeds.add(emitter.position());
            }
        }
        RandomSource random = level.random;
        for (Vec3 seed : seeds) {
            for (int i = 0; i < attempts; i++) {
                BlockPos pos = BlockPos.containing(
                        seed.x + random.nextInt(2 * REACH + 1) - REACH,
                        seed.y + random.nextInt(2 * REACH + 1) - REACH,
                        seed.z + random.nextInt(2 * REACH + 1) - REACH);
                if (level.isLoaded(pos)) {
                    tryTransmute(level, world, pos, recipes);
                }
            }
        }
    }

    /**
     * Transmutes the block at {@code pos} if some recipe matches the local fields.
     *
     * @return the id of the recipe that fired, or {@code null}
     */
    @Nullable
    public static ResourceLocation tryTransmute(ServerLevel level, FieldWorld world, BlockPos pos) {
        return tryTransmute(level, world, pos, all(level.getRecipeManager()));
    }

    @Nullable
    private static ResourceLocation tryTransmute(ServerLevel level, FieldWorld world, BlockPos pos, List<Entry> recipes) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.hasBlockEntity()) {
            return null;
        }
        Vec3 center = Vec3.atCenterOf(pos);
        Map<FieldType, Double> cache = new IdentityHashMap<>();
        ToDoubleFunction<FieldType> potential = type -> cache.computeIfAbsent(type, t -> world.sample(t, center).potential());
        for (Entry entry : recipes) {
            if (entry.recipe().matches(state, potential)) {
                level.setBlockAndUpdate(pos, entry.recipe().output().withPropertiesOf(state));
                ArcanaEffects.burst(level, center, FieldTypes.MUTATIO.get().color(), 12);
                level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.BLOCKS, 0.7F, 1.6F + level.random.nextFloat() * 0.3F);
                ArcanaTriggers.WITNESS.get().witness(level, center, WitnessPhenomenonTrigger.Kind.TRANSMUTATION, entry.id());
                return entry.id();
            }
        }
        return null;
    }
}
