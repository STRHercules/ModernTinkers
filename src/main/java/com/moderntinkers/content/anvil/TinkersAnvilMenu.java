package com.moderntinkers.content.anvil;

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
import com.moderntinkers.content.StaticContent;

/** Menu shared by both anvil variants with validated repair and shift-click paths. */
public final class TinkersAnvilMenu extends AbstractContainerMenu {
    public static final int OUTPUT_SLOT = 0;
    public static final int TOOL_SLOT = 1;
    public static final int MATERIAL_SLOT = 2;
    private static final int PLAYER_INVENTORY_START = 3;

    private final TinkersAnvilBlockEntity blockEntity;
    private final Container inputs;
    private final ContainerLevelAccess access;

    public TinkersAnvilMenu(int id, Inventory inventory, TinkersAnvilBlockEntity blockEntity) {
        this(id, inventory, blockEntity, blockEntity.getBlockPos());
    }

    private TinkersAnvilMenu(int id, Inventory inventory,
                             TinkersAnvilBlockEntity blockEntity, BlockPos pos) {
        super(StaticContent.TINKERS_ANVIL_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.inputs = blockEntity == null ? new SimpleContainer(2) : blockEntity.getInputInventory();
        Level level = blockEntity == null ? null : blockEntity.getLevel();
        this.access = level == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(level, pos);

        this.addSlot(new Slot(blockEntity == null ? new ResultContainer() : blockEntity.getResultInventory(),
                0, 134, 42) {
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
                    blockEntity.craft(player);
                }
                super.onTake(player, stack);
            }
        });
        this.addSlot(new Slot(inputs, TinkersAnvilBlockEntity.TOOL_SLOT, 44, 42) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return TinkersAnvilBlockEntity.isTool(stack);
            }
        });
        this.addSlot(new Slot(inputs, TinkersAnvilBlockEntity.MATERIAL_SLOT, 80, 42) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return TinkersAnvilBlockEntity.isRepairMaterial(stack);
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

    public TinkersAnvilMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, resolveClientTarget(inventory, buffer));
    }

    private TinkersAnvilMenu(int id, Inventory inventory, ClientTarget target) {
        this(id, inventory, target.blockEntity(), target.blockPos());
    }

    private static ClientTarget resolveClientTarget(Inventory inventory,
                                                    RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return new ClientTarget(pos,
                entity instanceof TinkersAnvilBlockEntity anvil ? anvil : null);
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity == null || AbstractContainerMenu.stillValid(
                access, player, blockEntity.getBlockState().getBlock());
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
            if (blockEntity == null
                    || !moveItemStackTo(source, PLAYER_INVENTORY_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            if (source.isEmpty()) {
                clicked.setByPlayer(ItemStack.EMPTY);
            } else {
                clicked.setChanged();
            }
            blockEntity.craft(player);
            return moved;
        }
        if (index == TOOL_SLOT || index == MATERIAL_SLOT) {
            if (!moveItemStackTo(source, PLAYER_INVENTORY_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (TinkersAnvilBlockEntity.isTool(source)) {
            if (!moveItemStackTo(source, TOOL_SLOT, TOOL_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (TinkersAnvilBlockEntity.isRepairMaterial(source)) {
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

    private record ClientTarget(BlockPos blockPos, TinkersAnvilBlockEntity blockEntity) {}
}
