package com.nanaios.fieldarcana.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nanaios.fieldarcana.item.StaffMode;
import com.nanaios.fieldtheory.api.FieldType;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code field_arcana:cast_spell} — the player cast the Interference Staff.
 *
 * <pre>{@code { "trigger": "field_arcana:cast_spell", "conditions": { "mode": "emit_positive", "field": "field_theory:gravis" } } }</pre>
 */
public class CastSpellTrigger extends SimpleCriterionTrigger<CastSpellTrigger.Instance> {

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, StaffMode mode, @Nullable FieldType field) {
        trigger(player, instance -> instance.matches(mode, field));
    }

    public record Instance(Optional<ContextAwarePredicate> player, Optional<StaffMode> mode, Optional<FieldType> field)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                StaffMode.CODEC.optionalFieldOf("mode").forGetter(Instance::mode),
                FieldType.CODEC.optionalFieldOf("field").forGetter(Instance::field)
        ).apply(i, Instance::new));

        boolean matches(StaffMode castMode, @Nullable FieldType castField) {
            return mode.map(m -> m == castMode).orElse(true)
                    && field.map(f -> f == castField).orElse(true);
        }
    }
}
