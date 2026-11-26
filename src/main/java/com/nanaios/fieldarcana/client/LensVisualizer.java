package com.nanaios.fieldarcana.client;

import com.nanaios.fieldarcana.item.ResonanceLensItem;
import com.nanaios.fieldarcana.spell.ArcanaEffects;
import com.nanaios.fieldtheory.api.FieldRegion;
import com.nanaios.fieldtheory.api.FieldSample;
import com.nanaios.fieldtheory.api.FieldSource;
import com.nanaios.fieldtheory.api.FieldTheoryAPI;
import com.nanaios.fieldtheory.api.FieldType;
import com.nanaios.fieldtheory.api.FieldWorld;
import com.nanaios.fieldtheory.api.charge.Charges;
import com.nanaios.fieldtheory.api.util.FieldLines;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Client-side rendering of the Resonance Lens: field lines traced through the synchronised field
 * (superposition included, so dipoles, lenses and shields all show up), plus a numeric read-out.
 */
public final class LensVisualizer {
    private static final double VIEW_RADIUS = 32.0;
    private static final int MAX_SEEDS = 10;
    private static final double STEP = 0.35;
    private static final int MAX_STEPS = 44;
    private static final double WAVE_RADIUS = 14.0;
    private static final Map<Integer, Vec3[]> SPHERES = new HashMap<>();

