package com.nanaios.fieldarcana.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.Vec3;

/**
 * {@code field_arcana:witness_phenomenon} — a phenomenon happened within
 * {@link #RADIUS} blocks of the player. Phenomena emerge from fields rather than from a caster,
 * so credit goes to everyone who saw it.
 *
 * <pre>{@code { "trigger": "field_arcana:witness_phenomenon",
 *   "conditions": { "kind": "transmutation", "detail": "field_arcana:fulgurite" } } }</pre>
 */
public class WitnessPhenomenonTrigger extends SimpleCriterionTrigger<WitnessPhenomenonTrigger.Instance> {
    public static final double RADIUS = 16.0;

    public enum Kind implements StringRepresentable {
        BREAKDOWN("breakdown"),
        TRANSMUTATION("transmutation"),
        TELEGRAPH("telegraph");

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
        private final String name;

        Kind(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    /** Fires for every player within {@link #RADIUS} of {@code at}. */
    public void witness(ServerLevel level, Vec3 at, Kind kind, @Nullable ResourceLocation detail) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(at) <= RADIUS * RADIUS) {
                trigger(player, instance -> instance.matches(kind, detail));
            }
        }
    }

    public record Instance(Optional<ContextAwarePredicate> player, Optional<Kind> kind, Optional<ResourceLocation> detail)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Kind.CODEC.optionalFieldOf("kind").forGetter(Instance::kind),
                ResourceLocation.CODEC.optionalFieldOf("detail").forGetter(Instance::detail)
        ).apply(i, Instance::new));

        boolean matches(Kind seen, @Nullable ResourceLocation seenDetail) {
            return kind.map(k -> k == seen).orElse(true)
                    && detail.map(d -> d.equals(seenDetail)).orElse(true);
        }
    }
}
