package com.moderntinkers.content.world;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;

import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.List;

/** Shared metal-core, equipment, and split handling for ported slime mobs. */
public abstract class ArmoredSlimeEntity extends Slime {
    private static final EntityDataAccessor<Boolean> METAL = SynchedEntityData.defineId(
            ArmoredSlimeEntity.class, EntityDataSerializers.BOOLEAN);
    public static final String TAG_METAL = "metal";

    protected ArmoredSlimeEntity(EntityType<? extends ArmoredSlimeEntity> type, Level level) {
        super(type, level);
        if (!level.isClientSide) {
            tryAddAttribute(Attributes.ARMOR, "armor", 3.0D);
            tryAddAttribute(Attributes.ARMOR_TOUGHNESS, "toughness", 3.0D);
            tryAddAttribute(Attributes.KNOCKBACK_RESISTANCE, "knockback", 3.0D);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(METAL, false);
    }

    protected final void setMetal(boolean metal) {
        entityData.set(METAL, metal);
    }

    public final boolean isMetal() {
        return entityData.get(METAL);
    }

    private void tryAddAttribute(Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                 String id, double amount) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.addTransientModifier(new AttributeModifier(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                            "moderntinkers", id), amount,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData);
        setCanPickUpLoot(random.nextFloat() < 0.55F * difficulty.getSpecialMultiplier());
        populateDefaultEquipmentSlots(random, difficulty);
        if (getItemBySlot(EquipmentSlot.HEAD).isEmpty()
                && LocalDate.now().get(ChronoField.MONTH_OF_YEAR) == 10
                && LocalDate.now().get(ChronoField.DAY_OF_MONTH) == 31
                && random.nextFloat() < 0.25F) {
            setItemSlot(EquipmentSlot.HEAD, new ItemStack(
                    random.nextFloat() < 0.1F ? Blocks.JACK_O_LANTERN : Blocks.CARVED_PUMPKIN));
            setDropChance(EquipmentSlot.HEAD, 0.0F);
        }
        return result;
    }

    protected abstract void populateDefaultEquipmentSlots(RandomSource random,
                                                           DifficultyInstance difficulty);

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return List.of(getItemBySlot(EquipmentSlot.HEAD));
    }

    @Override
    public boolean canHoldItem(ItemStack stack) {
        return getEquipmentSlotForItem(stack) == EquipmentSlot.HEAD;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source,
                                       boolean recentlyHit) {
        ItemStack stack = getItemBySlot(EquipmentSlot.HEAD);
        float chance = getEquipmentDropChance(EquipmentSlot.HEAD);
        if (chance > 0.25F && getSize() > 1) {
            chance = 0.25F;
        }
        boolean alwaysDrop = chance > 1.0F;
        if (!stack.isEmpty() && (recentlyHit || alwaysDrop)
                && random.nextFloat() < chance) {
            if (!alwaysDrop && stack.isDamageableItem()) {
                int max = stack.getMaxDamage();
                stack.setDamageValue(max - random.nextInt(
                        1 + random.nextInt(Math.max(max - 3, 1))));
            }
            spawnAtLocation(stack);
            setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        int size = getSize();
        if (!level().isClientSide && size > 1 && isDeadOrDying()) {
            Component name = getCustomName();
            boolean noAi = isNoAi();
            boolean invulnerable = isInvulnerable();
            float offset = size / 4.0F;
            int childSize = size / 2;
            int count = 2 + random.nextInt(3);
            ItemStack helmet = getItemBySlot(EquipmentSlot.HEAD);
            int helmetIndex = helmet.isEmpty() ? -1 : random.nextInt(count);
            float dropChance = getEquipmentDropChance(EquipmentSlot.HEAD);
            for (int index = 0; index < count; index++) {
                float x = (index % 2 - 0.5F) * offset;
                float z = (index / 2 - 0.5F) * offset;
                if (!(getType().create(level()) instanceof ArmoredSlimeEntity child)) {
                    continue;
                }
                if (isPersistenceRequired()) {
                    child.setPersistenceRequired();
                }
                child.setCustomName(name);
                child.setNoAi(noAi);
                child.setInvulnerable(invulnerable);
                child.setSize(childSize, true);
                child.setMetal(isMetal());
                if (index == helmetIndex) {
                    child.setItemSlot(EquipmentSlot.HEAD, helmet.copy());
                    setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                } else if (dropChance < 1.0F && random.nextFloat() < 0.25F) {
                    child.setItemSlot(EquipmentSlot.HEAD, helmet.copy());
                }
                child.moveTo(getX() + x, getY() + 0.5D, getZ() + z,
                        random.nextFloat() * 360.0F, 0.0F);
                level().addFreshEntity(child);
            }
        }
        setRemoved(reason);
        if (reason == Entity.RemovalReason.KILLED) {
            gameEvent(GameEvent.ENTITY_DIE);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(TAG_METAL, isMetal());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setMetal(tag.getBoolean(TAG_METAL));
    }
}
