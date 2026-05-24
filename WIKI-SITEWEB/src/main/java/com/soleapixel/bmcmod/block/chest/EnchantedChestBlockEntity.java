package com.soleapixel.bmcmod.block.chest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.soleapixel.bmcmod.item.SealedExperienceBottleItem;
import com.soleapixel.bmcmod.network.EnchantedChestPackets;
import com.soleapixel.bmcmod.registry.ModBlockEntityTypes;
import com.soleapixel.bmcmod.registry.ModBlocks;
import com.soleapixel.bmcmod.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.ChestBlock;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Stockage d’XP en <strong>dixièmes</strong> (10 = 1,0 point d’exp). Génération limitée, anti-abus par type d’item.
 */
public class EnchantedChestBlockEntity extends ChestBlockEntity {
    private static final String TAG_XP_TENTHS = "BmcModXpTenths";
    private static final String TAG_STORED_XP_LEGACY = "StoredXp";
    private static final String TAG_LAST_TENTHS = "BmcModLastTenths";
    /** 0.21 : liste d’items (handler) — migré vers compteur. */
    private static final String TAG_UPGRADES_LEGACY = "BmcModUpgrades";
    private static final String TAG_UPGRADE_COUNT = "BmcModUpgradeCount";
    private static final int DIMINISH_AFTER_ITEMS = 3 * 64;
    private static final double DIMINISH_PER_EXTRA = 0.995D;
    private static final int TICKS_PER_GAIN = 200; // 10 s
    /**
     * Facteur global de génération (0,1 par 0,1) + gros diviseur : beaucoup moins d’XP qu’avant.
     * {@code GAIN_DIV} est rehaussé pour compenser l’échelle de « poids » plus riche (minerais, lingots, armures).
     */
    private static final double GAIN_GLOBAL_SCALE = 0.12D;
    private static final double GAIN_DIV = 180.0D;

    /**
     * Gain d’XP sur la période (10 s) : ~+32 % par amélioration (0–3) pour un écart clair in-game
     * (1,0 → 1,32 → 1,64 → 1,96).
     */
    public static double getXpRewardMultiplierForUpgradeCount(int upgradeLevel) {
        int n = Mth.clamp(upgradeLevel, 0, 3);
        return 1.0D + 0.32D * n;
    }

    /** 0–3, stocké côté cœur. */
    private byte upgradeCount;
    /** Miroir client (sync getUpdateTag). */
    private byte clientUpgradeCount;

    /** Côté cœur : 10 = 1,0 XP */
    private long storedTenths;
    private int tickAccum;
    private long lastPeriodTenths;

    /** Côté client (mirroir) */
    private long clientStoredTenths;
    private long clientLastPeriodTenths;

