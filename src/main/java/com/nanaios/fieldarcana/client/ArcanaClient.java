package com.nanaios.fieldarcana.client;

import com.nanaios.fieldarcana.FieldArcana;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = FieldArcana.MOD_ID, dist = Dist.CLIENT)
public final class ArcanaClient {

    public ArcanaClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        NeoForge.EVENT_BUS.addListener(LensVisualizer::onClientTick);
    }
}
