package com.moderntinkers.content.modifier;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Dragon scales are the one modifier material that survives explosions. */
public final class DragonScaleItem extends Item {
    private final String tooltipKey;

    public DragonScaleItem(Properties properties, String tooltipKey) {
        super(properties);
        this.tooltipKey = tooltipKey;
    }

    @Override
    public boolean canBeHurtBy(ItemStack stack, DamageSource source) {
        return !source.is(DamageTypeTags.IS_EXPLOSION);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(tooltipKey));
    }
}
