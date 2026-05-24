package com.soleapixel.bmcmod.item.builderwand;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * Blocs posables présents dans l’inventaire (inspiré du fonctionnement type Paladium : le matériau vient des stacks).
 *
 * @see <a href="https://wiki.paladium-pvp.fr/fr/article/java-objets-debloques-mineur-1hm3mco/">Paladium — outil de construction</a>
 */
public final class BuilderWandMaterialPicker {
    private BuilderWandMaterialPicker() {
    }

    /** Types de blocs distincts, triés par id (ordre stable pour le cycle). */
    public static List<Block> sortedPlaceableBlocks(Inventory inventory) {
        Set<Block> seen = new LinkedHashSet<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (!(stack.getItem() instanceof BlockItem bi)) {
                continue;
            }
            Block block = bi.getBlock();
            if (block.defaultBlockState().isAir()) {
                continue;
            }
            if (block.asItem().getDefaultInstance().isEmpty()) {
                continue;
            }
            seen.add(block);
        }
        List<Block> list = new ArrayList<>(seen);
        list.sort(Comparator.comparing(b -> BuiltInRegistries.BLOCK.getKey(b).toString()));
        return list;
    }
}
