package com.moderntinkers.content.smeltery;

import com.moderntinkers.content.StaticContent;
import com.moderntinkers.content.material.MaterialManager;
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

/** Melter item/fuel menu; fluid transfer uses the block capability. */
public final class MelterMenu extends AbstractContainerMenu {
    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    private static final int PLAYER_INVENTORY_START = 2;

    private final MelterBlockEntity blockEntity;
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
                case 2 -> blockEntity.getFuelTotal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // The server owns all machine progress.
        }
    };

    public MelterMenu(int id, Inventory inventory, MelterBlockEntity blockEntity) {
        this(id, inventory, blockEntity, blockEntity.getBlockPos());
    }

    private MelterMenu(int id, Inventory inventory, MelterBlockEntity blockEntity, BlockPos pos) {
        super(SmelteryContent.MELTER_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.inputs = blockEntity == null ? new SimpleContainer(2) : blockEntity.getInputInventory();
        Level level = blockEntity == null ? null : blockEntity.getLevel();
        this.access = level == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(level, pos);

        this.addSlot(new Slot(inputs, MelterBlockEntity.INPUT_SLOT, 44, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return MelterBlockEntity.isMeltable(stack);
            }
        });
        this.addSlot(new Slot(inputs, MelterBlockEntity.FUEL_SLOT, 80, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return MelterBlockEntity.isFuel(stack);
            }
        });
        addPlayerSlots(inventory);
        addDataSlots(data);
    }

    public MelterMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, resolveClientTarget(inventory, buffer));
    }

    private MelterMenu(int id, Inventory inventory, ClientTarget target) {
        this(id, inventory, target.blockEntity(), target.blockPos());
    }

    private static ClientTarget resolveClientTarget(Inventory inventory, RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return new ClientTarget(pos, entity instanceof MelterBlockEntity melter ? melter : null);
    }

    private void addPlayerSlots(Inventory inventory) {
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        8 + column * 18, 94 + row * 18));
            }
        }
        for (int column = 0; column < 9; ++column) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 152));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity == null || AbstractContainerMenu.stillValid(
                access, player, SmelteryContent.MELTER.get());
    }

    public int progress() {
        return data.get(0);
    }

    public int fuelTime() {
        return data.get(1);
    }

    public int fuelTotal() {
        return data.get(2);
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
        if (index == INPUT_SLOT || index == FUEL_SLOT) {
            if (!moveItemStackTo(source, PLAYER_INVENTORY_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (MelterBlockEntity.isFuel(source)) {
            if (!moveItemStackTo(source, FUEL_SLOT, FUEL_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (MelterBlockEntity.isMeltable(source)) {
            if (!moveItemStackTo(source, INPUT_SLOT, INPUT_SLOT + 1, false)) {
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

    private record ClientTarget(BlockPos blockPos, MelterBlockEntity blockEntity) {}
}
