package com.soleapixel.bmcmod.obsidian;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.registry.ModItems;

@EventBusSubscriber(modid = BmcMod.MODID)
public final class ObsidianArmorEvents {
    private static final int FIRE_RESIST_DURATION = 400;
    private static final int BRIDGE_LIFETIME_TICKS = 100;
    private static final int BRIDGE_RADIUS = 2;

    private static final List<LavaBridge> BRIDGES = new ArrayList<>();

    private ObsidianArmorEvents() {
    }

    private static boolean hasFullObsidian(Player player) {
        return isObsidianArmorPiece(player.getItemBySlot(EquipmentSlot.HEAD))
                && isObsidianArmorPiece(player.getItemBySlot(EquipmentSlot.CHEST))
                && isObsidianArmorPiece(player.getItemBySlot(EquipmentSlot.LEGS))
                && isObsidianArmorPiece(player.getItemBySlot(EquipmentSlot.FEET));
    }

    private static boolean isObsidianArmorPiece(ItemStack stack) {
        return stack.is(ModItems.OBSIDIAN_HELMET.get())
                || stack.is(ModItems.OBSIDIAN_CHESTPLATE.get())
                || stack.is(ModItems.OBSIDIAN_LEGGINGS.get())
                || stack.is(ModItems.OBSIDIAN_BOOTS.get());
    }

    /**
     * Résistance au feu tant que le set complet est porté : réapplication fréquente pour éviter les trous d’effet.
     */
    private static void refreshFireResistance(ServerPlayer player) {
        MobEffectInstance ex = player.getEffect(MobEffects.FIRE_RESISTANCE);
        if (ex != null && ex.getAmplifier() > 0) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, FIRE_RESIST_DURATION, 0, true, false, true));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!hasFullObsidian(player)) {
            return;
        }

        refreshFireResistance(player);

        boolean inLavaFluid = player.isInLava();
        if (inLavaFluid) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, true, false));
            player.clearFire();
            if (player.tickCount % 15 == 0) {
                damageRandomObsidianPiece(player, 1);
            }
            if (player.getDeltaMovement().y < -0.08) {
                player.setDeltaMovement(player.getDeltaMovement().multiply(1.0, 0.25, 1.0));
            }
        }

        tryPlaceLavaBridges(player);
        refreshBridgesUnderPlayer(player);
    }

    /**
     * Ponts temporaires (type Frost Walker) : colonnes sous les pieds + voisins, critères de surface assouplis.
     */
    private static void tryPlaceLavaBridges(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos feet = player.blockPosition();
        double feetY = player.getBoundingBox().minY;
        for (int dx = -BRIDGE_RADIUS; dx <= BRIDGE_RADIUS; dx++) {
            for (int dz = -BRIDGE_RADIUS; dz <= BRIDGE_RADIUS; dz++) {
                BlockPos columnBase = feet.offset(dx, 0, dz);
                for (int dy = 0; dy >= -5; dy--) {
                    BlockPos lavaPos = columnBase.offset(0, dy, 0);
                    BlockState state = level.getBlockState(lavaPos);
                    if (!state.getFluidState().is(Fluids.LAVA)) {
                        continue;
                    }
                    BlockState above = level.getBlockState(lavaPos.above());
                    if (!above.isAir() && above.blocksMotion()) {
                        continue;
                    }
                    double surfaceY = lavaPos.getY() + state.getFluidState().getHeight(level, lavaPos);
                    if (surfaceY < feetY - 1.15 || surfaceY > feetY + 0.55) {
                        continue;
                    }
                    replaceLavaWithBridge(level, lavaPos, state);
                    break;
                }
            }
        }
    }

    private static void refreshBridgesUnderPlayer(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos belowFeet = player.blockPosition().below();
        BlockPos atFeet = player.blockPosition();
        for (LavaBridge bridge : BRIDGES) {
            if (bridge.level != level) {
                continue;
            }
            boolean onBridge = bridge.pos.equals(belowFeet) || bridge.pos.equals(atFeet);
            if (!onBridge) {
                continue;
            }
            if (level.getBlockState(bridge.pos).is(Blocks.BLACKSTONE)) {
                bridge.ticksLeft = BRIDGE_LIFETIME_TICKS;
            }
        }
    }

    private static void replaceLavaWithBridge(ServerLevel level, BlockPos lavaPos, BlockState lavaState) {
        for (LavaBridge bridge : BRIDGES) {
            if (bridge.level == level && bridge.pos.equals(lavaPos)) {
                bridge.ticksLeft = BRIDGE_LIFETIME_TICKS;
                return;
            }
        }
        BlockState toRestore = lavaState;
        level.setBlock(lavaPos, Blocks.BLACKSTONE.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        BRIDGES.add(new LavaBridge(level, lavaPos.immutable(), toRestore, BRIDGE_LIFETIME_TICKS));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (BRIDGES.isEmpty()) {
            return;
        }
        Iterator<LavaBridge> it = BRIDGES.iterator();
        while (it.hasNext()) {
            LavaBridge bridge = it.next();
            bridge.ticksLeft--;
            if (bridge.ticksLeft > 0) {
                continue;
            }
            ServerLevel level = bridge.level;
            if (level != null && level.hasChunkAt(bridge.pos)) {
                BlockState current = level.getBlockState(bridge.pos);
                if (current.is(Blocks.BLACKSTONE)) {
                    level.setBlock(bridge.pos, bridge.lavaState, Block.UPDATE_ALL_IMMEDIATE);
                }
            }
            it.remove();
        }
    }

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!hasFullObsidian(player)) {
            return;
        }
        if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
                || event.getSource().getMsgId().contains("lava")) {
            event.setNewDamage(0.0F);
            damageRandomObsidianPiece(player, 2);
        }
    }

    private static void damageRandomObsidianPiece(ServerPlayer player, int amount) {
        EquipmentSlot[] slots = { EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD };
        EquipmentSlot pick = slots[player.getRandom().nextInt(slots.length)];
        ItemStack stack = player.getItemBySlot(pick);
        if (isObsidianArmorPiece(stack)) {
            stack.hurtAndBreak(amount, player, pick);
        }
    }

    private static final class LavaBridge {
        final ServerLevel level;
        final BlockPos pos;
        final BlockState lavaState;
        int ticksLeft;

        LavaBridge(ServerLevel level, BlockPos pos, BlockState lavaState, int ticksLeft) {
            this.level = level;
            this.pos = pos;
            this.lavaState = lavaState;
            this.ticksLeft = ticksLeft;
        }
    }
}
