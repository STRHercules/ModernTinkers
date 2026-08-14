package com.moderntinkers.content.tinkerstation;

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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/** Server-authoritative Tinker Station assembly block. */
public final class TinkerStationBlock extends BaseEntityBlock {
    public static final MapCodec<TinkerStationBlock> CODEC = simpleCodec(TinkerStationBlock::new);

    public TinkerStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<TinkerStationBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(net.minecraft.core.BlockPos pos, BlockState state) {
        return new TinkerStationBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level,
            net.minecraft.core.BlockPos pos, Player player, BlockHitResult hit) {
        return openMenu(state, level, pos, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            net.minecraft.core.BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = openMenu(state, level, pos, player);
        return result == InteractionResult.CONSUME
                ? ItemInteractionResult.CONSUME : ItemInteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level,
            net.minecraft.core.BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof TinkerStationBlockEntity station ? station : null;
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
            if (blockEntity instanceof TinkerStationBlockEntity station) {
                Containers.dropContents(level, pos, station.getInputInventory());
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
