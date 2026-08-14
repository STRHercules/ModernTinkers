package com.moderntinkers.content.partbuilder;

import com.moderntinkers.content.StaticContent;
import com.moderntinkers.content.material.MaterialManager;
import com.moderntinkers.content.tools.MaterialPartItem;
import com.moderntinkers.content.tools.TinkersToolItem;
import com.moderntinkers.content.world.WorldContent;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal but real Part Builder transaction boundary. Inputs persist with the
 * block, the result is derived on the server, and material is consumed only
 * when the result slot is taken.
 */
public final class PartBuilderBlockEntity extends BlockEntity implements MenuProvider {
    public static final int PATTERN_SLOT = 0;
    public static final int MATERIAL_SLOT = 1;
    public static final String MATERIAL_DATA_KEY = "moderntinkers:material";
    private final SimpleContainer inputs;
    private final ResultContainer result = new ResultContainer();

    public PartBuilderBlockEntity(net.minecraft.core.BlockPos pos, BlockState state) {
        super(StaticContent.PART_BUILDER_BLOCK_ENTITY.get(), pos, state);
        this.inputs = new SimpleContainer(2) {
            @Override
            public void setChanged() {
                super.setChanged();
                PartBuilderBlockEntity.this.refreshResult();
                PartBuilderBlockEntity.this.setChanged();
            }
        };
    }

    public static boolean isMaterial(ItemStack stack) {
        return MaterialManager.findInput(stack).isPresent() || isRecyclingTool(stack);
    }

    public static String getMaterial(ItemStack stack) {
        return MaterialPartItem.getMaterial(stack);
    }

    public Container getInputInventory() {
        return inputs;
    }

    public ResultContainer getResultInventory() {
        return result;
    }

    public void refreshResult() {
        result.setItem(0, calculateResult());
    }

    public boolean canCraft() {
        return !calculateResult().isEmpty();
    }

    public boolean isRecyclingResult() {
        return isRecyclingTool(inputs.getItem(MATERIAL_SLOT))
                && !calculateResult().isEmpty();
    }

    /** Consume exactly the amount represented by the picked-up result stack. */
    public void craft(Player player, int resultCount) {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        ItemStack available = calculateResult();
        if (resultCount <= 0 || available.isEmpty() || resultCount > available.getCount()) {
            refreshResult();
            return;
        }

        String partId = PatternItem.getPart(inputs.getItem(PATTERN_SLOT));
        if (isRecyclingTool(inputs.getItem(MATERIAL_SLOT))) {
            if (resultCount != available.getCount()) {
                refreshResult();
                return;
            }
            ItemStack tool = inputs.getItem(MATERIAL_SLOT).copy();
            if (recyclingResult(tool, partId).isEmpty()) {
                refreshResult();
                return;
            }
            inputs.removeItem(MATERIAL_SLOT, 1);
            // The reference consumes the tool for one result-slot operation
            // and rolls one unselected part as a leftover, even when the
            // player only took part of a displayed output stack.
            ItemStack leftover = recyclingLeftover(tool, partId, level);
            if (!leftover.isEmpty() && !player.getInventory().add(leftover.copy())) {
                player.drop(leftover.copy(), false);
            }
            refreshResult();
            setChanged();
            return;
        }

        int partCost = partCost(partId);
        MaterialManager.MaterialInput material = detectMaterial(inputs.getItem(MATERIAL_SLOT));
        if (partCost <= 0 || material == null) {
            refreshResult();
            return;
        }

        int unitsUsed = partCost * resultCount;
        int itemsUsed = (unitsUsed + material.units() - 1) / material.units();
        ItemStack materialStack = inputs.getItem(MATERIAL_SLOT);
        if (itemsUsed > materialStack.getCount()) {
            refreshResult();
            return;
        }

        inputs.removeItem(MATERIAL_SLOT, itemsUsed);
        available = available.copyWithCount(resultCount);
        available.onCraftedBy(level, player, resultCount);
        refreshResult();
        setChanged();
    }

