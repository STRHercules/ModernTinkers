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
            while (tank.getFluidAmount() >= BUCKET) {
                FluidStack portion = tank.drain(BUCKET, IFluidHandler.FluidAction.SIMULATE);
                if (portion.isEmpty() || portion.getAmount() != BUCKET) {
                    break;
                }
                ItemStack bucket = FluidUtil.getFilledBucket(portion);
                if (bucket.isEmpty()) {
                    break;
                }
                FluidStack drained = tank.drain(portion, IFluidHandler.FluidAction.EXECUTE);
                if (!SmelteryFluidNetwork.isExact(drained, portion)) {
                    SmelteryFluidNetwork.restore(tank, drained);
                    break;
                }
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), bucket);
            }
            if (tank.getFluidAmount() > 0) {
                FluidStack remainder = tank.getFluid().copy();
                ItemStack sample = FluidRemainderItem.create(remainder);
                if (!sample.isEmpty()) {
                    FluidStack drained = tank.drain(remainder,
                            IFluidHandler.FluidAction.EXECUTE);
                    if (SmelteryFluidNetwork.isExact(drained, remainder)) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), sample);
                    } else {
                        SmelteryFluidNetwork.restore(tank, drained);
                    }
                }
            }
        }
    }
}
