package com.moderntinkers.content.tools;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/** NeoForge item capability backed by the same component tank used by tools. */
public final class TinkerItemFluidHandler implements IFluidHandlerItem {
    private final ItemStack stack;

    public TinkerItemFluidHandler(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public int getTanks() {
        return TinkersToolItem.hasFluidTank(stack) ? 1 : 0;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return TinkersToolItem.hasFluidTank(stack) && tank == 0
                ? TinkersToolItem.toolFluid(stack) : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return TinkersToolItem.hasFluidTank(stack) && tank == 0
                ? TinkersToolItem.TOOL_TANK_CAPACITY : 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack fluid) {
        return TinkersToolItem.hasFluidTank(stack) && tank == 0
                && fluid != null && !fluid.isEmpty()
                && fluid.getFluid() != Fluids.EMPTY;
    }

    @Override
    public int fill(FluidStack fluid, FluidAction action) {
        return isFluidValid(0, fluid) ? TinkersToolItem.fillToolFluid(stack, fluid, action) : 0;
    }

    @Override
    public FluidStack drain(FluidStack request, FluidAction action) {
        if (!TinkersToolItem.hasFluidTank(stack) || request == null || request.isEmpty()) {
            return FluidStack.EMPTY;
        }
        FluidStack stored = TinkersToolItem.toolFluid(stack);
        if (stored.isEmpty() || stored.getFluid() != request.getFluid()) {
            return FluidStack.EMPTY;
        }
        return TinkersToolItem.drainToolFluid(stack, request.getAmount(), action);
    }

    @Override
    public FluidStack drain(int amount, FluidAction action) {
        return TinkersToolItem.hasFluidTank(stack)
                ? TinkersToolItem.drainToolFluid(stack, amount, action) : FluidStack.EMPTY;
    }

    @Override
    public ItemStack getContainer() {
        return stack;
    }
}
