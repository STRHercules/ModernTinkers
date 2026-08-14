package com.moderntinkers.content.world;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Crystal cluster with Tinkers' chime feedback for projectile hits. */
public final class CrystalClusterBlock extends AmethystClusterBlock {
    public CrystalClusterBlock(int height, int width, Properties properties) {
        super(height, width, properties);
    }

    @Override
    public void onProjectileHit(Level level, BlockState state, BlockHitResult hit,
                                Projectile projectile) {
        if (!level.isClientSide) {
            BlockPos pos = hit.getBlockPos();
            level.playSound(null, pos, getSoundType(state, level, pos, projectile).getHitSound(),
                    SoundSource.BLOCKS, 1.0F, 0.5F + level.random.nextFloat() * 1.2F);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.BLOCKS, 1.0F, 0.5F + level.random.nextFloat() * 1.2F);
        }
    }
}
