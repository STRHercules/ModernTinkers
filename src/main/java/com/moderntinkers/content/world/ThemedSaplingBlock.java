package com.moderntinkers.content.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/** Small self-contained tree grower for the port's four slimewood families. */
public final class ThemedSaplingBlock extends SaplingBlock {
    private final Supplier<Block> log;
    private final Supplier<Block> leaves;

    public ThemedSaplingBlock(Properties properties, Supplier<Block> log,
                              Supplier<Block> leaves) {
        super(TreeGrower.OAK, properties);
        this.log = log;
        this.leaves = leaves;
    }

    @Override
    public void advanceTree(ServerLevel level, BlockPos pos, BlockState state,
                            RandomSource random) {
        if (!level.getBlockState(pos).is(this)) {
            return;
        }
        for (int y = 0; y < 4; y++) {
            if (!replaceable(level.getBlockState(pos.above(y)))) {
                return;
            }
        }
        BlockState logState = log.get().defaultBlockState();
        BlockState leafState = leaves.get().defaultBlockState();
        if (leafState.hasProperty(LeavesBlock.PERSISTENT)) {
            leafState = leafState.setValue(LeavesBlock.PERSISTENT, true);
        }
        level.setBlock(pos, logState, Block.UPDATE_ALL);
        for (int y = 1; y < 4; y++) {
            level.setBlock(pos.above(y), logState, Block.UPDATE_ALL);
        }
        for (int y = 2; y <= 4; y++) {
            int radius = y == 4 ? 1 : 2;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) + Math.abs(z) > radius + 1) {
                        continue;
                    }
                    BlockPos leafPos = pos.offset(x, y, z);
                    if (replaceable(level.getBlockState(leafPos))) {
                        level.setBlock(leafPos, leafState, Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos,
                                BlockState state) {
        advanceTree(level, pos, state, random);
    }

    private static boolean replaceable(BlockState state) {
        return state.isAir() || state.canBeReplaced();
    }
}
