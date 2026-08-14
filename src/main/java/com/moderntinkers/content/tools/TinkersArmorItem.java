package com.moderntinkers.content.tools;

import com.moderntinkers.ModernTinkers;
import com.moderntinkers.content.material.MaterialDefinition;
import com.moderntinkers.content.material.MaterialManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Native armor and Elytra behavior with validated material components. */
public final class TinkersArmorItem extends ArmorItem {
    public static final String MATERIAL_KEY = "moderntinkers:armor_material";
    public static final String MAILLE_KEY = "moderntinkers:maille_material";
    public static final String SLIME_KEY = "moderntinkers:slime_material";
    public static final String MODIFIERS_KEY = TinkersToolItem.MODIFIERS_KEY;

    private final Family family;

    public TinkersArmorItem(Properties properties, Type type) {
        this(properties, type, Family.TINKERS);
    }

    public TinkersArmorItem(Properties properties, Type type, Family family) {
        super(ArmorMaterials.IRON, type, properties.stacksTo(1));
        this.family = family;
    }

    public Family family() {
        return family;
    }

    public boolean isWings() {
        return family == Family.WINGS;
    }

    @Override
    public boolean makesPiglinsNeutral(ItemStack stack, LivingEntity wearer) {
        return "gold".equals(material(stack)) || super.makesPiglinsNeutral(stack, wearer);
    }

    @Override
    public boolean canWalkOnPowderedSnow(ItemStack stack, LivingEntity wearer) {
        return getType() == Type.BOOTS
                && (TinkersToolItem.hasModifier(stack, "snow_boots")
                || MaterialManager.hasTrait(material(stack), "snow_boots")
                || MaterialManager.hasTrait(customTag(stack).getString(MAILLE_KEY), "snow_boots"));
    }

    @Override
    public boolean isEnderMask(ItemStack stack, Player player, EnderMan enderman) {
        return getType() == Type.HELMET
                && (MaterialManager.hasTrait(material(stack), "endermask")
                || super.isEnderMask(stack, player, enderman));
    }

    /** Held-item fallback for filling and firing tanks on armor pieces. */
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

    public static boolean isArmor(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof TinkersArmorItem;
    }

    public static List<TinkersToolItem.MaterialAmount> meltingMaterials(ItemStack stack) {
        if (!isArmor(stack)) {
            return List.of();
        }
        TinkersArmorItem armor = (TinkersArmorItem) stack.getItem();
        String primary = material(stack);
        if (primary.isEmpty() || MaterialManager.get(primary) == null) {
            return List.of();
        }
        List<TinkersToolItem.MaterialAmount> result = new ArrayList<>();
        if (armor.isWings()) {
            result.add(new TinkersToolItem.MaterialAmount(primary, 90));
            return result;
        }
        int primaryUnits = MaterialPartItem.partUnits(armor.primaryPart());
        if (primaryUnits <= 0) {
            return List.of();
        }
        result.add(new TinkersToolItem.MaterialAmount(primary, primaryUnits));
        var tag = customTag(stack);
        if (armor.family == Family.SLIME) {
            addMaterial(result, tag.getString(SLIME_KEY), MaterialPartItem.partUnits("slime"));
        } else {
            addMaterial(result, tag.getString(MAILLE_KEY), MaterialPartItem.partUnits("maille"));
        }
        return result;
    }

    private static void addMaterial(List<TinkersToolItem.MaterialAmount> result,
                                     String material, int units) {
        if (!material.isEmpty() && MaterialManager.get(material) != null && units > 0) {
            result.add(new TinkersToolItem.MaterialAmount(material, units));
        }
    }

    public static String material(ItemStack stack) {
        return customTag(stack).getString(MATERIAL_KEY);
    }

    public static String slimeMaterial(ItemStack stack) {
        return customTag(stack).getString(SLIME_KEY);
    }

    public static List<String> repairMaterials(ItemStack stack) {
        if (!isArmor(stack)) {
            return List.of();
        }
        LinkedHashSet<String> materials = new LinkedHashSet<>();
        addRepairMaterial(materials, material(stack));
        addRepairMaterial(materials, customTag(stack).getString(MAILLE_KEY));
        addRepairMaterial(materials, customTag(stack).getString(SLIME_KEY));
        return List.copyOf(materials);
    }

    private static void addRepairMaterial(LinkedHashSet<String> materials, String material) {
        if (!material.isEmpty() && MaterialManager.get(material) != null) {
            materials.add(material);
        }
    }

