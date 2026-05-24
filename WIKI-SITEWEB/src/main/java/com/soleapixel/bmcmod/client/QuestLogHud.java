package com.soleapixel.bmcmod.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.quest.QuestLogData;
import com.soleapixel.bmcmod.registry.ModItems;

@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class QuestLogHud {
    private QuestLogHud() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) {
            return;
        }
        List<ItemStack> quests = new ArrayList<>();
        for (ItemStack s : mc.player.getInventory().items) {
            if (s.is(ModItems.QUEST_LOG.get())) {
                quests.add(s);
            }
        }
        ItemStack off = mc.player.getOffhandItem();
        if (off.is(ModItems.QUEST_LOG.get())) {
            quests.add(off);
        }
        if (quests.isEmpty()) {
            return;
        }
        List<ItemStack> sorted = new ArrayList<>(quests);
        sorted.sort(Comparator.comparingLong(QuestLogHud::sortKeyDeadline));
        int n = Math.min(4, sorted.size());
        GuiGraphics g = event.getGuiGraphics();
        int y = 6;
        for (int i = 0; i < n; i++) {
            ItemStack stack = sorted.get(i);
            CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
            if (cd == null) {
                continue;
            }
            QuestLogData d = QuestLogData.read(cd.copyTag());
            if (d == null) {
                continue;
            }
            d.ensureTasks();
            long left = d.started && mc.level != null ? d.deadlineTick - mc.level.getGameTime() : d.durationTicks;
            if (left < 0) {
                left = 0;
            }
            int sec = (int) (left / 20);
            int min = sec / 60;
            sec = sec % 60;
            String timeStr = String.format("%d:%02d", min, sec);
            Component line = d.tasks.size() <= 1
                    ? Component.translatable(
                            "quest.bmcmod.hud.compact",
                            Component.translatable(d.difficulty.translationKey()),
                            d.progress,
                            d.goal,
                            timeStr)
                    : Component.translatable(
                            "quest.bmcmod.hud.compact.multi",
                            Component.translatable(d.difficulty.translationKey()),
                            d.completedSubtaskCount(),
                            d.tasks.size(),
                            timeStr);
            g.drawString(mc.font, line, 6, y, 0xFFE8D4A8, true);
            y += 11;
        }
    }

    /** Quêtes démarrées en premier (échéance la plus proche), puis non démarrées. */
    private static long sortKeyDeadline(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) {
            return Long.MAX_VALUE;
        }
        QuestLogData d = QuestLogData.read(cd.copyTag());
        if (d == null) {
            return Long.MAX_VALUE;
        }
        if (!d.started || Minecraft.getInstance().level == null) {
            return Long.MAX_VALUE;
        }
        return d.deadlineTick;
    }
}
