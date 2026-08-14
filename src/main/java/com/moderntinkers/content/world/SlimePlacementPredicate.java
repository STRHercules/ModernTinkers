package com.moderntinkers.content.world;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;

/** Restricts a slime's natural placement to the matching block tag. */
public final class SlimePlacementPredicate<T extends Slime>
        implements SpawnPlacements.SpawnPredicate<T> {
    private final TagKey<Block> tag;

    public SlimePlacementPredicate(TagKey<Block> tag) {
        this.tag = tag;
    }

    @Override
    public boolean test(EntityType<T> type, ServerLevelAccessor level, MobSpawnType reason,
                        BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        return reason == MobSpawnType.SPAWNER || level.getBlockState(pos.below()).is(tag);
    }
}
