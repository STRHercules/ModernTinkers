package com.moderntinkers.content.modifier;

import com.moderntinkers.content.StaticContent;
import com.moderntinkers.content.tools.TinkersToolItem;
import com.moderntinkers.content.tools.TinkersArmorItem;
import com.moderntinkers.content.tools.TinkersShieldItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Persistent tool/modifier inputs and a validated single-level application. */
public final class ModifierWorktableBlockEntity extends BlockEntity implements MenuProvider {
    public static final int TOOL_SLOT = 0;
    public static final int MODIFIER_SLOT = 1;

    private final SimpleContainer inputs;
    private final ResultContainer result = new ResultContainer();

    public ModifierWorktableBlockEntity(net.minecraft.core.BlockPos pos, BlockState state) {
        super(StaticContent.MODIFIER_WORKTABLE_BLOCK_ENTITY.get(), pos, state);
        this.inputs = new SimpleContainer(2) {
            @Override
            public void setChanged() {
                super.setChanged();
                ModifierWorktableBlockEntity.this.refreshResult();
                ModifierWorktableBlockEntity.this.setChanged();
            }
        };
    }

    public static boolean isTool(ItemStack stack) {
        return TinkersToolItem.isAssembled(stack) || TinkersArmorItem.isAssembled(stack)
                || TinkersShieldItem.isAssembled(stack);
    }

    public static boolean isModifier(ItemStack stack) {
        return StaticContent.isModifierItem(stack);
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

    public void craft(Player player, int resultCount) {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        if (inputs.getItem(TOOL_SLOT).isEmpty() || inputs.getItem(MODIFIER_SLOT).isEmpty()) {
            refreshResult();
            return;
        }
        ItemStack output = calculateResult();
        if (output.isEmpty() || resultCount <= 0 || resultCount != output.getCount()) {
            refreshResult();
            return;
        }
        String extracted = ModifierCrystalItem.isCrystal(inputs.getItem(MODIFIER_SLOT))
                && !ModifierCrystalItem.isConfigured(inputs.getItem(MODIFIER_SLOT))
                ? extractionModifier(inputs.getItem(TOOL_SLOT)) : "";
        inputs.removeItem(TOOL_SLOT, 1);
        inputs.removeItem(MODIFIER_SLOT, 1);
        output.onCraftedBy(level, player, resultCount);
        if (!extracted.isEmpty()) {
            ItemStack crystal = ModifierCrystalItem.withModifier(extracted);
            if (!player.getInventory().add(crystal)) {
                player.drop(crystal, false);
            }
        }
        refreshResult();
        setChanged();
    }

    private ItemStack calculateResult() {
        ItemStack tool = inputs.getItem(TOOL_SLOT);
        ItemStack modifier = inputs.getItem(MODIFIER_SLOT);
        String modifierId = StaticContent.modifierId(modifier);
        if (!isTool(tool) || tool.getCount() < 1 || modifier.getCount() < 1
                || (!modifierId.isEmpty() && !ModifierManager.canApply(modifierId, tool))) {
            return ItemStack.EMPTY;
        }
        if (ModifierCrystalItem.isCrystal(modifier) && modifierId.isEmpty()) {
            String extracted = extractionModifier(tool);
            if (extracted.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack result = tool.copyWithCount(1);
            return TinkersToolItem.removeModifier(result, extracted)
                    ? result : ItemStack.EMPTY;
        }
        if (modifierId.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int existing = TinkersToolItem.modifierLevel(tool, modifierId);
        if (existing >= ModifierManager.maxLevel(modifierId)) {
            return ItemStack.EMPTY;
        }
        if (!TinkersToolItem.hasModifierCapacity(tool, modifierId)) {
            return ItemStack.EMPTY;
        }
        ItemStack output = tool.copyWithCount(1);
        if (!TinkersToolItem.addModifier(output, modifierId)) {
            return ItemStack.EMPTY;
        }
        return output;
    }

    private static String extractionModifier(ItemStack tool) {
        for (String id : ModifierManager.ids()) {
            if (TinkersToolItem.modifierLevel(tool, id) <= 0) {
                continue;
            }
            ItemStack copy = tool.copyWithCount(1);
            if (TinkersToolItem.removeModifier(copy, id)) {
                return id;
            }
        }
        return "";
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.moderntinkers.modifier_worktable");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ModifierWorktableMenu(id, playerInventory, this);
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
