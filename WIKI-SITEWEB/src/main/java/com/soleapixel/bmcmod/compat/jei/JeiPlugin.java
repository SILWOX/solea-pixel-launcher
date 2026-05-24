package com.soleapixel.bmcmod.compat.jei;

import net.minecraft.world.item.ItemStack;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.registry.ModBlocks;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

@mezz.jei.api.JeiPlugin
public final class JeiPlugin implements IModPlugin {
    public static final mezz.jei.api.recipe.RecipeType<JeiRecipes.JeiInfusionRecipe> INFUSION_TYPE =
            mezz.jei.api.recipe.RecipeType.create(BmcMod.MODID, "infusion", JeiRecipes.JeiInfusionRecipe.class);
    public static final mezz.jei.api.recipe.RecipeType<JeiRecipes.JeiFoundryRecipe> FOUNDRY_TYPE =
            mezz.jei.api.recipe.RecipeType.create(BmcMod.MODID, "foundry", JeiRecipes.JeiFoundryRecipe.class);
    public static final mezz.jei.api.recipe.RecipeType<JeiRecipes.JeiUpgradeRecipe> UPGRADE_TYPE =
            mezz.jei.api.recipe.RecipeType.create(BmcMod.MODID, "upgrade_table", JeiRecipes.JeiUpgradeRecipe.class);

    @Override
    public net.minecraft.resources.ResourceLocation getPluginUid() {
        return BmcMod.loc("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new JeiCategories.InfusionRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new JeiCategories.FoundryRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new JeiCategories.UpgradeRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(INFUSION_TYPE, JeiRecipes.allInfusion());
        registration.addRecipes(FOUNDRY_TYPE, JeiRecipes.allFoundry());
        registration.addRecipes(UPGRADE_TYPE, JeiRecipes.allUpgradeTable());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.INFUSION_TABLE.get()), INFUSION_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ENDSTONE_FURNACE.get()), RecipeTypes.SMELTING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.FOUNDRY.get()), FOUNDRY_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.UPGRADE_TABLE.get()), UPGRADE_TYPE);
    }

    static ItemStack iconStack() {
        return new ItemStack(ModBlocks.INFUSION_TABLE.get());
    }

    static ItemStack foundryIconStack() {
        return new ItemStack(ModBlocks.FOUNDRY.get());
    }
}
