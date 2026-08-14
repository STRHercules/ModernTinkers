package com.moderntinkers.content.world;

import com.moderntinkers.content.StaticContent;
import com.moderntinkers.content.tools.MaterialPartItem;
import com.moderntinkers.content.tools.TinkersArmorItem;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Slime variant that can spawn with a material-colored Tinkers helmet. */
public abstract class TravelersPlateSlimeEntity extends ArmoredSlimeEntity {
    protected TravelersPlateSlimeEntity(EntityType<? extends TravelersPlateSlimeEntity> type,
                                         Level level) {
        super(type, level);
    }

    protected abstract String platingMaterial();

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        float multiplier = difficulty.getSpecialMultiplier();
        if (random.nextFloat() < 0.15F) {
            setMetal(true);
        }
        if (random.nextFloat() >= 0.15F * multiplier) {
            return;
        }
        boolean plate = random.nextFloat() < 0.35F * multiplier;
        TinkersArmorItem.Family family = plate
                ? TinkersArmorItem.Family.PLATE : TinkersArmorItem.Family.TRAVELERS;
        ItemStack plating = MaterialPartItem.withMaterial(
                StaticContent.toolPart("plating_helmet").get(), platingMaterial());
        ItemStack maille = plate
                ? MaterialPartItem.withMaterial(StaticContent.toolPart("maille").get(), "iron")
                : ItemStack.EMPTY;
        ItemStack helmet = TinkersArmorItem.assemble(
                StaticContent.armor(family, ArmorItem.Type.HELMET).get(),
                ArmorItem.Type.HELMET, plating, maille);
        if (!helmet.isEmpty()) {
            setItemSlot(EquipmentSlot.HEAD, helmet);
        }
    }
}
