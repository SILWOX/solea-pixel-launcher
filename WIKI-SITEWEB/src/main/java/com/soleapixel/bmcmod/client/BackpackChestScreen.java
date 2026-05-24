package com.soleapixel.bmcmod.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.player.Inventory;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.menu.BackpackChestMenu;

/**
 * Fond par type de sac, fichiers dans {@code assets/bmcmod/textures/gui/backpack/} :
 * {@code backpack.png}, {@code iron_backpack.png}, {@code gold_backpack.png},
 * {@code diamond_backpack.png}, {@code emerald_backpack.png} (idéalement <b>256×256</b>, grille comme
 * {@code generic_54}). Aligné sur {@link net.minecraft.world.inventory.ChestMenu} : panneau dessiné sur
 * <b>176×(114+18×r)</b> pixels depuis le coin haut-gauche de la texture, blit <b>1:1</b> comme le coffre vanilla.
 * <p>
 * Si une texture n’est pas 256×256 ni 176×h exact, recadrage haut-gauche + mise à l’échelle sur {@code w×h}.
 */
public final class BackpackChestScreen extends AbstractContainerScreen<BackpackChestMenu> {
    private static final ResourceLocation VANILLA_CHEST_GUI = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    private static final Map<ResourceLocation, int[]> TEXTURE_PIXEL_SIZE = new ConcurrentHashMap<>();

    public BackpackChestScreen(BackpackChestMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        int rows = menu.getRowCount();
        this.imageWidth = 176;
        this.imageHeight = 114 + rows * 18;
        this.titleLabelY = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        // Comme le coffre vanilla / le libellé « Inventory » : titre aligné à gauche (x = 8).
        this.titleLabelX = 8;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /** Même convention que les items : {@code backpack.png}, {@code iron_backpack.png}, etc. */
    private static ResourceLocation textureForRows(int rows) {
        return switch (rows) {
            case 1 -> BmcMod.loc("textures/gui/backpack/backpack.png");
            case 2 -> BmcMod.loc("textures/gui/backpack/iron_backpack.png");
            case 3 -> BmcMod.loc("textures/gui/backpack/gold_backpack.png");
            case 5 -> BmcMod.loc("textures/gui/backpack/diamond_backpack.png");
            case 7 -> BmcMod.loc("textures/gui/backpack/emerald_backpack.png");
            default -> VANILLA_CHEST_GUI;
        };
    }

    private static int[] readTexturePixelSizeUncached(ResourceLocation loc) {
        try {
            Resource resource = Minecraft.getInstance().getResourceManager().getResource(loc).orElse(null);
            if (resource == null) {
                return new int[]{-1, -1};
            }
            try (var stream = resource.open(); NativeImage image = NativeImage.read(stream)) {
                return new int[]{image.getWidth(), image.getHeight()};
            }
        } catch (Exception e) {
            BmcMod.LOGGER.warn("Impossible de lire la texture GUI {} : {}", loc, e.toString());
            return new int[]{-1, -1};
        }
    }

    private static int[] pixelSizeOf(ResourceLocation texture) {
        return TEXTURE_PIXEL_SIZE.computeIfAbsent(texture, BackpackChestScreen::readTexturePixelSizeUncached);
    }

    public static void clearTextureSizeCache() {
        TEXTURE_PIXEL_SIZE.clear();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        int h = this.imageHeight;
        int w = this.imageWidth;
        ResourceLocation custom = textureForRows(this.menu.getRowCount());
        if (custom.equals(VANILLA_CHEST_GUI)) {
            blitVanillaChest(guiGraphics, x, y, w, h);
            return;
        }
        int[] dim = pixelSizeOf(custom);
        int fileW = dim[0];
        int fileH = dim[1];
        if (fileW < 1 || fileH < 1) {
            blitVanillaChest(guiGraphics, x, y, w, h);
            return;
        }
        blitBackpackTexture(guiGraphics, custom, x, y, w, h, fileW, fileH);
    }

    /**
     * Affiche la texture sur le rectangle exact des slots ({@code w×h}).
     * <p>
     * Recadrage <strong>haut-gauche</strong> sur {@code min(fileW,w)×min(fileH,h)} puis mise à l’échelle
     * sur {@code w×h} (évite le décalage d’un crop horizontal centré sur des fichiers plus larges que {@code w}).
     */
    private static void blitBackpackTexture(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, int fileW, int fileH) {
        if (fileW == 256 && fileH == 256) {
            g.blit(tex, x, y, 0, 0, w, h, 256, 256);
            return;
        }
        if (fileW == w && fileH == h) {
            g.blit(tex, x, y, 0, 0, w, h, w, h);
            return;
        }
        int cropW = Math.min(fileW, w);
        int cropH = Math.min(fileH, h);
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(w / (float) cropW, h / (float) cropH, 1f);
        g.blit(tex, 0, 0, 0, 0, cropW, cropH, fileW, fileH);
        g.pose().popPose();
    }

    private static void blitVanillaChest(GuiGraphics guiGraphics, int x, int y, int w, int h) {
        if (h <= 256) {
            guiGraphics.blit(VANILLA_CHEST_GUI, x, y, 0, 0, w, h, 256, 256);
        } else {
            guiGraphics.blit(VANILLA_CHEST_GUI, x, y, 0, 0, w, 256, 256, 256);
            guiGraphics.fill(x, y + 256, x + w, y + h, 0xFFC6C6C6);
        }
    }
}
