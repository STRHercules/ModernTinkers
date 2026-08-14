package com.moderntinkers.content;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Small common item type for the reference items that expose one fixed tooltip.
 */
public final class PortTooltipItem extends Item {
    private final String tooltipKey;

    public PortTooltipItem(Properties properties, String tooltipKey) {
        super(properties);
        this.tooltipKey = tooltipKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(tooltipKey));
    }
}
