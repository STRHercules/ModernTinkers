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

/** Formed smeltery controller with a larger persistent tank. */
public final class SmelteryControllerBlock extends BaseEntityBlock {
    public static final MapCodec<SmelteryControllerBlock> CODEC = simpleCodec(
            properties -> new SmelteryControllerBlock(properties, false));
    private final boolean scorched;

    public SmelteryControllerBlock(Properties properties) {
        this(properties, false);
    }

    public SmelteryControllerBlock(Properties properties, boolean scorched) {
        super(properties);
        this.scorched = scorched;
    }

    public boolean isScorched() {
        return scorched;
    }

    @Override
    public MapCodec<SmelteryControllerBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(net.minecraft.core.BlockPos pos, BlockState state) {
        return new SmelteryControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, SmelteryContent.SMELTERY_CONTROLLER_BLOCK_ENTITY.get(),
                        SmelteryControllerBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level,
            net.minecraft.core.BlockPos pos, Player player, BlockHitResult hit) {
        return openMenu(level, pos, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            net.minecraft.core.BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        InteractionResult result = openMenu(level, pos, player);
        return result == InteractionResult.CONSUME
                ? ItemInteractionResult.CONSUME : ItemInteractionResult.SUCCESS;
    }

    private static InteractionResult openMenu(Level level, net.minecraft.core.BlockPos pos,
                                              Player player) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof SmelteryControllerBlockEntity smeltery)) {
            return InteractionResult.PASS;
        }
        player.openMenu(smeltery, buffer -> buffer.writeBlockPos(pos));
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, net.minecraft.core.BlockPos pos,
            BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof SmelteryControllerBlockEntity smeltery) {
                Containers.dropContents(level, pos, smeltery.getInputInventory());
                smeltery.dropFluidContents();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
