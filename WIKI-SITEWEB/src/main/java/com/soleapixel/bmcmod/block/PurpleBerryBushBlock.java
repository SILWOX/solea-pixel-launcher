package com.soleapixel.bmcmod.block;

import com.mojang.serialization.MapCodec;
import com.soleapixel.bmcmod.entity.Blink;
import com.soleapixel.bmcmod.entity.RadiantSlime;
import com.soleapixel.bmcmod.registry.ModBlocks;
import com.soleapixel.bmcmod.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.minecraft.tags.BlockTags;

/**
 * Comportement proche du buisson de baies sucrées : croissance aléatoire, poudre d’os, récolte au clic.
 * Croissance sans exigence de lumière (End). Baies plus nourrissantes côté item ; traversable à tout âge (pas de
 * hitbox de collision) ; effet poison en restant dans le buisson une fois poussé.
 */
public final class PurpleBerryBushBlock extends BushBlock implements BonemealableBlock {
    public static final int MAX_AGE = 3;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    private static final VoxelShape SHAPE_0 = Block.box(3.0, 0.0, 3.0, 13.0, 8.0, 13.0);

    /** Croix fine comme les buissons de baies sucrées : on peut passer au centre du bloc. */
    private static VoxelShape crossShape(double minY, double maxY) {
        return Shapes.or(
                Block.box(4.0, minY, 0.0, 12.0, maxY, 16.0),
                Block.box(0.0, minY, 4.0, 16.0, maxY, 12.0));
    }

    private static final VoxelShape SHAPE_1 = crossShape(0.0, 10.0);
    private static final VoxelShape SHAPE_2 = crossShape(0.0, 14.0);
    private static final VoxelShape SHAPE_3 = crossShape(0.0, 16.0);

    public static final MapCodec<PurpleBerryBushBlock> CODEC = BlockBehaviour.simpleCodec(PurpleBerryBushBlock::new);

    public PurpleBerryBushBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(ModItems.PURPLE_BERRY.get());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AGE)) {
            case 0 -> SHAPE_0;
            case 1 -> SHAPE_1;
            case 2 -> SHAPE_2;
            default -> SHAPE_3;
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /** Sols autorisés : tag {@code dirt} + terre labourée + herbe creuse (End) + pierre de l’End. */
    public static boolean mayPlantOn(BlockState below) {
        return below.is(BlockTags.DIRT)
                || below.is(Blocks.FARMLAND)
                || below.is(ModBlocks.HOLLOW_GRASS.get())
                || below.is(Blocks.END_STONE);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return mayPlantOn(state);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return mayPlantOn(level.getBlockState(pos.below()));
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age < MAX_AGE && random.nextInt(5) == 0) {
            level.setBlock(pos, state.setValue(AGE, age + 1), 2);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (state.getValue(AGE) == MAX_AGE) {
            if (!level.isClientSide()) {
                int berries = 1 + level.getRandom().nextInt(3);
                popResource(level, pos, new ItemStack(ModItems.PURPLE_BERRY.get(), berries));
                level.playSound(null, pos, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 0.8F, 1.1F);
                level.setBlock(pos, state.setValue(AGE, 1), 2);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    private static boolean ignoresBerryPoison(Entity entity) {
        return entity instanceof EnderMan || entity instanceof Blink || entity instanceof RadiantSlime;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (state.getValue(AGE) <= 0) {
            return;
        }
        if (ignoresBerryPoison(entity)) {
            return;
        }
        if (entity.getType() == EntityType.FOX || entity.getType() == EntityType.BEE) {
            return;
        }
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        if (living instanceof Player p && p.isCreative()) {
            return;
        }
        living.setDeltaMovement(living.getDeltaMovement().multiply(0.8, 1.0, 0.8));
        if (!level.isClientSide() && level.getGameTime() % 12 == 0) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0, false, false, true));
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int age = state.getValue(AGE);
        if (age < MAX_AGE) {
            level.setBlock(pos, state.setValue(AGE, age + 1), 2);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    public static BlockBehaviour.Properties berryBushProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .randomTicks()
                .noCollission()
                .strength(0.2F, 0.2F)
                .sound(SoundType.SWEET_BERRY_BUSH)
                .pushReaction(PushReaction.DESTROY)
                .noOcclusion();
    }
}
