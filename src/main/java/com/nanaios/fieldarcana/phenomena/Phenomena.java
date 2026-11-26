package com.nanaios.fieldarcana.phenomena;

import com.nanaios.fieldarcana.ArcanaConfig;
import com.nanaios.fieldarcana.advancement.WitnessPhenomenonTrigger;
import com.nanaios.fieldarcana.registry.ArcanaInteractions;
import com.nanaios.fieldarcana.registry.ArcanaTriggers;
import com.nanaios.fieldarcana.spell.ArcanaEffects;
import com.nanaios.fieldtheory.api.FieldType;
import com.nanaios.fieldtheory.api.FieldTypes;
import com.nanaios.fieldtheory.api.charge.Charges;
import com.nanaios.fieldtheory.api.interaction.FieldContext;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Field interactions added by Field Arcana. */
public final class Phenomena {
    /** Below this source potential the Nexus field is too weak to bind anything. */
    public static final double BINDING_THRESHOLD = 0.5;
    public static final double BINDING_VISCOSITY = 0.12;

    private Phenomena() {
    }

    /**
     * 束縛 (Binding). Inside a Nexus field space itself becomes viscous: every body — charged or
     * not — loses a fraction {@code 1/(1 + 0.12·|φ|)} of its velocity each tick. Falling sand
     * floats, arrows stall, mobs wade.
     */
    public static void binding(FieldContext context) {
        if (!context.controlsMotion() || !ArcanaConfig.binding()) {
            return;
        }
        FieldType nexus = FieldTypes.NEXUS.get();
        if (!context.world().hasContent(nexus)) {
            return;
        }
        double phi = Math.abs(context.sample(nexus).sourcePotential());
        if (phi >= BINDING_THRESHOLD) {
            context.damp(1.0 / (1.0 + BINDING_VISCOSITY * phi));
        }
    }

    /**
     * 絶縁破壊 (Dielectric breakdown). When the Fulgur stress {@code |E|·(1+|q|)} on a living body
     * exceeds the breakdown threshold, the field discharges through it: it takes damage and loses
     * its induced Fulgur charge. Charged bodies break down in much weaker fields.
     */
    public static void breakdown(FieldContext context) {
        if (!context.isServerSide() || !ArcanaConfig.breakdown() || !(context.entity() instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }
        if ((living.tickCount + living.getId()) % 5 != 0) {
            return;
        }
        FieldType fulgur = FieldTypes.FULGUR.get();
        if (!context.world().hasContent(fulgur)) {
            return;
        }
        double charge = Math.abs(context.charge(fulgur));
        double stress = context.sample(fulgur).intensity() * (1.0 + charge);
        if (stress < ArcanaConfig.breakdownThreshold()) {
            return;
        }
        ServerLevel level = (ServerLevel) context.level();
        float damage = (float) Math.min(8.0, 1.0 + 0.25 * stress);
        living.hurt(level.damageSources().source(ArcanaInteractions.DIELECTRIC_BREAKDOWN), damage);
        if (Charges.induced(living).has(fulgur)) {
            Charges.clearInduced(living, fulgur);
        }
        spark(level, living, fulgur.color());
        ArcanaTriggers.WITNESS.get().witness(level, Charges.center(living), WitnessPhenomenonTrigger.Kind.BREAKDOWN, null);
    }

    private static void spark(ServerLevel level, Entity entity, int color) {
        Vec3 c = Charges.center(entity);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z, 24, entity.getBbWidth() * 0.6, entity.getBbHeight() * 0.4, entity.getBbWidth() * 0.6, 0.3);
        ArcanaEffects.burst(level, c, color, 8);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.NEUTRAL, 0.35F, 1.8F);
    }
}
