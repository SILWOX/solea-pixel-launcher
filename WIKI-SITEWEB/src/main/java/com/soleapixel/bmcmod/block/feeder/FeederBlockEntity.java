package com.soleapixel.bmcmod.block.feeder;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.soleapixel.bmcmod.registry.ModBlockEntityTypes;

public class FeederBlockEntity extends BlockEntity {
    /** Item stocké (une seule espèce) ; la quantité totale est dans {@link #storedCount}. */
    private Item storedItem = ItemStack.EMPTY.getItem();
    private int storedCount = 0;

    public FeederBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.FEEDER.get(), pos, state);
    }

    public int getStoredCount() {
        return storedCount;
    }

    public ItemStack getStoredPrototype() {
        if (storedCount <= 0 || storedItem == ItemStack.EMPTY.getItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack s = new ItemStack(storedItem);
        s.setCount(Math.min(storedCount, s.getMaxStackSize()));
        return s;
    }

    /** Pile représentant une unité pour tests {@link net.minecraft.world.entity.animal.Animal#isFood(ItemStack)}. */
    public ItemStack getRepresentativeStack() {
        if (storedCount <= 0 || storedItem == ItemStack.EMPTY.getItem()) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(storedItem, 1);
    }

    public boolean isEmpty() {
        return storedCount <= 0;
    }

    public FeederContentKind getContentKind() {
        return getBlockState().getValue(FeederBlock.CONTENTS);
    }

    private static FeederContentKind kindFromItem(Item item) {
        ItemStack probe = new ItemStack(item);
        FeederContentKind k = FeederFoodRules.classify(probe);
        return k != null ? k : FeederContentKind.EMPTY;
    }

    public void syncBlockVisual() {
        if (level == null || level.isClientSide()) {
            return;
        }
        FeederContentKind kind = isEmpty() ? FeederContentKind.EMPTY : kindFromItem(storedItem);
        BlockState st = getBlockState();
        if (st.getValue(FeederBlock.CONTENTS) != kind) {
            level.setBlock(worldPosition, st.setValue(FeederBlock.CONTENTS, kind), 3);
        }
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** Insert jusqu'à {@code maxItems} depuis {@code stack} ; retourne le nombre inséré. */
    public int insertFrom(ItemStack stack, int maxItems) {
        return insertFrom(stack, maxItems, false);
    }

    /**
     * @param simulate si {@code true}, aucune modification du stock (pour hopper / automation).
     */
    public int insertFrom(ItemStack stack, int maxItems, boolean simulate) {
        if (stack.isEmpty() || maxItems <= 0) {
            return 0;
        }
        if (stack.is(Items.HAY_BLOCK)) {
            return insertHayBlocks(stack, maxItems, simulate);
        }
        FeederContentKind incomingKind = FeederFoodRules.classify(stack);
        if (incomingKind == null) {
            return 0;
        }
        int space = FeederFoodRules.MAX_ITEMS - storedCount;
        if (space <= 0) {
            return 0;
        }
        if (!isEmpty()) {
            if (storedItem != stack.getItem()) {
                return 0;
            }
        }
        int insert = Math.min(maxItems, Math.min(space, stack.getCount()));
        if (insert <= 0) {
            return 0;
        }
        if (!simulate) {
            if (isEmpty()) {
                storedItem = stack.getItem();
            }
            storedCount += insert;
            syncBlockVisual();
        }
        return insert;
    }

    /** Un bloc de foin compte comme 9 « unités » de blé (réserve inchangée : item stocké = blé). */
    private int insertHayBlocks(ItemStack stack, int maxItems, boolean simulate) {
        int space = FeederFoodRules.MAX_ITEMS - storedCount;
        int maxBalesBySpace = space / 9;
        if (maxBalesBySpace <= 0) {
            return 0;
        }
        int maxTakeFromStack = Math.min(stack.getCount(), maxItems);
        int bales = Math.min(maxTakeFromStack, maxBalesBySpace);
        if (bales <= 0) {
            return 0;
        }
        if (!isEmpty() && storedItem != Items.WHEAT) {
            return 0;
        }
        if (!simulate) {
            if (isEmpty()) {
                storedItem = Items.WHEAT;
            }
            storedCount += bales * 9;
            syncBlockVisual();
        }
        return bales;
    }

    public ItemStack extractItems(int maxItems) {
        return extractItems(maxItems, false);
    }

    /** @param simulate si {@code true}, retourne une copie sans modifier le stock. */
    public ItemStack extractItems(int maxItems, boolean simulate) {
        if (isEmpty() || maxItems <= 0) {
            return ItemStack.EMPTY;
        }
        int take = Math.min(maxItems, storedCount);
        ItemStack out = new ItemStack(storedItem, take);
        if (!simulate) {
            storedCount -= take;
            if (storedCount <= 0) {
                storedItem = ItemStack.EMPTY.getItem();
                storedCount = 0;
            }
            syncBlockVisual();
        }
        return out;
    }

    public void dropContents(Level level, BlockPos pos) {
        if (!isEmpty()) {
            while (storedCount > 0) {
                int n = Math.min(storedCount, new ItemStack(storedItem).getMaxStackSize());
                ItemStack drop = new ItemStack(storedItem, n);
                storedCount -= n;
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, drop);
            }
            storedItem = ItemStack.EMPTY.getItem();
            storedCount = 0;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("StoredItem", BuiltInRegistries.ITEM.getKey(storedItem).toString());
        tag.putInt("StoredCount", storedCount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        storedCount = tag.getInt("StoredCount");
        String id = tag.getString("StoredItem");
        storedItem = BuiltInRegistries.ITEM.getOptional(net.minecraft.resources.ResourceLocation.parse(id)).orElse(ItemStack.EMPTY.getItem());
        if (storedCount <= 0) {
            storedItem = ItemStack.EMPTY.getItem();
            storedCount = 0;
        }
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        super.onDataPacket(net, pkt, registries);
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            loadAdditional(tag, registries);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            FeederNetwork.add(level, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide()) {
            FeederNetwork.remove(level, worldPosition);
        }
        super.setRemoved();
    }

    public void serverTick() {
        FeederAutoFeed.tickLostLinkParticles(this);
        FeederAutoFeed.tickLinkParticles(this);
        FeederAutoFeed.tickServer(this);
    }
}
