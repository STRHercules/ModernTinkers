package com.moderntinkers.content.tools;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.Level;

/** Common stack synchronization and pickup persistence for Tinker projectiles. */
public abstract class StackProjectileEntity extends AbstractArrow {
    private static final EntityDataAccessor<ItemStack> STACK =
            SynchedEntityData.defineId(StackProjectileEntity.class,
                    EntityDataSerializers.ITEM_STACK);
    private ItemStack stack = ItemStack.EMPTY;
    private boolean impactDamaged;

    protected StackProjectileEntity(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
    }

    public void initialize(ItemStack stack, LivingEntity shooter) {
        this.stack = stack.copyWithCount(1);
        this.entityData.set(STACK, this.stack);
        setOwner(shooter);
        setPos(shooter.getX(), shooter.getEyeY() - 0.1D, shooter.getZ());
    }

    public ItemStack getDisplayStack() {
        return entityData.get(STACK);
    }

    protected ItemStack storedStack() {
        return stack;
    }

    /**
     * Thrown tools spend durability when they hit, not when they leave the
     * player's hand.  Keeping the guard here prevents entity and block hit
     * callbacks from damaging the same stack twice.
     */
    protected final void damageStoredToolOnImpact() {
        if (impactDamaged || stack.isEmpty()
                || !(stack.getItem() instanceof TinkersToolItem tool)) {
            return;
        }
        impactDamaged = true;
        LivingEntity owner = getOwner() instanceof LivingEntity living ? living : null;
        tool.damageTool(stack, 1, owner, EquipmentSlot.MAINHAND);
        entityData.set(STACK, stack, true);
    }

    @Override
    public ItemStack getPickupItem() {
        return stack.copy();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STACK, ItemStack.EMPTY);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("ImpactDamaged", impactDamaged);
        if (!stack.isEmpty()) {
            tag.put("Stack", stack.save(registryAccess()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        impactDamaged = tag.getBoolean("ImpactDamaged");
        if (tag.contains("Stack", CompoundTag.TAG_COMPOUND)) {
            stack = ItemStack.parse(registryAccess(), tag.getCompound("Stack"))
                    .orElse(ItemStack.EMPTY);
            entityData.set(STACK, stack);
        }
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return stack.copy();
    }
}
