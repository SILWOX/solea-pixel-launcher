package com.soleapixel.bmcmod.client;

import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.EditGameRulesScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.GameRules;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

/**
 * Écran config du mod : entrée NeoForge + accès rapide aux gamerules du monde solo.
 */
public final class BmcModConfigurationScreen extends Screen {
    private final Screen parent;
    private final ModContainer modContainer;

    /** Ordre imposé par {@link net.neoforged.neoforge.client.gui.IConfigScreenFactory}. */
    public BmcModConfigurationScreen(ModContainer modContainer, Screen parent) {
        super(Component.translatable("bmcmod.config.screen.title"));
        this.parent = parent;
        this.modContainer = modContainer;
    }

    @Override
    protected void init() {
        int cx = this.width / 2 - 100;
        int y = this.height / 4;
        this.addRenderableWidget(
                Button.builder(Component.translatable("bmcmod.config.button.neoforge"), b ->
                                Minecraft.getInstance().setScreen(new ConfigurationScreen(this.modContainer, this)))
                        .bounds(cx, y, 200, 20)
                        .build());
        this.addRenderableWidget(
                Button.builder(Component.translatable("bmcmod.config.button.gamerules"), b -> openGameRules())
                        .bounds(cx, y + 28, 200, 20)
                        .build());
        this.addRenderableWidget(
                Button.builder(CommonComponents.GUI_DONE, b -> Minecraft.getInstance().setScreen(parent))
                        .bounds(cx, y + 56, 200, 20)
                        .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int maxW = this.width - 40;
        int x = 20;
        int lineY = this.height / 4 + 90;
        int color = 0xA0A0A0;
        for (FormattedCharSequence line : this.font.split(Component.translatable("bmcmod.config.server.restart_hint"), maxW)) {
            graphics.drawString(this.font, line, x, lineY, color, false);
            lineY += 12;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCurrentServer() != null && !mc.hasSingleplayerServer()) {
            lineY += 4;
            for (FormattedCharSequence line : this.font.split(Component.translatable("bmcmod.config.server.remote_hint"), maxW)) {
                graphics.drawString(this.font, line, x, lineY, color, false);
                lineY += 12;
            }
        }
    }

    private void openGameRules() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
            GameRules rules = mc.getSingleplayerServer().getWorldData().getGameRules();
            mc.setScreen(new EditGameRulesScreen(rules, (Optional<GameRules> accepted) -> mc.setScreen(this)));
        } else {
            mc.gui.getChat().addMessage(Component.translatable("bmcmod.config.gamerules.multiplayer_hint"));
        }
    }
}
