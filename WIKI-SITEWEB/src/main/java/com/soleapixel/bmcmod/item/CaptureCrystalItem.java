package com.soleapixel.bmcmod.item;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import com.soleapixel.bmcmod.capture.CaptureCrystalSoul;
import com.soleapixel.bmcmod.morph.MorphBossBlacklist;
import com.soleapixel.bmcmod.morph.MorphCrystalSoul;
import com.soleapixel.bmcmod.morph.MorphSoulSanitizer;
import com.soleapixel.bmcmod.rarity.BmcModRarity;
import com.soleapixel.bmcmod.registry.ModItems;

/**
 * Capture une entité vivante (visée 10 s sans lâcher ni perdre la visée), la relâche au clic sur un bloc (main principale).
 * Après capture : cooldown item (rechargement visible) avant de pouvoir poser l’entité — aussi en créatif.
 */
public final class CaptureCrystalItem extends Item {
    public static final int CHARGE_TICKS = 10 * 20;
    /** Annulation du canal de capture (clic trop tôt). */
    public static final int CANCEL_COOLDOWN_TICKS = 5 * 20;
    /** Après avoir reposé une entité sur un bloc : 2 s (rechargement visible sur l’item). */
    public static final int RELEASE_AFTER_PLACE_COOLDOWN_TICKS = 2 * 20;
    /**
     * Juste après une capture réussie : évite de poser tout de suite (même créatif, où vanilla ne mettait pas de cooldown).
     */
    public static final int POST_CAPTURE_PLACE_DELAY_TICKS = 5 * 20;
    public static final double REACH = 6.0;
    public static final int RELEASE_DURABILITY_COST = 40;

    private static final String CHARGE_MSB = "BmcModCapChargeMsb";
    private static final String CHARGE_LSB = "BmcModCapChargeLsb";

    public CaptureCrystalItem(Properties properties) {
        super(properties);
    }

    public BmcModRarity getBmcModRarity() {
        return BmcModRarity.EXOTIC;
    }

