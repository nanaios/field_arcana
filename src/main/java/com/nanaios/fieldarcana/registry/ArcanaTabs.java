package com.nanaios.fieldarcana.registry;

import com.nanaios.fieldarcana.FieldArcana;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ArcanaTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FieldArcana.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.field_arcana"))
            .icon(() -> ArcanaItems.INTERFERENCE_STAFF.get().getDefaultInstance())
            .displayItems((parameters, output) -> ArcanaItems.ITEMS.getEntries().forEach(item -> output.accept(item.get())))
            .build());

    private ArcanaTabs() {
    }
}
