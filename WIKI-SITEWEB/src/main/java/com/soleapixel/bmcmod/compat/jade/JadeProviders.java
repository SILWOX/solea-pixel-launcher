package com.soleapixel.bmcmod.compat.jade;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.block.endstonefurnace.EndstoneFurnaceBlock;
import com.soleapixel.bmcmod.block.foundry.FoundryBlock;
import com.soleapixel.bmcmod.block.foundry.FoundryBlockEntity;
import com.soleapixel.bmcmod.block.infusion.InfusionTableBlock;
import com.soleapixel.bmcmod.block.upgradetable.UpgradeTableBlock;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElementHelper;

@SuppressWarnings("unchecked")
public final class JadeProviders {
    private JadeProviders() {
    }

    public static void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(InfusionTableProvider.INSTANCE, InfusionTableBlock.class);
        registration.registerBlockComponent(EndstoneFurnaceProvider.INSTANCE, EndstoneFurnaceBlock.class);
        registration.registerBlockComponent(FoundryProvider.INSTANCE, FoundryBlock.class);
        registration.registerBlockComponent(UpgradeTableProvider.INSTANCE, UpgradeTableBlock.class);
    }

    public static void registerCommon(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(FoundryProvider.INSTANCE, FoundryBlockEntity.class);
    }

    private enum InfusionTableProvider implements IComponentProvider {
        INSTANCE;

        @Override
        public ResourceLocation getUid() {
            return BmcMod.loc(JadeConstants.UID_INFUSION_TABLE);
        }

        @Override
        public void appendTooltip(ITooltip tooltip, snownee.jade.api.Accessor accessor, IPluginConfig config) {
            tooltip.add(Component.translatable("jade.bmcmod.infusion_table"));
        }
    }

    private enum EndstoneFurnaceProvider implements IComponentProvider {
        INSTANCE;

        @Override
        public ResourceLocation getUid() {
            return BmcMod.loc(JadeConstants.UID_ENDSTONE_FURNACE);
        }

        @Override
        public void appendTooltip(ITooltip tooltip, snownee.jade.api.Accessor accessor, IPluginConfig config) {
            tooltip.add(Component.translatable("jade.bmcmod.endstone_furnace"));
        }
    }

    /**
     * Même présentation que le four vanilla dans Jade ({@code FurnaceProvider}) : entrée, combustible, barre,
     * sortie — via {@link StreamServerDataProvider} et les icônes + progression.
     */
    private enum FoundryProvider implements IBlockComponentProvider, StreamServerDataProvider<BlockAccessor, FoundryProvider.Data> {
        INSTANCE;

        @Override
        public ResourceLocation getUid() {
            return BmcMod.loc(JadeConstants.UID_FOUNDRY);
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            tooltip.add(Component.translatable("jade.bmcmod.foundry.title"));
            Data data = decodeFromData(accessor).orElse(null);
            if (data == null) {
                return;
            }
            IElementHelper helper = IElementHelper.get();
            List<ItemStack> inv = data.inventory();
            tooltip.add(helper.item(inv.get(0)));
            tooltip.append(helper.item(inv.get(1)));
            tooltip.append(helper.spacer(4, 0));
            float prog = data.total() > 0 ? Mth.clamp((float) data.progress() / data.total(), 0F, 1F) : 0F;
            tooltip.append(helper.progress(prog).translate(new Vec2(-2, 0)));
            tooltip.append(helper.item(inv.get(2)));
        }

        @Override
        public Data streamData(BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof FoundryBlockEntity be)) {
                return null;
            }
            var items = be.getItems();
            return new Data(
                    be.getCookTime(),
                    be.getCookTotalTime(),
                    List.of(
                            items.getStackInSlot(FoundryBlockEntity.SLOT_INPUT).copy(),
                            items.getStackInSlot(FoundryBlockEntity.SLOT_FUEL).copy(),
                            items.getStackInSlot(FoundryBlockEntity.SLOT_OUTPUT).copy()));
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Data> streamCodec() {
            return Data.STREAM_CODEC;
        }

        public record Data(int progress, int total, List<ItemStack> inventory) {
            public static final StreamCodec<RegistryFriendlyByteBuf, Data> STREAM_CODEC = StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    Data::progress,
                    ByteBufCodecs.VAR_INT,
                    Data::total,
                    ItemStack.OPTIONAL_LIST_STREAM_CODEC,
                    Data::inventory,
                    Data::new);
        }
    }

    private enum UpgradeTableProvider implements IComponentProvider {
        INSTANCE;

        @Override
        public ResourceLocation getUid() {
            return BmcMod.loc(JadeConstants.UID_UPGRADE_TABLE);
        }

        @Override
        public void appendTooltip(ITooltip tooltip, snownee.jade.api.Accessor accessor, IPluginConfig config) {
            tooltip.add(Component.translatable("jade.bmcmod.upgrade_table"));
        }
    }
}
