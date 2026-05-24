package com.soleapixel.bmcmod.block.feeder;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.soleapixel.bmcmod.registry.ModBlockEntityTypes;

import org.jetbrains.annotations.Nullable;

public class FeederBlock extends BaseEntityBlock {
    public static final MapCodec<FeederBlock> CODEC = simpleCodec(FeederBlock::new);
    public static final EnumProperty<FeederContentKind> CONTENTS = EnumProperty.create("contents", FeederContentKind.class);
    private static final VoxelShape SHAPE = Shapes.block();
    public FeederBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                        .setValue(CONTENTS, FeederContentKind.EMPTY));
    }

    @Override
    public MapCodec<FeederBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING, CONTENTS);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(CONTENTS, FeederContentKind.EMPTY);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FeederBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, ModBlockEntityTypes.FEEDER.get(), (l, p, s, be) -> be.serverTick());
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof FeederBlockEntity be)) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }
        if (!player.isShiftKeyDown()) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }
        if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel sl)) {
            return InteractionResult.CONSUME;
        }
        int n = trySneakBulkFill(sl, sp, be);
        if (n == 0 && resolveBulkFoodKind(sp, be) == null) {
            return InteractionResult.PASS;
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof FeederBlockEntity be)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        boolean sneak = player.isShiftKeyDown();
        if (!sneak) {
            if (stack.isEmpty() || !FeederFoodRules.isAcceptedFood(stack)) {
                return stack.isEmpty()
                        ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                        : ItemInteractionResult.CONSUME;
            }
            if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
                insertOneUnit(sl, sp, be, stack);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        int n = trySneakBulkFill(sl, sp, be);
        if (n == 0 && resolveBulkFoodKind(sp, be) == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * Remplissage sneak : même logique que {@link #trySneakBulkFill} — peut être appelé depuis un event NeoForge
     * avant l’interaction bloc vanilla (ordre des mains / CONSUME).
     *
     * @return nombre total d’unités insérées (blé, ou ballots de foin comme une unité par ballot, etc.)
     */
    public static int trySneakBulkFill(ServerLevel level, ServerPlayer player, FeederBlockEntity be) {
        FeederContentKind heldKind = resolveBulkFoodKind(player, be);
        if (heldKind == null) {
            return 0;
        }
        int totalInserted = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack slotStack = inv.getItem(i);
            if (slotStack.isEmpty()) {
                continue;
            }
            if (!FeederFoodRules.isAcceptedFood(slotStack)) {
                continue;
            }
            if (be.isEmpty() && FeederFoodRules.classify(slotStack) != heldKind) {
                continue;
            }

            net.minecraft.world.item.Item statItem = slotStack.getItem();
            int inserted = be.insertFrom(slotStack, slotStack.getCount());
            if (inserted <= 0) {
                continue;
            }
            slotStack.shrink(inserted);
            totalInserted += inserted;
            player.awardStat(Stats.ITEM_USED.get(statItem));
        }

        if (totalInserted > 0) {
            level.playSound(null, be.getBlockPos(), SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.35F, 1.05F);
        }
        return totalInserted;
    }

    /**
     * Détermine la catégorie pour un bulk : mains (priorité main puis secondaire), puis contenu de la mangeoire,
     * puis premier aliment dans l’inventaire — toujours à partir du joueur actuel, pas de la seule main du clic.
     */
    @Nullable
    public static FeederContentKind resolveBulkFoodKind(ServerPlayer player, FeederBlockEntity be) {
        if (FeederFoodRules.isAcceptedFood(player.getMainHandItem())) {
            return FeederFoodRules.classify(player.getMainHandItem());
        }
        if (FeederFoodRules.isAcceptedFood(player.getOffhandItem())) {
            return FeederFoodRules.classify(player.getOffhandItem());
        }
        if (!be.isEmpty()) {
            return FeederFoodRules.classify(be.getRepresentativeStack());
        }
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            FeederContentKind k = FeederFoodRules.classify(s);
            if (k != null) {
                return k;
            }
        }
        return null;
    }

    private static void insertOneUnit(ServerLevel level, ServerPlayer player, FeederBlockEntity be, ItemStack stack) {
        net.minecraft.world.item.Item statItem = stack.getItem();
        int inserted = be.insertFrom(stack, 1);
        if (inserted > 0) {
            stack.shrink(inserted);
            player.awardStat(Stats.ITEM_USED.get(statItem));
            level.playSound(null, be.getBlockPos(), SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.35F, 1.05F);
        }
    }

    /**
     * Vide la réserve avant que le bloc soit retiré dans les chemins où {@link #onRemove} ne voit plus le BE.
     */
    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
        if (!level.isClientSide() && blockEntity instanceof FeederBlockEntity feeder && !feeder.isEmpty()) {
            feeder.dropContents(level, pos);
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FeederBlockEntity be && !be.isEmpty()) {
                be.dropContents(level, pos);
            }
            if (!level.isClientSide()) {
                Block.popResource(level, pos, new ItemStack(state.getBlock().asItem()));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof FeederBlockEntity be) {
            return (int) (15.0 * be.getStoredCount() / (double) FeederFoodRules.MAX_ITEMS);
        }
        return 0;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(BlockStateProperties.HORIZONTAL_FACING, rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
    }
}
