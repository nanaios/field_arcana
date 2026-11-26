package com.nanaios.fieldarcana.compat.jei;

import com.nanaios.fieldarcana.phenomena.BlockMatcher;
import com.nanaios.fieldarcana.phenomena.Transmutation;
import com.nanaios.fieldarcana.registry.ArcanaItems;
import com.nanaios.fieldtheory.api.FieldType;
import com.nanaios.fieldtheory.api.FieldTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;

/** Shows a transmutation: input block → output block, with the field conditions underneath. */
public class TransmutationCategory extends AbstractRecipeCategory<RecipeHolder<Transmutation>> {
    private static final int WIDTH = 150;
    private static final int LINE = 10;
    private final IDrawableStatic arrow;

    public TransmutationCategory(IGuiHelper guiHelper) {
        super(ArcanaJeiPlugin.TRANSMUTATION, Component.translatable("jei.field_arcana.transmutation"),
                guiHelper.createDrawableItemLike(ArcanaItems.MUTATIO_CORE.get()), WIDTH, 22 + LINE * 4);
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<Transmutation> holder, IFocusGroup focuses) {
        Transmutation recipe = holder.value();
        builder.addInputSlot(30, 1).setStandardSlotBackground().addItemStacks(inputs(recipe.input()));
        builder.addOutputSlot(100, 1).setStandardSlotBackground().addItemStack(new ItemStack(recipe.output()));
    }

    @Override
    public void draw(RecipeHolder<Transmutation> holder, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, 62, 1);
        Font font = Minecraft.getInstance().font;
        int y = 22;
        for (Component line : conditionLines(holder.value())) {
            graphics.drawString(font, line, 2, y, 0xFF404040, false);
            y += LINE;
        }
    }

    static List<Component> conditionLines(Transmutation recipe) {
        List<Component> lines = new ArrayList<>();
        lines.add(bound(fieldName(FieldTypes.MUTATIO.get()), recipe.catalyst(), Double.POSITIVE_INFINITY));
        for (Transmutation.Condition c : recipe.conditions()) {
            lines.add(bound(fieldName(c.field()), c.min(), c.max()));
        }
        return lines;
    }

    /** The field's name in a darker shade of its colour, readable on JEI's light background. */
    private static MutableComponent fieldName(FieldType type) {
        int c = type.color();
        int dark = ((((c >> 16) & 0xFF) * 11 / 20) << 16) | ((((c >> 8) & 0xFF) * 11 / 20) << 8) | ((c & 0xFF) * 11 / 20);
        return Component.translatable(type.descriptionId()).withStyle(style -> style.withColor(dark));
    }

    private static Component bound(MutableComponent field, double min, double max) {
        boolean hasMin = Double.isFinite(min);
        boolean hasMax = Double.isFinite(max);
        String range;
        if (hasMin && hasMax) {
            range = String.format(Locale.ROOT, " %.1f ≤ φ ≤ %.1f", min, max);
        } else if (hasMin) {
            range = String.format(Locale.ROOT, " φ ≥ %.1f", min);
        } else if (hasMax) {
            range = String.format(Locale.ROOT, " φ ≤ %.1f", max);
        } else {
            range = "";
        }
        return field.append(Component.literal(range));
    }

    private static List<ItemStack> inputs(BlockMatcher matcher) {
        List<ItemStack> stacks = new ArrayList<>();
        matcher.block().ifPresent(b -> addIfItem(stacks, b));
        matcher.tag().ifPresent(tag -> {
            for (Holder<Block> holder : BuiltInRegistries.BLOCK.getTagOrEmpty(tag)) {
                addIfItem(stacks, holder.value());
            }
        });
        return stacks;
    }

    private static void addIfItem(List<ItemStack> stacks, Block block) {
        if (block.asItem() != Items.AIR) {
            stacks.add(new ItemStack(block));
        }
    }
}
