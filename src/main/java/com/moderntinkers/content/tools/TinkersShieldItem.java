package com.moderntinkers.content.tools;

import com.moderntinkers.ModernTinkers;
import com.moderntinkers.content.material.MaterialDefinition;
import com.moderntinkers.content.material.MaterialManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

import java.util.List;

/** Native ShieldItem blocking with a material-aware durability payload. */
public final class TinkersShieldItem extends ShieldItem {
    public static final String MATERIAL_KEY = "moderntinkers:shield_material";
    public static final String HANDLE_KEY = "moderntinkers:shield_handle_material";
    /** Stored separately because the reference plate shield has no plating_shield part item. */
    public static final String CORE_KEY = "moderntinkers:shield_core_material";
    public static final String PLATING_KEY = "moderntinkers:shield_plating_material";
    private static final int PLATE_PLATING_UNITS = 27;

    public TinkersShieldItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** Held-item fallback for filling and firing a shield tank. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (TinkersToolItem.hasModifier(stack, "spilling")
                || TinkersToolItem.hasModifier(stack, "spitting")) {
            InteractionResult bucket = TinkersToolItem.useBucketInteraction(stack, player, hand);
            if (bucket.consumesAction()) {
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
            if (TinkersToolItem.hasModifier(stack, "spitting")
                    && !TinkersToolItem.toolFluid(stack).isEmpty()
                    && !player.isShiftKeyDown()) {
                player.startUsingItem(hand);
                return InteractionResultHolder.consume(stack);
            }
        }
        return super.use(level, player, hand);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return TinkersToolItem.hasModifier(stack, "spitting") ? 72000
                : super.getUseDuration(stack, entity);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return TinkersToolItem.hasModifier(stack, "spitting") ? UseAnim.BOW
                : super.getUseAnimation(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (!level.isClientSide && living instanceof Player player
                && TinkersToolItem.hasModifier(stack, "spitting")) {
            TinkersToolItem.spitFluid(stack, player, player.getUsedItemHand(),
                    getUseDuration(stack, player) - timeLeft);
        }
    }

    public static List<TinkersToolItem.MaterialAmount> meltingMaterials(ItemStack stack) {
        if (!(stack.getItem() instanceof TinkersShieldItem)) {
            return List.of();
        }
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String core = tag.getString(MATERIAL_KEY);
        if (core.isEmpty()) {
            return List.of();
        }
        List<TinkersToolItem.MaterialAmount> result = new java.util.ArrayList<>();
        String coreMaterial = tag.getString(CORE_KEY);
        if (!coreMaterial.isEmpty()) {
            result.add(new TinkersToolItem.MaterialAmount(coreMaterial,
                    MaterialPartItem.partUnits("shield_core")));
            result.add(new TinkersToolItem.MaterialAmount(core,
                    PLATE_PLATING_UNITS));
        } else {
            result.add(new TinkersToolItem.MaterialAmount(core,
                    MaterialPartItem.partUnits("shield_core")));
        }
        String handle = tag.getString(HANDLE_KEY);
        if (!handle.isEmpty()) {
            result.add(new TinkersToolItem.MaterialAmount(handle,
                    MaterialPartItem.partUnits("tool_handle")));
        }
        return result;
    }

    public static List<String> repairMaterials(ItemStack stack) {
        if (!(stack.getItem() instanceof TinkersShieldItem)) {
            return List.of();
        }
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        java.util.LinkedHashSet<String> materials = new java.util.LinkedHashSet<>();
        for (String key : List.of(MATERIAL_KEY, CORE_KEY, PLATING_KEY, HANDLE_KEY)) {
            String material = tag.getString(key);
            if (!material.isEmpty()) {
                materials.add(material);
            }
        }
        return List.copyOf(materials);
    }

    public static ItemStack assemble(Item item, ItemStack core, ItemStack handle) {
        String materialId = MaterialPartItem.getMaterial(core);
        MaterialDefinition definition = MaterialManager.get(materialId);
        if (definition == null || !MaterialPartItem.getPartId(core).equals("shield_core")) {
            return ItemStack.EMPTY;
        }
        if (!handle.isEmpty() && (!MaterialPartItem.getPartId(handle).equals("tool_handle")
                && !MaterialPartItem.getPartId(handle).equals("tough_handle")
                || MaterialManager.get(MaterialPartItem.getMaterial(handle)) == null)) {
            return ItemStack.EMPTY;
        }
        ItemStack result = new ItemStack(item);
        CustomData.update(DataComponents.CUSTOM_DATA, result, tag -> {
            tag.putString(MATERIAL_KEY, materialId);
            String handleMaterial = MaterialPartItem.getMaterial(handle);
            if (!handleMaterial.isEmpty()) {
                tag.putString(HANDLE_KEY, handleMaterial);
            }
            tag.putInt(TinkersToolItem.MODIFIER_SLOTS_KEY, TinkersToolItem.DEFAULT_MODIFIER_SLOTS);
        });
        result.set(DataComponents.MAX_DAMAGE, Math.max(1, definition.headDurability()));
        result.set(DataComponents.DAMAGE, 0);
        result.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR, new AttributeModifier(
                        ResourceLocation.fromNamespaceAndPath(ModernTinkers.MOD_ID,
                                "shield_armor"),
                        Math.max(1.0F, definition.headAttackDamage()),
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.OFFHAND)
                .build());
        return result;
    }

