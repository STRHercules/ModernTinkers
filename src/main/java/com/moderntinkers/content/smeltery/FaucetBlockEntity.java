package com.moderntinkers.content.smeltery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** Server-side faucet state and transactional fluid buffering. */
public final class FaucetBlockEntity extends BlockEntity {
    public static final int PACKET_SIZE = 90;
    public static final int MB_PER_TICK = 10;

    private FaucetState state = FaucetState.OFF;
    private FluidStack buffered = FluidStack.EMPTY;
    private FluidStack renderFluid = FluidStack.EMPTY;
    private boolean stopPouring;
    private boolean lastRedstoneState;

    public FaucetBlockEntity(BlockPos pos, BlockState blockState) {
        super(SmelteryContent.FAUCET_BLOCK_ENTITY.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  FaucetBlockEntity faucet) {
        faucet.tickServer();
    }

    public boolean isPouring() {
        return state != FaucetState.OFF;
    }

    public FluidStack getRenderFluid() {
        return renderFluid;
    }

    /** Drops a buffered packet when the faucet is removed before it finishes pouring. */
    public void dropContents() {
        if (level == null || level.isClientSide || buffered.isEmpty()) {
            return;
        }
        FluidTank contents = new FluidTank(PACKET_SIZE);
        contents.fill(buffered, IFluidHandler.FluidAction.EXECUTE);
        FluidContents.dropBuckets(level, worldPosition, contents);
        buffered = FluidStack.EMPTY;
        renderFluid = FluidStack.EMPTY;
        state = FaucetState.OFF;
    }

    public void activate() {
        if (level == null || level.isClientSide) {
            return;
        }
        switch (state) {
            case OFF -> {
                stopPouring = false;
                if (!startPacket()) {
                    state = lastRedstoneState ? FaucetState.POWERED : FaucetState.OFF;
                }
            }
            case POWERED -> {
                state = FaucetState.OFF;
                stopPouring = false;
                setChanged();
            }
            case POURING -> stopPouring = true;
        }
    }

    public void neighborChanged(BlockPos neighbor) {
        // Capabilities are queried each transfer, so a replaced neighbor is
        // observed without retaining a stale optional capability.
    }

    public void handleRedstone(boolean powered) {
        if (powered == lastRedstoneState) {
            return;
        }
        lastRedstoneState = powered;
        if (powered) {
            if (level != null) {
                level.scheduleTick(worldPosition, getBlockState().getBlock(), 2);
            }
        } else if (state == FaucetState.POWERED) {
            state = FaucetState.OFF;
            setChanged();
        }
    }

    private void tickServer() {
        if (state == FaucetState.OFF) {
            return;
        }
        if (!buffered.isEmpty()) {
            pourPacket();
            if (!buffered.isEmpty()) {
                return;
            }
        }
        if (stopPouring) {
            reset();
        } else if (!startPacket()) {
            state = lastRedstoneState ? FaucetState.POWERED : FaucetState.OFF;
            setChanged();
        }
    }

    private boolean startPacket() {
        if (level == null) {
            return false;
        }
        Direction facing = getBlockState().getValue(FaucetBlock.FACING);
        IFluidHandler source = level.getCapability(Capabilities.FluidHandler.BLOCK,
                worldPosition.relative(facing.getOpposite()), facing);
        IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK,
                worldPosition.below(), Direction.UP);
        if (source == null || target == null) {
            return false;
        }
        FluidStack simulated = source.drain(PACKET_SIZE, IFluidHandler.FluidAction.SIMULATE);
        if (simulated.isEmpty()) {
            return false;
        }
        int accepted = target.fill(simulated, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            return false;
        }
        FluidStack request = simulated.copy();
        request.setAmount(accepted);
        FluidStack drained = source.drain(request, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return false;
        }
        buffered = drained;
        renderFluid = drained.copy();
        state = FaucetState.POURING;
        setChanged();
        return true;
    }

    private void pourPacket() {
        if (level == null || buffered.isEmpty()) {
            return;
        }
        IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK,
                worldPosition.below(), Direction.UP);
        if (target == null) {
            return;
        }
        FluidStack portion = buffered.copy();
        portion.setAmount(Math.min(MB_PER_TICK, buffered.getAmount()));
        int accepted = target.fill(portion, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            return;
        }
        portion.setAmount(accepted);
        int filled = target.fill(portion, IFluidHandler.FluidAction.EXECUTE);
        if (filled > 0) {
            buffered.shrink(filled);
            if (buffered.isEmpty()) {
                renderFluid = FluidStack.EMPTY;
            }
            setChanged();
        }
    }

    private void reset() {
        stopPouring = false;
        buffered = FluidStack.EMPTY;
        renderFluid = FluidStack.EMPTY;
        state = FaucetState.OFF;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putByte("State", (byte) state.ordinal());
        tag.putBoolean("Stop", stopPouring);
        tag.putBoolean("LastRedstone", lastRedstoneState);
        if (!buffered.isEmpty()) {
            tag.put("Buffered", buffered.save(provider));
        }
        if (!renderFluid.isEmpty()) {
            tag.put("RenderFluid", renderFluid.save(provider));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        int index = tag.getByte("State");
        state = index >= 0 && index < FaucetState.values().length
                ? FaucetState.values()[index] : FaucetState.OFF;
        stopPouring = tag.getBoolean("Stop");
        lastRedstoneState = tag.getBoolean("LastRedstone");
        buffered = tag.contains("Buffered", Tag.TAG_COMPOUND)
                ? FluidStack.parse(provider, tag.getCompound("Buffered")).orElse(FluidStack.EMPTY)
                : FluidStack.EMPTY;
        renderFluid = tag.contains("RenderFluid", Tag.TAG_COMPOUND)
                ? FluidStack.parse(provider, tag.getCompound("RenderFluid")).orElse(FluidStack.EMPTY)
                : FluidStack.EMPTY;
    }

    private enum FaucetState {
        OFF,
        POURING,
        POWERED
    }
}
