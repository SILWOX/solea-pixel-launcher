package com.soleapixel.bmcmod.block.endstonefurnace;

import java.util.List;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.soleapixel.bmcmod.menu.EndstoneFurnaceMenu;
import com.soleapixel.bmcmod.mixin.AbstractFurnaceBlockEntityHooks;
import com.soleapixel.bmcmod.registry.ModBlockEntityTypes;
public class EndstoneFurnaceBlockEntity extends AbstractFurnaceBlockEntity {
    public EndstoneFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.ENDSTONE_FURNACE.get(), pos, state, RecipeType.SMELTING);
    }

    /** Exposé pour {@link com.soleapixel.bmcmod.menu.EndstoneFurnaceMenu} (champ {@code dataAccess} hérité protégé). */
    public ContainerData getFurnaceDataAccess() {
        return dataAccess;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.bmcmod.endstone_furnace");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new EndstoneFurnaceMenu(id, inv, this);
    }

    private static final float XP_MULTIPLIER = 1.5F;

    /**
     * Recopie la logique vanilla (lambda invokedynamic) avec un multiplicateur d’XP pour ce four.
     */
    @Override
    public List<RecipeHolder<?>> getRecipesToAwardAndPopExperience(ServerLevel level, Vec3 pos) {
        List<RecipeHolder<?>> list = Lists.newArrayList();
        Object2IntOpenHashMap<ResourceLocation> recipesUsed =
                ((AbstractFurnaceBlockEntityHooks) (Object) this).bmcmod$recipesUsed();
        for (Object2IntMap.Entry<ResourceLocation> entry : recipesUsed.object2IntEntrySet()) {
            level.getRecipeManager().byKey(entry.getKey()).ifPresent(holder -> {
                list.add(holder);
                float xp = 0.0F;
                if (holder.value() instanceof AbstractCookingRecipe cooking) {
                    xp = cooking.getExperience() * XP_MULTIPLIER;
                }
                AbstractFurnaceBlockEntityHooks.bmcmod$createExperience(level, pos, entry.getIntValue(), xp);
            });
        }
        return list;
    }
}
