package com.moderntinkers.content.smeltery;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/** Directional, persistent faucet that transfers a buffered packet over ticks. */
public final class FaucetBlock extends BaseEntityBlock implements EntityBlock {
    public static final MapCodec<FaucetBlock> CODEC = simpleCodec(FaucetBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING_HOPPER;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public FaucetBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false));
    }

    @Override
    public MapCodec<FaucetBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        if (facing == Direction.UP) {
            facing = Direction.DOWN;
        }
        return defaultBlockState().setValue(FACING, facing)
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FaucetBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, SmelteryContent.FAUCET_BLOCK_ENTITY.get(),
                        FaucetBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.getBlockEntity(pos) instanceof FaucetBlockEntity faucet) {
            faucet.activate();
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof FaucetBlockEntity faucet) {
            faucet.activate();
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                   BlockPos fromPos, boolean isMoving) {
        boolean powered = level.hasNeighborSignal(pos);
        if (level.getBlockEntity(pos) instanceof FaucetBlockEntity faucet) {
            faucet.neighborChanged(fromPos);
            faucet.handleRedstone(powered);
        }
        if (powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
        }
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
    }

    @Override
    protected void tick(BlockState state, net.minecraft.server.level.ServerLevel level,
                        BlockPos pos, net.minecraft.util.RandomSource random) {
        if (level.getBlockEntity(pos) instanceof FaucetBlockEntity faucet) {
            faucet.activate();
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof FaucetBlockEntity faucet) {
            faucet.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
