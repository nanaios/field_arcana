package com.nanaios.fieldarcana.registry;

import com.nanaios.fieldarcana.FieldArcana;
import com.nanaios.fieldarcana.item.FieldCoreItem;
import com.nanaios.fieldarcana.item.FieldPrimerItem;
import com.nanaios.fieldarcana.item.InterferenceStaffItem;
import com.nanaios.fieldarcana.item.ResonanceLensItem;
import com.nanaios.fieldtheory.api.FieldTypes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ArcanaItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FieldArcana.MOD_ID);

    public static final DeferredItem<FieldCoreItem> GRAVIS_CORE = ITEMS.registerItem("gravis_core",
            p -> new FieldCoreItem(FieldTypes.GRAVIS, p), new Item.Properties());
    public static final DeferredItem<FieldCoreItem> FULGUR_CORE = ITEMS.registerItem("fulgur_core",
            p -> new FieldCoreItem(FieldTypes.FULGUR, p), new Item.Properties());
    public static final DeferredItem<FieldCoreItem> NEXUS_CORE = ITEMS.registerItem("nexus_core",
            p -> new FieldCoreItem(FieldTypes.NEXUS, p), new Item.Properties());
    public static final DeferredItem<FieldCoreItem> MUTATIO_CORE = ITEMS.registerItem("mutatio_core",
            p -> new FieldCoreItem(FieldTypes.MUTATIO, p), new Item.Properties());

    public static final DeferredItem<InterferenceStaffItem> INTERFERENCE_STAFF = ITEMS.registerItem("interference_staff",
            InterferenceStaffItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<ResonanceLensItem> RESONANCE_LENS = ITEMS.registerItem("resonance_lens",
            ResonanceLensItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredItem<FieldPrimerItem> FIELD_PRIMER = ITEMS.registerItem("field_primer",
            FieldPrimerItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredItem<BlockItem> FIELD_EMITTER = ITEMS.registerSimpleBlockItem(ArcanaBlocks.FIELD_EMITTER);
    public static final DeferredItem<BlockItem> FIELD_RESONATOR = ITEMS.registerSimpleBlockItem(ArcanaBlocks.FIELD_RESONATOR);
    public static final DeferredItem<BlockItem> FIELD_SHAPER = ITEMS.registerSimpleBlockItem(ArcanaBlocks.FIELD_SHAPER);
    public static final DeferredItem<BlockItem> FIELD_SENSOR = ITEMS.registerSimpleBlockItem(ArcanaBlocks.FIELD_SENSOR);

    private ArcanaItems() {
    }
}
