package com.moderntinkers.content.tools;

import com.moderntinkers.ModernTinkers;
import com.moderntinkers.content.material.MaterialDefinition;
import com.moderntinkers.content.material.MaterialManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/** Stackable, material-aware arrow assembled from three Tinker parts. */
public final class TinkersArrowItem extends ArrowItem {
    public static final String HEAD_KEY = "moderntinkers:arrow_head_material";
    public static final String SHAFT_KEY = "moderntinkers:arrow_shaft_material";
    public static final String FLETCHING_KEY = "moderntinkers:fletching_material";

    public TinkersArrowItem(Properties properties) {
        super(properties.stacksTo(64));
    }

    public static boolean isAssembled(ItemStack stack) {
        if (!(stack.getItem() instanceof TinkersArrowItem)) {
            return false;
        }
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return MaterialManager.get(tag.getString(HEAD_KEY)) != null
                && MaterialManager.get(tag.getString(SHAFT_KEY)) != null
                && MaterialManager.get(tag.getString(FLETCHING_KEY)) != null;
    }

    public static List<TinkersToolItem.MaterialAmount> meltingMaterials(ItemStack stack) {
        return meltingMaterials(stack, Math.max(1, stack.getCount()));
    }

    public static List<TinkersToolItem.MaterialAmount> meltingMaterials(ItemStack stack,
                                                                         int itemCount) {
        if (!isAssembled(stack)) {
            return List.of();
        }
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        List<TinkersToolItem.MaterialAmount> result = new java.util.ArrayList<>();
        int count = Math.max(1, itemCount);
        add(result, tag.getString(HEAD_KEY), "arrow_head", count);
        add(result, tag.getString(SHAFT_KEY), "arrow_shaft", count);
        add(result, tag.getString(FLETCHING_KEY), "fletching", count);
        return result;
    }

    private static void add(List<TinkersToolItem.MaterialAmount> result, String material,
                            String partId, int count) {
        if (!material.isEmpty()) {
            result.add(new TinkersToolItem.MaterialAmount(material,
                    MaterialPartItem.partUnits(partId) * count));
        }
    }

    public static ItemStack assemble(Item item, ItemStack head, ItemStack shaft,
                                     ItemStack fletching) {
        if (!(item instanceof TinkersArrowItem)
                || !MaterialPartItem.isPart(head)
                || !MaterialPartItem.isPart(shaft)
                || !MaterialPartItem.isPart(fletching)) {
            return ItemStack.EMPTY;
        }
        String headMaterial = MaterialPartItem.getMaterial(head);
        String shaftMaterial = MaterialPartItem.getMaterial(shaft);
        String fletchingMaterial = MaterialPartItem.getMaterial(fletching);
        MaterialDefinition headDefinition = MaterialManager.get(headMaterial);
        MaterialDefinition shaftDefinition = MaterialManager.get(shaftMaterial);
        MaterialDefinition fletchingDefinition = MaterialManager.get(fletchingMaterial);
        if (headDefinition == null || shaftDefinition == null || fletchingDefinition == null
                || headMaterial.isEmpty() || shaftMaterial.isEmpty() || fletchingMaterial.isEmpty()
                || !MaterialPartItem.getPartId(head).equals("arrow_head")
                || !MaterialPartItem.getPartId(shaft).equals("arrow_shaft")
                || !MaterialPartItem.getPartId(fletching).equals("fletching")) {
            return ItemStack.EMPTY;
        }
        ItemStack result = new ItemStack(item, 16);
        CustomData.update(DataComponents.CUSTOM_DATA, result, tag -> {
            tag.putString(HEAD_KEY, headMaterial);
            tag.putString(SHAFT_KEY, shaftMaterial);
            tag.putString(FLETCHING_KEY, fletchingMaterial);
        });
        return result;
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter,
                                     ItemStack weapon) {
        Arrow arrow = new Arrow(level, shooter, stack, weapon);
        String material = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getString(HEAD_KEY);
        MaterialDefinition definition = isAssembled(stack) ? MaterialManager.get(material) : null;
        if (definition != null) {
            arrow.setBaseDamage(arrow.getBaseDamage() + definition.headAttackDamage() * 0.25D);
        }
        return arrow;
    }

    @Override
    public Component getName(ItemStack stack) {
        String material = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getString(HEAD_KEY);
        return material.isEmpty() ? super.getName(stack)
                : Component.translatable("item.moderntinkers.arrow_name",
                        Component.translatable("material.moderntinkers." + material));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        String material = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getString(HEAD_KEY);
        if (!material.isEmpty()) {
            tooltip.add(Component.translatable("item.moderntinkers.part.material",
                    Component.translatable("material.moderntinkers." + material)));
        }
    }
}
