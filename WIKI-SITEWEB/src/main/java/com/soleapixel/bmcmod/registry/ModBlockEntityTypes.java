package com.soleapixel.bmcmod.registry;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.block.feeder.FeederBlockEntity;
import com.soleapixel.bmcmod.block.chest.EnchantedChestBlockEntity;
import com.soleapixel.bmcmod.block.chest.FakeChestBlockEntity;
import com.soleapixel.bmcmod.block.endstonefurnace.EndstoneFurnaceBlockEntity;
import com.soleapixel.bmcmod.block.foundry.FoundryBlockEntity;
import com.soleapixel.bmcmod.block.hollow.HollowBarrelBlockEntity;
import com.soleapixel.bmcmod.block.infusion.InfusionTableBlockEntity;
import com.soleapixel.bmcmod.block.upgradetable.UpgradeTableBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(
            Registries.BLOCK_ENTITY_TYPE, BmcMod.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnchantedChestBlockEntity>> ENCHANTED_CHEST = BLOCK_ENTITIES
            .register("enchanted_chest", () -> BlockEntityType.Builder.of(EnchantedChestBlockEntity::new, ModBlocks.ENCHANTED_CHEST.get())
                    .build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FakeChestBlockEntity>> FAKE_CHEST = BLOCK_ENTITIES
            .register("fake_chest", () -> BlockEntityType.Builder.of(FakeChestBlockEntity::new, ModBlocks.FAKE_CHEST.get())
                    .build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InfusionTableBlockEntity>> INFUSION_TABLE = BLOCK_ENTITIES
            .register("infusion_table", () -> BlockEntityType.Builder
                    .of(InfusionTableBlockEntity::new, ModBlocks.INFUSION_TABLE.get())
                    .build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EndstoneFurnaceBlockEntity>> ENDSTONE_FURNACE = BLOCK_ENTITIES.register(
            "endstone_furnace",
            () -> BlockEntityType.Builder.of(EndstoneFurnaceBlockEntity::new, ModBlocks.ENDSTONE_FURNACE.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FoundryBlockEntity>> FOUNDRY = BLOCK_ENTITIES.register(
            "foundry",
            () -> BlockEntityType.Builder.of(FoundryBlockEntity::new, ModBlocks.FOUNDRY.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<UpgradeTableBlockEntity>> UPGRADE_TABLE = BLOCK_ENTITIES.register(
            "upgrade_table",
            () -> BlockEntityType.Builder.of(UpgradeTableBlockEntity::new, ModBlocks.UPGRADE_TABLE.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HollowBarrelBlockEntity>> HOLLOW_BARREL = BLOCK_ENTITIES.register(
            "hollow_barrel",
            () -> BlockEntityType.Builder.of(HollowBarrelBlockEntity::new, HollowWoodBlocks.HOLLOW_BARREL.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FeederBlockEntity>> FEEDER = BLOCK_ENTITIES.register(
            "feeder",
            () -> BlockEntityType.Builder.of(FeederBlockEntity::new, ModBlocks.FEEDER.get()).build(null));

    private ModBlockEntityTypes() {
    }
}
