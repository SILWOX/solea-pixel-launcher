package com.soleapixel.bmcmod.block.upgradetable;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.soleapixel.bmcmod.item.upgrade.ChestplateUpgradeData;
import com.soleapixel.bmcmod.menu.UpgradeTableMenu;
import com.soleapixel.bmcmod.registry.ModBlockEntityTypes;
import com.soleapixel.bmcmod.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class UpgradeTableBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_UPGRADE = 0;
    public static final int SLOT_CHESTPLATE = 1;
    public static final int SLOT_SHARD = 2;
    public static final int SLOT_OUTPUT = 3;
    public static final int SLOT_COUNT = 4;

    public static final TagKey<Item> UPGRADE_INGREDIENTS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("bmcmod", "upgrade_ingredients"));
    /** Éclats de géodes (Nebrith, Topaz, Opal, Beryl) pour la table d’amélioration. */
    public static final TagKey<Item> GEODE_SHARDS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("bmcmod", "foundry_shards"));

    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            if (!levelIsClient()) {
                refreshOutput();
            }
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case SLOT_UPGRADE -> isUpgradeIngredient(stack);
                case SLOT_CHESTPLATE -> isChestplate(stack);
                case SLOT_SHARD -> isShard(stack);
                case SLOT_OUTPUT -> false;
                default -> false;
            };
        }
    };

    public final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            if (index == 0) {
                return ChestplateUpgradeData.size(items.getStackInSlot(SLOT_CHESTPLATE));
            }
            return 0;
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public UpgradeTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.UPGRADE_TABLE.get(), pos, state);
    }

    public ItemStackHandler getItems() {
        return this.items;
    }

    public static boolean isChestplate(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem armorItem && armorItem.getType() == ArmorItem.Type.CHESTPLATE;
    }

    public static boolean isShard(ItemStack stack) {
        return stack.is(GEODE_SHARDS);
    }

    public static boolean isUpgradeIngredient(ItemStack stack) {
        return stack.is(UPGRADE_INGREDIENTS);
    }

    public void refreshOutput() {
        ItemStack output = computeResult();
        ItemStack current = this.items.getStackInSlot(SLOT_OUTPUT);
        if (!ItemStack.isSameItemSameComponents(current, output) || current.getCount() != output.getCount()) {
            this.items.setStackInSlot(SLOT_OUTPUT, output);
            this.setChanged();
        }
    }

    public ItemStack computeResult() {
        ItemStack upgrade = this.items.getStackInSlot(SLOT_UPGRADE);
        ItemStack chestplate = this.items.getStackInSlot(SLOT_CHESTPLATE);
        ItemStack shard = this.items.getStackInSlot(SLOT_SHARD);
        if (upgrade.isEmpty() || chestplate.isEmpty() || shard.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (!isUpgradeIngredient(upgrade) || !isChestplate(chestplate) || !isShard(shard) || !ChestplateUpgradeData.canAdd(chestplate)) {
            return ItemStack.EMPTY;
        }
        ResourceLocation upgradeId = resolveUpgradeId(upgrade);
        if (!ChestplateUpgradeData.canAdd(chestplate, upgradeId)) {
            return ItemStack.EMPTY;
        }
        return ChestplateUpgradeData.withAppendedUpgrade(chestplate, upgradeId);
    }

    public ResourceLocation resolveUpgradeId(ItemStack upgrade) {
        ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(upgrade.getItem());
        return key == null ? ChestplateUpgradeData.upgradePlateId() : key;
    }

    public void consumeCraftInputs() {
        this.items.getStackInSlot(SLOT_UPGRADE).shrink(1);
        this.items.getStackInSlot(SLOT_CHESTPLATE).shrink(1);
        this.items.getStackInSlot(SLOT_SHARD).shrink(1);
        this.items.setStackInSlot(SLOT_OUTPUT, ItemStack.EMPTY);
        this.setChanged();
    }

    private boolean levelIsClient() {
        return this.level != null && this.level.isClientSide();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.bmcmod.upgrade_table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new UpgradeTableMenu(ModMenus.UPGRADE_TABLE.get(), id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", this.items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items.deserializeNBT(registries, tag.getCompound("items"));
        refreshOutput();
    }
}
