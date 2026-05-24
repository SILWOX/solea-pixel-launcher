package com.soleapixel.bmcmod.gameplay;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.soleapixel.bmcmod.BmcMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.soleapixel.bmcmod.registry.ModEnchantmentKeys;
import com.soleapixel.bmcmod.registry.ModMobEffects;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = BmcMod.MODID)
public final class EnchantmentGameplayEvents {
    private static final String EXPLOSIVE_TAG = "BmcModExplosive";
    /** Évite la récursion : chaque {@code destroyBlock} relance un BreakEvent sur le même arbre. */
    private static final String TIMBER_CHAIN = BmcMod.MODID + ":timber_chain";
    /** Évite la récursion quand {@link ModEnchantmentKeys#EMPATHIC_STRIKE} renvoie les dégâts sur le joueur. */
    private static final String EMPATHIC_REBOUND = BmcMod.MODID + ":empathic_rebound";
    /** Évite la récursion quand l'enchantement Excavator casse en chaîne le 3x3. */
    private static final String EXCAVATOR_CHAIN = BmcMod.MODID + ":excavator_chain";
    private static final String EXCAVATOR_MODE_KEY = "ExcavatorMode3x3";

    private EnchantmentGameplayEvents() {
    }

    private static int ench(ItemStack stack, RegistryAccess reg, ResourceKey<Enchantment> key) {
        return reg.lookupOrThrow(Registries.ENCHANTMENT).get(key).map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, stack)).orElse(0);
    }

    public static boolean isExcavator3x3Enabled(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return !tag.contains(EXCAVATOR_MODE_KEY) || tag.getBoolean(EXCAVATOR_MODE_KEY);
    }

    public static boolean toggleExcavatorMode(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        boolean next = !isExcavator3x3Enabled(stack);
        tag.putBoolean(EXCAVATOR_MODE_KEY, next);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return next;
    }

    private static int fireThornsOn(LivingEntity victim) {
        RegistryAccess reg = victim.level().registryAccess();
        var h = reg.lookupOrThrow(Registries.ENCHANTMENT).get(ModEnchantmentKeys.FIRE_THORN);
        if (h.isEmpty()) {
            return 0;
        }
        int sum = 0;
        for (EquipmentSlot slot : new EquipmentSlot[] {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            sum += EnchantmentHelper.getItemEnchantmentLevel(h.get(), victim.getItemBySlot(slot));
        }
        return sum;
    }

    private static ItemStack resolveMeleeWeapon(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof SwordItem || main.getItem() instanceof AxeItem || main.getItem() instanceof TridentItem) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof SwordItem || off.getItem() instanceof AxeItem || off.getItem() instanceof TridentItem) {
            return off;
        }
        return ItemStack.EMPTY;
    }

    private static boolean isPlayerPhysical(DamageSource src) {
        return src.getEntity() instanceof Player;
    }

    /**
     * Malédiction empathique : mêlée uniquement — la cible ne subit pas les dégâts, le joueur les prend
     * (comme si la cible ripostait).
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDamagePreEmpathicCurse(LivingDamageEvent.Pre event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        DamageSource src = event.getSource();
        if (!(src.getEntity() instanceof Player player)) {
            return;
        }
        if (src.getDirectEntity() != player) {
            return;
        }
        if (victim == player) {
            return;
        }
        if (player.getPersistentData().getBoolean(EMPATHIC_REBOUND)) {
            return;
        }
        RegistryAccess reg = victim.level().registryAccess();
        ItemStack weapon = player.getWeaponItem();
        if (weapon.isEmpty() || ench(weapon, reg, ModEnchantmentKeys.EMPATHIC_STRIKE) <= 0) {
            return;
        }
        float dmg = event.getNewDamage();
        if (dmg <= 0.0F) {
            return;
        }
        event.setNewDamage(0.0F);
        player.getPersistentData().putBoolean(EMPATHIC_REBOUND, true);
        try {
            DamageSource rebound = victim.damageSources().mobAttack(victim);
            player.hurt(rebound, dmg);
        } finally {
            player.getPersistentData().remove(EMPATHIC_REBOUND);
        }
    }

    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        DamageSource src = event.getSource();
        float dealt = event.getNewDamage();
        if (dealt <= 0) {
            return;
        }
        RegistryAccess reg = victim.level().registryAccess();
        Entity attackerEntity = src.getEntity();
        if (!(attackerEntity instanceof LivingEntity attacker)) {
            return;
        }
        Player playerAttacker = attacker instanceof Player p ? p : null;

        int fire = fireThornsOn(victim);
        if (fire > 0 && attacker != victim) {
            int seconds = 2 + fire * 2;
            int ticks = seconds * 20;
            attacker.setRemainingFireTicks(Math.max(attacker.getRemainingFireTicks(), ticks));
        }

        if (!isPlayerPhysical(src) || playerAttacker == null) {
            return;
        }
        ItemStack weapon = resolveMeleeWeapon(playerAttacker);
        if (weapon.isEmpty()) {
            return;
        }

        int bleed = ench(weapon, reg, ModEnchantmentKeys.BLEEDING);
        if (bleed > 0 && victim != attacker) {
            victim.addEffect(new MobEffectInstance(ModMobEffects.BLEED, 60 + 40 * bleed, bleed - 1, false, true, true));
        }

        int steal = ench(weapon, reg, ModEnchantmentKeys.LIFE_STEAL);
        if (steal > 0 && victim != attacker) {
            float heal = dealt * (0.04f + 0.03f * steal);
            attacker.heal(heal);
        }

        if (weapon.getItem() instanceof AxeItem) {
            int crush = ench(weapon, reg, ModEnchantmentKeys.CRUSHING_BLOW);
            if (crush > 0 && victim != attacker) {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40 + 20 * crush, crush - 1, false, true, true));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBlockBreakTimber(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (player.getPersistentData().getBoolean(TIMBER_CHAIN)) {
            return;
        }
        if (player.isShiftKeyDown()) {
            return;
        }
        BlockState state = event.getState();
        if (!state.is(BlockTags.LOGS)) {
            return;
        }
        ItemStack tool = player.getMainHandItem();
        if (!(tool.getItem() instanceof AxeItem)) {
            return;
        }
        int timber = ench(tool, level.registryAccess(), ModEnchantmentKeys.TIMBER);
        if (timber <= 0) {
            return;
        }
        List<BlockPos> logs = collectConnectedLogs(level, event.getPos(), 256);
        if (logs.size() <= 1) {
            return;
        }
        event.setCanceled(true);
        player.getPersistentData().putBoolean(TIMBER_CHAIN, true);
        try {
            for (BlockPos p : logs) {
                if (level.getBlockState(p).is(BlockTags.LOGS)) {
                    player.gameMode.destroyBlock(p);
                }
            }
        } finally {
            player.getPersistentData().remove(TIMBER_CHAIN);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockBreakExcavator(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.isClientSide()) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (player.getPersistentData().getBoolean(EXCAVATOR_CHAIN) || player.isShiftKeyDown()) {
            return;
        }
        ItemStack tool = player.getMainHandItem();
        if (!(tool.getItem() instanceof PickaxeItem)) {
            return;
        }
        if (ench(tool, level.registryAccess(), ModEnchantmentKeys.EXCAVATOR) <= 0) {
            return;
        }
        if (!isExcavator3x3Enabled(tool)) {
            return;
        }
        List<BlockPos> cluster = collectExcavator3x3(event.getPos(), player);
        if (cluster.size() <= 1) {
            return;
        }
        event.setCanceled(true);
        player.getPersistentData().putBoolean(EXCAVATOR_CHAIN, true);
        try {
            for (BlockPos p : cluster) {
                BlockState state = level.getBlockState(p);
                if (state.isAir()) {
                    continue;
                }
                if (state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state)) {
                    continue;
                }
                player.gameMode.destroyBlock(p);
            }
        } finally {
            player.getPersistentData().remove(EXCAVATOR_CHAIN);
        }
    }

    private static List<BlockPos> collectExcavator3x3(BlockPos center, ServerPlayer player) {
        List<BlockPos> out = new ArrayList<>(9);
        boolean horizontal = Math.abs(player.getXRot()) > 55.0F;
        Direction facing = player.getDirection();
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                BlockPos p;
                if (horizontal) {
                    p = center.offset(a, 0, b);
                } else if (facing.getAxis() == Direction.Axis.X) {
                    p = center.offset(0, a, b);
                } else {
                    p = center.offset(a, b, 0);
                }
                out.add(p);
            }
        }
        return out;
    }

    private static List<BlockPos> collectConnectedLogs(Level level, BlockPos start, int limit) {
        if (!level.getBlockState(start).is(BlockTags.LOGS)) {
            return List.of();
        }
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        q.add(start.immutable());
        seen.add(start.immutable());
        List<BlockPos> out = new ArrayList<>();
        while (!q.isEmpty() && out.size() < limit) {
            BlockPos p = q.poll();
            out.add(p);
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (seen.add(n) && level.getBlockState(n).is(BlockTags.LOGS)) {
                    q.add(n.immutable());
                }
            }
        }
        return out;
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getLevel().isClientSide()) {
            return;
        }
        Entity breaker = event.getBreaker();
        if (!(breaker instanceof Player player)) {
            return;
        }
        ItemStack tool = event.getTool();
        RegistryAccess reg = level.registryAccess();
        var silk = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
        if (EnchantmentHelper.getItemEnchantmentLevel(silk, tool) > 0) {
            return;
        }
        int auto = ench(tool, reg, ModEnchantmentKeys.AUTO_SMELT);
        if (auto > 0) {
            for (ItemEntity drop : event.getDrops()) {
                ItemStack s = drop.getItem();
                ItemStack out = smeltStack(level, s);
                if (!out.isEmpty()) {
                    drop.setItem(out);
                }
            }
        }

        if (!(player instanceof ServerPlayer sp)) {
            return;
        }
        if (!(tool.getItem() instanceof PickaxeItem)) {
            return;
        }
        if (!sp.hasEffect(ModMobEffects.VEIN_WHISPER)) {
            return;
        }
        BlockState broken = event.getState();
        if (!broken.is(BmcModBlockTags.VEIN_WHISPER_STONE)) {
            return;
        }
        int amp = Optional.ofNullable(sp.getEffect(ModMobEffects.VEIN_WHISPER)).map(MobEffectInstance::getAmplifier).orElse(0);
        RandomSource rnd = level.random;
        float chance = 0.015f + amp * 0.02f;
        if (rnd.nextFloat() > chance) {
            return;
        }
        ItemStack bonus = rollVeinBonus(rnd, amp);
        if (!bonus.isEmpty()) {
            Vec3 pos = Vec3.atCenterOf(event.getPos());
            event.getDrops().add(new ItemEntity(level, pos.x, pos.y, pos.z, bonus));
        }
    }

    private static ItemStack smeltStack(ServerLevel level, ItemStack in) {
        if (in.isEmpty()) {
            return ItemStack.EMPTY;
        }
        var input = new SingleRecipeInput(in);
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level)
                .map(h -> {
                    ItemStack out = h.value().getResultItem(level.registryAccess()).copy();
                    out.setCount(in.getCount());
                    return out;
                })
                .orElse(ItemStack.EMPTY);
    }

    private static ItemStack rollVeinBonus(RandomSource rnd, int amplifier) {
        float u = rnd.nextFloat();
        if (amplifier >= 1) {
            if (u < 0.003f) {
                return new ItemStack(Items.DIAMOND);
            }
            if (u < 0.006f) {
                return new ItemStack(Items.EMERALD);
            }
            if (u < 0.02f) {
                return new ItemStack(Items.GOLD_INGOT);
            }
            if (u < 0.04f) {
                return new ItemStack(Items.REDSTONE, 2 + rnd.nextInt(4));
            }
            if (u < 0.06f) {
                return new ItemStack(Items.LAPIS_LAZULI, 2 + rnd.nextInt(4));
            }
        }
        if (u < 0.05f) {
            return new ItemStack(Items.IRON_INGOT);
        }
        if (u < 0.09f) {
            return new ItemStack(Items.COAL, 1 + rnd.nextInt(2));
        }
        if (u < 0.12f) {
            return new ItemStack(Items.RAW_COPPER, 1 + rnd.nextInt(3));
        }
        if (u < 0.15f) {
            return new ItemStack(Items.RAW_IRON);
        }
        return ItemStack.EMPTY;
    }

    @SubscribeEvent
    public static void onArrowJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow)) {
            return;
        }
        if (!(arrow.getOwner() instanceof ServerPlayer player)) {
            return;
        }
        RegistryAccess reg = player.level().registryAccess();
        int a = explosiveOnRanged(player.getMainHandItem(), reg);
        int b = explosiveOnRanged(player.getOffhandItem(), reg);
        int lv = Math.max(a, b);
        if (lv <= 0) {
            return;
        }
        arrow.getPersistentData().putInt(EXPLOSIVE_TAG, lv);
    }

    private static int explosiveOnRanged(ItemStack stack, RegistryAccess reg) {
        if (!(stack.getItem() instanceof BowItem) && !(stack.getItem() instanceof CrossbowItem)) {
            return 0;
        }
        return ench(stack, reg, ModEnchantmentKeys.EXPLOSIVE_SHOT);
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile proj = event.getProjectile();
        if (proj.level().isClientSide() || !(proj instanceof AbstractArrow arrow)) {
            return;
        }
        if (!arrow.getPersistentData().contains(EXPLOSIVE_TAG)) {
            return;
        }
        int lv = arrow.getPersistentData().getInt(EXPLOSIVE_TAG);
        HitResult hit = event.getRayTraceResult();
        Vec3 pos = hit.getLocation();
        Level level = arrow.level();
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        Entity owner = arrow.getOwner();
        LivingEntity igniter = owner instanceof LivingEntity l ? l : null;
        DamageSource boom = arrow.damageSources().explosion(arrow, igniter != null ? igniter : arrow);
        // Puissance TNT-like : niveau 1 = petite détonation, niveaux suivants cassent plus de blocs et blessent plus.
        float power = 0.48f + (lv - 1) * 0.34f;
        sl.explode(arrow, boom, null, pos.x, pos.y, pos.z, power, false, Level.ExplosionInteraction.TNT);

        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
        int puffs = 8 + lv * 4;
        for (int i = 0; i < puffs; i++) {
            double ox = (sl.random.nextDouble() - 0.5) * 0.55;
            double oy = sl.random.nextDouble() * 0.35;
            double oz = (sl.random.nextDouble() - 0.5) * 0.55;
            sl.sendParticles(ParticleTypes.POOF, pos.x + ox, pos.y + oy, pos.z + oz, 1, 0.02, 0.02, 0.02, 0.02);
            if (i % 2 == 0) {
                sl.sendParticles(ParticleTypes.SMALL_FLAME, pos.x + ox * 0.7, pos.y + oy, pos.z + oz * 0.7, 1, 0.0, 0.05, 0.0, 0.01);
            }
        }
        sl.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.35f + lv * 0.08f, 0.85f + lv * 0.07f);
        sl.playSound(null, pos.x, pos.y, pos.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.25f, 1.2f);

        arrow.discard();
        event.setCanceled(true);
    }
}
