package com.soleapixel.bmcmod.shulker;

import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.minecraft.core.registries.Registries;

import java.util.List;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.network.ShulkerArmorPackets;
import com.soleapixel.bmcmod.registry.ModItems;

@EventBusSubscriber(modid = BmcMod.MODID)
public final class ShulkerEquipment {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, BmcMod.MODID);

    /** Proche du fer, réparation coquille de shulker. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SHULKER_ARMOR_MATERIAL = ARMOR_MATERIALS.register("shulker",
            () -> new ArmorMaterial(
                    Map.of(
                            ArmorItem.Type.BOOTS, 2,
                            ArmorItem.Type.LEGGINGS, 5,
                            ArmorItem.Type.CHESTPLATE, 6,
                            ArmorItem.Type.HELMET, 2),
                    12,
                    SoundEvents.ARMOR_EQUIP_CHAIN,
                    () -> Ingredient.of(Items.SHULKER_SHELL),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "shulker/shulker"))),
                    0.0F,
                    0.0F));

    private static final ResourceLocation SLOW_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "shulker_armor_weight");
    private static final double SLOW_AMOUNT = -0.07;

    private static final int MAX_CHARGES = 3;
    private static final int RECHARGE_TICKS = 15 * 20;

    private static final Map<UUID, ChargeState> SERVER_STATES = new ConcurrentHashMap<>();

    private ShulkerEquipment() {
    }

    private static int countShulkerPieces(Player player) {
        int n = 0;
        if (player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.SHULKER_HELMET.get())) {
            n++;
        }
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.SHULKER_CHESTPLATE.get())) {
            n++;
        }
        if (player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.SHULKER_LEGGINGS.get())) {
            n++;
        }
        if (player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.SHULKER_BOOTS.get())) {
            n++;
        }
        return n;
    }

    private static boolean hasFullShulkerSet(Player player) {
        return countShulkerPieces(player) == 4;
    }

    private static void applyOrRemoveSlowness(ServerPlayer player) {
        AttributeInstance move = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (move == null) {
            return;
        }
        boolean any = countShulkerPieces(player) > 0;
        if (any) {
            if (!move.hasModifier(SLOW_MODIFIER_ID)) {
                move.addTransientModifier(new AttributeModifier(SLOW_MODIFIER_ID, SLOW_AMOUNT, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        } else {
            move.removeModifier(SLOW_MODIFIER_ID);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }

        applyOrRemoveSlowness(player);

        UUID id = player.getUUID();
        if (hasFullShulkerSet(player)) {
            ChargeState state = SERVER_STATES.computeIfAbsent(id, u -> new ChargeState());
            state.tickRegen(player.serverLevel());
            syncIfDirty(player, state);
        } else {
            SERVER_STATES.remove(id);
            PacketDistributor.sendToPlayer(player, new ShulkerArmorPackets.ShulkerSyncPayload(-1, 0));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SERVER_STATES.remove(event.getEntity().getUUID());
    }

    private static void syncIfDirty(ServerPlayer player, ChargeState state) {
        int ticksLeft = state.ticksUntilNextCharge(player.serverLevel());
        if (state.charges != state.lastSentCharges || ticksLeft != state.lastSentTicks) {
            PacketDistributor.sendToPlayer(player, new ShulkerArmorPackets.ShulkerSyncPayload(state.charges, ticksLeft));
            state.lastSentCharges = state.charges;
            state.lastSentTicks = ticksLeft;
        }
    }

    public static void handleShootPacket(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.level().isClientSide()) {
            return;
        }
        if (!hasFullShulkerSet(serverPlayer)) {
            return;
        }
        ChargeState state = SERVER_STATES.get(serverPlayer.getUUID());
        if (state == null || state.charges <= 0) {
            return;
        }
        LivingEntity target = findNearestHostileTarget(serverPlayer);
        if (target == null) {
            serverPlayer.displayClientMessage(Component.translatable("message.bmcmod.shulker_shoot_no_target"), true);
            return;
        }
        ServerLevel level = serverPlayer.serverLevel();
        Direction.Axis axis = Direction.getNearest(target.getEyePosition().subtract(serverPlayer.getEyePosition())).getAxis();
        ShulkerBullet bullet = new ShulkerBullet(level, serverPlayer, target, axis);
        level.addFreshEntity(bullet);
        level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), SoundEvents.SHULKER_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);

        state.charges--;
        state.regenTimes.add(level.getGameTime() + RECHARGE_TICKS);
        syncIfDirty(serverPlayer, state);
    }

    private static LivingEntity findNearestHostileTarget(ServerPlayer player) {
        AABB box = player.getBoundingBox().inflate(20.0);
        double best = 400.0;
        LivingEntity pick = null;
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, box, ShulkerEquipment::isValidShulkerTarget)) {
            if (e == player || !e.isAlive()) {
                continue;
            }
            double d = e.distanceToSqr(player);
            if (d <= 400.0 && d < best) {
                best = d;
                pick = e;
            }
        }
        return pick;
    }

    private static boolean isValidShulkerTarget(LivingEntity e) {
        return e.isPickable() && !e.isSpectator();
    }

    private static final class ChargeState {
        int charges = MAX_CHARGES;
        final PriorityQueue<Long> regenTimes = new PriorityQueue<>();
        int lastSentCharges = -1;
        int lastSentTicks = -1;

        void tickRegen(ServerLevel level) {
            long t = level.getGameTime();
            while (!regenTimes.isEmpty() && regenTimes.peek() <= t) {
                regenTimes.poll();
                if (charges < MAX_CHARGES) {
                    charges++;
                }
            }
        }

        int ticksUntilNextCharge(ServerLevel level) {
            long t = level.getGameTime();
            if (regenTimes.isEmpty()) {
                return 0;
            }
            return (int) Math.max(0, regenTimes.peek() - t);
        }
    }
}
