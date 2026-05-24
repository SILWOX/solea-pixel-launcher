package com.soleapixel.bmcmod.item;

import java.util.List;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.soleapixel.bmcmod.gameplay.WitherStaffAllyEvents;
import com.soleapixel.bmcmod.rarity.BmcModRarity;
import com.soleapixel.bmcmod.rarity.RarityStickItem;

public final class WitherStaffItem extends RarityStickItem {
    public static final String MODE_KEY = "WitherStaffMode";
    public static final int MODE_HEAD = 1;
    public static final int MODE_ALLIES = 2;

    public static final int MAX_USES = 420;
    public static final int COST_HEAD = 5;
    public static final int COST_ALLIES = 24;
    public static final int CD_HEAD = 18;
    public static final int CD_ALLIES = 180;
    public static final int ALLY_COUNT = 3;
    public static final int ALLY_LIFETIME_TICKS = 20 * 90;

    public WitherStaffItem(Properties properties) {
        super(properties, BmcModRarity.EXOTIC);
    }

    public static int getMode(ItemStack stack) {
        int m = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(MODE_KEY);
        return m >= MODE_HEAD && m <= MODE_ALLIES ? m : MODE_HEAD;
    }

    public static void setMode(ItemStack stack, int mode) {
        int m = Mth.clamp(mode, MODE_HEAD, MODE_ALLIES);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(MODE_KEY, m);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void cycleMode(ItemStack stack) {
        setMode(stack, getMode(stack) == MODE_HEAD ? MODE_ALLIES : MODE_HEAD);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }
        if (!sp.getAbilities().instabuild && sp.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        int mode = getMode(stack);
        boolean ok = mode == MODE_HEAD ? shootWitherHead(sp) : summonWitherAllies(sp);
        if (!ok) {
            return InteractionResultHolder.fail(stack);
        }

        if (!sp.getAbilities().instabuild) {
            int cost = mode == MODE_HEAD ? COST_HEAD : COST_ALLIES;
            int cd = mode == MODE_HEAD ? CD_HEAD : CD_ALLIES;
            EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            for (int i = 0; i < cost; i++) {
                if (stack.getDamageValue() >= stack.getMaxDamage()) {
                    break;
                }
                stack.hurtAndBreak(1, sp, slot);
            }
            sp.getCooldowns().addCooldown(this, cd);
        }
        sp.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.success(stack);
    }

    private static boolean shootWitherHead(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 spawn = player.getEyePosition(1f).add(look.scale(0.72));
        WitherSkull skull = new WitherSkull(level, player, look);
        skull.setPos(spawn.x, spawn.y, spawn.z);
        level.addFreshEntity(skull);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.7F, 0.9F + level.random.nextFloat() * 0.1F);
        level.sendParticles(ParticleTypes.SMOKE, spawn.x, spawn.y, spawn.z, 6, 0.12, 0.12, 0.12, 0.01);
        return true;
    }

