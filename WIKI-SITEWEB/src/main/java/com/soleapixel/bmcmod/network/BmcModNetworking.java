package com.soleapixel.bmcmod.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.gameplay.EnchantmentGameplayEvents;
import com.soleapixel.bmcmod.gameplay.ScytheSweepAttackLogic;
import com.soleapixel.bmcmod.gameplay.SkyBootsGameplay;
import com.soleapixel.bmcmod.gameplay.UndeadTotemGameplay;
import com.soleapixel.bmcmod.gameplay.UpgradeChestplateGameplay;
import com.soleapixel.bmcmod.item.BuilderWandItem;
import com.soleapixel.bmcmod.item.QuiverContents;
import com.soleapixel.bmcmod.item.QuiverHelper;
import com.soleapixel.bmcmod.item.builderwand.BuilderWandMaterialPicker;
import com.soleapixel.bmcmod.item.DragonStaffItem;
import com.soleapixel.bmcmod.item.EndStaffItem;
import com.soleapixel.bmcmod.item.IceStaffItem;
import com.soleapixel.bmcmod.item.ScytheItem;
import com.soleapixel.bmcmod.item.WitherStaffItem;
import com.soleapixel.bmcmod.registry.ModEnchantmentKeys;
import com.soleapixel.bmcmod.registry.ModItems;
import com.soleapixel.bmcmod.shulker.ShulkerEquipment;
import com.soleapixel.bmcmod.witch.WitchMetamorphPackets;
import com.soleapixel.bmcmod.witch.WitchMetamorphServer;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;

