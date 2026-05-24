package com.soleapixel.bmcmod.compat.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

public final class JeiCategories {
    private JeiCategories() {
    }

    @SuppressWarnings("removal")
    public static final class InfusionRecipeCategory implements IRecipeCategory<JeiRecipes.JeiInfusionRecipe> {
        private final IDrawable background;
        private final IDrawable icon;

        public InfusionRecipeCategory(IGuiHelper guiHelper) {
            this.background = guiHelper.createBlankDrawable(JeiConstants.INFUSION_WIDTH, JeiConstants.INFUSION_HEIGHT);
            this.icon = guiHelper.createDrawableItemStack(JeiPlugin.iconStack());
        }

        @Override
        public RecipeType<JeiRecipes.JeiInfusionRecipe> getRecipeType() {
            return JeiPlugin.INFUSION_TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("jei.bmcmod.infusion.title");
        }

        @Override
        public IDrawable getBackground() {
            return background;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, JeiRecipes.JeiInfusionRecipe recipe, IFocusGroup focuses) {
            var stacks = recipe.ingredientStacks();
            int n = stacks.size();
            int cols = Math.min(4, Math.max(1, n));
            int startX = (JeiConstants.INFUSION_WIDTH - cols * 18) / 2;
            int startY = 8;
            for (int i = 0; i < n; i++) {
                int row = i / cols;
                int col = i % cols;
                builder.addSlot(RecipeIngredientRole.INPUT, startX + col * 18, startY + row * 18)
                        .addItemStack(stacks.get(i));
            }
            int outY = startY + ((n + cols - 1) / cols) * 18 + 8;
            int outX = (JeiConstants.INFUSION_WIDTH - 18) / 2;
            builder.addSlot(RecipeIngredientRole.OUTPUT, outX, Math.min(outY, JeiConstants.INFUSION_HEIGHT - 24))
                    .addItemStack(recipe.result());
        }

        @Override
        public void draw(
                JeiRecipes.JeiInfusionRecipe recipe,
                IRecipeSlotsView recipeSlotsView,
                GuiGraphics graphics,
                double mouseX,
                double mouseY) {
            String souls = String.valueOf(recipe.soulCost());
            graphics.drawString(
                    Minecraft.getInstance().font,
                    Component.translatable("jei.bmcmod.infusion.souls", souls),
                    4,
                    JeiConstants.INFUSION_HEIGHT - 14,
                    0x404040,
                    false);
        }
    }

    @SuppressWarnings("removal")
    public static final class FoundryRecipeCategory implements IRecipeCategory<JeiRecipes.JeiFoundryRecipe> {
        private static final ResourceLocation FURNACE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");
        private final IDrawable background;
        private final IDrawable icon;
        private final IDrawableAnimated flame;
        private final IDrawableAnimated arrow;

        public FoundryRecipeCategory(IGuiHelper guiHelper) {
            this.background = guiHelper.createDrawable(FURNACE_TEXTURE, 55, 16, JeiConstants.FOUNDRY_WIDTH, JeiConstants.FOUNDRY_HEIGHT);
            this.icon = guiHelper.createDrawableItemStack(JeiPlugin.foundryIconStack());
            IDrawableStatic flameStatic = guiHelper.createDrawable(FURNACE_TEXTURE, 176, 0, 14, 14);
            IDrawableStatic arrowStatic = guiHelper.createDrawable(FURNACE_TEXTURE, 176, 14, 24, 17);
            this.flame = guiHelper.createAnimatedDrawable(flameStatic, 300, IDrawableAnimated.StartDirection.TOP, true);
            this.arrow = guiHelper.createAnimatedDrawable(arrowStatic, 400, IDrawableAnimated.StartDirection.LEFT, false);
        }

        @Override
        public RecipeType<JeiRecipes.JeiFoundryRecipe> getRecipeType() {
            return JeiPlugin.FOUNDRY_TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("jei.bmcmod.foundry.title");
        }

        @Override
        public IDrawable getBackground() {
            return background;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, JeiRecipes.JeiFoundryRecipe recipe, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.INPUT, 1, 1).addItemStack(recipe.input());
            builder.addSlot(RecipeIngredientRole.CATALYST, 1, 37).addItemStacks(recipe.fuelStacks());
            builder.addSlot(RecipeIngredientRole.OUTPUT, 61, 19).addItemStack(recipe.displayOutput());
        }

        @Override
        public void draw(
                JeiRecipes.JeiFoundryRecipe recipe,
                IRecipeSlotsView recipeSlotsView,
                GuiGraphics graphics,
                double mouseX,
                double mouseY) {
            flame.draw(graphics, 24, 23);
            arrow.draw(graphics, 24, 18);
            var font = Minecraft.getInstance().font;
            String yields = recipe.minYield() == recipe.maxYield()
                    ? String.valueOf(recipe.maxYield())
                    : recipe.minYield() + "-" + recipe.maxYield();
            graphics.drawString(font, Component.translatable("jei.bmcmod.foundry.yield", yields), 1, 58, 0x404040, false);
        }
    }

    @SuppressWarnings("removal")
    public static final class UpgradeRecipeCategory implements IRecipeCategory<JeiRecipes.JeiUpgradeRecipe> {
        private final IDrawable background;
        private final IDrawable icon;

        public UpgradeRecipeCategory(IGuiHelper guiHelper) {
            this.background = guiHelper.createBlankDrawable(JeiConstants.UPGRADE_WIDTH, JeiConstants.UPGRADE_HEIGHT);
            this.icon = guiHelper.createDrawableItemStack(new ItemStack(com.soleapixel.bmcmod.registry.ModBlocks.UPGRADE_TABLE.get()));
        }

        @Override
        public RecipeType<JeiRecipes.JeiUpgradeRecipe> getRecipeType() {
            return JeiPlugin.UPGRADE_TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("jei.bmcmod.upgrade.title");
        }

        @Override
        public IDrawable getBackground() {
            return background;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, JeiRecipes.JeiUpgradeRecipe recipe, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.INPUT, 8, 17).addItemStack(recipe.upgradeItem());
            builder.addSlot(RecipeIngredientRole.INPUT, 26, 17).addItemStack(recipe.chestplate());
            builder.addSlot(RecipeIngredientRole.INPUT, 44, 17).addItemStack(recipe.shard());
            builder.addSlot(RecipeIngredientRole.OUTPUT, 94, 17).addItemStack(recipe.output());
        }

        @Override
        public void draw(JeiRecipes.JeiUpgradeRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, ">", 74, 22, 0x707070, false);
        }
    }
}