    private static boolean summonWitherAllies(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        UUID owner = player.getUUID();
        long expiresAt = level.getGameTime() + ALLY_LIFETIME_TICKS;
        int existing = countOwnedAllies(level, owner);
        int allowedToSpawn = Math.max(0, ALLY_COUNT - existing);
        if (allowedToSpawn <= 0) {
            return false;
        }
        int spawned = 0;
        for (int i = 0; i < allowedToSpawn; i++) {
            Vec3 pos = findSummonPos(level, player, i);
            WitherSkeleton ally = EntityType.WITHER_SKELETON.create(level);
            if (ally == null) {
                continue;
            }
            ally.setPos(pos.x, pos.y, pos.z);
            ally.setPersistenceRequired();
            ally.setCanPickUpLoot(false);
            ally.setTarget(null);
            ally.getPersistentData().putUUID(WitherStaffAllyEvents.TAG_OWNER, owner);
            ally.getPersistentData().putLong(WitherStaffAllyEvents.TAG_EXPIRES_AT, expiresAt);
            equipAlly(level, ally);
            if (level.addFreshEntity(ally)) {
                spawned++;
                level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y + 1.0, pos.z, 12, 0.2, 0.4, 0.2, 0.02);
            }
        }
        if (spawned > 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 0.55F, 1.25F);
            return true;
        }
        return false;
    }

    private static int countOwnedAllies(ServerLevel level, UUID owner) {
        int count = 0;
        for (WitherSkeleton ws : level.getEntitiesOfClass(WitherSkeleton.class, new net.minecraft.world.phys.AABB(
                -3.0E7, -1024.0, -3.0E7,
                3.0E7, 2048.0, 3.0E7))) {
            if (ws.isAlive() && ws.getPersistentData().hasUUID(WitherStaffAllyEvents.TAG_OWNER)
                    && owner.equals(ws.getPersistentData().getUUID(WitherStaffAllyEvents.TAG_OWNER))) {
                count++;
            }
        }
        return count;
    }

    private static void equipAlly(ServerLevel level, WitherSkeleton ally) {
        ItemStack helmet = new ItemStack(Items.IRON_HELMET);
        ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE);
        ItemStack legs = new ItemStack(Items.IRON_LEGGINGS);
        ItemStack boots = new ItemStack(Items.IRON_BOOTS);
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        var ench = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        helmet.enchant(ench.getOrThrow(Enchantments.PROTECTION), 1);
        chest.enchant(ench.getOrThrow(Enchantments.PROTECTION), 1);
        legs.enchant(ench.getOrThrow(Enchantments.PROTECTION), 1);
        boots.enchant(ench.getOrThrow(Enchantments.PROTECTION), 1);
        sword.enchant(ench.getOrThrow(Enchantments.SHARPNESS), 2);
        ally.setItemSlot(EquipmentSlot.HEAD, helmet);
        ally.setItemSlot(EquipmentSlot.CHEST, chest);
        ally.setItemSlot(EquipmentSlot.LEGS, legs);
        ally.setItemSlot(EquipmentSlot.FEET, boots);
        ally.setItemSlot(EquipmentSlot.MAINHAND, sword);
        ally.setDropChance(EquipmentSlot.HEAD, 0.0F);
        ally.setDropChance(EquipmentSlot.CHEST, 0.0F);
        ally.setDropChance(EquipmentSlot.LEGS, 0.0F);
        ally.setDropChance(EquipmentSlot.FEET, 0.0F);
        ally.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    private static Vec3 findSummonPos(ServerLevel level, ServerPlayer player, int index) {
        Vec3 base = player.position();
        double angle = (Mth.TWO_PI * index) / Math.max(ALLY_COUNT, 1);
        for (int attempt = 0; attempt < 8; attempt++) {
            double r = 2.0 + attempt * 0.45;
            double x = base.x + Mth.cos((float) angle) * r;
            double z = base.z + Mth.sin((float) angle) * r;
            double y = player.getY();
            if (level.noCollision(EntityType.WITHER_SKELETON.getDimensions().makeBoundingBox(x, y, z))) {
                return new Vec3(x, y, z);
            }
        }
        return base.add(0.0, 0.1, 0.0);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (!StaffTooltip.showDetailedLines(tooltip, flag)) {
            return;
        }
        int mode = getMode(stack);
        tooltip.add(Component.translatable("item.bmcmod.wither_staff.mode_line", Component.translatable("item.bmcmod.wither_staff.mode." + mode))
                .withStyle(ChatFormatting.DARK_GRAY));
        int left = stack.getMaxDamage() - stack.getDamageValue();
        tooltip.add(Component.translatable("item.bmcmod.wither_staff.fuel", left, stack.getMaxDamage()).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.bmcmod.wither_staff.usage").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.bmcmod.wither_staff.mode_switch").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.isDamageableItem();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * (stack.getMaxDamage() - stack.getDamageValue()) / (float) stack.getMaxDamage());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float f = Math.max(0.0F, ((float) stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage());
        return Mth.hsvToRgb(0.78F - f * 0.18F, 0.75F, 0.95F);
    }

    @Override
    public boolean canFitInsideContainerItems(ItemStack stack) {
        return false;
    }
}
