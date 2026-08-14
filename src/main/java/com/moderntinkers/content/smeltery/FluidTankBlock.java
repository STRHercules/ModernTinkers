package com.moderntinkers.content.smeltery;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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
import java.util.function.Supplier;

/** Simple persistent 4-bucket tank for molten materials. */
public final class FluidTankBlock extends BaseEntityBlock {
    public static final MapCodec<FluidTankBlock> CODEC = simpleCodec(FluidTankBlock::new);
    private final Supplier<BlockEntityType<?>> typeSupplier;

    public FluidTankBlock(Properties properties) {
        super(properties);
        this.typeSupplier = null;
    }

    public FluidTankBlock(Properties properties, Supplier<BlockEntityType<?>> typeSupplier) {
        super(properties);
        this.typeSupplier = typeSupplier;
    }

    @Override
    public MapCodec<FluidTankBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(net.minecraft.core.BlockPos pos, BlockState state) {
        BlockEntityType<?> type = typeSupplier != null ? typeSupplier.get()
                : !state.is(SmelteryContent.SEARED_TANK.get())
                        && !state.is(SmelteryContent.SCORCHED_TANK.get())
                        ? SmelteryContent.COMPONENT_BLOCK_ENTITY.get()
                : state.is(SmelteryContent.SCORCHED_TANK.get())
                ? SmelteryContent.SCORCHED_TANK_BLOCK_ENTITY.get()
                : SmelteryContent.SEARED_TANK_BLOCK_ENTITY.get();
        return new FluidTankBlockEntity(type, pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : (tickLevel, pos, tickState, blockEntity) ->
                        FluidTankBlockEntity.serverTick(tickLevel, pos, tickState,
                                (FluidTankBlockEntity) blockEntity);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level,
            net.minecraft.core.BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            net.minecraft.core.BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())
                ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void onRemove(BlockState state, Level level, net.minecraft.core.BlockPos pos,
            BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank) {
            FluidContents.dropBuckets(level, pos, tank.getTank());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
