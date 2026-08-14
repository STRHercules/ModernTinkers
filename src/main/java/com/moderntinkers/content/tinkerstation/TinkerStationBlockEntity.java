package com.moderntinkers.content.tinkerstation;

import com.moderntinkers.content.StaticContent;
import com.moderntinkers.content.tools.MaterialPartItem;
import com.moderntinkers.content.tools.TinkersArmorItem;
import com.moderntinkers.content.tools.TinkersArrowItem;
import com.moderntinkers.content.tools.TinkersShieldItem;
import com.moderntinkers.content.tools.TinkersToolItem;
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
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Persistent part inputs and transactional tool assembly. */
public final class TinkerStationBlockEntity extends BlockEntity implements MenuProvider {
    public static final int HEAD_SLOT = 0;
    public static final int HANDLE_SLOT = 1;
    public static final int BINDING_SLOT = 2;
    public static final int EXTRA_SLOT = 3;
    private static final int INPUT_SLOTS = 4;

    private final SimpleContainer inputs;
    private final ResultContainer result = new ResultContainer();

    public TinkerStationBlockEntity(net.minecraft.core.BlockPos pos, BlockState state) {
        super(StaticContent.TINKER_STATION_BLOCK_ENTITY.get(), pos, state);
        this.inputs = new SimpleContainer(INPUT_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                TinkerStationBlockEntity.this.refreshResult();
                TinkerStationBlockEntity.this.setChanged();
            }
        };
    }

    public static boolean isPart(ItemStack stack) {
        return MaterialPartItem.isPart(stack) && !MaterialPartItem.getMaterial(stack).isEmpty();
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
        ItemStack output = calculateResult();
        if (output.isEmpty() || resultCount != output.getCount()) {
            refreshResult();
            return;
        }
        boolean hasInput = false;
        for (int index = 0; index < INPUT_SLOTS; index++) {
            if (!inputs.getItem(index).isEmpty()) {
                hasInput = true;
            }
        }
        if (!hasInput) {
            refreshResult();
            return;
        }
        for (int index = 0; index < INPUT_SLOTS; index++) {
            if (!inputs.getItem(index).isEmpty()) {
                inputs.removeItem(index, 1);
            }
        }
        output.onCraftedBy(level, player, output.getCount());
        refreshResult();
        setChanged();
    }

    private ItemStack calculateResult() {
        ListPart list = collectParts();
        if (list.count() == 0) {
            return ItemStack.EMPTY;
        }
        if (list.count() == 3 && list.hasParts("arrow_head", "arrow_shaft", "fletching")) {
            return TinkersArrowItem.assemble(StaticContent.TINKERS_ARROW.get(),
                    list.part("arrow_head"), list.part("arrow_shaft"), list.part("fletching"));
        }

        ItemStack slimePart = list.firstMatching("skull", "ribcage", "shell", "laces");
        ItemStack slime = list.part("slime");
        if (!slimePart.isEmpty() && !slime.isEmpty() && list.count() == 2) {
            ArmorItem.Type type = slimeArmorType(MaterialPartItem.getPartId(slimePart));
            var armor = type == null ? null
                    : StaticContent.armor(com.moderntinkers.content.tools.TinkersArmorItem.Family.SLIME, type);
            if (type != null && armor != null) {
                return TinkersArmorItem.assemble(armor.get(), type, slimePart, slime);
            }
        }

        ItemStack plating = list.firstMatching("plating_helmet", "plating_chestplate",
                "plating_leggings", "plating_boots");
        if (!plating.isEmpty() && (list.count() == 1 || list.count() == 2)) {
            ArmorItem.Type type = armorType(MaterialPartItem.getPartId(plating));
            ItemStack maille = list.part("maille");
            if (type != null && maille.isEmpty() && list.count() == 1) {
                var armor = StaticContent.armor(
                        com.moderntinkers.content.tools.TinkersArmorItem.Family.TRAVELERS, type);
                return armor == null ? ItemStack.EMPTY
                        : TinkersArmorItem.assemble(armor.get(), type, plating, ItemStack.EMPTY);
            }
            if (type != null && !maille.isEmpty() && list.count() == 2) {
                var armor = StaticContent.armor(
                        com.moderntinkers.content.tools.TinkersArmorItem.Family.PLATE, type);
                return armor == null ? ItemStack.EMPTY
                        : TinkersArmorItem.assemble(armor.get(), type, plating, maille);
            }
        }

        ItemStack shieldCore = list.part("shield_core");
        if (!shieldCore.isEmpty() && (list.count() == 1 || list.count() == 2)) {
            ItemStack plate = list.part("large_plate");
            if (!plate.isEmpty() && list.count() == 2) {
                return TinkersShieldItem.assemblePlate(StaticContent.PLATE_SHIELD.get(),
                        shieldCore, plate);
            }
            ItemStack handle = list.firstMatching("tool_handle", "tough_handle");
            if (handle.isEmpty() || list.count() == 2) {
                return TinkersShieldItem.assemble(StaticContent.TRAVELERS_SHIELD.get(),
                        shieldCore, handle);
            }
        }

        for (TinkersToolItem.ToolKind kind : TinkersToolItem.ToolKind.values()) {
            if (kind.partCount() != list.count() || kind.partCount() == 0) {
                continue;
            }
            ItemStack[] ordered = new ItemStack[kind.partCount()];
            if (!list.orderFor(kind.parts(), ordered)) {
                continue;
            }
            var tool = StaticContent.tool(kind);
            if (tool != null) {
                return TinkersToolItem.assemble(tool.get(), kind, ordered);
            }
        }
        return ItemStack.EMPTY;
    }

    private ListPart collectParts() {
        List<ItemStack> parts = new java.util.ArrayList<>();
        for (int index = 0; index < INPUT_SLOTS; index++) {
            ItemStack stack = inputs.getItem(index);
            if (!stack.isEmpty()) {
                if (!isPart(stack)) {
                    return new ListPart(List.of());
                }
                parts.add(stack);
            }
        }
        return new ListPart(parts);
    }

    private record ListPart(List<ItemStack> values) {
        private int count() {
            return values.size();
        }

        private ItemStack part(String id) {
            for (ItemStack stack : values) {
                if (MaterialPartItem.getPartId(stack).equals(id)) {
                    return stack;
                }
            }
            return ItemStack.EMPTY;
        }

        private ItemStack firstMatching(String... ids) {
            for (String id : ids) {
                ItemStack stack = part(id);
                if (!stack.isEmpty()) {
                    return stack;
                }
            }
            return ItemStack.EMPTY;
        }

        private boolean hasParts(String... ids) {
            if (ids.length != values.size()) {
                return false;
            }
            for (String id : ids) {
                if (part(id).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        private boolean orderFor(java.util.List<String> ids, ItemStack[] result) {
            boolean[] used = new boolean[values.size()];
            for (int target = 0; target < ids.size(); target++) {
                for (int source = 0; source < values.size(); source++) {
                    if (!used[source] && MaterialPartItem.getPartId(values.get(source))
                            .equals(ids.get(target))) {
                        used[source] = true;
                        result[target] = values.get(source);
                        break;
                    }
                }
                if (result[target] == null) {
                    return false;
                }
            }
            return true;
        }
    }

    private static ArmorItem.Type armorType(String partId) {
        return switch (partId) {
            case "plating_helmet" -> ArmorItem.Type.HELMET;
            case "plating_chestplate" -> ArmorItem.Type.CHESTPLATE;
            case "plating_leggings" -> ArmorItem.Type.LEGGINGS;
            case "plating_boots" -> ArmorItem.Type.BOOTS;
            default -> null;
        };
    }

    private static ArmorItem.Type slimeArmorType(String partId) {
        return switch (partId) {
            case "skull" -> ArmorItem.Type.HELMET;
            case "ribcage" -> ArmorItem.Type.CHESTPLATE;
            case "shell" -> ArmorItem.Type.LEGGINGS;
            case "laces" -> ArmorItem.Type.BOOTS;
            default -> null;
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.moderntinkers.tinker_station");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new TinkerStationMenu(id, playerInventory, this);
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