    /** Builds travelers, plate, generic, or slime-suit armor from indexed parts. */
    public static ItemStack assemble(Item item, Type type, ItemStack primary, ItemStack secondary) {
        Family family = item instanceof TinkersArmorItem armor ? armor.family : Family.TINKERS;
        if (family == Family.WINGS) {
            return ItemStack.EMPTY;
        }
        String primaryMaterial = MaterialPartItem.getMaterial(primary);
        MaterialDefinition definition = MaterialManager.get(primaryMaterial);
        if (definition == null || !MaterialPartItem.isPart(primary)
                || !expectedPrimaryPart(family, type).equals(MaterialPartItem.getPartId(primary))) {
            return ItemStack.EMPTY;
        }

        String secondaryMaterial = MaterialPartItem.getMaterial(secondary);
        if (family == Family.SLIME) {
            if (!isMaterialPart(secondary, "slime") || MaterialManager.get(secondaryMaterial) == null) {
                return ItemStack.EMPTY;
            }
        } else if (family == Family.PLATE) {
            if (!isMaterialPart(secondary, "maille") || MaterialManager.get(secondaryMaterial) == null) {
                return ItemStack.EMPTY;
            }
        } else if (!secondary.isEmpty()
                && (!isMaterialPart(secondary, "maille") || MaterialManager.get(secondaryMaterial) == null)) {
            return ItemStack.EMPTY;
        }

        ItemStack result = new ItemStack(item);
        CustomData.update(DataComponents.CUSTOM_DATA, result, tag -> {
            tag.putString(MATERIAL_KEY, primaryMaterial);
            if (family == Family.SLIME) {
                tag.putString(SLIME_KEY, secondaryMaterial);
            } else if (!secondary.isEmpty()) {
                tag.putString(MAILLE_KEY, secondaryMaterial);
            }
            tag.putInt(TinkersToolItem.MODIFIER_SLOTS_KEY,
                    family == Family.SLIME ? 5 : TinkersToolItem.DEFAULT_MODIFIER_SLOTS);
        });
        refreshDerivedComponents(result);
        result.set(DataComponents.DAMAGE, 0);
        return result;
    }