    public EnchantedChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.ENCHANTED_CHEST.get(), pos, state);
    }

    @Override
    protected Component getDefaultName() {
        BlockState st = this.getBlockState();
        if (!st.is(ModBlocks.ENCHANTED_CHEST.get()) || st.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return Component.translatable("container.bmcmod.enchanted_chest");
        }
        return Component.translatable("container.bmcmod.enchanted_chest_double");
    }

    public void applyClientXpSync(long stored, long last, int upgradeCount) {
        if (this.level == null || !this.level.isClientSide()) {
            return;
        }
        this.clientStoredTenths = stored;
        this.clientLastPeriodTenths = last;
        getCore().orElse(this).clientUpgradeCount = (byte) Mth.clamp(upgradeCount, 0, 3);
    }

    public long getClientStoredTenths() {
        return getCore().map(EnchantedChestBlockEntity::getLocalClientTenths).orElse(0L);
    }

    private long getLocalClientTenths() {
        return this.clientStoredTenths;
    }

    public long getClientLastPeriodTenths() {
        return getCore().map(EnchantedChestBlockEntity::getLocalClientLastTenths).orElse(0L);
    }

    private long getLocalClientLastTenths() {
        return this.clientLastPeriodTenths;
    }

    public long getStoredTenthsInternal() {
        return this.storedTenths;
    }

    public void tickEnchanted() {
        if (this.level == null || this.level.isClientSide() || !(this.level instanceof ServerLevel) || isRemoved()) {
            return;
        }
        EnchantedChestBlockEntity core = getCore().orElse(this);
        if (core != this) {
            return;
        }
        this.tickAccum++;
        if (this.tickAccum < TICKS_PER_GAIN) {
            return;
        }
        this.tickAccum = 0;
        BlockState st = this.getBlockState();
        if (!st.is(ModBlocks.ENCHANTED_CHEST.get())) {
            return;
        }
        var chest = (ChestBlock) ModBlocks.ENCHANTED_CHEST.get();
        Container inv = ChestBlock.getContainer(chest, st, this.level, this.worldPosition, false);
        if (inv == null) {
            return;
        }
        if (isEmptyContainer(inv)) {
            if (this.lastPeriodTenths != 0) {
                this.lastPeriodTenths = 0L;
                this.setChanged();
                this.syncXPHud();
            }
            return;
        }
        long value = computeValueScoreWithDiminishing(inv);
        int used = countNonEmptySlots(inv);
        int size = inv.getContainerSize();
        if (size <= 0) {
            return;
        }
        double raw = (value * (double) used) / (size * GAIN_DIV) * GAIN_GLOBAL_SCALE;
        int up = this.getActiveUpgradeCount();
        double mult = getXpRewardMultiplierForUpgradeCount(up);
        long fromRaw = (long) Math.floor(raw * 10.0 * mult);
        long bonusTenths = (value > 0L && up > 0) ? up : 0L; // 0,1,2,3 dixièmes/10s selon le stade, si le contenu a une « valeur »
        long gainTenths = fromRaw + bonusTenths;
        if (gainTenths < 0) {
            gainTenths = 0;
        }
        this.storedTenths = Mth.clamp(this.storedTenths + gainTenths, 0L, Long.MAX_VALUE);
        this.lastPeriodTenths = gainTenths;
        this.setChanged();
        this.syncXPHud();
    }

    public void syncXPHud() {
        if (this.level == null) {
            return;
        }
        BlockState st = this.getBlockState();
        this.level.sendBlockUpdated(this.getBlockPos(), st, st, 3);
        if (st.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos other = this.worldPosition.relative(ChestBlock.getConnectedDirection(st));
            BlockState st2 = this.level.getBlockState(other);
            this.level.sendBlockUpdated(other, st2, st2, 3);
        }
        if (this.level instanceof ServerLevel sl) {
            EnchantedChestBlockEntity c = getCore().orElse(this);
            long stT = c.storedTenths;
            long lT = c.lastPeriodTenths;
            BlockPos p = c.getBlockPos();
            PacketDistributor.sendToPlayersTrackingChunk(
                    sl,
                    new ChunkPos(p),
                    new EnchantedChestPackets.XpStatePayload(p.getX(), p.getY(), p.getZ(), stT, lT, c.upgradeCount));
        }
    }

    private static boolean isEmptyContainer(Container c) {
        for (int i = 0; i < c.getContainerSize(); i++) {
            if (!c.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static int countNonEmptySlots(Container c) {
        int n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            if (!c.getItem(i).isEmpty()) {
                n++;
            }
        }
        return n;
    }

    private static long computeValueScoreWithDiminishing(Container inv) {
        Map<Item, int[]> m = new HashMap<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            Item it = s.getItem();
            int[] t = m.computeIfAbsent(it, k -> new int[2]);
            t[0] = Mth.clamp(t[0] + s.getCount(), 0, Integer.MAX_VALUE);
            long w = valuePerItem(s);
            t[1] = (int) Math.max(t[1], w);
        }
        long sum = 0L;
        for (int[] t : m.values()) {
            int c = t[0];
            long w = t[1];
            sum += diminishedValueForType(w, c);
        }
        return sum;
    }

    private static long diminishedValueForType(long baseW, int count) {
        if (count <= 0) {
            return 0L;
        }
        int full = Math.min(DIMINISH_AFTER_ITEMS, count);
        long head = full * baseW;
        int excess = count - DIMINISH_AFTER_ITEMS;
        if (excess <= 0) {
            return head;
        }
        return head + sumGeometricMarges(baseW, excess);
    }

    private static long sumGeometricMarges(long baseW, int excess) {
        if (excess <= 0) {
            return 0L;
        }
        double r = DIMINISH_PER_EXTRA;
        if (r >= 1.0) {
            return (long) excess * baseW;
        }
        double s = (baseW * r * (1.0 - Math.pow(r, excess))) / (1.0 - r);
        return Math.max(0L, (long) s);
    }

    /**
     * Poids par item pour le score (puis {@link #diminishedValueForType} par type, anti-spam de stacks).
     * Armure : 1 = 0,1 … 7 = 0,7 (meilleur vanilla), Enderite mod 8. Minerais / lingots : échelle plus fine.
     */
    private static long valuePerItem(ItemStack stack) {
        if (stack.getItem() instanceof ArmorItem) {
            return armorValuePerPiece(stack);
        }
        if (isZeroValueTrashBlock(stack)) {
            return 0L;
        }
        if (stack.is(ModItems.ENDERITE_BLOCK_ITEM.get())) {
            return 72L;
        }
        if (stack.is(ModItems.ENDERITE_INGOT.get())) {
            return 8L;
        }
        if (stack.is(ModItems.ENDERITE_SCRAP.get())) {
            return 3L;
        }
        if (stack.is(ModItems.ENDERITE_UPGRADE_SMITHING_TEMPLATE.get())) {
            return 3L;
        }
        if (stack.is(Items.NETHERITE_BLOCK)) {
            return 54L;
        }
        if (stack.is(Items.NETHERITE_INGOT)) {
            return 6L;
        }
        if (stack.is(Items.NETHERITE_SCRAP)) {
            return 2L;
        }
        if (stack.is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)) {
            return 2L;
        }
        if (stack.is(ModItems.FORGOTTEN_DEBRIS_ITEM.get())) {
            return 4L;
        }
        if (stack.is(ModItems.NEBRITH_SHARD.get()) || stack.is(ModItems.NEBRITH_CLUSTER_ITEM.get())) {
            return 2L;
        }
        if (stack.is(ModItems.RUBY.get())) {
            return 4L;
        }
        if (stack.is(ModItems.RUBY_ORE_ITEM.get()) || stack.is(ModItems.DEEPSLATE_RUBY_ORE_ITEM.get())) {
            return 5L;
        }
        if (stack.is(ModItems.RUBY_BLOCK_ITEM.get())) {
            return 36L;
        }
        if (stack.is(Items.DIAMOND_BLOCK)) {
            return 36L;
        }
        if (stack.is(Items.DIAMOND)) {
            return 4L;
        }
        if (stack.is(Items.EMERALD_BLOCK)) {
            return 27L;
        }
        if (stack.is(Items.EMERALD)) {
            return 2L;
        }
        if (stack.is(Items.GOLD_BLOCK)) {
            return 27L;
        }
        if (stack.is(Items.GOLD_INGOT)) {
            return 3L;
        }
        if (stack.is(Items.RAW_GOLD) || stack.is(Items.RAW_GOLD_BLOCK)) {
            return 2L;
        }
        if (stack.is(Items.IRON_BLOCK)) {
            return 18L;
        }
        if (stack.is(Items.IRON_INGOT)) {
            return 2L;
        }
        if (stack.is(Items.RAW_IRON) || stack.is(Items.RAW_IRON_BLOCK)) {
            return 2L;
        }
        if (stack.is(Items.COPPER_BLOCK)) {
            return 18L;
        }
        if (stack.is(Items.COPPER_INGOT)) {
            return 2L;
        }
        if (stack.is(Items.RAW_COPPER) || stack.is(Items.RAW_COPPER_BLOCK)) {
            return 1L;
        }
        if (stack.is(Items.LAPIS_BLOCK)) {
            return 18L;
        }
        if (stack.is(Items.LAPIS_LAZULI)) {
            return 2L;
        }
        if (stack.is(Items.REDSTONE_BLOCK)) {
            return 18L;
        }
        if (stack.is(Items.REDSTONE)) {
            return 2L;
        }
        if (stack.is(Items.COAL_BLOCK)) {
            return 9L;
        }
        if (stack.is(Items.COAL)) {
            return 1L;
        }
        if (stack.is(Items.AMETHYST_SHARD)) {
            return 1L;
        }
        if (stack.is(Items.AMETHYST_BLOCK)) {
            return 4L;
        }
        if (stack.is(Items.PRISMARINE_SHARD)) {
            return 1L;
        }
        if (stack.is(Items.PRISMARINE_CRYSTALS)) {
            return 2L;
        }
        if (stack.is(Items.PRISMARINE) || stack.is(Items.PRISMARINE_BRICKS) || stack.is(Items.DARK_PRISMARINE)) {
            return 2L;
        }
        if (stack.is(Items.QUARTZ) || stack.is(Items.NETHER_QUARTZ_ORE)) {
            return 2L;
        }
        if (stack.is(Items.QUARTZ_BLOCK) || stack.is(Items.SMOOTH_QUARTZ) || stack.is(Items.CHISELED_QUARTZ_BLOCK) || stack.is(Items.QUARTZ_BRICKS)) {
            return 8L;
        }
        if (stack.is(Items.ECHO_SHARD)) {
            return 3L;
        }
        if (stack.is(Items.IRON_NUGGET) || stack.is(Items.GOLD_NUGGET)) {
            return 1L;
        }
        if (stack.is(ItemTags.COAL_ORES)) {
            return 1L;
        }
        if (stack.is(ItemTags.IRON_ORES) || stack.is(ItemTags.COPPER_ORES)) {
            return 2L;
        }
        if (stack.is(ItemTags.DIAMOND_ORES)) {
            return 5L;
        }
        if (stack.is(ItemTags.EMERALD_ORES)) {
            return 4L;
        }
        if (stack.is(ItemTags.GOLD_ORES) || stack.is(Items.NETHER_GOLD_ORE)) {
            return 3L;
        }
        if (stack.is(ItemTags.LAPIS_ORES) || stack.is(ItemTags.REDSTONE_ORES)) {
            return 2L;
        }
        if (stack.is(Items.ANCIENT_DEBRIS)) {
            return 6L;
        }
        if (stack.is(Items.LEATHER_HORSE_ARMOR)) {
            return 1L;
        }
        if (stack.is(Items.IRON_HORSE_ARMOR)) {
            return 2L;
        }
        if (stack.is(Items.GOLDEN_HORSE_ARMOR)) {
            return 3L;
        }
        if (stack.is(Items.DIAMOND_HORSE_ARMOR)) {
            return 4L;
        }
        if (isAnyItem(
                stack,
                Items.ARMADILLO_SCUTE,
                Items.TURTLE_SCUTE,
                Items.SHULKER_SHELL,
                Items.HEART_OF_THE_SEA,
                Items.DRAGON_BREATH,
                Items.NAUTILUS_SHELL,
                Items.POPPED_CHORUS_FRUIT,
                Items.CHORUS_FRUIT,
                Items.PHANTOM_MEMBRANE)) {
            return 1L;
        }
        if (isAnyItem(stack, Items.ENDER_PEARL, Items.ENDER_EYE)) {
            return 1L;
        }
        if (isAnyItem(stack, Items.BLAZE_ROD, Items.GHAST_TEAR, Items.MAGMA_CREAM)) {
            return 2L;
        }
        return 0L;
    }

    /**
     * Armure (par pièce) : 0,1 = 1 … 0,7 = 7 (le meilleur vanilla) ; 8 = Enderite (mod) ; chapeaux villageois 1.
     * Maille 2, cuivre 3, fer/obsidienne 4, or 5, diamant/shulker/émeraude 6, netherite 7, cuir/tortue 1.
     */
    private static long armorValuePerPiece(ItemStack stack) {
        if (isModEnderiteArmorPiece(stack)) {
            return 8L;
        }
        if (isAnyItem(stack, Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS)) {
            return 7L;
        }
        if (isModShulkerOrEmeraldArmor(stack) || isAnyItem(
                stack, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS)) {
            return 6L;
        }
        if (isAnyItem(stack, Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS)) {
            return 5L;
        }
        if (isModObsidianArmorPiece(stack) || isAnyItem(stack, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS)) {
            return 4L;
        }
        if (isModCopperArmorPiece(stack)) {
            return 3L;
        }
        if (isChainmailPiece(stack)) {
            return 2L;
        }
        if (isModVillagerHatOrWitch(stack) || stack.is(Items.TURTLE_HELMET) || isAnyItem(
                stack, Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS)) {
            return 1L;
        }
        return 1L;
    }

    private static boolean isModEnderiteArmorPiece(ItemStack s) {
        return s.is(ModItems.ENDERITE_HELMET.get()) || s.is(ModItems.ENDERITE_CHESTPLATE.get()) || s.is(ModItems.ENDERITE_LEGGINGS.get()) || s.is(ModItems.ENDERITE_BOOTS.get());
    }

    private static boolean isModShulkerOrEmeraldArmor(ItemStack s) {
        return s.is(ModItems.EMERALD_HELMET.get()) || s.is(ModItems.EMERALD_CHESTPLATE.get()) || s.is(ModItems.EMERALD_LEGGINGS.get()) || s.is(ModItems.EMERALD_BOOTS.get()) || s.is(ModItems.SHULKER_HELMET.get()) || s.is(ModItems.SHULKER_CHESTPLATE.get()) || s.is(ModItems.SHULKER_LEGGINGS.get()) || s.is(ModItems.SHULKER_BOOTS.get());
    }

    private static boolean isModCopperArmorPiece(ItemStack s) {
        return s.is(ModItems.COPPER_HELMET.get()) || s.is(ModItems.COPPER_CHESTPLATE.get()) || s.is(ModItems.COPPER_LEGGINGS.get()) || s.is(ModItems.COPPER_BOOTS.get());
    }

    private static boolean isModObsidianArmorPiece(ItemStack s) {
        return s.is(ModItems.OBSIDIAN_HELMET.get()) || s.is(ModItems.OBSIDIAN_CHESTPLATE.get()) || s.is(ModItems.OBSIDIAN_LEGGINGS.get()) || s.is(ModItems.OBSIDIAN_BOOTS.get());
    }

    private static boolean isModVillagerHatOrWitch(ItemStack s) {
        return s.is(ModItems.VILLAGER_HAT_BUTCHER.get()) || s.is(ModItems.VILLAGER_HAT_LIBRARIAN.get()) || s.is(ModItems.VILLAGER_HAT_WEAPONSMITH.get()) || s.is(ModItems.VILLAGER_HAT_SHEPHERD.get()) || s.is(ModItems.VILLAGER_HAT_FISHERMAN.get()) || s.is(ModItems.VILLAGER_HAT_CARTOGRAPHER.get()) || s.is(ModItems.VILLAGER_HAT_ARMORER.get()) || s.is(ModItems.VILLAGER_HAT_FARMER.get()) || s.is(ModItems.VILLAGER_HAT_WITCH.get());
    }

    private static boolean isChainmailPiece(ItemStack s) {
        return isAnyItem(
                s,
                Items.CHAINMAIL_HELMET,
                Items.CHAINMAIL_CHESTPLATE,
                Items.CHAINMAIL_LEGGINGS,
                Items.CHAINMAIL_BOOTS);
    }

    private static boolean isZeroValueTrashBlock(ItemStack stack) {
        return stack.is(Items.COBBLESTONE) || stack.is(Items.DIRT) || stack.is(Items.NETHERRACK) || stack.is(Items.GRASS_BLOCK) || stack.is(Items.SAND) || stack.is(Items.GRAVEL) || stack.is(Items.DEEPSLATE);
    }

    private static boolean isAnyItem(ItemStack stack, Item... items) {
        for (Item it : items) {
            if (stack.is(it)) {
                return true;
            }
        }
        return false;
    }

    int getActiveUpgradeCount() {
        return Mth.clamp((int) getCore().orElse(this).upgradeCount, 0, 3);
    }

    public int getVisibleUpgradeCount() {
        if (this.level != null && this.level.isClientSide()) {
            return Mth.clamp((int) this.clientUpgradeCount, 0, 3);
        }
        return getActiveUpgradeCount();
    }

    /**
     * Côté serveur uniquement : +1 amélioration, consomme 1 item de la main (sauf créatif).
     */
    public boolean tryApplyUpgradeFromItem(Player player, ItemStack stack, InteractionHand hand) {
        if (this.level == null || this.level.isClientSide() || !stack.is(ModItems.ENCHANTED_CHEST_UPGRADE.get())) {
            return false;
        }
        EnchantedChestBlockEntity c = getCore().orElse(this);
        if (c.upgradeCount >= 3) {
            return false;
        }
        c.upgradeCount = (byte) (c.upgradeCount + 1);
        c.setChanged();
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        this.level.playSound(
                null, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5,
                SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.35F, 1.15F + 0.08F * c.upgradeCount);
        c.syncXPHud();
        return true;
    }

    /**
     * Côté serveur : retire 1 amélioration, fait tomber 1 item ; usure 1 le cisaillement / la hache.
     */
    public boolean tryRemoveOneUpgradeWithTool(Player player, ItemStack tool, InteractionHand hand) {
        if (this.level == null || this.level.isClientSide() || !isToolForRemovalUpgrade(tool)) {
            return false;
        }
        EnchantedChestBlockEntity c = getCore().orElse(this);
        if (c.upgradeCount <= 0) {
            return false;
        }
        c.upgradeCount = (byte) (c.upgradeCount - 1);
        c.setChanged();
        Block.popResource(
                this.level, this.worldPosition, new ItemStack(ModItems.ENCHANTED_CHEST_UPGRADE.get(), 1));
        this.level.playSound(
                null, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5,
                tool.is(Items.SHEARS) ? SoundEvents.SHEEP_SHEAR : SoundEvents.AXE_STRIP,
                SoundSource.PLAYERS, 0.4F, tool.is(Items.SHEARS) ? 1.1F : 0.9F);
        if (player instanceof ServerPlayer sp) {
            if (tool.isDamageableItem() && !sp.getAbilities().instabuild) {
                EquipmentSlot es = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                tool.hurtAndBreak(1, sp, es);
            }
        }
        c.syncXPHud();
        return true;
    }

    public static boolean isToolForRemovalUpgrade(ItemStack tool) {
        if (tool.isEmpty()) {
            return false;
        }
        if (tool.is(Items.SHEARS)) {
            return true;
        }
        return tool.getItem() instanceof AxeItem;
    }

    public void dropUpgrades() {
        Level l = this.level;
        if (l == null || l.isClientSide()) {
            return;
        }
        BlockPos p = this.worldPosition;
        EnchantedChestBlockEntity c = getCore().orElse(this);
        for (int i = 0; i < c.upgradeCount; i++) {
            Block.popResource(l, p, new ItemStack(ModItems.ENCHANTED_CHEST_UPGRADE.get()));
        }
        c.upgradeCount = 0;
        c.setChanged();
    }

    public Optional<EnchantedChestBlockEntity> getCore() {
        if (this.level == null) {
            return Optional.of(this);
        }
        BlockState st = this.getBlockState();
        if (!st.is(ModBlocks.ENCHANTED_CHEST.get()) || st.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return Optional.of(this);
        }
        BlockPos o = this.worldPosition.relative(ChestBlock.getConnectedDirection(st));
        @Nullable
        var oth = this.level.getBlockEntity(o);
        if (oth instanceof EnchantedChestBlockEntity b) {
            if (this.worldPosition.compareTo(o) <= 0) {
                return Optional.of(this);
            }
            return Optional.of(b);
        }
        return Optional.of(this);
    }

    private static boolean isCorePosition(BlockPos p, BlockState st) {
        if (!st.is(ModBlocks.ENCHANTED_CHEST.get()) || st.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return true;
        }
        BlockPos other = p.relative(ChestBlock.getConnectedDirection(st));
        return p.compareTo(other) <= 0;
    }

    public boolean tryBottleStorage(Player player, ItemStack handStack, InteractionHand hand) {
        if (this.level == null || this.level.isClientSide() || !handStack.is(Items.GLASS_BOTTLE) || !player.isShiftKeyDown()) {
            return false;
        }
        EnchantedChestBlockEntity core = getCore().orElse(this);
        if (core.storedTenths < 10) {
            return false;
        }
        int wholeXp = (int) (core.storedTenths / 10L);
        if (wholeXp < 1) {
            return false;
        }
        core.storedTenths -= (long) wholeXp * 10L;
        if (!player.getAbilities().instabuild) {
            handStack.shrink(1);
        }
        ItemStack out = SealedExperienceBottleItem.newStackWithXp(wholeXp);
        if (!player.getInventory().add(out)) {
            player.drop(out, false);
        }
        core.setChanged();
        this.level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.4F, 0.7F);
        if (this.level instanceof ServerLevel) {
            core.syncXPHud();
        }
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (isCorePosition(this.worldPosition, this.getBlockState())) {
            tag.putLong(TAG_XP_TENTHS, this.storedTenths);
            tag.putByte(TAG_UPGRADE_COUNT, this.upgradeCount);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_XP_TENTHS, Tag.TAG_LONG)) {
            this.storedTenths = tag.getLong(TAG_XP_TENTHS);
        } else if (tag.contains(TAG_STORED_XP_LEGACY, Tag.TAG_LONG)) {
            this.storedTenths = tag.getLong(TAG_STORED_XP_LEGACY) * 10L;
        } else {
            this.storedTenths = 0L;
        }
        this.clientStoredTenths = this.storedTenths;
        this.clientLastPeriodTenths = 0L;
        if (isCorePosition(this.worldPosition, this.getBlockState())) {
            if (tag.contains(TAG_UPGRADE_COUNT, Tag.TAG_BYTE)) {
                this.upgradeCount = (byte) Mth.clamp(tag.getByte(TAG_UPGRADE_COUNT), 0, 3);
            } else if (tag.contains(TAG_UPGRADES_LEGACY, Tag.TAG_COMPOUND)) {
                this.upgradeCount = 0;
                var tmp = new ItemStackHandler(3);
                tmp.deserializeNBT(registries, tag.getCompound(TAG_UPGRADES_LEGACY));
                for (int s = 0; s < 3; s++) {
                    ItemStack is = tmp.getStackInSlot(s);
                    if (!is.isEmpty() && is.is(ModItems.ENCHANTED_CHEST_UPGRADE.get())) {
                        this.upgradeCount++;
                    }
                }
                this.upgradeCount = (byte) Mth.clamp(this.upgradeCount, 0, 3);
            } else {
                this.upgradeCount = 0;
            }
            this.clientUpgradeCount = this.upgradeCount;
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (tag.contains(TAG_XP_TENTHS, Tag.TAG_LONG)) {
            this.clientStoredTenths = tag.getLong(TAG_XP_TENTHS);
        } else if (tag.contains(TAG_STORED_XP_LEGACY, Tag.TAG_LONG)) {
            this.clientStoredTenths = tag.getLong(TAG_STORED_XP_LEGACY) * 10L;
        } else {
            this.clientStoredTenths = 0L;
        }
        if (tag.contains(TAG_LAST_TENTHS, Tag.TAG_LONG)) {
            this.clientLastPeriodTenths = tag.getLong(TAG_LAST_TENTHS);
        } else {
            this.clientLastPeriodTenths = 0L;
        }
        if (tag.contains(TAG_UPGRADE_COUNT, Tag.TAG_BYTE)) {
            getCore().orElse(this).clientUpgradeCount = (byte) Mth.clamp(tag.getByte(TAG_UPGRADE_COUNT), 0, 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        EnchantedChestBlockEntity c = getCore().orElse(this);
        tag.putLong(TAG_XP_TENTHS, c.storedTenths);
        tag.putLong(TAG_LAST_TENTHS, c.lastPeriodTenths);
        tag.putByte(TAG_UPGRADE_COUNT, c.upgradeCount);
        return tag;
    }
}
