package com.moderntinkers.content.tools;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;

/** Stack-preserving projectile for javelins and other thrown tools. */
public final class ThrownToolEntity extends StackProjectileEntity {
    public ThrownToolEntity(EntityType<? extends AbstractArrow> type, Level level) {
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
        // Javelins are reusable: a successful hit returns the damaged stack as
        // well as a miss or block hit.  Losing it only when durability breaks
        // matches the reference thrown-tool contract.
        if (!isRemoved() && !storedStack().isEmpty()) {
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