    @Override
    public Component getName(ItemStack stack) {
        return BmcModRarity.EXOTIC.styleItemName(Component.translatable(getDescriptionId(stack)));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide() && !stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty()) {
            stack.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (CaptureCrystalSoul.hasCaptured(stack)) {
            return InteractionResultHolder.pass(stack);
        }
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return InteractionResultHolder.fail(stack);
        }
        LivingEntity target = findCapturableLookTarget(player, REACH);
        if (target == null) {
            return InteractionResultHolder.pass(stack);
        }
        return beginCaptureChannel(level, player, hand, stack, target);
    }

    /**
     * Utilisé quand le joueur clique une entité (vanilla n’appelle pas {@link #use} dans ce cas) — voir
     * {@link com.soleapixel.bmcmod.morph.CrystalEntityInteractEvents}.
     */
    public static InteractionResult tryBeginCaptureChannelOnEntity(
            Player player, InteractionHand hand, ItemStack stack, Entity target) {
        if (!(stack.getItem() instanceof CaptureCrystalItem)) {
            return InteractionResult.PASS;
        }
        if (!(target instanceof LivingEntity living)) {
            return InteractionResult.PASS;
        }
        if (CaptureCrystalSoul.hasCaptured(stack)) {
            return InteractionResult.PASS;
        }
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return InteractionResult.PASS;
        }
        if (!isValidCaptureCandidate(living, player)) {
            return InteractionResult.PASS;
        }
        InteractionResultHolder<ItemStack> holder = beginCaptureChannel(player.level(), player, hand, stack, living);
        return holder.getResult();
    }

    private static InteractionResultHolder<ItemStack> beginCaptureChannel(
            Level level, Player player, InteractionHand hand, ItemStack stack, LivingEntity target) {
        if (level.isClientSide()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.pass(stack);
        }
        setChargeTarget(sp, target.getUUID());
        sp.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getHand() != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        if (!CaptureCrystalSoul.hasCaptured(stack)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel sl) || !(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return InteractionResult.FAIL;
        }
        CompoundTag tag = CaptureCrystalSoul.getCaptured(stack).copy();
        Optional<Entity> created = EntityType.create(tag, sl);
        if (created.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.bmcmod.capture_crystal.spawn_fail"), true);
            return InteractionResult.FAIL;
        }
        Entity entity = created.get();
        BlockPos clicked = context.getClickedPos();
        BlockPos spawnPos = clicked.relative(context.getClickedFace());
        double x = spawnPos.getX() + 0.5;
        double z = spawnPos.getZ() + 0.5;
        double y = findSpawnY(sl, entity, spawnPos);
        entity.moveTo(x, y, z, player.getYRot(), 0.0F);
        if (entity instanceof Mob mob) {
            mob.setYHeadRot(player.getYRot());
        }
        if (!sl.noCollision(entity)) {
            player.displayClientMessage(Component.translatable("message.bmcmod.capture_crystal.no_room"), true);
            return InteractionResult.FAIL;
        }
        sl.addFreshEntity(entity);
        CaptureCrystalSoul.clearCaptured(stack);
        applyReleaseDurability(player, stack, context.getHand());
        if (!player.getAbilities().instabuild) {
            player.getCooldowns().addCooldown(stack.getItem(), RELEASE_AFTER_PLACE_COOLDOWN_TICKS);
        }
        sl.playSound(null, x, y, z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.85F, 1.05F);
        return InteractionResult.CONSUME;
    }

    private static double findSpawnY(ServerLevel level, Entity entity, BlockPos base) {
        double height = entity.getBbHeight();
        int startY = base.getY();
        for (int dy = 0; dy < 6; dy++) {
            double y = startY + dy;
            entity.setPos(base.getX() + 0.5, y, base.getZ() + 0.5);
            if (level.noCollision(entity)) {
                return y;
            }
        }
        return startY + 0.01 + Math.max(0, height * 0.5);
    }

    private static void applyReleaseDurability(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        if (!(stack.getItem() instanceof CaptureCrystalItem)) {
            return;
        }
        int max = stack.getMaxDamage();
        int cur = stack.getDamageValue();
        int next = cur + RELEASE_DURABILITY_COST;
        if (next >= max) {
            player.setItemInHand(hand, new ItemStack(ModItems.CRACKED_CRYSTAL.get()));
        } else {
            stack.setDamageValue(next);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return CHARGE_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer sp)) {
            return;
        }
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        UUID id = getChargeTarget(sp);
        if (id == null) {
            return;
        }
        Entity ent = sl.getEntity(id);
        if (!(ent instanceof LivingEntity living) || !living.isAlive()) {
            sp.stopUsingItem();
            return;
        }
        if (!isStillLookingAt(sp, living, REACH)) {
            sp.stopUsingItem();
            return;
        }
        double px = living.getX();
        double py = living.getY() + living.getBbHeight() * 0.55;
        double pz = living.getZ();
        for (int i = 0; i < 5; i++) {
            double ox = (sl.random.nextDouble() - 0.5) * living.getBbWidth();
            double oy = (sl.random.nextDouble() - 0.5) * living.getBbHeight();
            double oz = (sl.random.nextDouble() - 0.5) * living.getBbWidth();
            sl.sendParticles(ParticleTypes.ENCHANT, px + ox, py + oy, pz + oz, 1, 0, 0.02, 0, 0.04);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft) {
        if (level.isClientSide() || !(livingEntity instanceof ServerPlayer sp)) {
            return;
        }
        clearChargeTarget(sp);
        int used = getUseDuration(stack, livingEntity) - timeLeft;
        if (used < CHARGE_TICKS - 1) {
            sp.getCooldowns().addCooldown(stack.getItem(), CANCEL_COOLDOWN_TICKS);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!(user instanceof ServerPlayer sp) || !(level instanceof ServerLevel sl)) {
            return stack;
        }
        UUID id = getChargeTarget(sp);
        if (id == null) {
            return stack;
        }
        clearChargeTarget(sp);
        Entity ent = sl.getEntity(id);
        if (!(ent instanceof LivingEntity living) || !living.isAlive()) {
            return stack;
        }
        if (!isStillLookingAt(sp, living, REACH)) {
            return stack;
        }
        if (MorphBossBlacklist.isBoss(living) || living instanceof Player) {
            return stack;
        }
        String encodeId = living.getEncodeId();
        if (encodeId == null) {
            return stack;
        }
        CompoundTag tag = new CompoundTag();
        tag.putString("id", encodeId);
        living.saveWithoutId(tag);
        CaptureCrystalSoul.setCaptured(stack, MorphSoulSanitizer.sanitize(tag));
        living.remove(Entity.RemovalReason.DISCARDED);
        sp.getCooldowns().addCooldown(stack.getItem(), POST_CAPTURE_PLACE_DELAY_TICKS);
        sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7F, 1.15F);
        sp.displayClientMessage(Component.translatable("message.bmcmod.capture_crystal.captured"), true);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (!flag.hasShiftDown()) {
            if (CaptureCrystalSoul.hasCaptured(stack)) {
                tooltip.add(
                        Component.translatable(
                                        "item.bmcmod.capture_crystal.summary.filled",
                                        MorphCrystalSoul.getStoredSoulDisplayName(CaptureCrystalSoul.getCaptured(stack)))
                                .withStyle(ChatFormatting.GREEN));
            } else {
                tooltip.add(
                        Component.translatable("item.bmcmod.capture_crystal.summary.empty").withStyle(ChatFormatting.GRAY));
            }
            return;
        }
        if (CaptureCrystalSoul.hasCaptured(stack)) {
            tooltip.add(Component.translatable("item.bmcmod.capture_crystal.contains").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("item.bmcmod.capture_crystal.empty").withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.translatable("item.bmcmod.capture_crystal.hint").withStyle(ChatFormatting.DARK_AQUA));
    }

    public static LivingEntity findCapturableLookTarget(Player player, double reach) {
        Vec3 start = player.getEyePosition(1f);
        Vec3 dir = player.getViewVector(1f);
        Vec3 end = start.add(dir.scale(reach));
        LivingEntity best = null;
        double bestDist = reach * reach + 1.0;
        AABB area = player.getBoundingBox().expandTowards(dir.scale(reach)).inflate(1.0);
        for (LivingEntity liv : player.level().getEntitiesOfClass(LivingEntity.class, area, e -> isValidCaptureCandidate(e, player))) {
            Optional<Vec3> hit = liv.getBoundingBox().clip(start, end);
            if (hit.isEmpty()) {
                continue;
            }
            double dsq = start.distanceToSqr(hit.get());
            if (dsq < bestDist) {
                bestDist = dsq;
                best = liv;
            }
        }
        return best;
    }

    private static boolean isValidCaptureCandidate(LivingEntity e, Player player) {
        if (e == player || !e.isAlive() || e.isSpectator() || !e.isPickable()) {
            return false;
        }
        if (e instanceof Player) {
            return false;
        }
        if (MorphBossBlacklist.isBoss(e)) {
            return false;
        }
        return e.getEncodeId() != null;
    }

    private static boolean isStillLookingAt(ServerPlayer player, LivingEntity target, double reach) {
        LivingEntity t = findCapturableLookTarget(player, reach);
        return t != null && t.getUUID().equals(target.getUUID());
    }

    private static void setChargeTarget(Player player, UUID id) {
        player.getPersistentData().putLong(CHARGE_MSB, id.getMostSignificantBits());
        player.getPersistentData().putLong(CHARGE_LSB, id.getLeastSignificantBits());
    }

    private static UUID getChargeTarget(Player player) {
        if (!player.getPersistentData().contains(CHARGE_MSB)) {
            return null;
        }
        return new UUID(
                player.getPersistentData().getLong(CHARGE_MSB),
                player.getPersistentData().getLong(CHARGE_LSB));
    }

    private static void clearChargeTarget(Player player) {
        player.getPersistentData().remove(CHARGE_MSB);
        player.getPersistentData().remove(CHARGE_LSB);
    }
}
