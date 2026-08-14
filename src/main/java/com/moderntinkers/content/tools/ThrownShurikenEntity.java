package com.moderntinkers.content.tools;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;

/** Disposable stack-preserving projectile for shurikens and throwing axes. */
public final class ThrownShurikenEntity extends StackProjectileEntity {
    public ThrownShurikenEntity(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide) {
            super.onHitEntity(result);
            return;
        }
        Entity owner = getOwner();
        boolean hit = result.getEntity().hurt(damageSources().thrown(this, owner),
                (float) getBaseDamage());
        if (hit && owner instanceof LivingEntity livingOwner
                && result.getEntity() instanceof LivingEntity livingTarget
                && storedStack().getItem() instanceof TinkersToolItem tool) {
            tool.applyProjectileEffects(storedStack(), livingTarget, livingOwner);
        }
        damageStoredToolOnImpact();
        if (!hit && !isRemoved() && !storedStack().isEmpty()) {
            spawnAtLocation(storedStack().copy());
        }
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        damageStoredToolOnImpact();
        if (!level().isClientSide && !isRemoved() && !storedStack().isEmpty()) {
            spawnAtLocation(storedStack().copy());
            discard();
        }
    }
}
