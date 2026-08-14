package com.moderntinkers.content.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import java.util.function.Supplier;

/** Budding block that grows this geode's crystal family instead of vanilla amethyst. */
public final class BuddingCrystalBlock extends Block {
    private static final Direction[] DIRECTIONS = Direction.values();

    private final Supplier<? extends Block> smallBud;
    private final Supplier<? extends Block> mediumBud;
    private final Supplier<? extends Block> largeBud;
    private final Supplier<? extends Block> cluster;

    public BuddingCrystalBlock(Properties properties, Supplier<? extends Block> smallBud,
                               Supplier<? extends Block> mediumBud,
                               Supplier<? extends Block> largeBud,
                               Supplier<? extends Block> cluster) {
        super(properties);
        this.smallBud = smallBud;
        this.mediumBud = mediumBud;
        this.largeBud = largeBud;
        this.cluster = cluster;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos,
                           RandomSource random) {
        if (random.nextInt(5) != 0) {
            return;
        }
        Direction direction = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
        BlockPos targetPos = pos.relative(direction);
        BlockState targetState = level.getBlockState(targetPos);
        Block target = null;
        if (BuddingAmethystBlock.canClusterGrowAtState(targetState)) {
            target = smallBud.get();
        } else if (targetState.is(smallBud.get())) {
            target = mediumBud.get();
        } else if (targetState.is(mediumBud.get())) {
            target = largeBud.get();
        } else if (targetState.is(largeBud.get())) {
            target = cluster.get();
        }
        if (target != null) {
            BlockState budState = target.defaultBlockState()
                    .setValue(AmethystClusterBlock.FACING, direction)
                    .setValue(AmethystClusterBlock.WATERLOGGED,
                            targetState.getFluidState().getType() == Fluids.WATER);
            level.setBlockAndUpdate(targetPos, budState);
        }
    }
}
