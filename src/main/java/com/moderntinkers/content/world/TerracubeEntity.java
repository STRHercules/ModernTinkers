package com.moderntinkers.content.world;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/** Earth slime with water-compatible spawning, no fall damage, and helmets. */
public final class TerracubeEntity extends ArmoredSlimeEntity {
    public TerracubeEntity(EntityType<? extends TerracubeEntity> type, Level level) {
        super(type, level);
    }

    public static boolean canSpawnHere(EntityType<? extends Slime> type,
                                       ServerLevelAccessor level, MobSpawnType reason,
                                       BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (reason == MobSpawnType.SPAWNER) {
            return true;
        }
        BlockPos below = pos.below();
        if (level.getFluidState(pos).is(FluidTags.WATER)
                && level.getFluidState(below).is(FluidTags.WATER)) {
            return true;
        }
        return level.getBlockState(below).isValidSpawn(level, below, type)
                && Monster.isDarkEnoughToSpawn(level, pos, random);
    }

    @Override
    protected float getJumpPower() {
        return 0.5F * getBlockJumpFactor();
    }

    @Override
    protected float getAttackDamage() {
        return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) + 2.0F;
    }

    @Override
    protected int calculateFallDamage(float distance, float multiplier) {
        return 0;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        if (random.nextFloat() >= 0.15F * difficulty.getSpecialMultiplier()
                || !getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            return;
        }
        int armorQuality = random.nextInt(3);
        if (random.nextFloat() < 0.25F) armorQuality++;
        if (random.nextFloat() < 0.25F) armorQuality++;
        if (random.nextFloat() < 0.25F) armorQuality++;
        Item item = armorQuality == 5
                ? Items.TURTLE_HELMET : getEquipmentForSlot(EquipmentSlot.HEAD, armorQuality);
        if (item != null) {
            setItemSlot(EquipmentSlot.HEAD, new ItemStack(item));
        }
    }
}
