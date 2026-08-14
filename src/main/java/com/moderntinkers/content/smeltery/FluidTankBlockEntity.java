package com.moderntinkers.content.smeltery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;


/** Tank block entity shared by the seared and scorched tank skins. */
public final class FluidTankBlockEntity extends BlockEntity {
    public static final int CAPACITY = 4000;
    private final FluidTank tank = new CallbackTank();

    public FluidTankBlockEntity(BlockPos pos, BlockState state) {
        this(state.is(SmelteryContent.SCORCHED_TANK.get())
                        ? SmelteryContent.SCORCHED_TANK_BLOCK_ENTITY.get()
                        : SmelteryContent.SEARED_TANK_BLOCK_ENTITY.get(),
                pos, state);
    }

    public FluidTankBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,
                                BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public IFluidHandler getFluidHandler() {
        return tank;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  FluidTankBlockEntity component) {
        if (!SmelteryFluidNetwork.isRoutingComponent(state)
                || component.tank.getFluidAmount() <= 0) {
            return;
        }
        SmelteryControllerBlockEntity controller = SmelteryFluidNetwork.findController(level, pos);
        if (controller == null) {
            return;
        }
        FluidStack available = component.tank.drain(90, IFluidHandler.FluidAction.SIMULATE);
        if (available.isEmpty()) {
            return;
        }
        int accepted = controller.getFluidHandler().fill(available,
                IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            return;
        }
        FluidStack drained = component.tank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return;
        }
        int filled = controller.getFluidHandler().fill(drained,
                IFluidHandler.FluidAction.EXECUTE);
        if (filled < drained.getAmount()) {
            FluidStack remainder = drained.copy();
            remainder.setAmount(drained.getAmount() - filled);
            component.tank.fill(remainder, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    public FluidTank getTank() {
        return tank;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put("Tank", tank.writeToNBT(provider, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("Tank", Tag.TAG_COMPOUND)) {
            tank.readFromNBT(provider, tag.getCompound("Tank"));
        }
    }

    private final class CallbackTank extends FluidTank {
        private CallbackTank() {
            super(CAPACITY, stack -> com.moderntinkers.content.fluid.MaterialFluids
                    .findByFluid(stack.getFluid()) != null);
        }

        @Override
        protected void onContentsChanged() {
            FluidTankBlockEntity.this.setChanged();
        }
    }
}
