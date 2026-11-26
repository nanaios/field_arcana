package com.nanaios.fieldarcana.compat.jei;

import com.nanaios.fieldarcana.FieldArcana;
import com.nanaios.fieldarcana.phenomena.Transmutation;
import com.nanaios.fieldarcana.registry.ArcanaItems;
import com.nanaios.fieldarcana.registry.ArcanaRecipes;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

/** JEI integration (loaded by JEI only when JEI is installed). */
@JeiPlugin
public class ArcanaJeiPlugin implements IModPlugin {
    public static final RecipeType<RecipeHolder<Transmutation>> TRANSMUTATION =
            RecipeType.createRecipeHolderType(FieldArcana.id("transmutation"));

    @Override
    public ResourceLocation getPluginUid() {
        return FieldArcana.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new TransmutationCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        List<RecipeHolder<Transmutation>> recipes = level == null ? List.of()
                : level.getRecipeManager().getAllRecipesFor(ArcanaRecipes.TRANSMUTATION.get());
        registration.addRecipes(TRANSMUTATION, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(TRANSMUTATION, ArcanaItems.MUTATIO_CORE.get(), ArcanaItems.FIELD_EMITTER.get());
    }
}