@EventBusSubscriber(modid = BmcMod.MODID)
public final class BmcModNetworking {
    private BmcModNetworking() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("shulker_c2s").playToServer(
                ShulkerArmorPackets.ShulkerShootPayload.TYPE,
                ShulkerArmorPackets.ShulkerShootPayload.CODEC,
                BmcModNetworking::handleShoot);
        event.registrar("witch_c2s").playToServer(
                WitchMetamorphPackets.WitchTryTransformPayload.TYPE,
                WitchMetamorphPackets.WitchTryTransformPayload.CODEC,
                BmcModNetworking::handleWitchTry);
        event.registrar("sky_boots_c2s").playToServer(
                SkyBootsPackets.SkyDoubleJumpPayload.TYPE,
                SkyBootsPackets.SkyDoubleJumpPayload.CODEC,
                BmcModNetworking::handleSkyDoubleJump);
        event.registrar("end_staff_c2s").playToServer(
                EndStaffPackets.EndStaffCycleModePayload.TYPE,
                EndStaffPackets.EndStaffCycleModePayload.CODEC,
                BmcModNetworking::handleEndStaffCycle);
        event.registrar("dragon_staff_c2s").playToServer(
                DragonStaffPackets.DragonStaffCycleModePayload.TYPE,
                DragonStaffPackets.DragonStaffCycleModePayload.CODEC,
                BmcModNetworking::handleDragonStaffCycle);
        event.registrar("wither_staff_c2s").playToServer(
                WitherStaffPackets.WitherStaffCycleModePayload.TYPE,
                WitherStaffPackets.WitherStaffCycleModePayload.CODEC,
                BmcModNetworking::handleWitherStaffCycle);
        event.registrar("ice_staff_c2s").playToServer(
                IceStaffPackets.IceStaffCycleModePayload.TYPE,
                IceStaffPackets.IceStaffCycleModePayload.CODEC,
                BmcModNetworking::handleIceStaffCycle);
        event.registrar("excavator_c2s").playToServer(
                ExcavatorPackets.ExcavatorToggleModePayload.TYPE,
                ExcavatorPackets.ExcavatorToggleModePayload.CODEC,
                BmcModNetworking::handleExcavatorToggle);
        event.registrar("builder_wand_cycle_c2s").playToServer(
                BuilderWandPackets.BuilderWandCycleModePayload.TYPE,
                BuilderWandPackets.BuilderWandCycleModePayload.CODEC,
                BmcModNetworking::handleBuilderWandCycle);
        event.registrar("builder_wand_material_c2s").playToServer(
                BuilderWandPackets.BuilderWandCycleMaterialPayload.TYPE,
                BuilderWandPackets.BuilderWandCycleMaterialPayload.CODEC,
                BmcModNetworking::handleBuilderWandCycleMaterial);
        event.registrar("builder_wand_placement_c2s").playToServer(
                BuilderWandPackets.BuilderWandAdjustPlacementTurnPayload.TYPE,
                BuilderWandPackets.BuilderWandAdjustPlacementTurnPayload.CODEC,
                BmcModNetworking::handleBuilderWandAdjustPlacementTurn);
        event.registrar("undead_totem_c2s")
                .playToServer(
                        UndeadTotemPackets.SummonClonesPayload.TYPE,
                        UndeadTotemPackets.SummonClonesPayload.CODEC,
                        BmcModNetworking::handleUndeadTotemSummon);
        event.registrar("quiver_c2s").playToServer(
                QuiverPackets.CycleSelectedTypePayload.TYPE,
                QuiverPackets.CycleSelectedTypePayload.CODEC,
                BmcModNetworking::handleQuiverCycleType);
        event.registrar("scythe_c2s")
                .playToServer(
                        ScythePackets.ScytheCycleAreaPayload.TYPE,
                        ScythePackets.ScytheCycleAreaPayload.CODEC,
                        BmcModNetworking::handleScytheCycleArea)
                .playToServer(
                        ScythePackets.ScytheSweepAttackPayload.TYPE,
                        ScythePackets.ScytheSweepAttackPayload.CODEC,
                        BmcModNetworking::handleScytheSweepAttack)
                .playToServer(
                        ScythePackets.ScytheWhirlwindChargingPayload.TYPE,
                        ScythePackets.ScytheWhirlwindChargingPayload.CODEC,
                        BmcModNetworking::handleScytheWhirlwindCharging);
    }

    private static void handleShoot(ShulkerArmorPackets.ShulkerShootPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ShulkerEquipment.handleShootPacket(context.player()));
    }

    private static void handleWitchTry(WitchMetamorphPackets.WitchTryTransformPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) {
                WitchMetamorphServer.handleTryTransform(sp);
            }
        });
    }

    private static void handleSkyDoubleJump(SkyBootsPackets.SkyDoubleJumpPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) {
                SkyBootsGameplay.handleDoubleJumpPacket(sp, payload.forwardDash());
                UpgradeChestplateGameplay.handleDashUpgradePacket(sp, payload.forwardDash());
            }
        });
    }

    private static void handleDragonStaffCycle(DragonStaffPackets.DragonStaffCycleModePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sp)) {
                return;
            }
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = sp.getItemInHand(hand);
                if (!stack.is(ModItems.DRAGON_STAFF.get())) {
                    continue;
                }
                DragonStaffItem.cycleMode(stack);
                int mode = DragonStaffItem.getMode(stack);
                sp.playSound(SoundEvents.ENDER_DRAGON_AMBIENT, 0.2F, 1.2F + sp.getRandom().nextFloat() * 0.25F);
                sp.displayClientMessage(
                        Component.translatable("message.bmcmod.dragon_staff.mode_switched", Component.translatable("item.bmcmod.dragon_staff.mode." + mode))
                                .withStyle(ChatFormatting.LIGHT_PURPLE),
                        true);
                return;
            }
        });
    }

    private static void handleEndStaffCycle(EndStaffPackets.EndStaffCycleModePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sp)) {
                return;
            }
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = sp.getItemInHand(hand);
                if (!stack.is(ModItems.END_STAFF.get())) {
                    continue;
                }
                EndStaffItem.cycleMode(stack);
                int mode = EndStaffItem.getMode(stack);
                sp.playSound(SoundEvents.END_PORTAL_FRAME_FILL, 0.35F, 0.7F + sp.getRandom().nextFloat() * 0.35F);
                sp.displayClientMessage(
                        Component.translatable("message.bmcmod.end_staff.mode_switched", Component.translatable("item.bmcmod.end_staff.mode." + mode))
                                .withStyle(ChatFormatting.AQUA),
                        true);
                return;
            }
        });
    }

    private static void handleWitherStaffCycle(WitherStaffPackets.WitherStaffCycleModePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sp)) {
                return;
            }
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = sp.getItemInHand(hand);
                if (!stack.is(ModItems.WITHER_STAFF.get())) {
                    continue;
                }
                WitherStaffItem.cycleMode(stack);
                int mode = WitherStaffItem.getMode(stack);
                sp.playSound(SoundEvents.WITHER_AMBIENT, 0.25F, 1.15F + sp.getRandom().nextFloat() * 0.2F);
                sp.displayClientMessage(
                        Component.translatable("message.bmcmod.wither_staff.mode_switched", Component.translatable("item.bmcmod.wither_staff.mode." + mode))
                                .withStyle(ChatFormatting.DARK_GRAY),
                        true);
                return;
            }
        });
    }

    private static void handleIceStaffCycle(IceStaffPackets.IceStaffCycleModePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sp)) {
                return;
            }
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = sp.getItemInHand(hand);
                if (!stack.is(ModItems.ICE_STAFF.get())) {
                    continue;
                }
                IceStaffItem.cycleMode(stack);
                int mode = IceStaffItem.getMode(stack);
                sp.playSound(SoundEvents.GLASS_HIT, 0.25F, 1.05F + sp.getRandom().nextFloat() * 0.25F);
                sp.displayClientMessage(
                        Component.translatable("message.bmcmod.ice_staff.mode_switched", Component.translatable("item.bmcmod.ice_staff.mode." + mode))
                                .withStyle(ChatFormatting.AQUA),
                        true);
                return;
            }
        });
    }

    private static void handleBuilderWandCycle(BuilderWandPackets.BuilderWandCycleModePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sp)) {
                return;
            }
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = sp.getItemInHand(hand);
                if (!(stack.getItem() instanceof BuilderWandItem)) {
                    continue;
                }
                BuilderWandItem.cycleMode(stack);
                sp.playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 0.35F, 1.15F + sp.getRandom().nextFloat() * 0.25F);
                sp.displayClientMessage(
                        Component.translatable("message.bmcmod.builder_wand.mode_switched", BuilderWandItem.modeDescription(stack))
                                .withStyle(ChatFormatting.GREEN),
                        true);
                return;
            }
        });
    }

    private static void handleBuilderWandAdjustPlacementTurn(BuilderWandPackets.BuilderWandAdjustPlacementTurnPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sp)) {
                return;
            }
            int d = Integer.signum(payload.delta());
            if (d == 0) {
                return;
            }
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = sp.getItemInHand(hand);
                if (!(stack.getItem() instanceof BuilderWandItem)) {
                    continue;
                }
                BuilderWandItem.adjustPlacementTurn(stack, d);
                sp.playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 0.22F, 1.55F + sp.getRandom().nextFloat() * 0.12F);
                sp.displayClientMessage(BuilderWandItem.placementTurnDescription(stack).withStyle(ChatFormatting.AQUA), true);
                return;
            }
        });
    }

    private static void handleBuilderWandCycleMaterial(BuilderWandPackets.BuilderWandCycleMaterialPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sp)) {
                return;
            }
            List<Block> options = BuilderWandMaterialPicker.sortedPlaceableBlocks(sp.getInventory());
            if (options.isEmpty()) {
                sp.displayClientMessage(Component.translatable("message.bmcmod.builder_wand.no_blocks_in_inventory"), true);
                sp.playSound(SoundEvents.VILLAGER_NO, 0.35F, 1.05F);
                return;
            }
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = sp.getItemInHand(hand);
                if (!(stack.getItem() instanceof BuilderWandItem)) {
                    continue;
                }
                Block current = BuilderWandItem.getStoredBlock(stack);
                int nextIndex = 0;
                if (current != null) {
                    int idx = options.indexOf(current);
                    if (idx >= 0) {
                        nextIndex = (idx + 1) % options.size();
                    }
                }
                Block chosen = options.get(nextIndex);
                BuilderWandItem.setStoredBlock(stack, chosen);
                sp.playSound(SoundEvents.END_PORTAL_FRAME_FILL, 0.25F, 1.15F);
                sp.displayClientMessage(
                        Component.translatable("message.bmcmod.builder_wand.material_from_inventory", chosen.getName()),
                        true);
                return;
            }
        });
    }

    private static void handleExcavatorToggle(ExcavatorPackets.ExcavatorToggleModePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sp)) {
                return;
            }
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = sp.getItemInHand(hand);
                if (!(stack.getItem() instanceof PickaxeItem)) {
                    continue;
                }
                if (!stack.isEnchanted()) {
                    continue;
                }
                if (ModEnchantmentKeys.enchantmentLevel(stack, sp.registryAccess(), ModEnchantmentKeys.EXCAVATOR) <= 0) {
                    continue;
                }
                boolean mode3x3 = EnchantmentGameplayEvents.toggleExcavatorMode(stack);
                sp.playSound(SoundEvents.ANVIL_PLACE, 0.16F, mode3x3 ? 1.35F : 0.85F);
                sp.displayClientMessage(
                        Component.translatable(mode3x3 ? "message.bmcmod.excavator.mode_3x3" : "message.bmcmod.excavator.mode_single")
                                .withStyle(mode3x3 ? ChatFormatting.GOLD : ChatFormatting.GRAY),
                        true);
                return;
            }
        });
    }

    private static void handleUndeadTotemSummon(UndeadTotemPackets.SummonClonesPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) {
                UndeadTotemGameplay.handleSummonPacket(sp, payload.slimModel());
            }
        });
    }

    private static void handleScytheSweepAttack(ScythePackets.ScytheSweepAttackPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) {
                ScytheSweepAttackLogic.performSweep(sp, payload);
            }
        });
    }

    private static void handleScytheCycleArea(ScythePackets.ScytheCycleAreaPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sp)) {
                return;
            }
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = sp.getItemInHand(hand);
                if (!(stack.getItem() instanceof ScytheItem scythe)) {
                    continue;
                }
                ScytheItem.cycleMode(stack, scythe.scytheTier());
                int n = scythe.effectiveSize(stack);
                sp.playSound(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.35F, 1.1F + sp.getRandom().nextFloat() * 0.15F);
                sp.displayClientMessage(
                        Component.translatable("message.bmcmod.scythe.area_switched", n, n).withStyle(ChatFormatting.GREEN),
                        true);
                return;
            }
        });
    }

    private static void handleScytheWhirlwindCharging(ScythePackets.ScytheWhirlwindChargingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) {
                sp.getPersistentData().putBoolean(ScytheItem.WHIRLWIND_CHARGING_PDC_KEY, payload.active());
            }
        });
    }

    private static void handleQuiverCycleType(QuiverPackets.CycleSelectedTypePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sp)) {
                return;
            }
            if (!QuiverHelper.cycleSelection(sp, ModItems.QUIVER.get())) {
                return;
            }
            int slot = QuiverHelper.findPrimaryQuiverSlot(sp, ModItems.QUIVER.get());
            if (slot < 0) {
                return;
            }
            ItemStack qs = sp.getInventory().getItem(slot);
            QuiverContents c = QuiverHelper.normalize(QuiverHelper.get(qs));
            ItemStack sel = c.getChannel(c.selectedIndex());
            if (sel.isEmpty()) {
                int fi = c.firstNonEmpty();
                if (fi >= 0) {
                    sel = c.getChannel(fi);
                }
            }
            if (!sel.isEmpty()) {
                sp.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.35F, 1.15F + sp.getRandom().nextFloat() * 0.15F);
                sp.displayClientMessage(
                        Component.translatable("message.bmcmod.quiver.selected_type", sel.getHoverName())
                                .withStyle(ChatFormatting.YELLOW),
                        true);
            }
        });
    }
}
