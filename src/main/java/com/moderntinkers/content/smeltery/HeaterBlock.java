package com.moderntinkers.content.smeltery;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Fuel insert point for a melter placed directly above the heater. */
public final class HeaterBlock extends BaseEntityBlock {
    public static final MapCodec<HeaterBlock> CODEC = simpleCodec(HeaterBlock::new);

    public HeaterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<HeaterBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(net.minecraft.core.BlockPos pos, BlockState state) {
        return new HeaterBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            net.minecraft.core.BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof HeaterBlockEntity heater)
                || !MelterBlockEntity.isFuel(stack) || !heater.insertFuel(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level,
            net.minecraft.core.BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof HeaterBlockEntity heater)) {
            return InteractionResult.PASS;
        }
        ItemStack fuel = heater.removeFuel();
        if (fuel.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!player.getInventory().add(fuel)) {
            player.drop(fuel, false);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, net.minecraft.core.BlockPos pos,
            BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof HeaterBlockEntity heater) {
                Containers.dropContents(level, pos, heater.getFuelInventory());
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
