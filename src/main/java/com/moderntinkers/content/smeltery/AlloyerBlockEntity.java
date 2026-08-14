package com.moderntinkers.content.smeltery;

import com.moderntinkers.content.fluid.MaterialFluids;
import com.moderntinkers.content.recipe.TinkerRecipeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.ArrayList;
import java.util.List;

/** Fuelled, transactional alloy controller backed by directional fluid capabilities. */
public final class AlloyerBlockEntity extends BlockEntity implements MenuProvider {
    private static final int TANK_CAPACITY = 2000;
    /** Five inputs match the five non-output sides used by the reference mixer. */
    private static final int INPUT_TANKS = 5;
    private static final int PROCESS_TIME = 20;
    private static final int MAX_SAVED_FUEL = 24_000;

    private final FluidTank first = new CallbackTank();
    private final FluidTank second = new CallbackTank();
    private final FluidTank third = new CallbackTank();
    private final FluidTank fourth = new CallbackTank();
    private final FluidTank fifth = new CallbackTank();
    private final FluidTank output = new CallbackTank();
    private final FluidTank[] inputs = {first, second, third, fourth, fifth};
    private final SimpleContainer fuel = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            AlloyerBlockEntity.this.setChanged();
        }
    };
    private final IFluidHandler handler = new AlloyerFluidHandler();
    private int progress;
    private int fuelTime;
    private int fuelTotal;
    private int fuelTemperature;

    public AlloyerBlockEntity(BlockPos pos, BlockState state) {
        super(SmelteryContent.ALLOYER_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  AlloyerBlockEntity alloyer) {
        alloyer.tickServer();
    }

    private void tickServer() {
        if (!isOperational()) {
            progress = 0;
            return;
        }
        pullInputsFromNeighbors();
        AlloyMatch match = findMatch();
        if (match == null || output.fill(match.result(), IFluidHandler.FluidAction.SIMULATE)
                != match.result().getAmount()) {
            progress = 0;
            setChanged();
            return;
        }
        if (fuelTime > 0) {
            fuelTime--;
        }
        if (fuelTime > 0 && fuelTemperature < match.temperature()) {
            progress = 0;
            setChanged();
            return;
        }
        if (fuelTime <= 0 && !consumeFuel(match.temperature())) {
            progress = 0;
            setChanged();
            return;
        }
        progress++;
        if (progress >= PROCESS_TIME) {
            AlloyMatch current = findMatch();
            if (current != null && completeAlloy(current)) {
                progress = 0;
            }
        }
        setChanged();
    }

    /** An alloyer is active inside a formed structure or when paired with a heater below. */
    private boolean isOperational() {
        if (level == null) {
            return false;
        }
        if (SmelteryFluidNetwork.findController(level, worldPosition) != null) {
            return true;
        }
        BlockPos fuelSource = worldPosition.below();
        BlockState fuelState = level.getBlockState(fuelSource);
        return level.getBlockEntity(fuelSource) instanceof HeaterBlockEntity
                || (fuelState.is(SmelteryContent.SEARED_TANK.get())
                || fuelState.is(SmelteryContent.SCORCHED_TANK.get()))
                && level.getBlockEntity(fuelSource) instanceof FluidTankBlockEntity;
    }

    /** Pulls from adjacent seared/scorched tanks without losing fluid on partial fills. */
    private void pullInputsFromNeighbors() {
        if (level == null) {
            return;
        }
        for (Direction side : Direction.values()) {
            if (side == Direction.DOWN) {
                continue;
            }
            BlockPos neighbor = worldPosition.relative(side);
            if (!SmelteryFluidNetwork.isRoutingComponent(level.getBlockState(neighbor))) {
                continue;
            }
            IFluidHandler source = level.getCapability(Capabilities.FluidHandler.BLOCK,
                    neighbor, side.getOpposite());
            if (source == null) {
                continue;
            }
            FluidTank target = inputTank(side);
            SmelteryFluidNetwork.transferExact(source, target, 90);
        }
    }

    private AlloyMatch findMatch() {
        for (TinkerRecipeManager.AlloyRecipe recipe : TinkerRecipeManager.alloyRecipes()) {
            List<TankDrain> drains = new ArrayList<>();
            boolean[] used = new boolean[INPUT_TANKS];
            boolean matched = true;
            int temperature = 0;
            for (TinkerRecipeManager.FluidRequirement required : recipe.inputs()) {
                int tankIndex = -1;
                MaterialFluids.FluidSet inputFluid = MaterialFluids.get(required.fluidId());
                if (inputFluid != null) {
                    temperature = Math.max(temperature, inputFluid.temperature());
                }
                for (int index = 0; index < inputs.length; index++) {
                    if (!used[index] && required.fluidId().equals(fluidId(inputs[index].getFluid()))
                            && inputs[index].getFluidAmount() >= required.amount()) {
                        tankIndex = index;
                        break;
                    }
                }
                if (tankIndex < 0) {
                    matched = false;
                    break;
                }
                used[tankIndex] = true;
                drains.add(new TankDrain(inputs[tankIndex], required.amount(), required.fluidId()));
            }
            if (!matched) {
                continue;
            }
            MaterialFluids.FluidSet fluid = MaterialFluids.get(recipe.resultFluid());
            if (fluid != null && fluid.source().isBound()) {
                return new AlloyMatch(drains,
                        new FluidStack(fluid.source().get(), recipe.resultAmount()), temperature);
            }
        }
        return null;
    }

    /** Completes an alloy only when every input and the full output transfer succeed. */
    private boolean completeAlloy(AlloyMatch match) {
        if (output.fill(match.result(), IFluidHandler.FluidAction.SIMULATE)
                != match.result().getAmount()) {
            return false;
        }
        for (TankDrain requirement : match.inputs()) {
            FluidStack simulated = requirement.tank().drain(requirement.amount(),
                    IFluidHandler.FluidAction.SIMULATE);
            if (simulated.getAmount() != requirement.amount()
                    || !requirement.fluidId().equals(fluidId(simulated))) {
                return false;
            }
        }

        // Reserve the result before mutating any input. The tanks are all on
        // the server thread, so a successful simulation should be enough for
        // the normal path; keeping the rollback still protects capability
        // implementations that violate that contract.
        FluidStack outputBefore = output.getFluid().copy();
        int filled = output.fill(match.result(), IFluidHandler.FluidAction.EXECUTE);
        boolean outputMatches = filled == match.result().getAmount()
                && output.getFluidAmount() == outputBefore.getAmount() + match.result().getAmount()
                && FluidStack.isSameFluid(output.getFluid(), match.result());
        if (!outputMatches) {
            if (filled > 0) {
                rollbackOutput(match.result(), filled);
            }
            return false;
        }

        List<FluidStack> executed = new ArrayList<>();
        for (int index = 0; index < match.inputs().size(); index++) {
            TankDrain requirement = match.inputs().get(index);
            FluidStack expected = requirement.tank().getFluid().copy();
            expected.setAmount(requirement.amount());
            FluidStack actual = requirement.tank().drain(requirement.amount(),
                    IFluidHandler.FluidAction.EXECUTE);
            if (!SmelteryFluidNetwork.isExact(actual, expected)
                    || !requirement.fluidId().equals(fluidId(actual))) {
                if (!actual.isEmpty()) {
                    executed.add(actual);
                }
                refund(match.inputs(), executed);
                rollbackOutput(match.result(), filled);
                return false;
            }
            executed.add(actual);
        }
        return true;
    }

    private boolean rollbackOutput(FluidStack expected, int amount) {
        FluidStack rolledBack = output.drain(Math.max(0, amount),
                IFluidHandler.FluidAction.EXECUTE);
        return SmelteryFluidNetwork.isExactAmount(rolledBack, expected, amount);
    }

    private static void refund(List<TankDrain> requirements, List<FluidStack> drained) {
        for (int index = 0; index < drained.size() && index < requirements.size(); index++) {
            FluidStack portion = drained.get(index);
            if (!portion.isEmpty()) {
                requirements.get(index).tank().fill(portion, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    private boolean consumeFuel(int requiredTemperature) {
        ItemStack stack = fuel.getItem(0);
        int burn = MelterBlockEntity.burnTime(level, stack);
        if (burn > 0 && MelterBlockEntity.fuelTemperature(stack) >= requiredTemperature) {
            fuelTemperature = MelterBlockEntity.fuelTemperature(stack);
            fuelTime = burn;
            fuelTotal = burn;
            consumeFuelStack(stack);
            return true;
        }
        burn = MelterBlockEntity.consumeLavaFuel(level, worldPosition.below(), requiredTemperature);
        if (burn > 0) {
            fuelTime = burn;
            fuelTotal = burn;
            fuelTemperature = 2000;
            return true;
        }
        if (level != null && level.getBlockEntity(worldPosition.below())
                instanceof HeaterBlockEntity heater) {
            burn = heater.consumeFuel(requiredTemperature);
            if (burn > 0) {
                fuelTime = burn;
                fuelTotal = burn;
                fuelTemperature = heater.fuelTemperature();
                return true;
            }
        }
        return false;
    }

    private void consumeFuelStack(ItemStack stack) {
        Item remainder = stack.is(Items.LAVA_BUCKET)
                ? Items.BUCKET : stack.getItem().getCraftingRemainingItem();
        stack.shrink(1);
        if (stack.isEmpty() && remainder != null) {
            fuel.setItem(0, new ItemStack(remainder));
        }
    }

    private FluidTank inputTank(Direction side) {
        return switch (side) {
            case UP -> first;
            case NORTH -> second;
            case SOUTH -> third;
            case EAST -> fourth;
            case WEST -> fifth;
            case DOWN -> first;
        };
    }

    private static String fluidId(FluidStack stack) {
        MaterialFluids.FluidSet set = stack.isEmpty()
                ? null : MaterialFluids.findByFluid(stack.getFluid());
        return set == null ? "" : set.id();
    }

    public FluidTank firstTank() {
        return first;
    }

    public FluidTank secondTank() {
        return second;
    }

    public FluidTank thirdTank() {
        return third;
    }

    public FluidTank outputTank() {
        return output;
    }

    void dropFluidContents() {
        if (level != null) {
            FluidContents.dropBuckets(level, worldPosition, first, second, third, fourth,
                    fifth, output);
        }
    }

    public Container getFuelInventory() {
        return fuel;
    }

    public int getProgress() {
        return progress;
    }

    public int getFuelTime() {
        return fuelTime;
    }

    public int getFuelTotal() {
        return fuelTotal;
    }

    public IFluidHandler getFluidHandler(Direction side) {
        if (side == null) {
            return handler;
        }
        return side == Direction.DOWN ? new OutputFluidHandler() : new InputFluidHandler(inputTank(side));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.moderntinkers.alloyer");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new AlloyerMenu(id, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put("First", first.writeToNBT(provider, new CompoundTag()));
        tag.put("Second", second.writeToNBT(provider, new CompoundTag()));
        tag.put("Third", third.writeToNBT(provider, new CompoundTag()));
        tag.put("Fourth", fourth.writeToNBT(provider, new CompoundTag()));
        tag.put("Fifth", fifth.writeToNBT(provider, new CompoundTag()));
        tag.put("Output", output.writeToNBT(provider, new CompoundTag()));
        tag.put("Fuel", fuel.createTag(provider));
        tag.putInt("Progress", progress);
        tag.putInt("FuelTime", fuelTime);
        tag.putInt("FuelTotal", fuelTotal);
        tag.putInt("FuelTemperature", fuelTemperature);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        readTank(tag, "First", first, provider);
        readTank(tag, "Second", second, provider);
        readTank(tag, "Third", third, provider);
        readTank(tag, "Fourth", fourth, provider);
        readTank(tag, "Fifth", fifth, provider);
        readTank(tag, "Output", output, provider);
        if (tag.contains("Fuel", Tag.TAG_COMPOUND)) {
            fuel.fromTag(tag.getList("Fuel", Tag.TAG_COMPOUND), provider);
        }
        ItemStack loadedFuel = fuel.getItem(0);
        if (!loadedFuel.isEmpty() && !MelterBlockEntity.isFuel(loadedFuel)) {
            fuel.setItem(0, ItemStack.EMPTY);
        } else if (!loadedFuel.isEmpty()) {
            loadedFuel.setCount(Math.min(loadedFuel.getCount(), loadedFuel.getMaxStackSize()));
        }
        progress = Math.max(0, Math.min(PROCESS_TIME, tag.getInt("Progress")));
        fuelTime = Math.max(0, Math.min(MAX_SAVED_FUEL, tag.getInt("FuelTime")));
        fuelTotal = Math.max(0, Math.min(MAX_SAVED_FUEL, tag.getInt("FuelTotal")));
        if (fuelTotal > 0) {
            fuelTime = Math.min(fuelTime, fuelTotal);
        } else {
            fuelTime = 0;
        }
        fuelTemperature = Math.max(0, Math.min(10_000, tag.getInt("FuelTemperature")));
    }

    private static void readTank(CompoundTag tag, String key, FluidTank tank,
                                 HolderLookup.Provider provider) {
        if (tag.contains(key, Tag.TAG_COMPOUND)) {
            tank.readFromNBT(provider, tag.getCompound(key));
            if (!isValidLoadedFluid(tank.getFluid())) {
                tank.drain(tank.getFluidAmount(), IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    private static boolean isValidLoadedFluid(FluidStack stack) {
        return !stack.isEmpty() && MaterialFluids.findByFluid(stack.getFluid()) != null;
    }

    private record TankDrain(FluidTank tank, int amount, String fluidId) {}

    private record AlloyMatch(List<TankDrain> inputs, FluidStack result, int temperature) {}

    private final class CallbackTank extends FluidTank {
        private CallbackTank() {
            super(TANK_CAPACITY, stack -> MaterialFluids.findByFluid(stack.getFluid()) != null);
        }

        @Override
        protected void onContentsChanged() {
            AlloyerBlockEntity.this.setChanged();
        }
    }

    private final class InputFluidHandler implements IFluidHandler {
        private final FluidTank tank;

        private InputFluidHandler(FluidTank tank) {
            this.tank = tank;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? this.tank.getFluid() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? TANK_CAPACITY : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && MaterialFluids.findByFluid(stack.getFluid()) != null;
        }

        @Override
        public int fill(FluidStack stack, FluidAction action) {
            return isFluidValid(0, stack) ? this.tank.fill(stack, action) : 0;
        }

        @Override
        public FluidStack drain(FluidStack stack, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int amount, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }

    private final class OutputFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? output.getFluid() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? TANK_CAPACITY : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack stack, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack stack, FluidAction action) {
            return output.drain(stack, action);
        }

        @Override
        public FluidStack drain(int amount, FluidAction action) {
            return output.drain(amount, action);
        }
    }

    private final class AlloyerFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return INPUT_TANKS + 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank >= 0 && tank < INPUT_TANKS ? inputs[tank].getFluid()
                    : tank == INPUT_TANKS ? output.getFluid() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank >= 0 && tank <= INPUT_TANKS ? TANK_CAPACITY : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank >= 0 && tank < INPUT_TANKS
                    && MaterialFluids.findByFluid(stack.getFluid()) != null;
        }

        @Override
        public int fill(FluidStack stack, FluidAction action) {
            if (stack.isEmpty() || MaterialFluids.findByFluid(stack.getFluid()) == null) {
                return 0;
            }
            for (FluidTank input : inputs) {
                if (input.isEmpty() || FluidStack.isSameFluid(input.getFluid(), stack)) {
                    return input.fill(stack, action);
                }
            }
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack stack, FluidAction action) {
            return output.drain(stack, action);
        }

        @Override
        public FluidStack drain(int amount, FluidAction action) {
            return output.drain(amount, action);
        }
    }
}