    /** Builds the large plate shield using shield_core plus a cast plating input. */
    public static ItemStack assemblePlate(Item item, ItemStack core, ItemStack plating) {
        String coreMaterial = MaterialPartItem.getMaterial(core);
        String platingMaterial = MaterialPartItem.getMaterial(plating);
        if (!"shield_core".equals(MaterialPartItem.getPartId(core))
                || !"large_plate".equals(MaterialPartItem.getPartId(plating))
                || MaterialManager.get(coreMaterial) == null
                || MaterialManager.get(platingMaterial) == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = new ItemStack(item);
        CustomData.update(DataComponents.CUSTOM_DATA, result, tag -> {
            tag.putString(MATERIAL_KEY, platingMaterial);
            tag.putString(PLATING_KEY, platingMaterial);
            tag.putString(CORE_KEY, coreMaterial);
            tag.putInt(TinkersToolItem.MODIFIER_SLOTS_KEY, TinkersToolItem.DEFAULT_MODIFIER_SLOTS);
        });
        MaterialDefinition platingDefinition = MaterialManager.get(platingMaterial);
        result.set(DataComponents.MAX_DAMAGE, Math.max(1, platingDefinition.headDurability()));
        result.set(DataComponents.DAMAGE, 0);
        result.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR, new AttributeModifier(
                        ResourceLocation.fromNamespaceAndPath(ModernTinkers.MOD_ID,
                                "plate_shield_armor"),
                        Math.max(2.0F, platingDefinition.headAttackDamage() * 2.0F),
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.OFFHAND)
                .build());
        return result;
    }

    public static void refreshDerivedComponents(ItemStack stack) {
        if (!(stack.getItem() instanceof TinkersShieldItem)) {
            return;
        }
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String material = tag.getString(MATERIAL_KEY);
        String platingMaterial = tag.getString(PLATING_KEY);
        boolean plate = !platingMaterial.isEmpty() || !tag.getString(CORE_KEY).isEmpty();
        if (plate && !platingMaterial.isEmpty()) {
            material = platingMaterial;
        }
        MaterialDefinition definition = MaterialManager.get(material);
        if (definition == null) {
            return;
        }
        int durability = Math.max(1, definition.headDurability());
        int reinforcement = TinkersToolItem.modifierLevel(stack, "emerald_reinforcement");
        if (reinforcement > 0) {
            durability += Math.round(durability * 0.20F * reinforcement);
        }
        stack.set(DataComponents.MAX_DAMAGE, durability);
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR, new AttributeModifier(
                        ResourceLocation.fromNamespaceAndPath(ModernTinkers.MOD_ID,
                                plate ? "plate_shield_armor" : "shield_armor"),
                        plate ? Math.max(2.0F, definition.headAttackDamage() * 2.0F)
                                : Math.max(1.0F, definition.headAttackDamage()),
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.OFFHAND)
                .build());
    }

    @Override
    public Component getName(ItemStack stack) {
        String material = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getString(MATERIAL_KEY);
        return material.isEmpty() ? super.getName(stack)
                : Component.translatable("item.moderntinkers.armor_name",
                        Component.translatable("material.moderntinkers." + material),
                        Component.translatable("item.moderntinkers.shield"));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        String material = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getString(MATERIAL_KEY);
        if (!material.isEmpty()) {
            tooltip.add(Component.translatable("item.moderntinkers.armor.material",
                    Component.translatable("material.moderntinkers." + material)));
        }
        tooltip.add(Component.translatable("item.moderntinkers.tool.modifier_slots",
                TinkersToolItem.modifierSlotsUsed(stack), TinkersToolItem.modifierSlots(stack)));
        int overslimeCapacity = TinkersToolItem.overslimeCapacity(stack);
        if (overslimeCapacity > 0) {
            tooltip.add(Component.translatable("item.moderntinkers.tool.overslime",
                    TinkersToolItem.overslimeAmount(stack), overslimeCapacity));
        }
    }
}
