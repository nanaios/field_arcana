package com.nanaios.fieldarcana.registry;

import com.mojang.serialization.MapCodec;
import com.nanaios.fieldarcana.FieldArcana;
import com.nanaios.fieldarcana.phenomena.Transmutation;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ArcanaRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, FieldArcana.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, FieldArcana.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<Transmutation>> TRANSMUTATION =
            TYPES.register("transmutation", () -> RecipeType.simple(FieldArcana.id("transmutation")));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<Transmutation>> TRANSMUTATION_SERIALIZER =
            SERIALIZERS.register("transmutation", () -> new RecipeSerializer<>() {
                @Override
                public MapCodec<Transmutation> codec() {
                    return Transmutation.CODEC;
                }

                @Override
                public StreamCodec<RegistryFriendlyByteBuf, Transmutation> streamCodec() {
                    return Transmutation.STREAM_CODEC;
                }
            });

    private ArcanaRecipes() {
    }
}
