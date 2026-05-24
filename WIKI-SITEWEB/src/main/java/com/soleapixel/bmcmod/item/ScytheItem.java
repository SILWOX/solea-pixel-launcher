package com.soleapixel.bmcmod.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

import com.soleapixel.bmcmod.emerald.EmeraldEquipment;
import com.soleapixel.bmcmod.equipment.BorealToolTier;
import com.soleapixel.bmcmod.equipment.EnderiteToolTier;
import com.soleapixel.bmcmod.registry.ModEnchantmentKeys;

import net.minecraft.tags.BlockTags;

/**
 * Faux : pelle au niveau dégâts / cassable ; clic droit pour labourer et récolter en zone N×N (axes monde).
 */
public final class ScytheItem extends DiggerItem {

    public static final String MODE_SIZE_TAG = "ScytheAreaSize";
    /** NBT joueur (serveur) : maintien charge tourbillon — pas de labour/clic bloc pendant ce temps. */
    public static final String WHIRLWIND_CHARGING_PDC_KEY = "bmcmod_scythe_whirlwind_charging";
    /** ticks — 0,5 s entre deux usages réussis */
    public static final int USE_COOLDOWN_TICKS = 10;

    /** Maintien clic droit pour armer le tourbillon (~2,4 s à 20 t/s). */
    public static final int SWEEP_ARM_HOLD_TICKS = 48;
    /** Cooldown serveur après un tourbillon. */
    public static final int SWEEP_COOLDOWN_TICKS = 50;
    public static final String SWEEP_COOLDOWN_GAME_TIME_TAG = "bmcmod_scythe_sweep_game_time";
    /** Portée du cône devant le joueur (blocs). */
    public static final float SWEEP_RANGE = 5.0F;
    /** cos(angle/2) pour un cône ~100° total (tolérant vs client/serveur). */
    public static final double SWEEP_MIN_DOT = Math.cos(Math.toRadians(50.0));
    /** Durabilité de base consommée par un tourbillon (+ bonus par cible touchée). */
    public static final int SWEEP_DURABILITY_BASE = 3;
    public static final int SWEEP_DURABILITY_PER_TARGET = 1;
    public static final int SWEEP_MAX_EXTRA_TARGETS = 6;

    /** Animation item main / spin tiers joueur (ticks). */
    public static final int SWEEP_VISUAL_SPIN_TICKS = 14;
    public static final int SWEEP_VISUAL_FP_TICKS = 10;
    /** Avec Tourbillon : 2 rotations visuelles et ×2 durabilité sur le coup spécial. */
    public static final int WHIRLWIND_ROTATION_MULTIPLIER = 2;
    /** Tornades horizontales lancées par le tourbillon enchanté. */
    public static final int TORNADO_COUNT = 12;
    public static final float TORNADO_SHOOT_SPEED = 0.42F;
    /** Blocs ajoutés au rayon du tourbillon par niveau de Grand balayage (serveur + visée client). */
    private static final float WIDE_SWEEP_RANGE_PER_LEVEL = 1.15F;

    /** « Stun » : lenteur + fatigue des mines (cible visée plus longtemps). */
    public static final int SWEEP_STUN_PRIMARY_TICKS = 45;
    public static final int SWEEP_STUN_AOE_TICKS = 30;
    public static final int SWEEP_STUN_SLOWNESS_AMPLIFIER = 5;
    public static final int SWEEP_STUN_FATIGUE_AMPLIFIER = 2;

    private final ScytheTier tier;

    public ScytheItem(ScytheTier tier, net.minecraft.world.item.Item.Properties properties) {
        super(tier.toolTier(), BlockTags.MINEABLE_WITH_SHOVEL, properties);
        this.tier = tier;
    }

    public ScytheTier scytheTier() {
        return tier;
    }

