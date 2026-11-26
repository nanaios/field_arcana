package com.nanaios.fieldarcana.client;

import com.nanaios.fieldarcana.item.FieldPrimerItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;

/**
 * Client-only entry points called from common code. Only invoked when {@code level.isClientSide},
 * so this class is never loaded on a dedicated server.
 */
public final class ArcanaClientHooks {
    private ArcanaClientHooks() {
    }

    public static void openPrimer() {
        List<Component> pages = new ArrayList<>(FieldPrimerItem.PAGES);
        for (int i = 1; i <= FieldPrimerItem.PAGES; i++) {
            pages.add(Component.translatable("guide.field_arcana.page." + i));
        }
        Minecraft.getInstance().setScreen(new BookViewScreen(new BookViewScreen.BookAccess(pages)));
    }
}
