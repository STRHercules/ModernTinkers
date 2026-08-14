package com.moderntinkers.content.tools;

import com.moderntinkers.content.material.MaterialManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/** A reusable part item whose material variant is stored on the stack. */
public final class MaterialPartItem extends Item {
    public static final String MATERIAL_KEY = "moderntinkers:material";
    private final String partId;

    public MaterialPartItem(Properties properties, String partId) {
        super(properties);
        this.partId = partId;
    }

    public String partId() {
        return partId;
    }

    public static boolean isPart(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof MaterialPartItem;
    }

    public static String getPartId(ItemStack stack) {
        return stack.getItem() instanceof MaterialPartItem part ? part.partId : "";
    }

    public static String getMaterial(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getString(MATERIAL_KEY);
    }

    public static ItemStack withMaterial(Item part, String material) {
        ItemStack stack = new ItemStack(part);
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putString(MATERIAL_KEY, material));
        return stack;
    }

    /** Material units recovered when a part is recycled through a melter. */
    public static int materialUnits(ItemStack stack) {
        return partUnits(getPartId(stack));
    }

    /** Material units represented by a part ID when it is cast or recycled. */
    public static int partUnits(String partId) {
        return switch (partId) {
            case "repair_kit", "fake_ingot", "pick_head", "small_axe_head", "small_blade",
                    "bow_grip", "bowstring", "arrow_head", "arrow_shaft", "fletching",
                    "tool_binding", "tool_handle", "plating_helmet", "plating_chestplate",
                    "plating_leggings", "plating_boots", "maille", "laces", "slime" -> 9;
            case "hammer_head", "broad_blade", "adze_head", "bow_limb", "tough_binding",
                    "tough_handle" -> 18;
            case "broad_axe_head" -> 27;
            case "ribcage" -> 36;
            case "skull" -> 36;
            case "large_plate", "shell" -> 72;
            case "shield_core" -> 27;
            default -> 0;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        String material = getMaterial(stack);
        if (!material.isEmpty() && MaterialManager.get(material) != null) {
            tooltip.add(Component.translatable("item.moderntinkers.part.material",
                    Component.translatable("material.moderntinkers." + material)));
        }
    }
}
