package com.moderntinkers.content.partbuilder;

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

/** Part Builder menu with explicit pattern, material, output, and player slots. */
public final class PartBuilderMenu extends AbstractContainerMenu {
    public static final int OUTPUT_SLOT = 0;
    public static final int PATTERN_SLOT = 1;
    public static final int MATERIAL_SLOT = 2;
    private static final int PLAYER_INVENTORY_START = 3;

    private final PartBuilderBlockEntity blockEntity;
    private final Container inputs;
    private final ResultContainer result;
    private final ContainerLevelAccess access;

    public PartBuilderMenu(int id, Inventory playerInventory, PartBuilderBlockEntity blockEntity) {
        this(id, playerInventory, blockEntity, blockEntity.getBlockPos());
    }

    private PartBuilderMenu(int id, Inventory playerInventory,
            PartBuilderBlockEntity blockEntity, BlockPos blockPos) {
        super(StaticContent.PART_BUILDER_MENU.get(), id);
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
        this.addSlot(new Slot(inputs, PartBuilderBlockEntity.PATTERN_SLOT, 8, 43) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return PatternItem.isPattern(stack);
            }
        });
        this.addSlot(new Slot(inputs, PartBuilderBlockEntity.MATERIAL_SLOT, 29, 43) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return PartBuilderBlockEntity.isMaterial(stack);
            }
        });

        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * 18, 102 + row * 18));
            }
        }
        for (int column = 0; column < 9; ++column) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, 160));
        }
    }

    public PartBuilderMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(id, playerInventory, resolveClientTarget(playerInventory, buffer));
    }

    private PartBuilderMenu(int id, Inventory playerInventory, ClientTarget target) {
        this(id, playerInventory, target.blockEntity(), target.blockPos());
    }

    private static ClientTarget resolveClientTarget(Inventory playerInventory,
            RegistryFriendlyByteBuf buffer) {
        BlockPos blockPos = buffer.readBlockPos();
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(blockPos);
        return new ClientTarget(blockPos, blockEntity instanceof PartBuilderBlockEntity partBuilder
                ? partBuilder : null);
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
                || AbstractContainerMenu.stillValid(access, player, StaticContent.PART_BUILDER.get());
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
            if (blockEntity != null && blockEntity.isRecyclingResult()
                    && !TinkerMenuTransfer.canMoveEntireStack(slots, source,
                    PLAYER_INVENTORY_START, slots.size())) {
                return ItemStack.EMPTY;
            }
            int originalCount = source.getCount();
            if (!moveItemStackTo(source, PLAYER_INVENTORY_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            int movedCount = originalCount - source.getCount();
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
        } else if (index == PATTERN_SLOT || index == MATERIAL_SLOT) {
            if (!moveItemStackTo(source, PLAYER_INVENTORY_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (PatternItem.isPattern(source)) {
            if (!moveItemStackTo(source, PATTERN_SLOT, PATTERN_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (PartBuilderBlockEntity.isMaterial(source)) {
            if (!moveItemStackTo(source, MATERIAL_SLOT, MATERIAL_SLOT + 1, false)) {
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

    private record ClientTarget(BlockPos blockPos, PartBuilderBlockEntity blockEntity) {}
}
