package com.moderntinkers.content.smeltery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Bounded connected-component lookup shared by smeltery fluid consumers. */
final class SmelteryFluidNetwork {
    private static final int MAX_COMPONENTS = 1024;

    private SmelteryFluidNetwork() {}

    static SmelteryControllerBlockEntity findController(Level level, BlockPos origin) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(origin);
        visited.add(origin);
        int visitedCount = 0;
        while (!queue.isEmpty() && visitedCount++ < MAX_COMPONENTS) {
            BlockPos current = queue.removeFirst();
            if (level.getBlockEntity(current) instanceof SmelteryControllerBlockEntity controller
                    && controller.isFormed() && controller.owns(origin)) {
                return controller;
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (!visited.add(next) || !level.hasChunkAt(next)) {
                    continue;
                }
                BlockState state = level.getBlockState(next);
                if (isRoutingComponent(state)
                        || state.is(SmelteryContent.SMELTERY_CONTROLLER.get())
                        || state.is(SmelteryContent.FOUNDRY_CONTROLLER.get())) {
                    queue.addLast(next);
                }
            }
        }

        // Casting blocks and interior tanks are separated from the controller
        // by open smeltery space. Search only a bounded area and require the
        // candidate structure to own the block; proximity alone is unsafe
        // when two formed smelteries are nearby.
        SmelteryControllerBlockEntity best = null;
        int bestDistance = Integer.MAX_VALUE;
        int searched = 0;
        for (int y = -4; y <= 12 && searched < MAX_COMPONENTS; y++) {
            for (int x = -8; x <= 8 && searched < MAX_COMPONENTS; x++) {
                for (int z = -8; z <= 8 && searched < MAX_COMPONENTS; z++) {
                    searched++;
                    BlockPos candidate = origin.offset(x, y, z);
                    if (level.getBlockEntity(candidate) instanceof SmelteryControllerBlockEntity controller
                            && controller.isFormed() && controller.owns(origin)) {
                        int distance = Math.abs(x) + Math.abs(y) + Math.abs(z);
                        if (distance < bestDistance) {
                            best = controller;
                            bestDistance = distance;
                        }
                    }
                }
            }
        }
        return best;
    }

    static boolean pullFromController(CastingBlockEntity casting, FluidTank target, int amount) {
        if (casting.getLevel() == null || target.getFluidAmount() >= target.getCapacity()) {
            return false;
        }
        SmelteryControllerBlockEntity controller = findController(
                casting.getLevel(), casting.getBlockPos());
        if (controller == null) {
            return false;
        }
        FluidStack simulated = controller.getFluidHandler().drain(
                amount, IFluidHandler.FluidAction.SIMULATE);
        if (simulated.isEmpty()
                || target.fill(simulated, IFluidHandler.FluidAction.SIMULATE)
                != simulated.getAmount()) {
            return false;
        }
        FluidStack drained = controller.getFluidHandler().drain(
                simulated, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return false;
        }
        int filled = target.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        if (filled == drained.getAmount()) {
            return true;
        }
        if (filled > 0) {
            FluidStack remainder = drained.copy();
            remainder.setAmount(drained.getAmount() - filled);
            controller.getFluidHandler().fill(remainder, IFluidHandler.FluidAction.EXECUTE);
        } else {
            controller.getFluidHandler().fill(drained, IFluidHandler.FluidAction.EXECUTE);
        }
        return false;
    }

    static boolean isRoutingComponent(BlockState state) {
        return state.is(SmelteryContent.SEARED_DRAIN.get())
                || state.is(SmelteryContent.SCORCHED_DRAIN.get())
                || state.is(SmelteryContent.SEARED_DUCT.get())
                || state.is(SmelteryContent.SCORCHED_DUCT.get())
                || state.is(SmelteryContent.SEARED_CHANNEL.get())
                || state.is(SmelteryContent.SCORCHED_CHANNEL.get())
                || state.is(SmelteryContent.COPPER_GAUGE.get())
                || state.is(SmelteryContent.OBSIDIAN_GAUGE.get())
                || state.is(SmelteryContent.SEARED_CASTING_TANK.get())
                || state.is(SmelteryContent.SEARED_TANK.get())
                || state.is(SmelteryContent.SCORCHED_TANK.get());
    }
}
