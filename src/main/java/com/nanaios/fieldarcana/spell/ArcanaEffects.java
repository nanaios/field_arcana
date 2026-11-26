package com.nanaios.fieldarcana.spell;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Server-side particle helpers (sent to every nearby client). */
public final class ArcanaEffects {
    private ArcanaEffects() {
    }

    public static Vector3f rgb(int color) {
        return new Vector3f(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F);
    }

    public static DustParticleOptions dust(int color, float scale) {
        return new DustParticleOptions(rgb(color), scale);
    }

    public static void beam(ServerLevel level, Vec3 from, Vec3 to, int color) {
        Vec3 delta = to.subtract(from);
        int steps = Mth.clamp((int) (delta.length() * 3), 2, 96);
        DustParticleOptions dust = dust(color, 0.8F);
        for (int i = 1; i <= steps; i++) {
            Vec3 p = from.add(delta.scale(i / (double) steps));
            level.sendParticles(dust, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    public static void burst(ServerLevel level, Vec3 at, int color, int count) {
        level.sendParticles(dust(color, 1.4F), at.x, at.y, at.z, count, 0.4, 0.4, 0.4, 0.0);
    }

    public static void ring(ServerLevel level, Vec3 center, double radius, int color) {
        DustParticleOptions dust = dust(color, 1.0F);
        int n = Mth.clamp((int) (radius * 8), 12, 64);
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            level.sendParticles(dust, center.x + Math.cos(a) * radius, center.y, center.z + Math.sin(a) * radius, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