    /** {@link ItemStack#getEnchantmentValue()} — sinon enchantabilité 0 et prévisualisations table bloquées. */
    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return tier.toolTier().getEnchantmentValue();
    }

    /**
     * La faux n’est pas une pelle : la terre, l’herbe, la terre labourée et le foin ne se cassent pas comme avec un outil « excavation ».
     */
    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        if (slowExcavationLikeVanillaWrongTool(state)) {
            return false;
        }
        return super.isCorrectToolForDrops(stack, state);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (slowExcavationLikeVanillaWrongTool(state)) {
            return 1.0F;
        }
        return super.getDestroySpeed(stack, state);
    }

    private static boolean slowExcavationLikeVanillaWrongTool(BlockState state) {
        if (state.is(Blocks.HAY_BLOCK)) {
            return true;
        }
        if (state.is(Blocks.FARMLAND)) {
            return true;
        }
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT_PATH)) {
            return true;
        }
        return state.is(BlockTags.DIRT);
    }

    public static int getModeSize(ItemStack stack) {
        int v = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(MODE_SIZE_TAG);
        return v >= 2 && v <= 8 ? v : -1;
    }

    public static void setModeSize(ItemStack stack, int size) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        var tag = data.copyTag();
        tag.putInt(MODE_SIZE_TAG, size);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** Cycle 2 → … → max → 2 pour ce palier (sans tag : même comportement que taille 2). */
    public static void cycleMode(ItemStack stack, ScytheTier tier) {
        int max = tier.maxAreaSize();
        int cur = getModeSize(stack);
        if (cur < 2 || cur > max) {
            cur = 2;
        }
        int next = cur >= max ? 2 : cur + 1;
        setModeSize(stack, next);
    }

    public int effectiveSize(ItemStack stack) {
        int max = tier.maxAreaSize();
        int s = getModeSize(stack);
        if (s < 2 || s > max) {
            return 2;
        }
        return s;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer sp
                && sp.getPersistentData().getBoolean(WHIRLWIND_CHARGING_PDC_KEY)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            if (stack.getDamageValue() >= stack.getMaxDamage()) {
                return InteractionResult.PASS;
            }
            if (!player.getAbilities().instabuild && player.getCooldowns().isOnCooldown(this)) {
                return InteractionResult.PASS;
            }
            return clientScytheUseWouldSucceed(level, player, stack, context.getClickedPos())
                    ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            return InteractionResult.FAIL;
        }
        if (!sp.getAbilities().instabuild && sp.getCooldowns().isOnCooldown(this)) {
            return InteractionResult.FAIL;
        }

        BlockPos origin = context.getClickedPos();
        int size = effectiveSize(stack);
        int minOff = -(size / 2);
        int maxOff = (size - 1) / 2;

        double reach = sp.blockInteractionRange();
        double reachSq = reach * reach;
        Vec3 eye = sp.getEyePosition(1.0F);

        boolean any = false;
        for (int dz = minOff; dz <= maxOff; dz++) {
            for (int dx = minOff; dx <= maxOff; dx++) {
                BlockPos soil = origin.offset(dx, 0, dz);
                boolean inReach = eye.distanceToSqr(Vec3.atCenterOf(soil)) <= reachSq
                        || eye.distanceToSqr(Vec3.atCenterOf(soil.above())) <= reachSq;
                if (!inReach) {
                    continue;
                }
                any |= tryHarvestCrop(sl, soil.above(), sp, stack);
                any |= tryHarvestCrop(sl, soil, sp, stack);
                any |= tryTill(sl, soil, sp);
            }
        }

        if (!any) {
            return InteractionResult.PASS;
        }

        int cost = size;
        if (!sp.getAbilities().instabuild) {
            EquipmentSlot slot = context.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            stack.hurtAndBreak(cost, sp, slot);
            sp.getCooldowns().addCooldown(this, USE_COOLDOWN_TICKS);
        }
        sp.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.SUCCESS;
    }

    private static boolean tryHarvestCrop(ServerLevel level, BlockPos pos, ServerPlayer player, ItemStack tool) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (!(block instanceof CropBlock crop)) {
            return false;
        }
        if (!crop.isMaxAge(state)) {
            return false;
        }
        if (!level.destroyBlock(pos, true, player)) {
            return false;
        }
        tryReapingBonusDrop(level, pos, state, player, tool);
        return true;
    }

    /**
     * Moisson : chances élevées + plusieurs unités bonus (drops calculés comme la récolte, avec Fortune sur la faux),
     * indépendant de ce bonus mais cumulable.
     */
    private static void tryReapingBonusDrop(
            ServerLevel level, BlockPos pos, BlockState harvestedState, ServerPlayer player, ItemStack tool) {
        int lvl = ModEnchantmentKeys.enchantmentLevel(tool, level.registryAccess(), ModEnchantmentKeys.REAPING);
        if (lvl <= 0) {
            return;
        }
        var rng = level.random;
        // Chance qu'une récolte déclenche le bonus Moisson (~34 % à I, jusqu'à ~92 % à V)
        float activateChance = Math.min(0.92F, 0.22F + 0.14F * lvl);
        if (rng.nextFloat() >= activateChance) {
            return;
        }
        var drops = Block.getDrops(harvestedState, level, pos, null, player, tool);
        if (drops.isEmpty()) {
            return;
        }

        // Quantité d'unités bonus : augmente avec le niveau (esprit Fortune sur cultures)
        int bonusUnits = lvl + rng.nextInt(lvl * 2 + 2);
        bonusUnits = Mth.clamp(bonusUnits, 1, 22);

        double dx = pos.getX() + 0.5;
        double dy = pos.getY() + 0.5;
        double dz = pos.getZ() + 0.5;

        for (int i = 0; i < bonusUnits; i++) {
            ItemStack template = drops.get(rng.nextInt(drops.size())).copy();
            if (template.isEmpty()) {
                continue;
            }
            int maxStack = template.getMaxStackSize();
            int n = 1;
            if (lvl >= 2 && rng.nextFloat() < 0.22F + 0.06F * lvl) {
                n = 2;
            }
            if (lvl >= 4 && rng.nextFloat() < 0.38F) {
                n = Math.max(n, 2);
            }
            if (lvl >= 5 && rng.nextFloat() < 0.28F) {
                n = Math.max(n, Math.min(3, maxStack));
            }
            template.setCount(Mth.clamp(n, 1, maxStack));
            Containers.dropItemStack(level, dx, dy, dz, template);
        }
    }

    /**
     * Côté client : prédit si le serveur ferait au moins un labour / une récolte (même zone N×N), pour ne pas
     * jouer l’animation de faux sur la pierre, etc.
     */
    private static boolean clientScytheUseWouldSucceed(Level level, Player player, ItemStack stack, BlockPos origin) {
        double reach = player.blockInteractionRange();
        double reachSq = reach * reach;
        Vec3 eye = player.getEyePosition(1.0F);
        int size = ((ScytheItem) stack.getItem()).effectiveSize(stack);
        int minOff = -(size / 2);
        int maxOff = (size - 1) / 2;
        for (int dz = minOff; dz <= maxOff; dz++) {
            for (int dx = minOff; dx <= maxOff; dx++) {
                BlockPos soil = origin.offset(dx, 0, dz);
                boolean inReach = eye.distanceToSqr(Vec3.atCenterOf(soil)) <= reachSq
                        || eye.distanceToSqr(Vec3.atCenterOf(soil.above())) <= reachSq;
                if (!inReach) {
                    continue;
                }
                if (wouldHarvestCropPreview(level, soil.above()) || wouldHarvestCropPreview(level, soil)) {
                    return true;
                }
                if (wouldTillPreview(level, soil)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean wouldHarvestCropPreview(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (!(block instanceof CropBlock crop)) {
            return false;
        }
        return crop.isMaxAge(state);
    }

    private static boolean wouldTillPreview(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT_PATH)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.MUD)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MYCELIUM);
    }

    private static boolean tryTill(ServerLevel level, BlockPos pos, ServerPlayer player) {
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT_PATH) || state.is(Blocks.DIRT)) {
            return setFarmland(level, pos, player, state);
        }
        if (state.is(Blocks.COARSE_DIRT)) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
            playTillSound(level, pos, player, state);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, level.getBlockState(pos)));
            return true;
        }
        if (state.is(Blocks.ROOTED_DIRT)) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
            Block.popResourceFromFace(level, pos, Direction.UP, new ItemStack(net.minecraft.world.item.Items.HANGING_ROOTS));
            playTillSound(level, pos, player, state);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, level.getBlockState(pos)));
            return true;
        }
        if (state.is(Blocks.MUD)) {
            level.setBlock(pos, Blocks.PACKED_MUD.defaultBlockState(), Block.UPDATE_CLIENTS);
            playTillSound(level, pos, player, state);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, level.getBlockState(pos)));
            return true;
        }
        if (state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
            playTillSound(level, pos, player, state);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, level.getBlockState(pos)));
            return true;
        }
        return false;
    }

    private static boolean setFarmland(ServerLevel level, BlockPos pos, ServerPlayer player, BlockState oldState) {
        BlockState farmland = Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 0);
        level.setBlock(pos, farmland, Block.UPDATE_CLIENTS);
        playTillSound(level, pos, player, oldState);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, farmland));
        return true;
    }

    private static void playTillSound(ServerLevel level, BlockPos pos, Player player, BlockState oldState) {
        SoundType st = oldState.getSoundType(level, pos, player);
        level.playSound(player, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, (st.getVolume() + 1.0F) / 2.0F, st.getPitch() * 0.8F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        int n = effectiveSize(stack);
        tooltipComponents.add(Component.translatable("item.bmcmod.scythe.area_mode", n, n).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.bmcmod.scythe.toggle_hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** Palier : taille max N×N et tier outil (réparation / vitesse). */
    public enum ScytheTier {
        IRON(Tiers.IRON, 2),
        GOLD(Tiers.GOLD, 3),
        DIAMOND(Tiers.DIAMOND, 4),
        EMERALD(EmeraldEquipment.EMERALD_TIER, 5),
        NETHERITE(Tiers.NETHERITE, 6),
        ENDERITE(EnderiteToolTier.INSTANCE, 7),
        BOREAL(BorealToolTier.INSTANCE, 8);

        private final Tier toolTier;
        private final int maxAreaSize;

        ScytheTier(Tier toolTier, int maxAreaSize) {
            this.toolTier = toolTier;
            this.maxAreaSize = maxAreaSize;
        }

        public Tier toolTier() {
            return toolTier;
        }

        public int maxAreaSize() {
            return maxAreaSize;
        }
    }

    /** Dégâts de zone du tourbillon (hors cible prioritaire). */
    public static float sweepAoeDamage(ScytheTier tier) {
        return switch (tier) {
            case IRON -> 5.25F;
            case GOLD -> 6.25F;
            case DIAMOND -> 7.5F;
            case EMERALD -> 8.25F;
            case NETHERITE -> 9.25F;
            case ENDERITE -> 10.5F;
            case BOREAL -> 11.75F;
        };
    }

    /** Multiplicateur sur la cible visée (primaire). */
    public static float sweepPrimaryBonusMultiplier() {
        return 2.2F;
    }

    /** Portée du tourbillon (distance et cône), avec Grand balayage. */
    public static float sweepEffectiveRange(ItemStack stack, RegistryAccess reg) {
        int lvl = ModEnchantmentKeys.enchantmentLevel(stack, reg, ModEnchantmentKeys.WIDE_SWEEP);
        return SWEEP_RANGE + lvl * WIDE_SWEEP_RANGE_PER_LEVEL;
    }

    /**
     * 2 si Tourbillon est présent, sinon 1 — durabilité du coup spécial, dégâts du tourbillon (× tours), animations.
     */
    public static int whirlwindDurabilityMultiplier(ItemStack stack, RegistryAccess reg) {
        return ModEnchantmentKeys.enchantmentLevel(stack, reg, ModEnchantmentKeys.WHIRLWIND) > 0 ? WHIRLWIND_ROTATION_MULTIPLIER : 1;
    }

    public static int sweepVisualTotalSpinTicks(ItemStack stack, RegistryAccess reg) {
        return SWEEP_VISUAL_SPIN_TICKS * whirlwindDurabilityMultiplier(stack, reg);
    }

    public static int sweepVisualFullRotations(ItemStack stack, RegistryAccess reg) {
        return ModEnchantmentKeys.enchantmentLevel(stack, reg, ModEnchantmentKeys.WHIRLWIND) > 0 ? WHIRLWIND_ROTATION_MULTIPLIER : 1;
    }

    public static int sweepVisualFpTicks(ItemStack stack, RegistryAccess reg) {
        return SWEEP_VISUAL_FP_TICKS * whirlwindDurabilityMultiplier(stack, reg);
    }
}
