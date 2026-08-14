package com.moderntinkers.content;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Small inventory preflight used by result slots with an all-or-nothing recipe. */
public final class TinkerMenuTransfer {
    private TinkerMenuTransfer() {}

    public static boolean canMoveEntireStack(List<Slot> slots, ItemStack stack,
                                             int start, int end) {
        if (stack.isEmpty()) {
            return false;
        }
        int remaining = stack.getCount();
        int limit = stack.getMaxStackSize();
        for (int index = Math.max(0, start); index < Math.min(end, slots.size()); index++) {
            Slot slot = slots.get(index);
            if (!slot.mayPlace(stack)) {
                continue;
            }
            ItemStack current = slot.getItem();
            if (current.isEmpty()) {
                remaining -= Math.min(limit, slot.getMaxStackSize());
            } else if (ItemStack.isSameItemSameComponents(current, stack)) {
                remaining -= Math.max(0,
                        Math.min(limit, slot.getMaxStackSize()) - current.getCount());
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }
}
