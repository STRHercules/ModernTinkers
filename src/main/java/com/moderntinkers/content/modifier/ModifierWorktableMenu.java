package com.moderntinkers.content.modifier;

import com.moderntinkers.content.StaticContent;
import com.moderntinkers.content.TinkerMenuTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Modifier Worktable menu with server-side slot validation and shift-click paths. */
public final class ModifierWorktableMenu extends AbstractContainerMenu {
    public static final int OUTPUT_SLOT = 0;
    public static final int TOOL_SLOT = 1;
    public static final int MODIFIER_SLOT = 2;
    private static final int PLAYER_INVENTORY_START = 3;

    private final ModifierWorktableBlockEntity blockEntity;
    private final Container inputs;
    private final ResultContainer result;
    private final ContainerLevelAccess access;

    public ModifierWorktableMenu(int id, Inventory inventory,
                                 ModifierWorktableBlockEntity blockEntity) {
        this(id, inventory, blockEntity, blockEntity.getBlockPos());
    }

    private ModifierWorktableMenu(int id, Inventory inventory,
                                  ModifierWorktableBlockEntity blockEntity, BlockPos blockPos) {
        super(StaticContent.MODIFIER_WORKTABLE_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.inputs = blockEntity == null ? new SimpleContainer(2) : blockEntity.getInputInventory();
        this.result = blockEntity == null ? new ResultContainer() : blockEntity.getResultInventory();
        Level level = blockEntity == null ? null : blockEntity.getLevel();
        this.access = level == null ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(level, blockPos);

        this.addSlot(new Slot(result, 0, 148, 42) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return blockEntity == null || blockEntity.canCraft();
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                if (blockEntity != null) {
                    blockEntity.craft(player, stack.getCount());
                }
                super.onTake(player, stack);
            }
        });
        this.addSlot(new Slot(inputs, ModifierWorktableBlockEntity.TOOL_SLOT, 44, 43) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ModifierWorktableBlockEntity.isTool(stack);
            }
        });
        this.addSlot(new Slot(inputs, ModifierWorktableBlockEntity.MODIFIER_SLOT, 80, 43) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ModifierWorktableBlockEntity.isModifier(stack);
            }
        });

        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9,
                        8 + column * 18, 102 + row * 18));
            }
        }
        for (int column = 0; column < 9; ++column) {
            this.addSlot(new Slot(inventory, column, 8 + column * 18, 160));
        }
    }

    public ModifierWorktableMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, resolveClientTarget(inventory, buffer));
    }

    private ModifierWorktableMenu(int id, Inventory inventory, ClientTarget target) {
        this(id, inventory, target.blockEntity(), target.blockPos());
    }

    private static ClientTarget resolveClientTarget(Inventory inventory,
                                                    RegistryFriendlyByteBuf buffer) {
        BlockPos blockPos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(blockPos);
        return new ClientTarget(blockPos,
                blockEntity instanceof ModifierWorktableBlockEntity worktable ? worktable : null);
    }

    @Override
    public void slotsChanged(Container inventory) {
        super.slotsChanged(inventory);
        if (blockEntity != null && inventory == inputs) {
            blockEntity.refreshResult();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity == null
                || AbstractContainerMenu.stillValid(access, player, StaticContent.MODIFIER_WORKTABLE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot clicked = slots.get(index);
        if (!clicked.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack source = clicked.getItem();
        ItemStack moved = source.copy();
        if (index == OUTPUT_SLOT) {
            if (!TinkerMenuTransfer.canMoveEntireStack(
                    slots, source, PLAYER_INVENTORY_START, slots.size())) {
                return ItemStack.EMPTY;
            }
            if (!moveItemStackTo(source, PLAYER_INVENTORY_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            int movedCount = moved.getCount() - source.getCount();
            if (movedCount <= 0) {
                return ItemStack.EMPTY;
            }
            if (source.isEmpty()) {
                clicked.setByPlayer(ItemStack.EMPTY);
            } else {
                clicked.setChanged();
            }
            if (blockEntity != null) {
                blockEntity.craft(player, movedCount);
            }
            return moved.copyWithCount(movedCount);
        } else if (index == TOOL_SLOT || index == MODIFIER_SLOT) {
            if (!moveItemStackTo(source, PLAYER_INVENTORY_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (ModifierWorktableBlockEntity.isTool(source)) {
            if (!moveItemStackTo(source, TOOL_SLOT, TOOL_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (ModifierWorktableBlockEntity.isModifier(source)) {
            if (!moveItemStackTo(source, MODIFIER_SLOT, MODIFIER_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (source.isEmpty()) {
            clicked.setByPlayer(ItemStack.EMPTY);
        } else {
            clicked.setChanged();
        }
        return moved;
    }

    private record ClientTarget(BlockPos blockPos, ModifierWorktableBlockEntity blockEntity) {}
}
