package com.nanaios.fieldarcana.registry;

import com.nanaios.fieldarcana.FieldArcana;
import com.nanaios.fieldarcana.phenomena.Phenomena;
import com.nanaios.fieldtheory.api.FieldTheoryAPI;
import com.nanaios.fieldtheory.api.interaction.FieldInteraction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Phenomena this mod adds to Field Theory's interaction registry. */
public final class ArcanaInteractions {
    public static final DeferredRegister<FieldInteraction> INTERACTIONS = DeferredRegister.create(FieldTheoryAPI.INTERACTION_REGISTRY, FieldArcana.MOD_ID);

    /** Nexus: strong binding field acts as a viscous medium. */
    public static final DeferredHolder<FieldInteraction, FieldInteraction> BINDING = INTERACTIONS.register("binding", () -> Phenomena::binding);

    /** Fulgur: field stress above the breakdown threshold discharges through charged bodies. */
    public static final DeferredHolder<FieldInteraction, FieldInteraction> BREAKDOWN = INTERACTIONS.register("breakdown", () -> Phenomena::breakdown);

    public static final ResourceKey<DamageType> DIELECTRIC_BREAKDOWN = ResourceKey.create(Registries.DAMAGE_TYPE, FieldArcana.id("dielectric_breakdown"));

    private ArcanaInteractions() {
    }
}
