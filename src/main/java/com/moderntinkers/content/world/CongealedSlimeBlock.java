package com.moderntinkers.content.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Thin, non-sticky slime platform with fall protection and a small bounce. */
public final class CongealedSlimeBlock extends Block {
    public CongealedSlimeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity,
                       float fallDistance) {
        entity.causeFallDamage(fallDistance, 0.0F, level.damageSources().fall());
    }

    @Override
    public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        if (entity.isSuppressingBounce()
                || !(entity instanceof LivingEntity) && !(entity instanceof net.minecraft.world.entity.item.ItemEntity)) {
            super.updateEntityAfterFallOn(level, entity);
            return;
        }
        Vec3 movement = entity.getDeltaMovement();
        if (movement.y < 0.0D) {
            double multiplier = entity instanceof LivingEntity ? 1.0D : 0.8D;
            entity.setDeltaMovement(movement.x, -movement.y * multiplier, movement.z);
            entity.fallDistance = 0.0F;
        } else {
            super.updateEntityAfterFallOn(level, entity);
        }
    }
}
