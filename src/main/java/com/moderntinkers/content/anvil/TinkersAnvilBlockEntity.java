package com.moderntinkers.content.anvil;

import com.moderntinkers.content.StaticContent;
import com.moderntinkers.content.material.MaterialManager;
import com.moderntinkers.content.tools.MaterialPartItem;
import com.moderntinkers.content.tools.TinkersToolItem;
import com.moderntinkers.content.tools.TinkersArmorItem;
import com.moderntinkers.content.tools.TinkersShieldItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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

/** Server-authoritative repair transaction for assembled Tinkers tools. */
public final class TinkersAnvilBlockEntity extends BlockEntity implements MenuProvider {
    public static final int TOOL_SLOT = 0;
    public static final int MATERIAL_SLOT = 1;

    private final SimpleContainer inputs = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            TinkersAnvilBlockEntity.this.refreshResult();
            TinkersAnvilBlockEntity.this.setChanged();
        }
    };
    private final ResultContainer result = new ResultContainer();

    public TinkersAnvilBlockEntity(net.minecraft.core.BlockPos pos, BlockState state) {
        super(StaticContent.TINKERS_ANVIL_BLOCK_ENTITY.get(), pos, state);
    }

    public static boolean isTool(ItemStack stack) {
        return TinkersToolItem.isTool(stack) || TinkersArmorItem.isArmor(stack)
                || stack.getItem() instanceof TinkersShieldItem;
    }

    public static boolean isRepairMaterial(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (MaterialPartItem.isPart(stack)) {
            return "repair_kit".equals(MaterialPartItem.getPartId(stack))
                    && !MaterialPartItem.getMaterial(stack).isEmpty();
        }
        return MaterialManager.findInput(stack).isPresent();
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

    public void craft(Player player) {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        if (inputs.getItem(TOOL_SLOT).isEmpty() || inputs.getItem(MATERIAL_SLOT).isEmpty()) {
            refreshResult();
            return;
        }
        ItemStack output = calculateResult();
        if (output.isEmpty()) {
            refreshResult();
            return;
        }
        inputs.removeItem(TOOL_SLOT, 1);
        inputs.removeItem(MATERIAL_SLOT, 1);
        output.onCraftedBy(level, player, 1);
        refreshResult();
        setChanged();
    }

    private ItemStack calculateResult() {
        ItemStack tool = inputs.getItem(TOOL_SLOT);
        ItemStack material = inputs.getItem(MATERIAL_SLOT);
        if (!isTool(tool) || tool.getDamageValue() <= 0
                || !isRepairMaterial(material)) {
            return ItemStack.EMPTY;
        }

        String inputMaterial = materialId(material);
        if (inputMaterial.isEmpty() || !repairMaterials(tool).contains(inputMaterial)) {
            return ItemStack.EMPTY;
        }

        int units = materialUnits(material);
        int repair = Math.max(1, tool.getMaxDamage() * units / 36);
        ItemStack output = tool.copyWithCount(1);
        output.setDamageValue(Math.max(0, tool.getDamageValue() - repair));
        return output;
    }

    private static String materialId(ItemStack stack) {
        if (MaterialPartItem.isPart(stack)) {
            return MaterialPartItem.getMaterial(stack);
        }
        return MaterialManager.findInput(stack).map(MaterialManager.MaterialInput::materialId)
                .orElse("");
    }

    private static java.util.List<String> repairMaterials(ItemStack stack) {
        if (TinkersToolItem.isTool(stack)) {
            return TinkersToolItem.repairMaterials(stack);
        }
        if (TinkersArmorItem.isArmor(stack)) {
            return TinkersArmorItem.repairMaterials(stack);
        }
        if (stack.getItem() instanceof TinkersShieldItem) {
            return TinkersShieldItem.repairMaterials(stack);
        }
        return java.util.List.of();
    }

    private static int materialUnits(ItemStack stack) {
        if (MaterialPartItem.isPart(stack)) {
            return MaterialPartItem.materialUnits(stack);
        }
        return MaterialManager.findInput(stack).map(MaterialManager.MaterialInput::units).orElse(0);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(((TinkersAnvilBlock) getBlockState().getBlock()).isScorched()
                ? "container.moderntinkers.scorched_anvil"
                : "container.moderntinkers.tinkers_anvil");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new TinkersAnvilMenu(id, playerInventory, this);
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
