package com.nanaios.fieldarcana.item;

import com.nanaios.fieldtheory.api.FieldType;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A crystallised resonance with one field. Selects the field for staffs, emitters and sensors. */
public class FieldCoreItem extends Item {
    private final Supplier<FieldType> field;

    public FieldCoreItem(Supplier<FieldType> field, Properties properties) {
        super(properties);
        this.field = field;
    }

    public FieldType field() {
        return field.get();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FieldType type = field();
        tooltip.add(Component.translatable("tooltip.field_arcana.core.field", type.displayName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.field_arcana.core." + type.id().getPath()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
