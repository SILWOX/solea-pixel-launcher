package com.soleapixel.bmcmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.item.upgrade.ChestplateUpgradeData;
import com.soleapixel.bmcmod.network.SkyBootsPackets;
import com.soleapixel.bmcmod.registry.ModItems;

/**
 * Double saut : une charge remise au sol. En l’air, un <strong>second appui</strong> sur saut
 * (relâcher puis réappuyer sur espace si on sautait en maintenant la touche) envoie le boost au serveur.
 */
@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class SkyBootsDoubleJumpClient {
    private static boolean airChargeAvailable = true;
    private static boolean jumpWasDown;
    /**
     * Tant que vrai, on ignore les fronts montants de saut : évite de confondre la touche encore
     * enfoncée pour le saut vanilla avec un « second saut ».
     */
    private static boolean ignoreJumpUntilRelease;
    private static int ticksAirborne;

    private SkyBootsDoubleJumpClient() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.isPaused()) {
            return;
        }
        boolean skyBoots = player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.SKY_BOOTS.get());
        boolean dashUpgrade = ChestplateUpgradeData.count(player.getItemBySlot(EquipmentSlot.CHEST), ChestplateUpgradeData.DASH_UPGRADE_ID) > 0;
        boolean hasAirTrigger = skyBoots || dashUpgrade;
        boolean down = mc.options.keyJump.isDown();

        if (!hasAirTrigger) {
            if (player.onGround() || player.onClimbable()) {
                airChargeAvailable = true;
            }
            ignoreJumpUntilRelease = false;
            ticksAirborne = 0;
            jumpWasDown = down;
            return;
        }

        boolean grounded = player.onGround() || player.onClimbable();
        if (grounded) {
            airChargeAvailable = true;
            ignoreJumpUntilRelease = false;
            ticksAirborne = 0;
            jumpWasDown = down;
            return;
        }

        if (player.isFallFlying() || player.isInWater() || player.isInLava()) {
            ticksAirborne = 0;
            ignoreJumpUntilRelease = false;
            jumpWasDown = down;
            return;
        }

        ticksAirborne++;
        if (ticksAirborne == 1) {
            // Premier tick en l’air : si saut encore enfoncé, attendre un relâchement avant d’accepter un second appui.
            ignoreJumpUntilRelease = down;
        }
        if (ignoreJumpUntilRelease && !down) {
            ignoreJumpUntilRelease = false;
        }

        if (airChargeAvailable && !ignoreJumpUntilRelease && down && !jumpWasDown) {
            boolean forwardDash = wantsForwardDash(player);
            PacketDistributor.sendToServer(new SkyBootsPackets.SkyDoubleJumpPayload(forwardDash));
            airChargeAvailable = false;
        }

        jumpWasDown = down;
    }

    /**
     * Dash avant seulement si le joueur demande un déplacement (WASD ou stick manette via {@code xxa}/{@code zza}).
     * Immobile (aucune entrée directionnelle) → second saut vertical seul.
     */
    private static boolean wantsForwardDash(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        var o = mc.options;
        if (o.keyUp.isDown() || o.keyDown.isDown() || o.keyLeft.isDown() || o.keyRight.isDown()) {
            return true;
        }
        return Math.abs(player.xxa) > 0.04f || Math.abs(player.zza) > 0.04f;
    }
}
