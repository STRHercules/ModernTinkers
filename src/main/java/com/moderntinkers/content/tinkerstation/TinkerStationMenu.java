package com.moderntinkers.content.tinkerstation;

import com.moderntinkers.content.StaticContent;
import com.moderntinkers.content.TinkerMenuTransfer;
import com.moderntinkers.content.tools.MaterialPartItem;
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

/** Tinker Station menu with explicit part slots and a transactional result. */
public final class TinkerStationMenu extends AbstractContainerMenu {
    public static final int OUTPUT_SLOT = 0;
    public static final int HEAD_SLOT = 1;
    public static final int HANDLE_SLOT = 2;
    public static final int BINDING_SLOT = 3;
    public static final int EXTRA_SLOT = 4;
    private static final int PART_SLOT_START = HEAD_SLOT;
    private static final int PART_SLOT_END = EXTRA_SLOT + 1;
    private static final int PLAYER_INVENTORY_START = PART_SLOT_END;

    private final TinkerStationBlockEntity blockEntity;
    private final Container inputs;
    private final ResultContainer result;
    private final ContainerLevelAccess access;

    public TinkerStationMenu(int id, Inventory playerInventory,
                             TinkerStationBlockEntity blockEntity) {
        this(id, playerInventory, blockEntity, blockEntity.getBlockPos());
    }

    private TinkerStationMenu(int id, Inventory playerInventory,
                              TinkerStationBlockEntity blockEntity, BlockPos blockPos) {
        super(StaticContent.TINKER_STATION_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.inputs = blockEntity == null ? new SimpleContainer(4) : blockEntity.getInputInventory();
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
        this.addSlot(partSlot(TinkerStationBlockEntity.HEAD_SLOT, 44, 25));
        this.addSlot(partSlot(TinkerStationBlockEntity.HANDLE_SLOT, 80, 43));
        this.addSlot(partSlot(TinkerStationBlockEntity.BINDING_SLOT, 116, 61));
        this.addSlot(partSlot(TinkerStationBlockEntity.EXTRA_SLOT, 80, 7));

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

    public TinkerStationMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(id, playerInventory, resolveClientTarget(playerInventory, buffer));
    }

    private TinkerStationMenu(int id, Inventory playerInventory, ClientTarget target) {
        this(id, playerInventory, target.blockEntity(), target.blockPos());
    }

    private static ClientTarget resolveClientTarget(Inventory playerInventory,
                                                    RegistryFriendlyByteBuf buffer) {
        BlockPos blockPos = buffer.readBlockPos();
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(blockPos);
        return new ClientTarget(blockPos,
                blockEntity instanceof TinkerStationBlockEntity station ? station : null);
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
                || AbstractContainerMenu.stillValid(access, player, StaticContent.TINKER_STATION.get());
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
            int originalCount = source.getCount();
            if (!TinkerMenuTransfer.canMoveEntireStack(slots, source,
                    PLAYER_INVENTORY_START, slots.size())) {
                return ItemStack.EMPTY;
            }
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
        }

        if (index >= PART_SLOT_START && index < PART_SLOT_END) {
            if (!moveItemStackTo(source, PLAYER_INVENTORY_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (TinkerStationBlockEntity.isPart(source)
                && moveItemStackTo(source, PART_SLOT_START, PART_SLOT_END, false)) {
            // Part order is resolved by the block entity, so any empty part slot
            // is a valid shift-click destination.
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

    private Slot partSlot(int index, int x, int y) {
        return new Slot(inputs, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return TinkerStationBlockEntity.isPart(stack);
            }
        };
    }

    private record ClientTarget(BlockPos blockPos, TinkerStationBlockEntity blockEntity) {}
}
