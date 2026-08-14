package com.moderntinkers.content.modifier;

import com.moderntinkers.content.StaticContent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/** A crystal optionally carrying one registered modifier ID. */
public final class ModifierCrystalItem extends Item {
    public static final String MODIFIER_KEY = "moderntinkers:modifier_id";

    public ModifierCrystalItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isConfigured(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        String modifier = getModifier(stack);
        return modifier.isEmpty()
                ? super.getName(stack)
                : Component.translatable("item.moderntinkers.modifier_crystal.format",
                        Component.translatable("item.moderntinkers." + modifier));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        String modifier = getModifier(stack);
        if (modifier.isEmpty()) {
            tooltip.add(Component.translatable("item.moderntinkers.modifier_crystal.empty"));
        } else {
            tooltip.add(Component.translatable("item.moderntinkers.modifier_crystal.apply",
                    Component.translatable("item.moderntinkers." + modifier)));
        }
    }

    public static boolean isCrystal(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ModifierCrystalItem;
    }

    public static boolean isConfigured(ItemStack stack) {
        return !getModifier(stack).isEmpty();
    }

    /** Returns a known modifier ID, or an empty string for an unconfigured crystal. */
    public static String getModifier(ItemStack stack) {
        if (!isCrystal(stack)) {
            return "";
        }
        String id = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getString(MODIFIER_KEY);
        return ModifierManager.isKnown(id) ? id : "";
    }

    public static ItemStack withModifier(String modifierId) {
        return withModifier(StaticContent.modifierItem("modifier_crystal").get(), modifierId, 1);
    }

    public static ItemStack withModifier(Item item, String modifierId) {
        return withModifier(item, modifierId, 1);
    }

    public static ItemStack withModifier(Item item, String modifierId, int count) {
        if (item == null || !ModifierManager.isKnown(modifierId)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item, Math.max(1, count));
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putString(MODIFIER_KEY, modifierId));
        return stack;
    }
}
