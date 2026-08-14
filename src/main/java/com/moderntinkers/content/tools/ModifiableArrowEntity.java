package com.moderntinkers.content.tools;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/** Arrow entity retaining the exact ammo stack used to create it. */
public final class ModifiableArrowEntity extends StackProjectileEntity {
    private ItemStack weapon = ItemStack.EMPTY;

    public ModifiableArrowEntity(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
    }

    public void initialize(ItemStack ammo, ItemStack weapon, LivingEntity shooter) {
        initialize(ammo, shooter);
        this.weapon = weapon.copyWithCount(1);
        applyAmmoStats(ammo);
    }

    private void applyAmmoStats(ItemStack ammo) {
        if (TinkersArrowItem.isAssembled(ammo)) {
            String material = TinkersToolItem.material(ammo, TinkersArrowItem.HEAD_KEY);
            var definition = com.moderntinkers.content.material.MaterialManager.get(material);
            if (definition != null) {
                setBaseDamage(getBaseDamage() + definition.headAttackDamage() * 0.25D);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide && result.getEntity() instanceof LivingEntity target
                && getOwner() instanceof LivingEntity attacker
                && weapon.getItem() instanceof TinkersToolItem tool) {
            tool.applyProjectileEffects(weapon, target, attacker);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (!weapon.isEmpty()) {
            tag.put("Weapon", weapon.save(registryAccess()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Weapon", CompoundTag.TAG_COMPOUND)) {
            weapon = ItemStack.parse(registryAccess(), tag.getCompound("Weapon"))
                    .orElse(ItemStack.EMPTY);
        }
    }
}
