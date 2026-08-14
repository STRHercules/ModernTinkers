package com.moderntinkers.content.partbuilder;

import com.moderntinkers.content.StaticContent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable Part Builder pattern. Sneak-use cycles through the currently
 * registered tool-part IDs and stores the choice in the modern custom-data
 * component instead of relying on legacy item damage or NBT conventions.
 */
public final class PatternItem extends Item {
    public static final String PART_DATA_KEY = "moderntinkers:part";
    public static final String RECYCLING_BLOCK = "block";
    public static final String RECYCLING_INGOT = "ingot";
    public static final String RECYCLING_CRYSTAL = "crystal";

    public PatternItem(Properties properties) {
        super(properties);
    }

    public static boolean isPattern(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof PatternItem;
    }

    public static String getPart(ItemStack stack) {
        if (!isPattern(stack)) {
            return "";
        }
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getString(PART_DATA_KEY);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return super.use(level, player, hand);
        }

        String nextPart = nextPart(getPart(stack));
        if (nextPart.isEmpty()) {
            return super.use(level, player, hand);
        }
        if (!level.isClientSide) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack,
                    tag -> tag.putString(PART_DATA_KEY, nextPart));
            player.displayClientMessage(Component.translatable(
                    "item.moderntinkers.pattern.selected",
                    Component.translatable("item.moderntinkers." + nextPart)), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        String part = getPart(stack);
        tooltip.add(part.isEmpty()
                ? Component.translatable("item.moderntinkers.pattern.unselected")
                : Component.translatable("item.moderntinkers.pattern.selected",
                        Component.translatable("item.moderntinkers." + part)));
    }

    private static String nextPart(String current) {
        List<String> parts = new ArrayList<>(StaticContent.toolPartIds());
        parts.add(RECYCLING_BLOCK);
        parts.add(RECYCLING_INGOT);
        parts.add(RECYCLING_CRYSTAL);
        if (parts.isEmpty()) {
            return "";
        }
        int index = parts.indexOf(current);
        return parts.get((index + 1 + parts.size()) % parts.size());
    }
}
