package com.moderntinkers.content.smeltery;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Bounded, server-side structure scan for the ModernTinkers smeltery.
 *
 * <p>The controller is the center of the bottom wall in this port. A one
 * block radius ring preserves the small starter smeltery, while additional
 * wall layers and height increase the internal volume. The scan deliberately
 * has a hard bound so malformed worlds cannot turn a block interaction into
 * an unbounded search.</p>
 */
public record SmelteryStructure(boolean formed, int radius, int height, int interiorVolume) {
    public static final int MAX_RADIUS = 4;
    public static final int MAX_HEIGHT = 16;

    private static final SmelteryStructure UNFORMED = new SmelteryStructure(false, 0, 0, 0);

    public static SmelteryStructure scan(Level level, BlockPos controller, boolean scorched) {
        if (level == null) {
            return UNFORMED;
        }

        SmelteryStructure best = UNFORMED;
        for (int radius = 1; radius <= MAX_RADIUS; radius++) {
            if (!ringValid(level, controller, radius, controller.getY(), scorched)) {
                continue;
            }
            int height = 1;
            while (height < MAX_HEIGHT
                    && ringValid(level, controller, radius, controller.getY() + height, scorched)
                    && interiorValid(level, controller, radius, height + 1, scorched)) {
                height++;
            }
            if (!interiorValid(level, controller, radius, height, scorched)) {
                continue;
            }
            int interior = Math.max(0, (radius * 2 - 1) * (radius * 2 - 1) * height - 1);
            SmelteryStructure candidate = new SmelteryStructure(true, radius, height, interior);
            if (candidate.volume() > best.volume()) {
                best = candidate;
            }
        }
        return best;
    }

    public int volume() {
        return radius * radius * height;
    }

    /** Four buckets for the base tank plus one bucket per usable interior cell. */
    public int fluidCapacity() {
        return Math.min(64_000, 4_000 + interiorVolume * 1_000);
    }

    /** Returns whether a block is inside this controller's formed footprint. */
    public boolean owns(BlockPos controller, BlockPos target) {
        if (!formed) {
            return false;
        }
        int x = target.getX() - controller.getX();
        int y = target.getY() - controller.getY();
        int z = target.getZ() - controller.getZ();
        return y >= 0 && y <= height && Math.abs(x) <= radius && Math.abs(z) <= radius;
    }

    private static boolean ringValid(Level level, BlockPos center, int radius, int y,
                                     boolean scorched) {
        int offset = y - center.getY();
        for (int x = -radius; x <= radius; x++) {
            if (!shell(level.getBlockState(center.offset(x, offset, -radius)), scorched)
                    || !shell(level.getBlockState(center.offset(x, offset, radius)), scorched)) {
                return false;
            }
        }
        for (int z = -radius + 1; z < radius; z++) {
            if (!shell(level.getBlockState(center.offset(-radius, offset, z)), scorched)
                    || !shell(level.getBlockState(center.offset(radius, offset, z)), scorched)) {
                return false;
            }
        }
        return true;
    }

    private static boolean interiorValid(Level level, BlockPos center, int radius, int height,
                                         boolean scorched) {
        for (int y = 0; y < height; y++) {
            for (int x = -radius + 1; x < radius; x++) {
                for (int z = -radius + 1; z < radius; z++) {
                    if (x == 0 && z == 0 && y == 0) {
                        continue;
                    }
                    BlockState state = level.getBlockState(center.offset(x, y, z));
                    if (!state.isAir() && state.getFluidState().isEmpty()
                            && !(y == 0 && shell(state, scorched))
                            && !component(state)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean shell(BlockState state, boolean scorched) {
        if (scorched) {
            return state.is(SmelteryContent.SCORCHED_BRICKS.get())
                    || state.is(SmelteryContent.SCORCHED_TANK.get())
                    || state.is(SmelteryContent.SCORCHED_DRAIN.get())
                    || state.is(SmelteryContent.SCORCHED_DUCT.get())
                    || state.is(SmelteryContent.SCORCHED_CHANNEL.get())
                    || state.is(SmelteryContent.FOUNDRY_CONTROLLER.get());
        }
        return state.is(SmelteryContent.SEARED_BRICKS.get())
                || state.is(SmelteryContent.SEARED_TANK.get())
                || state.is(SmelteryContent.SEARED_DRAIN.get())
                || state.is(SmelteryContent.SEARED_DUCT.get())
                || state.is(SmelteryContent.SEARED_CHANNEL.get())
                || state.is(SmelteryContent.SMELTERY_CONTROLLER.get());
    }

    private static boolean component(BlockState state) {
        return state.is(SmelteryContent.COPPER_GAUGE.get())
                || state.is(SmelteryContent.OBSIDIAN_GAUGE.get())
                || state.is(SmelteryContent.SEARED_CASTING_TANK.get())
                || state.is(SmelteryContent.CASTING_TABLE.get())
                || state.is(SmelteryContent.CASTING_BASIN.get());
    }
}
