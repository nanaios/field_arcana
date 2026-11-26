package com.nanaios.fieldarcana;

import com.nanaios.fieldarcana.phenomena.Transmutations;
import com.nanaios.fieldarcana.registry.ArcanaBlockEntities;
import com.nanaios.fieldarcana.registry.ArcanaBlocks;
import com.nanaios.fieldarcana.registry.ArcanaComponents;
import com.nanaios.fieldarcana.registry.ArcanaInteractions;
import com.nanaios.fieldarcana.registry.ArcanaItems;
import com.nanaios.fieldarcana.registry.ArcanaTabs;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * Field Arcana — magic as field engineering, built on Field Theory.
 *
 * <p>Nothing here deals damage or moves things directly. Spells and machines only create
 * sources, charges and lenses; every visible effect is a <em>phenomenon</em> that emerges from
 * the resulting field configuration (forces from Field Theory itself, and the binding, dielectric
 * breakdown and transmutation phenomena defined in this mod).</p>
 */
@Mod(FieldArcana.MOD_ID)
public final class FieldArcana {
    public static final String MOD_ID = "field_arcana";

    public FieldArcana(IEventBus modBus) {
        ArcanaComponents.COMPONENTS.register(modBus);
        ArcanaBlocks.BLOCKS.register(modBus);
        ArcanaItems.ITEMS.register(modBus);
        ArcanaBlockEntities.BLOCK_ENTITIES.register(modBus);
        ArcanaTabs.TABS.register(modBus);
        ArcanaInteractions.INTERACTIONS.register(modBus);

        NeoForge.EVENT_BUS.addListener(Transmutations::onLevelTick);
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent event) -> event.addListener(new Transmutations.Loader()));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
