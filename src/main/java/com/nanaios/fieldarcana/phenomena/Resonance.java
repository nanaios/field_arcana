package com.nanaios.fieldarcana.phenomena;

import com.nanaios.fieldarcana.ArcanaConfig;
import com.nanaios.fieldarcana.advancement.WitnessPhenomenonTrigger;
import com.nanaios.fieldarcana.registry.ArcanaTriggers;
import com.nanaios.fieldtheory.api.FieldSource;
import com.nanaios.fieldtheory.api.FieldTheoryAPI;
import com.nanaios.fieldtheory.api.FieldWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * 共鳴 (Resonance). Amethyst sings where an oscillating field is strong: the pitch follows the
 * period, so a pair of resonators audibly marks the antinodes of its interference pattern.
 */
public final class Resonance {
    public static final int INTERVAL = 20;
    public static final int REACH = 6;
    public static final int ATTEMPTS = 3;
    /** Amplitude of the potential above which amethyst sings. */
    public static final double THRESHOLD = 1.5;

    private Resonance() {
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % INTERVAL != 0 || !ArcanaConfig.resonance()) {
            return;
        }
        FieldWorld world = FieldTheoryAPI.get(level);
        RandomSource random = level.random;
        for (FieldSource source : world.sources()) {
            if (!source.isOscillating()) {
                continue;
            }
            Vec3 seed = source.position();
            for (int i = 0; i < ATTEMPTS; i++) {
                BlockPos pos = BlockPos.containing(
                        seed.x + random.nextInt(2 * REACH + 1) - REACH,
                        seed.y + random.nextInt(2 * REACH + 1) - REACH,
                        seed.z + random.nextInt(2 * REACH + 1) - REACH);
                if (level.isLoaded(pos)) {
                    trySing(level, world, pos, source);
                }
            }
        }
    }

    public static boolean isCrystal(BlockState state) {
        return state.is(BlockTags.CRYSTAL_SOUND_BLOCKS) || state.getBlock() instanceof AmethystClusterBlock;
    }

    /** Makes the crystal at {@code pos} sing if the wave of {@code source}'s field is strong there. */
    public static boolean trySing(ServerLevel level, FieldWorld world, BlockPos pos, FieldSource source) {
        if (!isCrystal(level.getBlockState(pos))) {
            return false;
        }
        Vec3 center = Vec3.atCenterOf(pos);
        double amplitude = world.amplitude(source.type(), center);
        if (amplitude < THRESHOLD) {
            return false;
        }
        // One octave per doubling of frequency, A = period 12.
        float pitch = (float) Mth.clamp(Math.pow(2.0, Math.log(12.0 / source.period()) / Math.log(2.0)), 0.5, 2.0);
        float volume = (float) Mth.clamp(amplitude / 6.0, 0.3, 1.5);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, volume, pitch);
        level.sendParticles(ParticleTypes.NOTE, center.x, center.y + 0.7, center.z, 1, 0.0, 0.0, 0.0, (pitch - 0.5) / 1.5);
        ArcanaTriggers.WITNESS.get().witness(level, center, WitnessPhenomenonTrigger.Kind.RESONANCE, null);
        return true;
    }
}