    private LensVisualizer() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null || mc.isPaused()) {
            return;
        }
        ItemStack lens = player.getMainHandItem().getItem() instanceof ResonanceLensItem ? player.getMainHandItem()
                : player.getOffhandItem().getItem() instanceof ResonanceLensItem ? player.getOffhandItem() : ItemStack.EMPTY;
        if (lens.isEmpty()) {
            return;
        }
        long time = level.getGameTime();
        List<FieldType> observed = ResonanceLensItem.observed(lens);
        FieldWorld world = FieldTheoryAPI.get(level);
        if (time % 2 == 0) {
            // Dots advance one step along E every refresh: the field visibly "flows" from + to -.
            int phase = (int) ((time / 2) % 3);
            for (FieldType type : observed) {
                drawFieldLines(mc, world, type, player.getEyePosition(), phase);
            }
        }
        if (time % 4 == 0) {
            drawRegions(mc, world, player.getEyePosition(), observed, time);
        }
        if (time % 5 == 0) {
            player.displayClientMessage(readout(world, observed, player), true);
        }
    }

    private record Seed(Vec3 position, double charge) {
    }

    /** Spawns a motionless, short-lived particle so that lines stay crisp. */
    private static void dot(Minecraft mc, DustParticleOptions options, Vec3 p, int lifetime) {
        Particle particle = mc.particleEngine.createParticle(options, p.x, p.y, p.z, 0.0, 0.0, 0.0);
        if (particle != null) {
            particle.setParticleSpeed(0.0, 0.0, 0.0);
            particle.setLifetime(lifetime);
        }
    }

    private static void drawFieldLines(Minecraft mc, FieldWorld world, FieldType type, Vec3 eye, int phase) {
        List<Seed> seeds = new ArrayList<>();
        for (FieldSource source : world.sourcesNear(type, eye, VIEW_RADIUS)) {
            if (source.isOscillating()) {
                drawWavefronts(mc, type, source);
            } else {
                seeds.add(new Seed(source.position(), source.charge()));
            }
        }
        for (FieldWorld.Emitter emitter : world.emitters(type)) {
            if (emitter.position().distanceToSqr(eye) < VIEW_RADIUS * VIEW_RADIUS) {
                seeds.add(new Seed(emitter.position(), emitter.charge()));
            }
        }
        if (seeds.isEmpty()) {
            return;
        }
        seeds.sort(Comparator.comparingDouble(s -> s.position().distanceToSqr(eye)));
        if (seeds.size() > MAX_SEEDS) {
            seeds = seeds.subList(0, MAX_SEEDS);
        }
        DustParticleOptions dust = ArcanaEffects.dust(type.color(), 0.55F);
        DustParticleOptions bright = ArcanaEffects.dust(0xFFFFFF, 0.5F);
        DustParticleOptions core = ArcanaEffects.dust(type.color(), 2.0F);
        List<Seed> all = seeds;
        for (Seed seed : seeds) {
            Vec3 p = seed.position();
            dot(mc, core, p, 3);
            int lines = Mth.clamp(6 + (int) (Math.abs(seed.charge()) / 2), 6, 16);
            boolean forward = seed.charge() > 0;
            for (Vec3 dir : sphere(lines)) {
                int[] step = {0};
                FieldLines.trace(world, type, p.add(dir.scale(0.4)), forward, STEP, MAX_STEPS, point -> {
                    step[0]++;
                    // Lines are traced away from + and away from -; flip the phase so flow always reads + -> -.
                    int k = forward ? step[0] - phase : step[0] + phase;
                    dot(mc, Math.floorMod(k, 3) == 0 ? bright : dust, point, 3);
                    return point.distanceToSqr(eye) < VIEW_RADIUS * VIEW_RADIUS && !nearSink(point, all, forward);
                });
            }
        }
    }

    /**
     * Crests of an oscillating source on its horizontal plane: the circles where the retarded
     * phase ωt − kr + φ0 is a multiple of 2π. They expand at the field's wave speed.
     */
    private static void drawWavefronts(Minecraft mc, FieldType type, FieldSource source) {
        double omega = Math.abs(source.frequency());
        double k = omega / type.waveSpeed();
        double wavelength = 2 * Math.PI / k;
        long time = mc.level == null ? 0 : mc.level.getGameTime();
        double phase = omega * time + source.phase() * Math.signum(source.frequency());
        double first = (phase % (2 * Math.PI) + 2 * Math.PI) % (2 * Math.PI) / k;
        DustParticleOptions crest = ArcanaEffects.dust(type.color(), 0.7F);
        Vec3 c = source.position();
        dot(mc, ArcanaEffects.dust(type.color(), 2.0F), c, 3);
        for (double r = first; r < WAVE_RADIUS; r += wavelength) {
            if (r < 0.3) {
                continue;
            }
            int n = Mth.clamp((int) (r * 2 * Math.PI / 0.6), 8, 160);
            for (int i = 0; i < n; i++) {
                double a = 2 * Math.PI * i / n;
                dot(mc, crest, new Vec3(c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r), 3);
            }
        }
    }

    /** A line ends when it reaches a charge of the opposite sign. */
    private static boolean nearSink(Vec3 point, List<Seed> seeds, boolean forward) {
        for (Seed s : seeds) {
            if ((s.charge() > 0) != forward && s.position().distanceToSqr(point) < 0.2) {
                return true;
            }
        }
        return false;
    }

    private static Vec3[] sphere(int n) {
        return SPHERES.computeIfAbsent(n, FieldLines::fibonacciSphere);
    }

    private static void drawRegions(Minecraft mc, FieldWorld world, Vec3 eye, List<FieldType> observed, long time) {
        for (FieldRegion region : world.regions()) {
            if (region.center().distanceToSqr(eye) > VIEW_RADIUS * VIEW_RADIUS) {
                continue;
            }
            boolean relevant = region.type().isEmpty() || observed.contains(region.type().get());
            if (!relevant) {
                continue;
            }
            int color = region.gain() >= 1.0 ? 0xFFD24A : 0xB0B4C8;
            DustParticleOptions dust = ArcanaEffects.dust(color, 0.7F);
            int n = 20;
            double r = region.radius();
            Vec3 c = region.center();
            for (int i = 0; i < n; i++) {
                double a = Math.PI * 2 * i / n + time * 0.02;
                dot(mc, dust, new Vec3(c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r), 5);
                dot(mc, dust, new Vec3(c.x + Math.cos(a) * r, c.y + Math.sin(a) * r, c.z), 5);
            }
        }
    }

    private static Component readout(FieldWorld world, List<FieldType> observed, LocalPlayer player) {
        Vec3 at = Charges.center(player);
        MutableComponent text = Component.empty();
        boolean compact = observed.size() > 1;
        for (FieldType type : observed) {
            FieldSample s = world.sample(type, at, player);
            if (!text.getSiblings().isEmpty()) {
                text.append(Component.literal("  "));
            }
            text.append(type.displayName());
            double amplitude = world.amplitude(type, at);
            String wave = amplitude > 0.005 ? String.format(Locale.ROOT, compact ? "~%.1f" : "   ~A = %.2f", amplitude) : "";
            text.append(Component.literal(compact
                    ? String.format(Locale.ROOT, " %.1f|%.2f", s.potential(), s.intensity()) + wave
                    : String.format(Locale.ROOT, "  φ = %.2f  (ambient %.2f)   |E| = %.3f   gain ×%.2f", s.potential(), s.ambient(), s.intensity(), s.gain()) + wave));
        }
        return text;
    }
}
