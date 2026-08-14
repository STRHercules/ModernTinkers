package com.moderntinkers.content.smeltery;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Controller menu exposing three material slots and one fuel slot. */
public final class SmelteryMenu extends AbstractContainerMenu {
    private static final int PLAYER_INVENTORY_START = 4;
    private final SmelteryControllerBlockEntity blockEntity;
    private final Container inputs;
    private final ContainerLevelAccess access;
    private final SimpleContainerData data = new SimpleContainerData(3) {
        @Override
        public int get(int index) {
            if (blockEntity == null) {
                return 0;
            }
            return switch (index) {
                case 0 -> blockEntity.getProgress();
                case 1 -> blockEntity.getFuelTime();
                case 2 -> blockEntity.isFormed() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}
    };

    public SmelteryMenu(int id, Inventory inventory,
                        SmelteryControllerBlockEntity blockEntity) {
        this(id, inventory, blockEntity, blockEntity.getBlockPos());
    }

    private SmelteryMenu(int id, Inventory inventory,
                         SmelteryControllerBlockEntity blockEntity, BlockPos pos) {
        super(SmelteryContent.SMELTERY_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.inputs = blockEntity == null ? new SimpleContainer(4) : blockEntity.getInputInventory();
        Level level = blockEntity == null ? null : blockEntity.getLevel();
        this.access = level == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(level, pos);
        for (int slot = 0; slot < 3; slot++) {
            addSlot(new Slot(inputs, slot, 44 + slot * 18, 42) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return MelterBlockEntity.isMeltable(stack);
                }
            });
        }
        addSlot(new Slot(inputs, SmelteryControllerBlockEntity.FUEL_SLOT, 80, 62) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return MelterBlockEntity.isFuel(stack);
            }
        });
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        8 + column * 18, 102 + row * 18));
            }
        }
        for (int column = 0; column < 9; ++column) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 160));
        }
        addDataSlots(data);
    }

    public SmelteryMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, resolveClientTarget(inventory, buffer));
    }

    private SmelteryMenu(int id, Inventory inventory, ClientTarget target) {
        this(id, inventory, target.blockEntity(), target.blockPos());
    }

    private static ClientTarget resolveClientTarget(Inventory inventory,
                                                    RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return new ClientTarget(pos,
                entity instanceof SmelteryControllerBlockEntity smeltery ? smeltery : null);
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity == null || AbstractContainerMenu.stillValid(
                access, player, SmelteryContent.SMELTERY_CONTROLLER.get());
    }

    public int progress() {
        return data.get(0);
    }

    public boolean formed() {
        return data.get(2) > 0;
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
        if (index < PLAYER_INVENTORY_START) {
            if (!moveItemStackTo(source, PLAYER_INVENTORY_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (MelterBlockEntity.isFuel(source)) {
            if (!moveItemStackTo(source, 3, 4, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(source, 0, 3, false)) {
            return ItemStack.EMPTY;
        }
        if (source.isEmpty()) {
            clicked.setByPlayer(ItemStack.EMPTY);
        } else {
            clicked.setChanged();
        }
        return moved;
    }

    private record ClientTarget(BlockPos blockPos, SmelteryControllerBlockEntity blockEntity) {}
}
