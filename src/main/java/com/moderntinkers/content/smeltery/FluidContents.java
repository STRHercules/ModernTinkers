package com.moderntinkers.content.smeltery;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** Converts removable machine contents into real full fluid buckets. */
final class FluidContents {
    private static final int BUCKET = 1000;

    private FluidContents() {}

    static void dropBuckets(Level level, BlockPos pos, FluidTank... tanks) {
        if (level.isClientSide) {
            return;
        }
        for (FluidTank tank : tanks) {
            while (tank.getFluidAmount() > 0) {
                FluidStack portion = tank.getFluid().copy();
                portion.setAmount(Math.min(BUCKET, portion.getAmount()));
                ItemStack container = portion.getAmount() == BUCKET
                        ? FluidUtil.getFilledBucket(portion) : ItemStack.EMPTY;
                if (container.isEmpty()) {
                    container = FluidRemainderItem.create(portion);
                }
                if (container.isEmpty()) {
                    break;
                }
                FluidStack drained = tank.drain(portion, IFluidHandler.FluidAction.EXECUTE);
                if (!SmelteryFluidNetwork.isExact(drained, portion)) {
                    SmelteryFluidNetwork.restore(tank, drained);
                    break;
                }
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), container);
            }
        }
    }
}
