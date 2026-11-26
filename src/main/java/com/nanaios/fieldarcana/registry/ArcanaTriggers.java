package com.nanaios.fieldarcana.registry;

import com.nanaios.fieldarcana.FieldArcana;
import com.nanaios.fieldarcana.advancement.CastSpellTrigger;
import com.nanaios.fieldarcana.advancement.WitnessPhenomenonTrigger;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ArcanaTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, FieldArcana.MOD_ID);

    public static final DeferredHolder<CriterionTrigger<?>, CastSpellTrigger> CAST_SPELL =
            TRIGGERS.register("cast_spell", CastSpellTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, WitnessPhenomenonTrigger> WITNESS =
            TRIGGERS.register("witness_phenomenon", WitnessPhenomenonTrigger::new);

    private ArcanaTriggers() {
    }
}
