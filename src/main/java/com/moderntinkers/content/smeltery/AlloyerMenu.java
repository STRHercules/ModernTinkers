package com.moderntinkers.content.smeltery;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Player inventory menu for the capability-driven alloyer. */
public final class AlloyerMenu extends AbstractContainerMenu {
    private static final int FUEL_SLOT = 0;
    private static final int PLAYER_INVENTORY_START = 1;
    private static final int PLAYER_INVENTORY_END = 37;
    private final AlloyerBlockEntity blockEntity;
    private final net.minecraft.world.Container fuel;
    private final ContainerLevelAccess access;
    private final SimpleContainerData data = new SimpleContainerData(1) {
        @Override
        public int get(int index) {
            return blockEntity == null ? 0 : blockEntity.getProgress();
        }

        @Override
        public void set(int index, int value) {}
    };

    public AlloyerMenu(int id, Inventory inventory, AlloyerBlockEntity blockEntity) {
        this(id, inventory, blockEntity, blockEntity.getBlockPos());
    }

    private AlloyerMenu(int id, Inventory inventory, AlloyerBlockEntity blockEntity, BlockPos pos) {
        super(SmelteryContent.ALLOYER_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.fuel = blockEntity == null ? new net.minecraft.world.SimpleContainer(1)
                : blockEntity.getFuelInventory();
        Level level = blockEntity == null ? null : blockEntity.getLevel();
        this.access = level == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(level, pos);
        addSlot(new Slot(fuel, FUEL_SLOT, 151, 32) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return MelterBlockEntity.isFuel(stack);
            }
        });
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; ++column) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
        addDataSlots(data);
    }

    public AlloyerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, resolveClientTarget(inventory, buffer));
    }

    private static ClientTarget resolveClientTarget(Inventory inventory, RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return new ClientTarget(pos, entity instanceof AlloyerBlockEntity alloyer ? alloyer : null);
    }

    private AlloyerMenu(int id, Inventory inventory, ClientTarget target) {
        this(id, inventory, target.blockEntity(), target.blockPos());
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity == null || AbstractContainerMenu.stillValid(
                access, player, SmelteryContent.ALLOYER.get());
    }

    public int progress() {
        return data.get(0);
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
        if (index == FUEL_SLOT) {
            if (!moveItemStackTo(source, PLAYER_INVENTORY_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= PLAYER_INVENTORY_START
                && index < PLAYER_INVENTORY_END && MelterBlockEntity.isFuel(source)) {
            if (!moveItemStackTo(source, FUEL_SLOT, FUEL_SLOT + 1, false)) {
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

    private record ClientTarget(BlockPos blockPos, AlloyerBlockEntity blockEntity) {}
}
