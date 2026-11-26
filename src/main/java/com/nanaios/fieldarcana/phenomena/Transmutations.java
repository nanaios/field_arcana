package com.nanaios.fieldarcana.phenomena;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.nanaios.fieldarcana.ArcanaConfig;
import com.nanaios.fieldarcana.advancement.WitnessPhenomenonTrigger;
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
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.slf4j.Logger;

/** Loads transmutations and lets every Mutatio source act on the blocks around it. */
public final class Transmutations {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** How far from a Mutatio source blocks are probed (≈ 2σ of the Gaussian). */
    public static final int REACH = 4;
    public static final int INTERVAL = 10;

    /** A loaded recipe together with its data-pack id. */
    public record Entry(ResourceLocation id, Transmutation recipe) {
    }

    private static volatile List<Entry> recipes = List.of();

    private Transmutations() {
    }

    public static List<Entry> all() {
        return recipes;
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || recipes.isEmpty() || level.getGameTime() % INTERVAL != 0) {
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
                    tryTransmute(level, world, pos);
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

    public static final class Loader extends SimpleJsonResourceReloadListener {
        public static final String DIRECTORY = "field_arcana/transmutation";

        public Loader() {
            super(new Gson(), DIRECTORY);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
            List<Entry> loaded = new ArrayList<>();
            files.forEach((id, json) -> Transmutation.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> LOGGER.error("Invalid transmutation {}: {}", id, error))
                    .ifPresent(t -> loaded.add(new Entry(id, t))));
            // Most specific first: more conditions win, then a stable order by id.
            loaded.sort(Comparator.<Entry>comparingInt(e -> -e.recipe().conditions().size()).thenComparing(Entry::id));
            recipes = List.copyOf(loaded);
            LOGGER.info("Loaded {} transmutations", recipes.size());
        }
    }
}
