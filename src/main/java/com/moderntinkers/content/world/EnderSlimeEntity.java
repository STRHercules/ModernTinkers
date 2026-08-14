package com.moderntinkers.content.world;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;

/** Ender slime that teleports its victim on hit and itself after damage. */
public final class EnderSlimeEntity extends TravelersPlateSlimeEntity {
    public EnderSlimeEntity(EntityType<? extends EnderSlimeEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void dealDamage(LivingEntity target) {
        super.dealDamage(target);
        teleportNearby(target);
    }

    @Override
    protected void actuallyHurt(DamageSource source, float amount) {
        float health = getHealth();
        super.actuallyHurt(source, amount);
        if (isAlive() && getHealth() < health) {
            teleportNearby(this);
        }
    }

    private void teleportNearby(LivingEntity target) {
        for (int attempt = 0; attempt < 16; attempt++) {
            double x = target.getX() + (random.nextDouble() - 0.5D) * 16.0D;
            double y = target.getY() + (random.nextInt(8) - 4);
            double z = target.getZ() + (random.nextDouble() - 0.5D) * 16.0D;
            if (target.randomTeleport(x, y, z, true)) {
                return;
            }
        }
    }

    @Override
    protected String platingMaterial() {
        return "knightmetal";
    }
}