    /** Wings have one fixed material stat in the reference; the port uses blood. */
    public static ItemStack assembleWings(Item item, String material) {
        if (!(item instanceof TinkersArmorItem armor) || armor.family != Family.WINGS
                || MaterialManager.get(material) == null || material.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = new ItemStack(item);
        CustomData.update(DataComponents.CUSTOM_DATA, result, tag -> {
            tag.putString(MATERIAL_KEY, material);
            tag.putInt(TinkersToolItem.MODIFIER_SLOTS_KEY, 5);
        });
        refreshDerivedComponents(result);
        result.set(DataComponents.DAMAGE, 0);
        return result;
    }

    private static boolean isMaterialPart(ItemStack stack, String partId) {
        return MaterialPartItem.isPart(stack) && partId.equals(MaterialPartItem.getPartId(stack))
                && !MaterialPartItem.getMaterial(stack).isEmpty();
    }

    public static void refreshDerivedComponents(ItemStack stack) {
        if (!isArmor(stack)) {
            return;
        }
        TinkersArmorItem armor = (TinkersArmorItem) stack.getItem();
        MaterialDefinition definition = MaterialManager.get(material(stack));
        if (definition == null) {
            return;
        }
        int durability = Math.max(1, Math.round(definition.headDurability()
                * armor.family.durabilityFactor * durabilityFactor(armor.getType())));
        if (armor.family == Family.SLIME) {
            MaterialDefinition slime = MaterialManager.get(slimeMaterial(stack));
            if (slime != null) {
                durability += Math.max(1, slime.headDurability() / 4);
            }
        }
        int reinforcement = TinkersToolItem.modifierLevel(stack, "emerald_reinforcement");
        if (reinforcement > 0) {
            durability += Math.round(durability * 0.20F * reinforcement);
        }
        stack.set(DataComponents.MAX_DAMAGE, durability);

        float defense = armor.family.defense(armor.getType());
        if (armor.family != Family.WINGS) {
            defense += definition.headAttackDamage() * (armor.family == Family.SLIME ? 0.15F : 0.25F);
        }
        EquipmentSlotGroup group = equipmentGroup(armor.getType());
        String modifierId = armor.family.id + "_armor_" + armor.getType().getName();
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR, new AttributeModifier(
                        ResourceLocation.fromNamespaceAndPath(ModernTinkers.MOD_ID, modifierId),
                        defense, AttributeModifier.Operation.ADD_VALUE), group)
                .add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(
                        ResourceLocation.fromNamespaceAndPath(ModernTinkers.MOD_ID,
                                armor.family.id + "_toughness_" + armor.getType().getName()),
                        armor.family == Family.WINGS ? 0.0F
                                : Math.max(0.0F, definition.handleDurability() * 10.0F),
                        AttributeModifier.Operation.ADD_VALUE), group)
                .build());
    }

    private static float durabilityFactor(Type type) {
        return switch (type) {
            case HELMET, BOOTS -> 1.0F;
            case LEGGINGS -> 1.25F;
            case CHESTPLATE -> 1.5F;
            default -> 1.0F;
        };
    }

    private static EquipmentSlotGroup equipmentGroup(Type type) {
        return switch (type) {
            case HELMET -> EquipmentSlotGroup.HEAD;
            case CHESTPLATE -> EquipmentSlotGroup.CHEST;
            case LEGGINGS -> EquipmentSlotGroup.LEGS;
            case BOOTS -> EquipmentSlotGroup.FEET;
            default -> EquipmentSlotGroup.CHEST;
        };
    }

    private String primaryPart() {
        return expectedPrimaryPart(family, getType());
    }

    private static String expectedPrimaryPart(Family family, Type type) {
        if (family == Family.SLIME) {
            return switch (type) {
                case HELMET -> "skull";
                case CHESTPLATE -> "ribcage";
                case LEGGINGS -> "shell";
                case BOOTS -> "laces";
                default -> "";
            };
        }
        return "plating_" + type.getName();
    }

    private static CompoundTag customTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    @Override
    public Component getName(ItemStack stack) {
        String material = material(stack);
        if (material.isEmpty()) {
            return super.getName(stack);
        }
        return Component.translatable("item.moderntinkers.armor_name",
                Component.translatable("material.moderntinkers." + material),
                Component.translatable("item.moderntinkers." + displayNameKey()));
    }

    private String displayNameKey() {
        return switch (family) {
            case SLIME -> getType() == Type.CHESTPLATE ? "slimy_chestplate"
                    : "slime_" + getType().getName();
            case WINGS -> "slime_wings";
            default -> family.id + "_" + getType().getName();
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        String primary = material(stack);
        if (!primary.isEmpty()) {
            tooltip.add(Component.translatable("item.moderntinkers.armor.material",
                    Component.translatable("material.moderntinkers." + primary)));
            MaterialDefinition definition = MaterialManager.get(primary);
            if (definition != null) {
                for (String trait : definition.traits()) {
                    tooltip.add(Component.translatable("item.moderntinkers.tool.trait", trait));
                }
            }
        }
        String secondary = family == Family.SLIME ? slimeMaterial(stack)
                : customTag(stack).getString(MAILLE_KEY);
        if (!secondary.isEmpty()) {
            tooltip.add(Component.translatable("item.moderntinkers.armor.secondary",
                    Component.translatable("material.moderntinkers." + secondary)));
        }
        CompoundModifierTooltip.append(stack, tooltip);
        tooltip.add(Component.translatable("item.moderntinkers.tool.modifier_slots",
                TinkersToolItem.modifierSlotsUsed(stack), TinkersToolItem.modifierSlots(stack)));
        int overslimeCapacity = TinkersToolItem.overslimeCapacity(stack);
        if (overslimeCapacity > 0) {
            tooltip.add(Component.translatable("item.moderntinkers.tool.overslime",
                    TinkersToolItem.overslimeAmount(stack), overslimeCapacity));
        }
    }

    @Override
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        return isWings() && ElytraItem.isFlyEnabled(stack);
    }

    @Override
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        return isWings() && ((ElytraItem) Items.ELYTRA).elytraFlightTick(stack, entity, flightTicks);
    }

    private static final class CompoundModifierTooltip {
        private static void append(ItemStack stack, List<Component> tooltip) {
            var modifiers = customTag(stack).getCompound(MODIFIERS_KEY);
            for (String modifier : modifiers.getAllKeys()) {
                tooltip.add(Component.translatable("item.moderntinkers.tool.modifier",
                        modifier, modifiers.getInt(modifier)));
            }
        }
    }

    public enum Family {
        TINKERS("tinkers", 1.0F),
        TRAVELERS("travelers", 0.75F),
        PLATE("plate", 1.25F),
        SLIME("slime", 1.0F),
        WINGS("slime_wings", 1.0F);

        private final String id;
        private final float durabilityFactor;

        Family(String id, float durabilityFactor) {
            this.id = id;
            this.durabilityFactor = durabilityFactor;
        }

        private float defense(Type type) {
            return switch (this) {
                case TINKERS -> switch (type) {
                    case HELMET -> 3.0F;
                    case CHESTPLATE -> 8.0F;
                    case LEGGINGS -> 6.0F;
                    case BOOTS -> 3.0F;
                    default -> 0.0F;
                };
                case TRAVELERS -> switch (type) {
                    case HELMET -> 2.0F;
                    case CHESTPLATE -> 5.0F;
                    case LEGGINGS -> 4.0F;
                    case BOOTS -> 1.0F;
                    default -> 0.0F;
                };
                case PLATE -> switch (type) {
                    case HELMET -> 3.0F;
                    case CHESTPLATE -> 8.0F;
                    case LEGGINGS -> 6.0F;
                    case BOOTS -> 3.0F;
                    default -> 0.0F;
                };
                case SLIME -> switch (type) {
                    case HELMET -> 2.0F;
                    case CHESTPLATE -> 6.0F;
                    case LEGGINGS -> 5.0F;
                    case BOOTS -> 2.0F;
                    default -> 0.0F;
                };
                case WINGS -> 0.0F;
            };
        }
    }
}
