package com.moderntinkers.content.anvil;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Shared anvil shell for the standard and scorched repair stations. */
public final class TinkersAnvilBlock extends BaseEntityBlock {
    public static final MapCodec<TinkersAnvilBlock> CODEC = simpleCodec(
            properties -> new TinkersAnvilBlock(properties, false));

    private final boolean scorched;

    public TinkersAnvilBlock(Properties properties, boolean scorched) {
        super(properties);
        this.scorched = scorched;
    }

    public boolean isScorched() {
        return scorched;
    }

    @Override
    public MapCodec<TinkersAnvilBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(net.minecraft.core.BlockPos pos, BlockState state) {
        return new TinkersAnvilBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level,
            net.minecraft.core.BlockPos pos, Player player, BlockHitResult hit) {
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
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            net.minecraft.core.BlockPos pos, Player player,
            net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = useWithoutItem(state, level, pos, player, hit);
        return result == InteractionResult.CONSUME
                ? ItemInteractionResult.CONSUME : ItemInteractionResult.SUCCESS;
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level,
            net.minecraft.core.BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        return entity instanceof TinkersAnvilBlockEntity anvil ? anvil : null;
    }

    @Override
    public void onRemove(BlockState state, Level level, net.minecraft.core.BlockPos pos,
            BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof TinkersAnvilBlockEntity anvil) {
                Containers.dropContents(level, pos, anvil.getInputInventory());
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
