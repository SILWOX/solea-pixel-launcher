package com.soleapixel.bmcmod.item;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.gameevent.GameEvent;

import com.soleapixel.bmcmod.gameplay.ChargedCreeperSpawnEggEvents;

import net.neoforged.neoforge.common.DeferredSpawnEggItem;

/**
 * Œuf d’apparition qui ne fait naître que des creepers déjà chargés (comme après un éclair).
 */
public final class ChargedCreeperSpawnEggItem extends DeferredSpawnEggItem {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChargedCreeperSpawnEggItem.class);

    public ChargedCreeperSpawnEggItem(Properties properties) {
        super(() -> EntityType.CREEPER, 0x3A5F2F, 0x6FE8FF, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            ChargedCreeperSpawnEggEvents.beginChargedSpawn();
            try {
                return super.use(level, player, hand);
            } finally {
                ChargedCreeperSpawnEggEvents.endChargedSpawn();
            }
        }
        return super.use(level, player, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide()) {
            ChargedCreeperSpawnEggEvents.beginChargedSpawn();
            try {
                return super.useOn(context);
            } finally {
                ChargedCreeperSpawnEggEvents.endChargedSpawn();
            }
        }
        return super.useOn(context);
    }

    @Override
    protected DispenseItemBehavior createDispenseBehavior() {
        return (source, stack) -> {
            Direction face = source.state().getValue(DispenserBlock.FACING);
            EntityType<?> type = ((SpawnEggItem) stack.getItem()).getType(stack);
            ServerLevel lev = source.level();
            try {
                Entity spawned = type.spawn(lev, stack, null, source.pos().relative(face), MobSpawnType.DISPENSER, face != Direction.UP, false);
                if (spawned instanceof Creeper creeper) {
                    ChargedCreeperSpawnEggEvents.applyCharge(creeper);
                    lev.playSound(null, creeper.getX(), creeper.getY(), creeper.getZ(), SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 0.5F, 1.2F);
                }
            } catch (Exception exception) {
                LOGGER.error("Error while dispensing charged creeper spawn egg at {}", source.pos(), exception);
                return ItemStack.EMPTY;
            }
            stack.shrink(1);
            lev.gameEvent(GameEvent.ENTITY_PLACE, source.pos(), GameEvent.Context.of(source.state()));
            return stack;
        };
    }
}
