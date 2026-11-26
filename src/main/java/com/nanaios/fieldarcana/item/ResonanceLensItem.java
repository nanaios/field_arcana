package com.nanaios.fieldarcana.item;

import com.nanaios.fieldarcana.registry.ArcanaComponents;
import com.nanaios.fieldtheory.api.FieldTheoryAPI;
import com.nanaios.fieldtheory.api.FieldType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 共鳴鏡 — makes fields visible. While held, field lines are traced from every nearby source and
 * the local potential and field strength are shown above the hotbar. Right-click to choose which
 * field to observe.
 */
public class ResonanceLensItem extends Item {

    public ResonanceLensItem(Properties properties) {
        super(properties);
    }

    /** The observed fields (all of them when none is selected). */
    public static List<FieldType> observed(ItemStack stack) {
        ResourceLocation id = stack.get(ArcanaComponents.LENS_FIELD.get());
        FieldType type = id == null ? null : FieldTheoryAPI.fieldTypes().get(id);
        if (type != null) {
            return List.of(type);
        }
        List<FieldType> all = new ArrayList<>();
        FieldTheoryAPI.fieldTypes().forEach(all::add);
        return all;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack lens = player.getItemInHand(hand);
        if (!level.isClientSide) {
            List<ResourceLocation> ids = new ArrayList<>(FieldTheoryAPI.fieldTypes().keySet());
            ids.sort(ResourceLocation::compareTo);
            ResourceLocation current = lens.get(ArcanaComponents.LENS_FIELD.get());
            int index = current == null ? -1 : ids.indexOf(current);
            int next = index + 1;
            Component name;
            if (next >= ids.size()) {
                lens.remove(ArcanaComponents.LENS_FIELD.get());
                name = Component.translatable("message.field_arcana.lens.all");
            } else {
                lens.set(ArcanaComponents.LENS_FIELD.get(), ids.get(next));
                name = FieldTheoryAPI.fieldTypes().get(ids.get(next)).displayName();
            }
            player.displayClientMessage(Component.translatable("message.field_arcana.lens.observing", name), true);
            level.playSound(null, player.blockPosition(), SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS, 0.8F, 1.3F);
        }
        return InteractionResultHolder.sidedSuccess(lens, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        List<FieldType> observed = observed(stack);
        Component what = observed.size() == 1 ? observed.get(0).displayName() : Component.translatable("message.field_arcana.lens.all");
        tooltip.add(Component.translatable("message.field_arcana.lens.observing", what).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.field_arcana.lens.usage").withStyle(ChatFormatting.DARK_GRAY));
    }
}
