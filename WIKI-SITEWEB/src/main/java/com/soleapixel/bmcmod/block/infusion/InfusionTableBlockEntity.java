package com.soleapixel.bmcmod.block.infusion;

import java.util.Optional;

import javax.annotation.Nullable;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.item.CrystalItem;
import com.soleapixel.bmcmod.menu.InfusionTableMenu;
import com.soleapixel.bmcmod.recipe.infusion.InfusionRecipe;
import com.soleapixel.bmcmod.recipe.infusion.InfusionRecipes;
import com.soleapixel.bmcmod.registry.ModBlockEntityTypes;
import com.soleapixel.bmcmod.registry.ModItems;
import com.soleapixel.bmcmod.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Table d'infusion : cristal (0), 4 ingrédients (1–4), sortie (5, résultat réel ou aperçu
 * fantôme synchronisé, non retirable), animation du livre héritée de la table d'enchantement.
 */
public class InfusionTableBlockEntity extends BlockEntity implements Nameable, MenuProvider {
    public static final int SLOT_CRYSTAL = 0;
    public static final int INPUT_START = 1;
    public static final int INPUT_END = 4;
    public static final int SLOT_OUTPUT = 5;
    public static final int SLOT_COUNT = 6;
    public static final int INFUSION_DURATION = 200;

    public int time;
    public float flip;
    public float oFlip;
    public float flipT;
    public float flipA;
    public float open;
    public float oOpen;
    public float rot;
    public float oRot;
    public float tRot;
    private static final RandomSource RANDOM = RandomSource.create();
    @Nullable
    private Component name;

    private int infusionProgress;
    private boolean infusing;
    @Nullable
    private InfusionRecipe currentRecipe;

