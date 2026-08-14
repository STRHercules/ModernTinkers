package com.moderntinkers.content.smeltery;

import com.moderntinkers.content.fluid.MaterialFluids;
import net.minecraft.core.BlockPos;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** Fluid-backed casting transaction for ingots, nuggets, and tool parts. */
public final class CastingBlockEntity extends BlockEntity implements MenuProvider {
    public static final int TANK_CAPACITY = 2000;
    public static final int CAST_SLOT = 0;

    private final boolean basin;
    private final SimpleContainer cast = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            CastingBlockEntity.this.refreshResult();
            CastingBlockEntity.this.setChanged();
        }
    };
    private final FluidTank tank = new CallbackTank(TANK_CAPACITY,
            stack -> MaterialFluids.findByFluid(stack.getFluid()) != null);
    private final ResultContainer result = new ResultContainer();

    public CastingBlockEntity(BlockPos pos, BlockState state, boolean basin) {
        super(basin ? SmelteryContent.CASTING_BASIN_BLOCK_ENTITY.get()
                : SmelteryContent.CASTING_TABLE_BLOCK_ENTITY.get(), pos, state);
        this.basin = basin;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  CastingBlockEntity casting) {
        if (casting.tank.getFluidAmount() < casting.tank.getCapacity()) {
            SmelteryFluidNetwork.pullFromController(casting, casting.tank, 90);
        }
        casting.refreshResult();
    }

    public boolean isBasin() {
        return basin;
    }

    public Container getCastInventory() {
        return cast;
    }

    public FluidTank getFluidTank() {
        return tank;
    }

    public IFluidHandler getFluidHandler() {
        return tank;
    }

    public ItemStack getResult() {
        return result.getItem(0);
    }

    public ResultContainer getResultInventory() {
        return result;
    }

    public void refreshResult() {
        result.setItem(0, calculateResult());
    }

    public boolean canCraft() {
        return !result.isEmpty();
    }

    public void craft(Player player, int resultCount) {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        ItemStack castStack = cast.getItem(CAST_SLOT);
        if (!(castStack.getItem() instanceof CastingCastItem castItem)) {
            refreshResult();
            return;
        }
        int amount = castItem.amount();
        ItemStack output = castItem.createOutput(currentMaterialId());
        FluidStack expected = tank.getFluid().copy();
        expected.setAmount(amount);
        FluidStack simulated = tank.drain(amount, IFluidHandler.FluidAction.SIMULATE);
        if (amount <= 0 || output.isEmpty() || resultCount != output.getCount()
                || !SmelteryFluidNetwork.isExact(simulated, expected)) {
            refreshResult();
            return;
        }
        FluidStack drained = SmelteryFluidNetwork.drainExact(tank, expected);
        if (drained.isEmpty()) {
            refreshResult();
            return;
        }
        if (!castItem.reusable()) {
            cast.removeItem(CAST_SLOT, 1);
        }
        result.setItem(0, ItemStack.EMPTY);
        output.onCraftedBy(level, player, output.getCount());
        refreshResult();
        setChanged();
    }

    private ItemStack calculateResult() {
        ItemStack castStack = cast.getItem(CAST_SLOT);
        if (!(castStack.getItem() instanceof CastingCastItem castItem)) {
            return ItemStack.EMPTY;
        }
        MaterialFluids.FluidSet fluid = MaterialFluids.findByFluid(tank.getFluid().getFluid());
        if (fluid == null || tank.getFluidAmount() < castItem.amount()) {
            return ItemStack.EMPTY;
        }
        return castItem.createOutput(fluid.materialId());
    }

    private String currentMaterialId() {
        MaterialFluids.FluidSet fluid = MaterialFluids.findByFluid(tank.getFluid().getFluid());
        return fluid == null ? "" : fluid.materialId();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(basin
                ? "container.moderntinkers.casting_basin"
                : "container.moderntinkers.casting_table");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new CastingMenu(id, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put("Cast", cast.createTag(provider));
        tag.put("Tank", tank.writeToNBT(provider, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        cast.fromTag(tag.getList("Cast", Tag.TAG_COMPOUND), provider);
        ItemStack loadedCast = cast.getItem(CAST_SLOT);
        if (!(loadedCast.getItem() instanceof CastingCastItem)) {
            cast.setItem(CAST_SLOT, ItemStack.EMPTY);
        } else {
            loadedCast.setCount(Math.min(loadedCast.getCount(), loadedCast.getMaxStackSize()));
        }
        if (tag.contains("Tank", Tag.TAG_COMPOUND)) {
            tank.readFromNBT(provider, tag.getCompound("Tank"));
            if (!isValidLoadedFluid(tank.getFluid())) {
                tank.drain(tank.getFluidAmount(), IFluidHandler.FluidAction.EXECUTE);
            }
        }
        refreshResult();
    }

    private static boolean isValidLoadedFluid(FluidStack stack) {
        return !stack.isEmpty() && MaterialFluids.findByFluid(stack.getFluid()) != null;
    }

    private final class CallbackTank extends FluidTank {
        private CallbackTank(int capacity, java.util.function.Predicate<FluidStack> validator) {
            super(capacity, validator);
        }

        @Override
        protected void onContentsChanged() {
            CastingBlockEntity.this.refreshResult();
            CastingBlockEntity.this.setChanged();
        }
    }
}
