package com.soleapixel.bmcmod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.menu.InfusionTableMenu;
import com.soleapixel.bmcmod.recipe.infusion.InfusionRecipe;
import com.soleapixel.bmcmod.recipe.infusion.InfusionRecipes;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class InfusionTableMenuScreen extends AbstractContainerScreen<InfusionTableMenu> {
    public static final ResourceLocation GUI = BmcMod.loc("textures/gui/infusion/infusion_table.png");
    /** Flèche résultat (défaut 48×16) ; remplacez le PNG (ex. 100×32) en gardant cohérence des constantes. */
    public static final ResourceLocation ARROW = BmcMod.loc("textures/gui/infusion/infusion_arrow.png");
    public static final ResourceLocation SLOT_DIS = BmcMod.loc("textures/gui/infusion/infusion_slot.png");
    public static final ResourceLocation SLOT_HI = BmcMod.loc("textures/gui/infusion/infusion_slot_highlighted.png");
    public static final ResourceLocation SLOT_OFF = BmcMod.loc("textures/gui/infusion/infusion_slot_disabled.png");

    /** Planche PNG 256x256, zone d’affichage utile 176x184 (comme le coffre vanilla + un peu de hauteur). */
    private static final int TX_SHEET_W = 256;
    private static final int TX_SHEET_H = 256;
    private static final int IMAGE_W = 176;
    private static final int IMAGE_H = 184;

    /** Fichiers infusion_slot*.png : 108x19, zone utile en plein dans la texture. */
    private static final int BTN_DX = 59;
    private static final int BTN_DY = 19;
    private static final int BTN_W = 108;
    private static final int BTN_H = 19;
    private static final int BTN_TEX_W = 108;
    private static final int BTN_TEX_H = 19;

    /** Grille 176×184 : flèche entre l’atelier 2×2 et le slot de sortie. */
    private static final int ARROW_DX = 73;
    private static final int ARROW_DY = 59;
    private static final int ARROW_W = 48;
    private static final int ARROW_H = 16;
    private static final int ARROW_TEX_W = 48;
    private static final int ARROW_TEX_H = 16;

    private static final ResourceLocation SGA_FONT = ResourceLocation.withDefaultNamespace("alt");

    /** Bouton livre (sprites vanilla) — sous la flèche, comme demandé. */
    private static final int RECIPE_BOOK_BTN_W = 20;
    private static final int RECIPE_BOOK_BTN_H = 18;
    private static final int RECIPE_BOOK_BTN_X = ARROW_DX + ARROW_W / 2 - RECIPE_BOOK_BTN_W / 2;
    private static final int RECIPE_BOOK_BTN_Y = ARROW_DY + ARROW_H + 2;
    /** Zéro : le cadre 147×166 du PNG vanilla est utilisé tel quel (comme le livre de l’établi). */
    private static final int BOOK_MARGIN = 0;
    /** Dimensions officielles du livre ({@link RecipeBookComponent}). */
    private static final int BOOK_BODY_W = RecipeBookComponent.IMAGE_WIDTH;
    private static final int BOOK_BODY_H = RecipeBookComponent.IMAGE_HEIGHT;
    private static final int BOOK_COLS = 5;
    private static final int BOOK_ROWS = 4;
    private static final int BOOK_CELL = 25;
    private static final int BOOK_RECIPES_PER_PAGE = BOOK_COLS * BOOK_ROWS;
    /** Cases de résultat (sprites vanilla). */
    private static final ResourceLocation BOOK_SLOT_OK =
            ResourceLocation.withDefaultNamespace("recipe_book/slot_craftable");
    private static final ResourceLocation BOOK_SLOT_NO =
            ResourceLocation.withDefaultNamespace("recipe_book/slot_uncraftable");
    /**
     * Fond du panneau : atlas vanilla {@code textures/gui/recipe_book.png} (le sprite
     * {@code recipe_book/list_background} n’existe pas en 1.21.1 — damier magenta).
     */
    private static final ResourceLocation BOOK_PANEL_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/recipe_book.png");
    /** UV du cadre 147×166 (même découpe que {@link RecipeBookComponent#render}). */
    private static final int BOOK_PANEL_U = 1;
    private static final int BOOK_PANEL_V = 1;

    /** Offsets identiques à {@code RecipeBookComponent.initVisuals} / {@code RecipeBookPage.init}. */
    private static final int BOOK_SEARCH_X = 25;
    private static final int BOOK_SEARCH_Y = 13;
    private static final int BOOK_SEARCH_W = 81;
    private static final int BOOK_SEARCH_H = 14;
    private static final int BOOK_FILTER_X = 110;
    private static final int BOOK_FILTER_Y = 12;
    private static final int BOOK_FILTER_W = 26;
    private static final int BOOK_FILTER_H = 16;
    private static final int BOOK_GRID_ORIGIN_X = 11;
    private static final int BOOK_GRID_ORIGIN_Y = 31;
    /** Limite Y exclusive des cases (31 + 4×25). */
    private static final int BOOK_GRID_Y_END = BOOK_GRID_ORIGIN_Y + BOOK_ROWS * BOOK_CELL;
    private static final int BOOK_PAGE_BACK_X = 38;
    private static final int BOOK_PAGE_FWD_X = 93;
    private static final int BOOK_PAGE_Y = 137;
    private static final int BOOK_PAGE_W = 12;
    private static final int BOOK_PAGE_H = 17;
    private static final int BOOK_PAGE_TEXT_ANCHOR_X = 73;
    private static final int BOOK_PAGE_TEXT_Y = 141;

    private static final Component BOOK_SEARCH_HINT =
            Component.translatable("gui.recipebook.search_hint")
                    .withStyle(ChatFormatting.ITALIC)
                    .withStyle(ChatFormatting.GRAY);

    private static final WidgetSprites BOOK_FILTER_SPRITES =
            new WidgetSprites(
                    ResourceLocation.withDefaultNamespace("recipe_book/filter_enabled"),
                    ResourceLocation.withDefaultNamespace("recipe_book/filter_disabled"),
                    ResourceLocation.withDefaultNamespace("recipe_book/filter_enabled_highlighted"),
                    ResourceLocation.withDefaultNamespace("recipe_book/filter_disabled_highlighted"));
    private static final WidgetSprites BOOK_PAGE_FORWARD_SPRITES =
            new WidgetSprites(
                    ResourceLocation.withDefaultNamespace("recipe_book/page_forward"),
                    ResourceLocation.withDefaultNamespace("recipe_book/page_forward_highlighted"));
    private static final WidgetSprites BOOK_PAGE_BACKWARD_SPRITES =
            new WidgetSprites(
                    ResourceLocation.withDefaultNamespace("recipe_book/page_backward"),
                    ResourceLocation.withDefaultNamespace("recipe_book/page_backward_highlighted"));

    private ImageButton recipeBookButton;
    private EditBox infusionRecipeSearch;
    private boolean recipeBookOpen;
    private int infusionRecipeBookPage;
    private boolean infusionFilterCraftableOnly;
    private InfusionRecipe hoveredBookRecipe;
    /** Recette choisie dans le livre : aperçu « fantôme » dans la grille 2×2. */
    private InfusionRecipe selectedRecipe;
    private int filterBtnX;
    private int filterBtnY;
    private int pageNextBtnX;
    private int pageNextBtnY;
    private int pagePrevBtnX;
    private int pagePrevBtnY;

    /** Liste triée des recettes (stable, évite de retrier à chaque ouverture du livre). */
    private static volatile List<InfusionRecipe> sortedRecipesCached;

    /** Cache du filtre livre : invalidé si inventaire, recherche ou filtre changent. */
    private List<InfusionRecipe> filteredRecipeCache;
    private int filteredRecipeCacheSlotsStamp = Integer.MIN_VALUE;
    private String filteredRecipeCacheQuery = "";
    private boolean filteredRecipeCacheCraftableOnly;

    /** Texte « runes » (police d’enclume) — uniquement pour le bandeau, comme l’enchant. */
    private static Component withEnchantFont(Component c) {
        return c.plainCopy().withStyle(Style.EMPTY.withFont(SGA_FONT));
    }

    public InfusionTableMenuScreen(InfusionTableMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = IMAGE_W;
        this.imageHeight = IMAGE_H;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 7;
        this.titleLabelY = 6;
        this.inventoryLabelX = 7;
        /* Première rangée d’inventaire à y=102 ({@link InfusionTableMenu}) : 97 recouvrait les cases ; ~12 px comme un coffre vanilla. */
        this.inventoryLabelY = 90;
        this.recipeBookButton = this.addRenderableWidget(
                new ImageButton(
                        this.leftPos + RECIPE_BOOK_BTN_X,
                        this.topPos + RECIPE_BOOK_BTN_Y,
                        RECIPE_BOOK_BTN_W,
                        RECIPE_BOOK_BTN_H,
                        RecipeBookComponent.RECIPE_BUTTON_SPRITES,
                        b -> {
                            this.recipeBookOpen = !this.recipeBookOpen;
                            if (!this.recipeBookOpen) {
                                this.infusionRecipeBookPage = 0;
                                this.hoveredBookRecipe = null;
                                this.selectedRecipe = null;
                                this.infusionRecipeSearch.setFocused(false);
                            } else {
                                this.infusionRecipeBookPage = 0;
                            }
                        },
                        Component.translatable("gui.bmcmod.infusion.recipe_book")));
        this.recipeBookButton.setTooltip(
                Tooltip.create(Component.translatable("gui.bmcmod.infusion.recipe_book.tooltip")));
        this.infusionRecipeSearch = new EditBox(this.font, 0, 0, BOOK_SEARCH_W, BOOK_SEARCH_H, Component.empty());
        this.infusionRecipeSearch.setMaxLength(50);
        this.infusionRecipeSearch.setTextColor(16777215);
        this.infusionRecipeSearch.setHint(BOOK_SEARCH_HINT);
        this.infusionRecipeSearch.setResponder(s -> {
            this.infusionRecipeBookPage = 0;
            this.invalidateFilteredRecipeCache();
        });
        this.infusionRecipeSearch.setVisible(false);
        /*
         * addWidget sans renderables : comme {@link net.minecraft.client.gui.screens.inventory.CraftingScreen},
         * le livre est dessiné après renderBackground (voile semi-transparent), sinon le panneau paraît assombri.
         * On appelle {@link EditBox#render} nous-mêmes après le fond du livre.
         */
        this.addWidget(this.infusionRecipeSearch);
    }

    @Override
    public void removed() {
        super.removed();
        this.filteredRecipeCache = null;
    }

    private static List<InfusionRecipe> sortedRecipesLazy() {
        if (sortedRecipesCached == null) {
            synchronized (InfusionTableMenuScreen.class) {
                if (sortedRecipesCached == null) {
                    List<InfusionRecipe> list = new ArrayList<>(InfusionRecipes.ENTRIES);
                    list.sort(
                            Comparator.comparing(
                                    r -> BuiltInRegistries.ITEM.getKey(r.result.getItem()).toString()));
                    sortedRecipesCached = Collections.unmodifiableList(list);
                }
            }
        }
        return sortedRecipesCached;
    }

    /** Empreinte des stacks de tous les slots (grille + joueur) pour invalider le cache filtré craftable. */
    private int computeAllMenuSlotsStamp() {
        int h = 1;
        for (Slot slot : this.menu.slots) {
            h = 31 * h + ItemStack.hashItemAndComponents(slot.getItem());
        }
        return h;
    }

    private void invalidateFilteredRecipeCache() {
        this.filteredRecipeCache = null;
        this.filteredRecipeCacheSlotsStamp = Integer.MIN_VALUE;
    }

    /**
     * Liste filtrée pour le livre (recherche + filtre craftable), avec cache tant que l’inventaire et
     * les critères sont inchangés.
     */
    private List<InfusionRecipe> filteredInfusionRecipesCached() {
        String q = this.infusionRecipeSearch.getValue().trim();
        boolean craftable = this.infusionFilterCraftableOnly;
        int stamp = this.computeAllMenuSlotsStamp();
        if (this.filteredRecipeCache != null
                && stamp == this.filteredRecipeCacheSlotsStamp
                && q.equals(this.filteredRecipeCacheQuery)
                && craftable == this.filteredRecipeCacheCraftableOnly) {
            return this.filteredRecipeCache;
        }
        List<InfusionRecipe> base = sortedRecipesLazy();
        List<InfusionRecipe> out = new ArrayList<>();
        for (InfusionRecipe r : base) {
            if (craftable && !InfusionRecipeBookHelper.isFullyCraftable(this.menu, r)) {
                continue;
            }
            if (!q.isEmpty() && !this.recipeMatchesSearch(r, q)) {
                continue;
            }
            out.add(r);
        }
        this.filteredRecipeCache = out;
        this.filteredRecipeCacheSlotsStamp = stamp;
        this.filteredRecipeCacheQuery = q;
        this.filteredRecipeCacheCraftableOnly = craftable;
        return this.filteredRecipeCache;
    }

    @Override
    protected void updateNarratedWidget(NarrationElementOutput narration) {
        super.updateNarratedWidget(narration);
        if (this.recipeBookOpen) {
            narration.add(
                    NarratedElementType.TITLE,
                    Component.translatable("gui.bmcmod.infusion.recipe_book.accessibility_panel")
                            .getString());
            this.infusionRecipeSearch.updateWidgetNarration(narration.nest());
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.recipeBookOpen && !this.infusionRecipeSearch.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_PAGE_UP || keyCode == GLFW.GLFW_KEY_LEFT) {
                int pages = Math.max(1, this.recipeBookPageCount());
                if (pages > 1) {
                    this.infusionRecipeBookPage = Math.max(0, this.infusionRecipeBookPage - 1);
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN || keyCode == GLFW.GLFW_KEY_RIGHT) {
                int pages = Math.max(1, this.recipeBookPageCount());
                if (pages > 1) {
                    this.infusionRecipeBookPage = Math.min(pages - 1, this.infusionRecipeBookPage + 1);
                }
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.recipeBookOpen) {
            this.positionInfusionRecipeBookWidgets();
        }
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (this.recipeBookOpen) {
            this.positionInfusionRecipeBookWidgets();
        }
        super.render(g, mouseX, mouseY, partialTick);
        if (this.recipeBookOpen) {
            this.renderRecipeBookBackdrop(g, mouseX, mouseY);
            this.infusionRecipeSearch.render(g, mouseX, mouseY, partialTick);
            this.renderRecipePanelTooltip(g, mouseX, mouseY);
            this.renderRecipeBookFilterTooltip(g, mouseX, mouseY);
        }
        this.renderSelectedRecipeGhosts(g);
        /* Comme {@link BackpackChestScreen} : infobulles d’inventaire et pile sous le curseur. */
        this.renderTooltip(g, mouseX, mouseY);
        if (shouldRenderInfusionButtonHoverTooltip(mouseX, mouseY)) {
            this.renderInfusionButtonTooltip(g, mouseX, mouseY);
        } else if (shouldRenderInfusionButtonDisabledTooltip(mouseX, mouseY)) {
            this.renderInfusionButtonDisabledTooltip(g, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        g.blit(
                GUI,
                x,
                y,
                0,
                0,
                this.imageWidth,
                this.imageHeight,
                TX_SHEET_W,
                TX_SHEET_H);
        /*
         * Flèche (73,59) : comme un four — uniquement pendant l’infusion, largeur p/pmax
         * (fields 0/1, field 4 = 1 ; sinon pas de flèche).
         */
        int pmax = Math.max(1, this.menu.getField(1));
        int p = Mth.clamp(this.menu.getField(0), 0, pmax);
        if (this.menu.getField(4) == 1) {
            int aw;
            if (p >= pmax) {
                aw = ARROW_W;
            } else if (p > 0) {
                aw = Mth.clamp((p * ARROW_W + pmax - 1) / pmax, 1, ARROW_W);
            } else {
                aw = 0;
            }
            if (aw > 0) {
                g.blit(ARROW, x + ARROW_DX, y + ARROW_DY, 0, 0, aw, ARROW_H, ARROW_TEX_W, ARROW_TEX_H);
            }
        }
        int can = this.menu.getField(3);
        if (can == 0) {
            g.blit(
                    SLOT_OFF, x + BTN_DX, y + BTN_DY, 0, 0, BTN_W, BTN_H, BTN_TEX_W, BTN_TEX_H);
        } else if (isOverButton(mouseX, mouseY)) {
            g.blit(
                    SLOT_HI, x + BTN_DX, y + BTN_DY, 0, 0, BTN_W, BTN_H, BTN_TEX_W, BTN_TEX_H);
        } else {
            g.blit(
                    SLOT_DIS, x + BTN_DX, y + BTN_DY, 0, 0, BTN_W, BTN_H, BTN_TEX_W, BTN_TEX_H);
        }
        this.drawInfusionButtonFrame(g, x, y, can, isOverButton(mouseX, mouseY));
        this.renderButtonEnchantRuneText(g, x, y, can);
    }

    /**
     * Cadre discret (style case vanilla) pour le bandeau, plus accentué au survol.
     */
    private void drawInfusionButtonFrame(GuiGraphics g, int baseX, int baseY, int canStart, boolean hovering) {
        int x0 = baseX + BTN_DX;
        int y0 = baseY + BTN_DY;
        int x1 = x0 + BTN_W;
        int y1 = y0 + BTN_H;
        if (canStart == 0) {
            int dim = 0xFF0D0B09;
            g.hLine(x0, x1 - 1, y0, dim);
            g.hLine(x0, x1 - 1, y1 - 1, 0xFF1A1210);
            g.vLine(x0, y0, y1 - 1, dim);
            g.vLine(x1 - 1, y0, y1 - 1, 0xFF1A1210);
            return;
        }
        int topLeft = hovering ? 0xFF4A3D32 : 0xFF1E1915;
        int bottomRight = 0xFF0C0907;
        g.hLine(x0, x1 - 1, y0, topLeft);
        g.vLine(x0, y0, y1 - 1, topLeft);
        g.hLine(x0, x1 - 1, y1 - 1, bottomRight);
        g.vLine(x1 - 1, y0, y1 - 1, bottomRight);
    }

    /** Infobulle du bandeau : seulement si l’inventaire ne tient rien (sinon on laisse la tooltips item / curseur). */
    private boolean shouldRenderInfusionButtonHoverTooltip(int mouseX, int mouseY) {
        if (!isOverButton(mouseX, mouseY) || !this.menu.getCarried().isEmpty()) {
            return false;
        }
        return this.menu.getField(3) == 1 && this.localPreviewRecipe().isPresent();
    }

    private boolean shouldRenderInfusionButtonDisabledTooltip(int mouseX, int mouseY) {
        if (!isOverButton(mouseX, mouseY) || !this.menu.getCarried().isEmpty()) {
            return false;
        }
        return this.menu.getField(3) == 0;
    }

    /** Bandeau grisé : expliquer pourquoi l’infusion ne part pas encore. */
    private void renderInfusionButtonDisabledTooltip(GuiGraphics g, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        Optional<InfusionRecipe> rec = this.localPreviewRecipe();
        if (rec.isEmpty()) {
            lines.add(Component.translatable("gui.bmcmod.infusion.button.disabled.no_recipe"));
        } else {
            InfusionRecipe r = rec.get();
            lines.add(r.result.getHoverName().copy());
            if (this.menu.getSlot(0).getItem().isEmpty()) {
                lines.add(
                        Component.translatable("gui.bmcmod.infusion.button.disabled.need_crystal")
                                .withStyle(ChatFormatting.RED));
            } else if (!InfusionRecipeBookHelper.hasEnoughSouls(this.menu, r)) {
                lines.add(
                        Component.translatable("gui.bmcmod.infusion.button.disabled.need_souls")
                                .withStyle(ChatFormatting.RED));
            }
            if (!InfusionRecipeBookHelper.hasEnoughIngredients(this.menu, r)) {
                lines.add(
                        Component.translatable("gui.bmcmod.infusion.button.disabled.need_items")
                                .withStyle(ChatFormatting.RED));
            }
            if (!InfusionRecipeBookHelper.outputCanAcceptResult(this.menu, r)) {
                lines.add(
                        Component.translatable("gui.bmcmod.infusion.button.disabled.output_blocked")
                                .withStyle(ChatFormatting.RED));
            }
            if (lines.size() == 1) {
                lines.add(
                        Component.translatable("gui.bmcmod.infusion.button.disabled.wait")
                                .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    /**
     * Style table d’enchantement (sans lapis) : runes (police {@code alt}) au centre, coût en
     * vert à droite en texte standard (même principe que le chiffre vert d’expérience vanilla).
     */
    private void renderButtonEnchantRuneText(GuiGraphics g, int baseX, int baseY, int canStart) {
        Optional<InfusionRecipe> rec = this.localPreviewRecipe();
        if (rec.isEmpty()) {
            return;
        }
        InfusionRecipe r = rec.get();
        int souls = r.soulCost;
        if (souls <= 0) {
            souls = this.menu.getField(2);
        }
        int tx = baseX + BTN_DX;
        int ty = baseY + BTN_DY;
        int runeColor = (canStart == 1) ? 0x2D1F1A : 0x8A7A6A;
        int textY = ty + 5;
        /* Pas d’icône XP. Coût aligné droite, vert, police vanilla. */
        Component cost = Component.literal(String.valueOf(souls))
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
        int rightX = tx + BTN_W - 4 - this.font.width(cost);
        int maxRuneW = Math.max(0, rightX - tx - 6);
        if (maxRuneW < 4) {
            g.drawString(this.font, cost, rightX, textY, 0, true);
            return;
        }
        Component nameRune = withEnchantFont(r.result.getHoverName().plainCopy());
        Component trunc = truncateSgaName(nameRune, maxRuneW);
        int runeW = this.font.width(trunc);
        int runeX = tx + 4 + Math.max(0, (maxRuneW - runeW) / 2);
        g.drawString(this.font, trunc, runeX, textY, runeColor, true);
        g.drawString(this.font, cost, rightX, textY, 0, true);
    }

    /** Infobulle lisible (comme l’enchant) : item + coût en âmes, sans runes. */
    private void renderInfusionButtonTooltip(GuiGraphics g, int mouseX, int mouseY) {
        Optional<InfusionRecipe> rec = this.localPreviewRecipe();
        if (rec.isEmpty() || this.minecraft == null) {
            return;
        }
        InfusionRecipe r = rec.get();
        int souls = r.soulCost;
        if (souls <= 0) {
            souls = this.menu.getField(2);
        }
        List<Component> lines = new ArrayList<>();
        lines.add(r.result.getHoverName().copy());
        lines.add(
                Component.translatable("gui.bmcmod.infusion.soul_cost", souls)
                        .withStyle(ChatFormatting.DARK_GRAY));
        g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    private Component truncateSgaName(Component line, int maxW) {
        if (this.font.width(line) <= maxW) {
            return line;
        }
        String s = line.getString();
        String ell = "…";
        for (int len = s.length(); len >= 0; len--) {
            String t = len >= s.length() ? s : s.substring(0, len) + ell;
            Component c = withEnchantFont(Component.literal(t));
            if (this.font.width(c) <= maxW) {
                return c;
            }
        }
        return withEnchantFont(Component.literal(ell));
    }

    private Optional<InfusionRecipe> localPreviewRecipe() {
        ItemStack a = this.menu.getSlot(1).getItem();
        ItemStack b = this.menu.getSlot(2).getItem();
        ItemStack c = this.menu.getSlot(3).getItem();
        ItemStack d = this.menu.getSlot(4).getItem();
        return InfusionRecipes.findMatch(a, b, c, d);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isOverButton(mouseX, mouseY) && this.menu.getField(3) == 1) {
            Minecraft mc = Minecraft.getInstance();
            mc.gameMode.handleInventoryButtonClick(this.menu.containerId, InfusionTableMenu.BUTTON_ID_START);
            mc.getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0F));
            return true;
        }
        if (this.recipeBookOpen && this.handleRecipeBookMouse(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.recipeBookOpen && this.isMouseOverRecipePanel(mouseX, mouseY)) {
            int pages = Math.max(1, this.recipeBookPageCount());
            int dir = scrollY > 0 ? -1 : 1;
            this.infusionRecipeBookPage = Mth.clamp(this.infusionRecipeBookPage + dir, 0, pages - 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean isOverButton(double mouseX, double mouseY) {
        int x0 = this.leftPos + BTN_DX;
        int y0 = this.topPos + BTN_DY;
        return mouseX >= x0
                && mouseX < x0 + BTN_W
                && mouseY >= y0
                && mouseY < y0 + BTN_H;
    }

    private int recipePanelTotalWidth() {
        return BOOK_BODY_W + BOOK_MARGIN * 2;
    }

    /**
     * Place le panneau livre à gauche de la fenêtre d’infusion, sans le faire sortir de l’écran
     * (GUI scale / petites résolutions).
     */
    private int recipePanelScreenLeft() {
        int pw = this.recipePanelTotalWidth();
        int ideal = Math.max(2, this.leftPos - pw - 6);
        int maxLeft = Math.max(2, this.width - pw - 2);
        return Mth.clamp(ideal, 2, maxLeft);
    }

    private int recipePanelTop() {
        int t = this.topPos + (this.imageHeight - BOOK_BODY_H) / 2;
        return Mth.clamp(t, this.topPos + 2, this.topPos + this.imageHeight - BOOK_BODY_H - 2);
    }

    private int recipeBookBodyLeft() {
        return this.recipePanelScreenLeft() + BOOK_MARGIN;
    }

    private int recipeBookBodyTop() {
        return this.recipePanelTop();
    }

    private boolean isMouseOverRecipePanel(double mouseX, double mouseY) {
        if (!this.recipeBookOpen) {
            return false;
        }
        int px = this.recipePanelScreenLeft();
        int py = this.recipePanelTop();
        int pw = this.recipePanelTotalWidth();
        return mouseX >= px && mouseX < px + pw && mouseY >= py && mouseY < py + BOOK_BODY_H;
    }

    private void positionInfusionRecipeBookWidgets() {
        int bodyL = this.recipeBookBodyLeft();
        int bodyT = this.recipeBookBodyTop();
        this.infusionRecipeSearch.setX(bodyL + BOOK_SEARCH_X);
        this.infusionRecipeSearch.setY(bodyT + BOOK_SEARCH_Y);
        this.infusionRecipeSearch.setWidth(BOOK_SEARCH_W);
        this.infusionRecipeSearch.setHeight(BOOK_SEARCH_H);
        this.filterBtnX = bodyL + BOOK_FILTER_X;
        this.filterBtnY = bodyT + BOOK_FILTER_Y;
        this.pagePrevBtnX = bodyL + BOOK_PAGE_BACK_X;
        this.pageNextBtnX = bodyL + BOOK_PAGE_FWD_X;
        this.pagePrevBtnY = bodyT + BOOK_PAGE_Y;
        this.pageNextBtnY = bodyT + BOOK_PAGE_Y;
        this.infusionRecipeSearch.setVisible(this.recipeBookOpen);
    }

    private boolean recipeMatchesSearch(InfusionRecipe r, String q) {
        String ql = q.toLowerCase(Locale.ROOT);
        if (r.result.getHoverName().getString().toLowerCase(Locale.ROOT).contains(ql)) {
            return true;
        }
        if (BuiltInRegistries.ITEM
                .getKey(r.result.getItem())
                .toString()
                .toLowerCase(Locale.ROOT)
                .contains(ql)) {
            return true;
        }
        for (ItemStack st : ingredientsForDisplay(r)) {
            if (!st.isEmpty()
                    && st.getHoverName().getString().toLowerCase(Locale.ROOT).contains(ql)) {
                return true;
            }
        }
        return false;
    }

    private int recipeBookPageCount() {
        int n = this.filteredInfusionRecipesCached().size();
        return Math.max(1, (n + BOOK_RECIPES_PER_PAGE - 1) / BOOK_RECIPES_PER_PAGE);
    }

    private static List<ItemStack> ingredientsForDisplay(InfusionRecipe r) {
        List<Map.Entry<Item, Integer>> entries = new ArrayList<>(r.required.entrySet());
        entries.sort(
                Comparator.comparing(e -> BuiltInRegistries.ITEM.getKey(e.getKey()).toString()));
        List<ItemStack> stacks = new ArrayList<>(4);
        for (Map.Entry<Item, Integer> e : entries) {
            stacks.add(new ItemStack(e.getKey(), e.getValue()));
        }
        while (stacks.size() < 4) {
            stacks.add(ItemStack.EMPTY);
        }
        return stacks;
    }

    private void renderRecipeBookBackdrop(GuiGraphics g, int mouseX, int mouseY) {
        int bodyL = this.recipeBookBodyLeft();
        int bodyT = this.recipeBookBodyTop();
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 100.0F);
        g.blit(BOOK_PANEL_TEXTURE, bodyL, bodyT, BOOK_PANEL_U, BOOK_PANEL_V, BOOK_BODY_W, BOOK_BODY_H);

        boolean hoverFilter =
                mouseX >= this.filterBtnX
                        && mouseX < this.filterBtnX + BOOK_FILTER_W
                        && mouseY >= this.filterBtnY
                        && mouseY < this.filterBtnY + BOOK_FILTER_H;

        RenderSystem.disableDepthTest();
        g.blitSprite(
                BOOK_FILTER_SPRITES.get(this.infusionFilterCraftableOnly, hoverFilter),
                this.filterBtnX,
                this.filterBtnY,
                BOOK_FILTER_W,
                BOOK_FILTER_H);

        int pages = this.recipeBookPageCount();
        this.infusionRecipeBookPage = Mth.clamp(this.infusionRecipeBookPage, 0, pages - 1);
        boolean canPrev = pages > 1 && this.infusionRecipeBookPage > 0;
        boolean canNext = pages > 1 && this.infusionRecipeBookPage < pages - 1;
        boolean hoverPrev =
                canPrev
                        && mouseX >= this.pagePrevBtnX
                        && mouseX < this.pagePrevBtnX + BOOK_PAGE_W
                        && mouseY >= this.pagePrevBtnY
                        && mouseY < this.pagePrevBtnY + BOOK_PAGE_H;
        boolean hoverNext =
                canNext
                        && mouseX >= this.pageNextBtnX
                        && mouseX < this.pageNextBtnX + BOOK_PAGE_W
                        && mouseY >= this.pageNextBtnY
                        && mouseY < this.pageNextBtnY + BOOK_PAGE_H;

        if (canPrev) {
            g.blitSprite(
                    BOOK_PAGE_BACKWARD_SPRITES.get(true, hoverPrev),
                    this.pagePrevBtnX,
                    this.pagePrevBtnY,
                    BOOK_PAGE_W,
                    BOOK_PAGE_H);
        }
        if (canNext) {
            g.blitSprite(
                    BOOK_PAGE_FORWARD_SPRITES.get(true, hoverNext),
                    this.pageNextBtnX,
                    this.pageNextBtnY,
                    BOOK_PAGE_W,
                    BOOK_PAGE_H);
        }
        RenderSystem.enableDepthTest();

        Component pageComponent =
                Component.translatable("gui.recipebook.page", this.infusionRecipeBookPage + 1, pages);
        int tw = this.font.width(pageComponent);
        g.drawString(
                this.font,
                pageComponent,
                bodyL + BOOK_PAGE_TEXT_ANCHOR_X - tw / 2,
                bodyT + BOOK_PAGE_TEXT_Y,
                -1,
                false);

        List<InfusionRecipe> list = this.filteredInfusionRecipesCached();
        int gridTop = bodyT + BOOK_GRID_ORIGIN_Y;
        int gridBottomExclusive = bodyT + BOOK_GRID_Y_END;
        int gx0 = bodyL + BOOK_GRID_ORIGIN_X;
        int start = this.infusionRecipeBookPage * BOOK_RECIPES_PER_PAGE;
        this.hoveredBookRecipe =
                this.pickHoveredBookRecipe(list, start, gx0, gridTop, mouseX, mouseY, gridBottomExclusive);
        for (int i = 0; i < BOOK_RECIPES_PER_PAGE; i++) {
            int idx = start + i;
            if (idx >= list.size()) {
                break;
            }
            int col = i % BOOK_COLS;
            int row = i / BOOK_COLS;
            int cx = gx0 + col * BOOK_CELL;
            int cy = gridTop + row * BOOK_CELL;
            InfusionRecipe r = list.get(idx);
            boolean ok = InfusionRecipeBookHelper.isFullyCraftable(this.menu, r);
            g.blitSprite(ok ? BOOK_SLOT_OK : BOOK_SLOT_NO, cx, cy, BOOK_CELL, BOOK_CELL);
            int ix = cx + (BOOK_CELL - 16) / 2;
            int iy = cy + (BOOK_CELL - 16) / 2;
            g.renderFakeItem(r.result, ix, iy);
            g.renderItemDecorations(this.font, r.result, ix, iy);
            if (this.selectedRecipe == r) {
                g.fill(cx + 1, cy + 1, cx + BOOK_CELL - 1, cy + 2, 0xFFFFFFFF);
                g.fill(cx + 1, cy + BOOK_CELL - 2, cx + BOOK_CELL - 1, cy + BOOK_CELL - 1, 0xFFFFFFFF);
                g.fill(cx + 1, cy + 1, cx + 2, cy + BOOK_CELL - 1, 0xFFFFFFFF);
                g.fill(cx + BOOK_CELL - 2, cy + 1, cx + BOOK_CELL - 1, cy + BOOK_CELL - 1, 0xFFFFFFFF);
            }
        }
        g.pose().popPose();
    }

    private void renderRecipeBookFilterTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (this.hoveredBookRecipe != null) {
            return;
        }
        if (!this.recipeBookOpen
                || mouseX < this.filterBtnX
                || mouseX >= this.filterBtnX + BOOK_FILTER_W
                || mouseY < this.filterBtnY
                || mouseY >= this.filterBtnY + BOOK_FILTER_H) {
            return;
        }
        Component line =
                this.infusionFilterCraftableOnly
                        ? Component.translatable("gui.recipebook.toggleRecipes.craftable")
                        : Component.translatable("gui.recipebook.toggleRecipes.all");
        g.renderComponentTooltip(this.font, List.of(line), mouseX, mouseY);
    }

    /** Index 0 … (colonnes × rangées − 1) dans la grille du livre, ou vide si hors cellule. */
    private OptionalInt pickBookGridLocalSlot(
            int gx0, int gridTop, int gridBottomExclusive, int mouseX, int mouseY) {
        if (mouseY < gridTop || mouseY >= gridBottomExclusive) {
            return OptionalInt.empty();
        }
        int relX = mouseX - gx0;
        int relY = mouseY - gridTop;
        if (relX < 0 || relY < 0) {
            return OptionalInt.empty();
        }
        int col = relX / BOOK_CELL;
        int row = relY / BOOK_CELL;
        if (col < 0 || col >= BOOK_COLS || row < 0 || row >= BOOK_ROWS) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(row * BOOK_COLS + col);
    }

    private InfusionRecipe pickHoveredBookRecipe(
            List<InfusionRecipe> list,
            int startIndex,
            int gx0,
            int gridTop,
            int mouseX,
            int mouseY,
            int gridBottomExclusive) {
        if (!this.isMouseOverRecipePanel(mouseX, mouseY) || list.isEmpty()) {
            return null;
        }
        if (this.infusionRecipeSearch.isMouseOver(mouseX, mouseY)) {
            return null;
        }
        OptionalInt local = pickBookGridLocalSlot(gx0, gridTop, gridBottomExclusive, mouseX, mouseY);
        if (local.isEmpty()) {
            return null;
        }
        int idx = startIndex + local.getAsInt();
        if (idx < 0 || idx >= list.size()) {
            return null;
        }
        return list.get(idx);
    }

    private boolean handleRecipeBookMouse(double mouseX, double mouseY, int button) {
        if (button != 0 || !this.isMouseOverRecipePanel(mouseX, mouseY)) {
            return false;
        }
        if (this.infusionRecipeSearch.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        int bodyL = this.recipeBookBodyLeft();
        int bodyT = this.recipeBookBodyTop();
        if (mouseX >= this.filterBtnX
                && mouseX < this.filterBtnX + BOOK_FILTER_W
                && mouseY >= this.filterBtnY
                && mouseY < this.filterBtnY + BOOK_FILTER_H) {
            this.infusionFilterCraftableOnly = !this.infusionFilterCraftableOnly;
            this.invalidateFilteredRecipeCache();
            this.infusionRecipeBookPage = 0;
            return true;
        }
        int pages = this.recipeBookPageCount();
        boolean canPrev = pages > 1 && this.infusionRecipeBookPage > 0;
        boolean canNext = pages > 1 && this.infusionRecipeBookPage < pages - 1;
        if (canPrev
                && mouseX >= this.pagePrevBtnX
                && mouseX < this.pagePrevBtnX + BOOK_PAGE_W
                && mouseY >= this.pagePrevBtnY
                && mouseY < this.pagePrevBtnY + BOOK_PAGE_H) {
            this.infusionRecipeBookPage = Math.max(0, this.infusionRecipeBookPage - 1);
            return true;
        }
        if (canNext
                && mouseX >= this.pageNextBtnX
                && mouseX < this.pageNextBtnX + BOOK_PAGE_W
                && mouseY >= this.pageNextBtnY
                && mouseY < this.pageNextBtnY + BOOK_PAGE_H) {
            this.infusionRecipeBookPage =
                    Math.min(this.recipeBookPageCount() - 1, this.infusionRecipeBookPage + 1);
            return true;
        }
        int gridTop = bodyT + BOOK_GRID_ORIGIN_Y;
        int gridBottomExclusive = bodyT + BOOK_GRID_Y_END;
        int gx0 = bodyL + BOOK_GRID_ORIGIN_X;
        OptionalInt local =
                pickBookGridLocalSlot(
                        gx0, gridTop, gridBottomExclusive, (int) mouseX, (int) mouseY);
        if (local.isPresent()) {
            List<InfusionRecipe> list = this.filteredInfusionRecipesCached();
            int idx = this.infusionRecipeBookPage * BOOK_RECIPES_PER_PAGE + local.getAsInt();
            if (idx >= 0 && idx < list.size()) {
                InfusionRecipe h = list.get(idx);
                if (h == this.selectedRecipe) {
                    this.selectedRecipe = null;
                } else {
                    this.selectedRecipe = h;
                }
            }
            return true;
        }
        return true;
    }

    private void renderRecipePanelTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (this.hoveredBookRecipe == null) {
            return;
        }
        InfusionRecipe r = this.hoveredBookRecipe;
        List<Component> lines = new ArrayList<>();
        lines.add(r.result.getHoverName().copy());
        lines.add(
                Component.translatable("gui.bmcmod.infusion.soul_cost", r.soulCost)
                        .withStyle(ChatFormatting.DARK_GRAY));
        if (!InfusionRecipeBookHelper.hasEnoughSouls(this.menu, r)) {
            lines.add(
                    Component.translatable("gui.bmcmod.infusion.recipe_book.need_souls")
                            .withStyle(ChatFormatting.RED));
        }
        if (!InfusionRecipeBookHelper.hasEnoughIngredients(this.menu, r)) {
            lines.add(
                    Component.translatable("gui.bmcmod.infusion.recipe_book.need_items")
                            .withStyle(ChatFormatting.RED));
        }
        if (!InfusionRecipeBookHelper.outputCanAcceptResult(this.menu, r)) {
            lines.add(
                    Component.translatable("gui.bmcmod.infusion.recipe_book.output_blocked")
                            .withStyle(ChatFormatting.RED));
        }
        lines.add(
                Component.translatable("gui.bmcmod.infusion.ingredients_header")
                        .withStyle(ChatFormatting.GRAY));
        for (ItemStack st : ingredientsForDisplay(r)) {
            if (!st.isEmpty()) {
                lines.add(
                        Component.literal("• ")
                                .append(st.getHoverName())
                                .append(
                                        Component.literal(" ×" + st.getCount())
                                                .withStyle(ChatFormatting.DARK_GRAY)));
            }
        }
        lines.add(Component.translatable("gui.bmcmod.infusion.recipe_book.click_hint").withStyle(ChatFormatting.DARK_AQUA));
        g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    private static void renderMissingSlotOutline(GuiGraphics g, int x, int y) {
        int c = 0xFFFF3030;
        g.hLine(x, x + 15, y, c);
        g.hLine(x, x + 15, y + 15, c);
        g.vLine(x, y, y + 15, c);
        g.vLine(x + 15, y, y + 15, c);
    }

    private void renderSelectedRecipeGhosts(GuiGraphics g) {
        if (this.selectedRecipe == null) {
            return;
        }
        List<ItemStack> stacks = ingredientsForDisplay(this.selectedRecipe);
        ItemStack[] disp = new ItemStack[] {stacks.get(0), stacks.get(1), stacks.get(2), stacks.get(3)};
        for (int i = 0; i < 4; i++) {
            ItemStack ghost = disp[i];
            if (ghost.isEmpty()) {
                continue;
            }
            Slot sl = this.menu.getSlot(1 + i);
            int x = this.leftPos + sl.x;
            int y = this.topPos + sl.y;
            boolean missing =
                    InfusionRecipeBookHelper.displaySlotMissing(this.menu, this.selectedRecipe, i, disp);
            if (sl.getItem().isEmpty()) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                try {
                    RenderSystem.setShaderColor(1f, 1f, 1f, 0.32f);
                    g.renderItem(ghost, x, y);
                    g.renderItemDecorations(this.font, ghost, x, y);
                } finally {
                    RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                }
            }
            if (missing) {
                renderMissingSlotOutline(g, x, y);
            }
        }
    }
}
