package com.moderntinkers.content.world;

import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiPredicate;

/** Slime block with the reference's explicit adhesion rule. */
public final class StickySlimeBlock extends SlimeBlock {
    private final BiPredicate<BlockState, BlockState> stickyPredicate;

    public StickySlimeBlock(Properties properties,
                            BiPredicate<BlockState, BlockState> stickyPredicate) {
        super(properties);
        this.stickyPredicate = stickyPredicate;
    }

    @Override
    public boolean isSlimeBlock(BlockState state) {
        return true;
    }

    @Override
    public boolean isStickyBlock(BlockState state) {
        return true;
    }

    @Override
    public boolean canStickTo(BlockState state, BlockState other) {
        return stickyPredicate.test(state, other);
    }
}
