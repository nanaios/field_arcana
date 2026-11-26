package com.nanaios.fieldarcana.registry;

import com.nanaios.fieldarcana.FieldArcana;
import com.nanaios.fieldarcana.item.StaffMode;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ArcanaComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, FieldArcana.MOD_ID);

    /** Operation the Interference Staff performs. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StaffMode>> STAFF_MODE =
            COMPONENTS.registerComponentType("staff_mode", b -> b.persistent(StaffMode.CODEC).networkSynchronized(StaffMode.STREAM_CODEC));

    /** Stored resonance (the staff's reserve, drawn from ambient fields). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RESONANCE =
            COMPONENTS.registerComponentType("resonance", b -> b.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Field shown by the Resonance Lens; absent = all fields. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> LENS_FIELD =
            COMPONENTS.registerComponentType("lens_field", b -> b.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));

    private ArcanaComponents() {
    }
}
