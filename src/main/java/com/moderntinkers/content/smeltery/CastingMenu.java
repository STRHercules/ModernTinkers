package com.moderntinkers.content.smeltery;

import com.moderntinkers.content.TinkerMenuTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Casting menu with reusable-cast validation and server-side output pickup. */
public final class CastingMenu extends AbstractContainerMenu {
    public static final int OUTPUT_SLOT = 0;
    public static final int CAST_SLOT = 1;
    private static final int PLAYER_INVENTORY_START = 2;

    private final CastingBlockEntity blockEntity;
    private final Container casts;
    private final ContainerLevelAccess access;

    public CastingMenu(int id, Inventory inventory, CastingBlockEntity blockEntity) {
        this(id, inventory, blockEntity, blockEntity.getBlockPos(), blockEntity.isBasin());
    }

    private CastingMenu(int id, Inventory inventory, CastingBlockEntity blockEntity,
                        BlockPos pos, boolean basin) {
        super(basin ? SmelteryContent.CASTING_BASIN_MENU.get()
                : SmelteryContent.CASTING_TABLE_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.casts = blockEntity == null ? new SimpleContainer(1) : blockEntity.getCastInventory();
        Level level = blockEntity == null ? null : blockEntity.getLevel();
        this.access = level == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(level, pos);

        this.addSlot(new Slot(blockEntity == null ? new ResultContainerView()
                : blockEntity.getResultInventory(), 0, 148, 42) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return blockEntity != null && blockEntity.canCraft();
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                if (blockEntity != null) {
                    blockEntity.craft(player, stack.getCount());
                }
                super.onTake(player, stack);
            }
        });
        this.addSlot(new Slot(casts, CastingBlockEntity.CAST_SLOT, 80, 42) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof CastingCastItem;
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
    }

    public CastingMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer, boolean basin) {
        this(id, inventory, resolveClientTarget(inventory, buffer, basin));
    }

    private CastingMenu(int id, Inventory inventory, ClientTarget target) {
        this(id, inventory, target.blockEntity(), target.blockPos(), target.basin());
    }

    private static ClientTarget resolveClientTarget(Inventory inventory,
                                                    RegistryFriendlyByteBuf buffer,
                                                    boolean basin) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return new ClientTarget(pos,
                entity instanceof CastingBlockEntity casting && casting.isBasin() == basin
                        ? casting : null, basin);
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity == null || AbstractContainerMenu.stillValid(
                access, player, blockEntity.isBasin()
                        ? SmelteryContent.CASTING_BASIN.get() : SmelteryContent.CASTING_TABLE.get());
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
            if (!TinkerMenuTransfer.canMoveEntireStack(slots, source,
                    PLAYER_INVENTORY_START, slots.size())) {
                return ItemStack.EMPTY;
            }
            if (blockEntity == null
                    || !moveItemStackTo(source, PLAYER_INVENTORY_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            if (source.isEmpty()) {
                clicked.setByPlayer(ItemStack.EMPTY);
            } else {
                clicked.setChanged();
            }
            blockEntity.craft(player, moved.getCount());
            return moved;
        }
        if (index == CAST_SLOT) {
            if (!moveItemStackTo(source, PLAYER_INVENTORY_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (source.getItem() instanceof CastingCastItem) {
            if (!moveItemStackTo(source, CAST_SLOT, CAST_SLOT + 1, false)) {
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

    /** ResultContainer is deliberately virtual: the fluid transaction owns the result. */
    private static final class ResultContainerView implements Container {
        @Override public int getContainerSize() { return 1; }
        @Override public boolean isEmpty() { return true; }
        @Override public ItemStack getItem(int slot) { return ItemStack.EMPTY; }
        @Override public ItemStack removeItem(int slot, int amount) { return ItemStack.EMPTY; }
        @Override public ItemStack removeItemNoUpdate(int slot) { return ItemStack.EMPTY; }
        @Override public void setItem(int slot, ItemStack stack) {}
        @Override public void setChanged() {}
        @Override public boolean stillValid(Player player) { return true; }
        @Override public void clearContent() {}
    }

    private record ClientTarget(BlockPos blockPos, CastingBlockEntity blockEntity, boolean basin) {}
}