    private ItemStack calculateResult() {
        String partId = PatternItem.getPart(inputs.getItem(PATTERN_SLOT));
        ItemStack source = inputs.getItem(MATERIAL_SLOT);
        if (isRecyclingTool(source) || isRecyclingPattern(partId)) {
            return recyclingResult(source, partId);
        }
        int partCost = partCost(partId);
        MaterialManager.MaterialInput material = detectMaterial(inputs.getItem(MATERIAL_SLOT));
        if (partCost <= 0 || material == null) {
            return ItemStack.EMPTY;
        }

        var toolPart = StaticContent.toolPart(partId);
        if (toolPart == null) {
            return ItemStack.EMPTY;
        }

        int availableUnits = material.units() * inputs.getItem(MATERIAL_SLOT).getCount();
        int count = Math.min(toolPart.get().getDefaultMaxStackSize(), availableUnits / partCost);
        if (count <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack output = new ItemStack(toolPart.get(), count);
        CustomData.update(DataComponents.CUSTOM_DATA, output,
                tag -> tag.putString(MATERIAL_DATA_KEY, material.materialId()));
        return output;
    }

    public static boolean isRecyclingTool(ItemStack stack) {
        if (!(stack.getItem() instanceof TinkersToolItem tool) || stack.isEnchanted()
                || !TinkersToolItem.isAssembled(stack)) {
            return false;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tool.kind().id().equals(tag.getString(TinkersToolItem.KIND_KEY))
                || !tag.getCompound(TinkersToolItem.MODIFIERS_KEY).isEmpty()) {
            return false;
        }
        if (tool.kind().partCount() == 0) {
            return switch (tool.kind()) {
                case SKY_STAFF, EARTH_STAFF, ICHOR_STAFF, ENDER_STAFF -> true;
                default -> false;
            };
        }
        for (int index = 0; index < tool.kind().partCount(); index++) {
            String material = TinkersToolItem.partMaterial(stack, tool.kind(), index);
            if (MaterialManager.get(material) == null
                    || MaterialPartItem.partUnits(tool.kind().parts().get(index)) <= 0
                    || StaticContent.toolPart(tool.kind().parts().get(index)) == null) {
                return false;
            }
        }
        return true;
    }

    private static boolean isRecyclingPattern(String partId) {
        return PatternItem.RECYCLING_BLOCK.equals(partId)
                || PatternItem.RECYCLING_INGOT.equals(partId)
                || PatternItem.RECYCLING_CRYSTAL.equals(partId);
    }

    private static ItemStack recyclingResult(ItemStack source, String pattern) {
        List<ItemStack> outputs = recyclingOutputs(source);
        if (outputs.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int index = switch (pattern) {
            case PatternItem.RECYCLING_BLOCK -> 0;
            case PatternItem.RECYCLING_CRYSTAL -> 1;
            case PatternItem.RECYCLING_INGOT -> 2;
            default -> -1;
        };
        if (index < 0) {
            for (int outputIndex = 0; outputIndex < outputs.size(); outputIndex++) {
                if (pattern.equals(MaterialPartItem.getPartId(outputs.get(outputIndex)))) {
                    index = outputIndex;
                    break;
                }
            }
        }
        if (index < 0 || index >= outputs.size()) {
            return ItemStack.EMPTY;
        }
        int available = recyclingAvailableCount(source, outputs);
        if (available <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack selected = outputs.get(index);
        return selected.copyWithCount(Math.min(available, selected.getCount()));
    }

    /** Returns one random non-selected part when durability leaves spare output. */
    private static ItemStack recyclingLeftover(ItemStack source, String pattern, Level level) {
        List<ItemStack> outputs = recyclingOutputs(source);
        int damage = source.getDamageValue();
        if (damage > 0 && level.getRandom().nextInt(Math.max(1, source.getMaxDamage())) < damage) {
            return ItemStack.EMPTY;
        }
        int selectedIndex = switch (pattern) {
            case PatternItem.RECYCLING_BLOCK -> 0;
            case PatternItem.RECYCLING_CRYSTAL -> 1;
            case PatternItem.RECYCLING_INGOT -> 2;
            default -> -1;
        };
        if (selectedIndex < 0) {
            for (int index = 0; index < outputs.size(); index++) {
                if (pattern.equals(MaterialPartItem.getPartId(outputs.get(index)))) {
                    selectedIndex = index;
                    break;
                }
            }
        }
        int remaining = recyclingAvailableCount(source, outputs)
                - (selectedIndex >= 0 && selectedIndex < outputs.size()
                ? outputs.get(selectedIndex).getCount() : 0);
        if (remaining <= 0) {
            return ItemStack.EMPTY;
        }
        List<ItemStack> alternatives = new ArrayList<>();
        for (int index = 0; index < outputs.size(); index++) {
            if (index != selectedIndex && !outputs.get(index).isEmpty()) {
                alternatives.add(outputs.get(index));
            }
        }
        if (alternatives.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack chosen = alternatives.get(level.getRandom().nextInt(alternatives.size()));
        return chosen.copyWithCount(Math.min(remaining, chosen.getCount()));
    }

    private static int recyclingAvailableCount(ItemStack source, List<ItemStack> outputs) {
        if (outputs.isEmpty()) {
            return 0;
        }
        int total = outputs.stream().mapToInt(ItemStack::getCount).sum();
        int maxDamage = source.getMaxDamage();
        if (maxDamage <= 0) {
            return total;
        }
        int remaining = Math.max(0, maxDamage - Math.min(maxDamage, source.getDamageValue()));
        return total * remaining / maxDamage;
    }

    private static List<ItemStack> recyclingOutputs(ItemStack source) {
        if (!isRecyclingTool(source)) {
            return List.of();
        }
        TinkersToolItem.ToolKind kind = ((TinkersToolItem) source.getItem()).kind();
        List<ItemStack> outputs = switch (kind) {
            case EARTH_STAFF -> List.of(
                    new ItemStack(WorldContent.GREENHEART_LOG.get(), 2),
                    new ItemStack(WorldContent.EARTH_SLIME_CRYSTAL.get(), 2),
                    new ItemStack(StaticContent.COBALT.ingot().get()));
            case SKY_STAFF -> List.of(
                    new ItemStack(WorldContent.SKYROOT_LOG.get(), 2),
                    new ItemStack(WorldContent.SKY_SLIME_CRYSTAL.get(), 2),
                    new ItemStack(StaticContent.ROSE_GOLD.ingot().get()));
            case ICHOR_STAFF -> List.of(
                    new ItemStack(WorldContent.BLOODSHROOM_LOG.get(), 2),
                    new ItemStack(WorldContent.ICHOR_SLIME_CRYSTAL.get(), 2),
                    new ItemStack(StaticContent.QUEENS_SLIME.ingot().get()));
            case ENDER_STAFF -> List.of(
                    new ItemStack(WorldContent.ENDERBARK_LOG.get(), 2),
                    new ItemStack(WorldContent.ENDER_SLIME_CRYSTAL.get(), 2),
                    new ItemStack(Items.NETHERITE_INGOT));
            default -> {
                List<ItemStack> parts = new ArrayList<>();
                for (int index = 0; index < kind.partCount(); index++) {
                    String partId = kind.parts().get(index);
                    String material = TinkersToolItem.partMaterial(source, kind, index);
                    var part = StaticContent.toolPart(partId);
                    if (part == null || MaterialManager.get(material) == null) {
                        yield List.of();
                    }
                    parts.add(MaterialPartItem.withMaterial(part.get(), material));
                }
                yield parts;
            }
        };
        return outputs;
    }

    private static MaterialManager.MaterialInput detectMaterial(ItemStack stack) {
        return MaterialManager.findInput(stack).orElse(null);
    }

    private static int partCost(String part) {
        return switch (part) {
            case "repair_kit", "fake_ingot", "pick_head", "small_axe_head", "small_blade",
                    "bow_grip", "bowstring", "arrow_head", "arrow_shaft", "fletching",
                    "tool_binding", "tool_handle", "plating_helmet", "plating_chestplate",
                    "plating_leggings", "plating_boots", "maille", "laces" -> 9;
            case "hammer_head", "broad_blade", "adze_head", "bow_limb", "tough_binding",
                    "tough_handle" -> 18;
            case "broad_axe_head" -> 27;
            case "skull", "ribcage" -> 36;
            case "large_plate", "shell" -> 72;
            case "shield_core" -> 27;
            case "slime" -> 9;
            default -> 0;
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.moderntinkers.part_builder");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new PartBuilderMenu(id, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put("Items", inputs.createTag(provider));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        inputs.fromTag(tag.getList("Items", Tag.TAG_COMPOUND), provider);
        refreshResult();
    }

}
