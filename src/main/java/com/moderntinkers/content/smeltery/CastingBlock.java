package com.moderntinkers.content.smeltery;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

import javax.annotation.Nullable;

/** Casting table/basin shell sharing one server-authoritative fluid path. */
public final class CastingBlock extends BaseEntityBlock {
    public enum Mode {
        TABLE,
        BASIN
    }

    public static final MapCodec<CastingBlock> CODEC = simpleCodec(CastingBlock::new);
    private final Mode mode;

    public CastingBlock(Properties properties) {
        this(properties, Mode.TABLE);
    }

    public CastingBlock(Properties properties, Mode mode) {
        super(properties);
        this.mode = mode;
    }

    public Mode mode() {
        return mode;
    }

    @Override
    public MapCodec<CastingBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(net.minecraft.core.BlockPos pos, BlockState state) {
        return new CastingBlockEntity(pos, state, mode == Mode.BASIN);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type,
                        mode == Mode.BASIN
                                ? SmelteryContent.CASTING_BASIN_BLOCK_ENTITY.get()
                                : SmelteryContent.CASTING_TABLE_BLOCK_ENTITY.get(),
                        CastingBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level,
            net.minecraft.core.BlockPos pos, Player player, BlockHitResult hit) {
        return openMenu(state, level, pos, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            net.minecraft.core.BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        InteractionResult result = openMenu(state, level, pos, player);
        return result == InteractionResult.CONSUME
                ? ItemInteractionResult.CONSUME : ItemInteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level,
            net.minecraft.core.BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof CastingBlockEntity casting ? casting : null;
    }

    private InteractionResult openMenu(BlockState state, Level level,
            net.minecraft.core.BlockPos pos, Player player) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        MenuProvider provider = getMenuProvider(state, level, pos);
        if (provider == null) {
            return InteractionResult.PASS;
        }
        player.openMenu(provider, buffer -> buffer.writeBlockPos(pos));
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, net.minecraft.core.BlockPos pos,
            BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof CastingBlockEntity casting) {
                Containers.dropContents(level, pos, casting.getCastInventory());
                FluidContents.dropBuckets(level, pos, casting.getFluidTank());
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