    private int lastSyncedSoulCost;
    private int canStartFlag;
    private boolean internalSync;
    /**
     * Si l’item dans le slot de sortie est l’aperçu de recette (copie de résultat) et non
     * encore produit valide par une infusion terminée.
     */
    private boolean outputIsPreview;

    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            if (InfusionTableBlockEntity.this.level == null || InfusionTableBlockEntity.this.level.isClientSide()) {
                return;
            }
            if (internalSync) {
                return;
            }
            recompute();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == SLOT_CRYSTAL) {
                return stack.is(ModItems.CRYSTAL.get());
            }
            if (slot == SLOT_OUTPUT) {
                return false;
            }
            return true;
        }

        @NotNull
        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (InfusionTableBlockEntity.this.infusing) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @NotNull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (InfusionTableBlockEntity.this.infusing) {
                if (slot < SLOT_COUNT) {
                    return ItemStack.EMPTY;
                }
            }
            if (slot == SLOT_OUTPUT && InfusionTableBlockEntity.this.outputIsPreview) {
                return ItemStack.EMPTY;
            }
            return super.extractItem(slot, amount, simulate);
        }
    };

    public final ContainerData dataAccess = new ContainerData() {
        @Override
        public int getCount() {
            return 5;
        }

        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> InfusionTableBlockEntity.this.infusionProgress;
                case 1 -> INFUSION_DURATION;
                case 2 -> InfusionTableBlockEntity.this.lastSyncedSoulCost;
                case 3 -> InfusionTableBlockEntity.this.canStartFlag;
                case 4 -> InfusionTableBlockEntity.this.infusing ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> InfusionTableBlockEntity.this.infusionProgress = value;
                case 1 -> {
                }
                case 2 -> InfusionTableBlockEntity.this.lastSyncedSoulCost = value;
                case 3 -> InfusionTableBlockEntity.this.canStartFlag = value;
                case 4 -> InfusionTableBlockEntity.this.infusing = (value == 1);
                default -> {
                }
            }
        }
    };

    public InfusionTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.INFUSION_TABLE.get(), pos, state);
    }

    @NotNull
    public ItemStackHandler getItems() {
        return this.items;
    }

    public void serverTick() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        if (!this.infusing) {
            return;
        }
        if (this.currentRecipe == null) {
            this.stopInfusing();
            return;
        }
        this.infusionProgress++;
        if (this.infusionProgress >= INFUSION_DURATION) {
            this.finishInfusion();
        } else {
            this.setChanged();
        }
    }

    private void stopInfusing() {
        this.infusing = false;
        this.infusionProgress = 0;
        this.currentRecipe = null;
        recompute();
    }

    private void finishInfusion() {
        if (this.currentRecipe == null) {
            stopInfusing();
            return;
        }
        InfusionRecipe r = this.currentRecipe;
        ItemStack crystal = this.items.getStackInSlot(SLOT_CRYSTAL);
        if (!crystal.is(ModItems.CRYSTAL.get()) || CrystalItem.getTotalSoulCount(crystal) < r.soulCost) {
            stopInfusing();
            return;
        }
        if (CrystalItem.removeSouls(crystal, r.soulCost) < r.soulCost) {
            stopInfusing();
            return;
        }
        this.items.setStackInSlot(SLOT_CRYSTAL, crystal);
        consumeIngredients(r);
        ItemStack out = r.result.copy();
        if (this.outputIsPreview) {
            this.internalSync = true;
            try {
                this.items.setStackInSlot(SLOT_OUTPUT, out);
            } finally {
                this.internalSync = false;
            }
            this.outputIsPreview = false;
        } else {
            ItemStack current = this.items.getStackInSlot(SLOT_OUTPUT);
            if (current.isEmpty()) {
                this.items.setStackInSlot(SLOT_OUTPUT, out);
            } else {
                int room = current.getMaxStackSize() - current.getCount();
                int add = Math.min(out.getCount(), room);
                current.grow(add);
                out.shrink(add);
                this.items.setStackInSlot(SLOT_OUTPUT, current);
                if (!out.isEmpty() && this.level != null) {
                    Containers.dropItemStack(
                            this.level, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.0, this.worldPosition.getZ() + 0.5, out);
                }
            }
        }
        this.stopInfusing();
    }

    private void consumeIngredients(InfusionRecipe recipe) {
        for (var e : recipe.required.entrySet()) {
            int need = e.getValue();
            for (int s = INPUT_START; s <= INPUT_END && need > 0; s++) {
                ItemStack st = this.items.getStackInSlot(s);
                if (st.getItem() != e.getKey()) {
                    continue;
                }
                int t = Math.min(need, st.getCount());
                st.shrink(t);
                need -= t;
            }
        }
    }

    public void tryStartInfusion() {
        if (this.level == null || this.level.isClientSide() || this.infusing) {
            return;
        }
        recompute();
        if (this.canStartFlag == 0 || this.currentRecipe == null) {
            return;
        }
        ItemStack crystal = this.items.getStackInSlot(SLOT_CRYSTAL);
        if (CrystalItem.getTotalSoulCount(crystal) < this.currentRecipe.soulCost) {
            return;
        }
        this.infusing = true;
        this.infusionProgress = 0;
        this.setChanged();
    }

    void recompute() {
        if (this.level == null || this.level.isClientSide() || this.infusing) {
            return;
        }
        this.currentRecipe = null;
        this.lastSyncedSoulCost = 0;
        this.canStartFlag = 0;
        ItemStack crystal = this.items.getStackInSlot(SLOT_CRYSTAL);
        if (crystal.isEmpty() || !crystal.is(ModItems.CRYSTAL.get())) {
            clearOutputPreview();
            return;
        }
        ItemStack a = this.items.getStackInSlot(1);
        ItemStack b = this.items.getStackInSlot(2);
        ItemStack c = this.items.getStackInSlot(3);
        ItemStack d = this.items.getStackInSlot(4);
        Optional<InfusionRecipe> match = InfusionRecipes.findMatch(a, b, c, d);
        if (match.isEmpty()) {
            clearOutputPreview();
            return;
        }
        InfusionRecipe r = match.get();
        this.currentRecipe = r;
        this.lastSyncedSoulCost = r.soulCost;
        ItemStack cur = this.items.getStackInSlot(SLOT_OUTPUT);
        if (cur.isEmpty() || this.outputIsPreview) {
            this.internalSync = true;
            try {
                this.items.setStackInSlot(SLOT_OUTPUT, r.result.copy());
            } finally {
                this.internalSync = false;
            }
            this.outputIsPreview = true;
        }
        boolean outOk = canOutputAccept(r.result);
        boolean soulOk = CrystalItem.getTotalSoulCount(crystal) >= r.soulCost;
        this.canStartFlag = (soulOk && outOk) ? 1 : 0;
        this.setChanged();
    }

    private void clearOutputPreview() {
        if (!this.outputIsPreview) {
            return;
        }
        this.internalSync = true;
        try {
            this.items.setStackInSlot(SLOT_OUTPUT, ItemStack.EMPTY);
        } finally {
            this.internalSync = false;
        }
        this.outputIsPreview = false;
    }

    private boolean canOutputAccept(ItemStack result) {
        ItemStack cur = this.items.getStackInSlot(SLOT_OUTPUT);
        if (cur.isEmpty()) {
            return true;
        }
        if (this.outputIsPreview && ItemStack.isSameItemSameComponents(cur, result)) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(cur, result)
                && cur.getCount() + result.getCount() <= cur.getMaxStackSize();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.hasCustomName()) {
            tag.putString("CustomName", Component.Serializer.toJson(this.name, registries));
        }
        tag.put("Items", this.items.serializeNBT(registries));
        tag.putInt("InfusionProgress", this.infusionProgress);
        tag.putBoolean("Infusing", this.infusing);
        tag.putBoolean("OutPreview", this.outputIsPreview);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("CustomName", 8)) {
            this.name = parseCustomNameSafe(tag.getString("CustomName"), registries);
        }
        if (tag.contains("Items", Tag.TAG_LIST) || tag.contains("Items", Tag.TAG_COMPOUND)) {
            this.items.deserializeNBT(registries, tag.getCompound("Items"));
        } else {
            this.items.deserializeNBT(registries, new CompoundTag());
        }
        this.infusionProgress = tag.getInt("InfusionProgress");
        this.infusing = tag.getBoolean("Infusing");
        this.outputIsPreview = tag.getBoolean("OutPreview");
    }

    public static void bookAnimationTick(
            Level level, BlockPos pos, BlockState state, InfusionTableBlockEntity table) {
        table.oOpen = table.open;
        table.oRot = table.rot;
        Player player = level.getNearestPlayer(
                (double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5, 3.0, false);
        if (player != null) {
            double d0 = player.getX() - ((double) pos.getX() + 0.5);
            double d1 = player.getZ() - ((double) pos.getZ() + 0.5);
            table.tRot = (float) Mth.atan2(d1, d0);
            table.open += 0.1F;
            if (table.open < 0.5F || RANDOM.nextInt(40) == 0) {
                float f1 = table.flipT;
                do {
                    table.flipT = table.flipT + (float) (RANDOM.nextInt(4) - RANDOM.nextInt(4));
                } while (f1 == table.flipT);
            }
        } else {
            table.tRot += 0.02F;
            table.open -= 0.1F;
        }

        while (table.rot >= (float) Math.PI) {
            table.rot -= (float) (Math.PI * 2);
        }
        while (table.rot < (float) -Math.PI) {
            table.rot += (float) (Math.PI * 2);
        }
        while (table.tRot >= (float) Math.PI) {
            table.tRot -= (float) (Math.PI * 2);
        }
        while (table.tRot < (float) -Math.PI) {
            table.tRot += (float) (Math.PI * 2);
        }

        float f2 = table.tRot - table.rot;
        while (f2 >= (float) Math.PI) {
            f2 -= (float) (Math.PI * 2);
        }
        while (f2 < (float) -Math.PI) {
            f2 += (float) (Math.PI * 2);
        }

        table.rot += f2 * 0.4F;
        table.open = Mth.clamp(table.open, 0.0F, 1.0F);
        table.time++;
        table.oFlip = table.flip;
        float f = (table.flipT - table.flip) * 0.4F;
        f = Mth.clamp(f, -0.2F, 0.2F);
        table.flipA = table.flipA + (f - table.flipA) * 0.9F;
        table.flip = table.flip + table.flipA;
    }

    @Override
    public Component getName() {
        return (Component) (this.name != null
                ? this.name
                : Component.translatable("block." + BmcMod.MODID + ".infusion_table"));
    }

    public void setCustomName(@Nullable Component customName) {
        this.name = customName;
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return this.name;
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        this.name = componentInput.get(DataComponents.CUSTOM_NAME);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CUSTOM_NAME, this.name);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("CustomName");
    }

    @Override
    @NotNull
    public Component getDisplayName() {
        return this.name != null
                ? this.name
                : Component.translatable("container.bmcmod.infusion_table");
    }

    @Override
    @NotNull
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inv, @NotNull Player p) {
        return new InfusionTableMenu(ModMenus.INFUSION_TABLE.get(), containerId, inv, this);
    }

    public void dropAllContents(Level level, BlockPos pos, BlockState state) {
        for (int s = 0; s < SLOT_COUNT; s++) {
            if (s == SLOT_OUTPUT && this.outputIsPreview) {
                continue;
            }
            ItemStack st = this.items.getStackInSlot(s);
            if (!st.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, st);
                this.items.setStackInSlot(s, ItemStack.EMPTY);
            }
        }
        this.outputIsPreview = false;
    }

    /**
     * Comme la table de craft : au fermeture du menu, renvoie les objets dans l’inventaire du joueur.
     * L’aperçu de recette dans le slot sortie n’est pas dupliqué ; une infusion en cours est annulée.
     */
    public void returnContentsToPlayerOnMenuClosed(Player player) {
        if (player.level().isClientSide() || this.level == null) {
            return;
        }
        this.infusing = false;
        this.infusionProgress = 0;
        this.currentRecipe = null;

        this.internalSync = true;
        try {
            for (int s = SLOT_CRYSTAL; s <= INPUT_END; s++) {
                ItemStack stack = this.items.getStackInSlot(s);
                if (!stack.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(stack.copy());
                    this.items.setStackInSlot(s, ItemStack.EMPTY);
                }
            }
            ItemStack out = this.items.getStackInSlot(SLOT_OUTPUT);
            if (!out.isEmpty()) {
                if (!this.outputIsPreview) {
                    player.getInventory().placeItemBackInInventory(out.copy());
                }
                this.items.setStackInSlot(SLOT_OUTPUT, ItemStack.EMPTY);
                this.outputIsPreview = false;
            }
        } finally {
            this.internalSync = false;
        }
        this.setChanged();
        this.recompute();
    }

}
