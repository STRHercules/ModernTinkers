package com.moderntinkers.content.world;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Sky slime that converts falls into a stronger bounce. */
public final class SkySlimeEntity extends TravelersPlateSlimeEntity {
    private double bounceAmount;

    public SkySlimeEntity(EntityType<? extends SkySlimeEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected float getJumpPower() {
        return (float) Math.sqrt(getSize()) * getBlockJumpFactor() / 2.0F;
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, DamageSource source) {
        if (isSuppressingBounce()) {
            return super.causeFallDamage(distance, damageMultiplier * 0.2F, source);
        }
        if (distance > 2.0F) {
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(motion.x / 0.95D, motion.y * -0.9D, motion.z / 0.95D);
            bounceAmount = getDeltaMovement().y;
            fallDistance = 0.0F;
            hasImpulse = true;
            setOnGround(false);
            playSound(SoundEvents.SLIME_JUMP, 1.0F, 1.0F);
        }
        return false;
    }

    @Override
    public void move(MoverType type, Vec3 position) {
        super.move(type, position);
        if (bounceAmount > 0.0D) {
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(motion.x, bounceAmount, motion.z);
            bounceAmount = 0.0D;
        }
    }

    @Override
    protected String platingMaterial() {
        return "steel";
    }
}
