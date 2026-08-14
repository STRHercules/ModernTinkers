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
        return transferExact(controller.getFluidHandler(), target, amount);
    }

    /** Moves one simulated packet and rolls back any partial or mismatched execution. */
    static boolean transferExact(IFluidHandler source, IFluidHandler target, int amount) {
        if (source == null || target == null || amount <= 0) {
            return false;
        }
        FluidStack simulated = source.drain(amount, IFluidHandler.FluidAction.SIMULATE);
        if (simulated.isEmpty()) {
            return false;
        }
        int expected = simulated.getAmount();
        if (target.fill(simulated, IFluidHandler.FluidAction.SIMULATE) != expected) {
            return false;
        }
        FluidStack drained = source.drain(simulated, IFluidHandler.FluidAction.EXECUTE);
        if (!isExact(drained, simulated)) {
            restore(source, drained);
            return false;
        }
        int filled = target.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        if (filled == expected) {
            return true;
        }

        int accepted = Math.max(0, Math.min(filled, expected));
        FluidStack rollbackRequest = drained.copy();
        rollbackRequest.setAmount(accepted);
        FluidStack rolledBack = accepted == 0
                ? FluidStack.EMPTY : target.drain(rollbackRequest, IFluidHandler.FluidAction.EXECUTE);
        int removed = rolledBack != null && !rolledBack.isEmpty()
                && FluidStack.isSameFluid(rolledBack, drained)
                ? Math.min(accepted, rolledBack.getAmount()) : 0;
        int retained = accepted - removed;
        FluidStack refund = drained.copy();
        refund.setAmount(Math.max(0, expected - retained));
        restore(source, refund);
        return false;
    }

    /** Executes a previously simulated drain, rejecting and refunding mismatches. */
    static FluidStack drainExact(IFluidHandler source, FluidStack expected) {
        if (source == null || expected == null || expected.isEmpty()) {
            return FluidStack.EMPTY;
        }
        FluidStack simulated = source.drain(expected, IFluidHandler.FluidAction.SIMULATE);
        if (!isExact(simulated, expected)) {
            return FluidStack.EMPTY;
        }
        FluidStack drained = source.drain(expected, IFluidHandler.FluidAction.EXECUTE);
        if (isExact(drained, expected)) {
            return drained;
        }
        restore(source, drained);
        return FluidStack.EMPTY;
    }

    static boolean isExact(FluidStack actual, FluidStack expected) {
        return isExactAmount(actual, expected, expected == null ? 0 : expected.getAmount())
                && (expected == null || FluidStack.isSameFluid(actual, expected));
    }

    static boolean isExactAmount(FluidStack actual, FluidStack expected, int amount) {
        return amount == 0 ? actual == null || actual.isEmpty()
                : actual != null && actual.getAmount() == amount
                && !actual.isEmpty()
                && expected != null
                && FluidStack.isSameFluid(actual, expected);
    }

    static void restore(IFluidHandler target, FluidStack stack) {
        if (target != null && stack != null && !stack.isEmpty()) {
            if (target.fill(stack, IFluidHandler.FluidAction.SIMULATE) == stack.getAmount()) {
                target.fill(stack, IFluidHandler.FluidAction.EXECUTE);
            }
        }
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
