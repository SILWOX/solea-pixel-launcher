package com.soleapixel.bmcmod.gameplay;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.entity.CloneEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** Mort : transfert équipement + inventaire vers un coffre près du propriétaire (excédant en drops). */
@EventBusSubscriber(modid = BmcMod.MODID)
public final class CloneDeathLootHandler {

    private CloneDeathLootHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onCloneDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof CloneEntity clone)) {
            return;
        }
        if (!(clone.level() instanceof ServerLevel level)) {
            return;
        }
        NonNullList<ItemStack> stacks = collectAllStacks(clone);
        wipeEquipmentAndInventory(clone);
        ServerPlayer owner = clone.getOwnerPlayer() instanceof ServerPlayer sp ? sp : null;
        if (owner == null) {
            dropItemsNear(level, clone.position(), stacks, null);
            return;
        }
        BlockPos chestPos = pickChestNear(level, owner.blockPosition());
        Direction face = Direction.fromYRot(owner.getYRot()).getOpposite();
        BlockState state = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, face);
        if (chestPos == null || !mayPlaceChest(level, chestPos)) {
            dropItemsNear(level, owner.position().add(0.0D, 0.2D, 0.0D), stacks, owner);
            return;
        }
        if (!level.setBlock(chestPos, state, 11)) {
            dropItemsNear(level, owner.position().add(0.0D, 0.2D, 0.0D), stacks, owner);
            return;
        }
        if (!(level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest)) {
            level.removeBlock(chestPos, false);
            dropItemsNear(level, owner.position(), stacks, owner);
            return;
        }
        chest.clearContent();
        NonNullList<ItemStack> overflow = mergeIntoChest(chest, stacks);
        if (!overflow.isEmpty()) {
            Vec3 spill = chestPos.relative(face).above().getBottomCenter();
            dropItemsNear(level, spill, overflow, owner);
        }
        level.playSound(null, chestPos.getX() + 0.5D, chestPos.getY(), chestPos.getZ() + 0.5D, SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.4F, 1.65F);
        level.sendParticles(ParticleTypes.PORTAL, chestPos.getX() + 0.5D, chestPos.getY() + 0.6D, chestPos.getZ() + 0.5D,
                22, 0.25, 0.2, 0.25, 0.01);
        owner.displayClientMessage(Component.translatable("message.bmcmod.clone_death_chest").withStyle(ChatFormatting.GOLD), false);
    }

    private static boolean mayPlaceChest(Level level, BlockPos chestPos) {
        return level.isEmptyBlock(chestPos) && level.isEmptyBlock(chestPos.above());
    }

    private static NonNullList<ItemStack> collectAllStacks(CloneEntity clone) {
        NonNullList<ItemStack> stacks = NonNullList.create();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack st = clone.getItemBySlot(slot);
            if (st.isEmpty()) {
                continue;
            }
            if (!shouldDropEquipment(slot, st)) {
                continue;
            }
            stacks.add(st.copy());
        }
        for (int i = 0; i < clone.getCloneInventory().getContainerSize(); i++) {
            ItemStack st = clone.getCloneInventory().getItem(i);
            if (!st.isEmpty()) {
                stacks.add(st.copy());
            }
        }
        return stacks;
    }

    /**
     * Anti-duplication :
     * - OFFHAND ne drop jamais (totem de seconde vie du clone).
     * - MAINHAND drop seulement si ce n'est pas un outil par defaut.
     */
    private static boolean shouldDropEquipment(EquipmentSlot slot, ItemStack stack) {
        if (slot.isArmor()) {
            return false;
        }
        if (slot == EquipmentSlot.OFFHAND) {
            return false;
        }
        if (slot == EquipmentSlot.MAINHAND && isDefaultCloneMainHand(stack)) {
            return false;
        }
        return true;
    }

    private static boolean isDefaultCloneMainHand(ItemStack stack) {
        return stack.is(Items.STONE_SWORD) || stack.is(Items.STONE_AXE) || stack.is(Items.STONE_PICKAXE);
    }

    private static void wipeEquipmentAndInventory(CloneEntity clone) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            clone.setItemSlot(slot, ItemStack.EMPTY);
        }
        for (int i = 0; i < clone.getCloneInventory().getContainerSize(); i++) {
            clone.getCloneInventory().setItem(i, ItemStack.EMPTY);
        }
    }

    private static NonNullList<ItemStack> mergeIntoChest(ChestBlockEntity chest, Iterable<ItemStack> stacks) {
        NonNullList<ItemStack> overflow = NonNullList.create();
        int size = chest.getContainerSize();
        for (ItemStack want : stacks) {
            if (want.isEmpty()) {
                continue;
            }
            ItemStack rem = want.copy();
            rem = chestMergeStack(chest, size, rem);
            if (!rem.isEmpty()) {
                overflow.add(rem);
            }
        }
        return overflow;
    }

    private static ItemStack chestMergeStack(ChestBlockEntity chest, int size, ItemStack stack) {
        ItemStack rem = stack.copy();
        for (int i = 0; i < size && !rem.isEmpty(); i++) {
            ItemStack slot = chest.getItem(i);
            if (slot.isEmpty() || !ItemStack.isSameItemSameComponents(slot, rem)) {
                continue;
            }
            int mv = Math.min(rem.getCount(), slot.getMaxStackSize() - slot.getCount());
            if (mv > 0) {
                slot.grow(mv);
                rem.shrink(mv);
                chest.setItem(i, slot);
            }
        }
        for (int i = 0; i < size && !rem.isEmpty(); i++) {
            if (chest.getItem(i).isEmpty()) {
                int mv = Math.min(rem.getMaxStackSize(), rem.getCount());
                ItemStack deposit = rem.split(mv);
                chest.setItem(i, deposit);
            }
        }
        return rem.isEmpty() ? ItemStack.EMPTY : rem;
    }

    private static void dropItemsNear(ServerLevel level, Vec3 origin, Iterable<ItemStack> stacks, ServerPlayer notify) {
        for (ItemStack st : stacks) {
            if (st.isEmpty()) {
                continue;
            }
            ItemEntity ei = new ItemEntity(level, origin.x, origin.y + 0.15D, origin.z, st.copy());
            level.addFreshEntity(ei);
        }
        if (notify != null && notify.isAlive()) {
            notify.displayClientMessage(Component.translatable("message.bmcmod.clone_death_inventory").withStyle(ChatFormatting.YELLOW),
                    false);
        }
    }

    private static BlockPos pickChestNear(Level level, BlockPos anchor) {
        for (int dy = 0; dy <= 5; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos cand = anchor.offset(dx, dy, dz);
                    if (mayPlaceChest(level, cand)) {
                        return cand;
                    }
                }
            }
        }
        BlockPos cand = anchor.above(2);
        return mayPlaceChest(level, cand) ? cand : null;
    }
}
