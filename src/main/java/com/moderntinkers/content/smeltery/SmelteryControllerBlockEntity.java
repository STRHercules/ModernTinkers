package com.moderntinkers.content.smeltery;

import com.moderntinkers.content.material.MaterialManager;
import com.moderntinkers.content.fluid.MaterialFluids;
import com.moderntinkers.content.recipe.TinkerRecipeManager;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

/** Server-side formed-smeltery controller and three-slot melting inventory. */
public final class SmelteryControllerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int TANK_CAPACITY = 64000;
    public static final int INPUT_SLOTS = 3;
    public static final int FUEL_SLOT = 3;
    public static final int PROCESS_TIME = 10;
    private static final int MAX_SAVED_FUEL = 24_000;

    private final SimpleContainer inputs = new SimpleContainer(INPUT_SLOTS + 1) {
        @Override
        public void setChanged() {
            super.setChanged();
            SmelteryControllerBlockEntity.this.setChanged();
        }
    };
    private final FluidTank tank = new CallbackTank();
    private final IFluidHandler handler = new StructureFluidHandler();
    private int progress;
    private int fuelTime;
    private int fuelTotal;
    private int fuelTemperature;
    private int meltStage;
    private String meltSignature = "";
    private long lastStructureCheck = Long.MIN_VALUE;
    private SmelteryStructure structure = new SmelteryStructure(false, 0, 0, 0);

    public SmelteryControllerBlockEntity(BlockPos pos, BlockState state) {
        super(SmelteryContent.SMELTERY_CONTROLLER_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  SmelteryControllerBlockEntity smeltery) {
        smeltery.tickServer();
    }

    private void tickServer() {
        // Advance existing fuel before checking the next recipe. Otherwise a
        // newly selected hotter recipe can freeze an old, cooler fuel stack
        // forever at the early temperature guard.
        if (fuelTime > 0) {
            fuelTime--;
        }
        if (!isFormed()) {
            progress = 0;
            setChanged();
            return;
        }
        Optional<Plan> plan = plan();
        if (plan.isEmpty() || !canFill(plan.get().fluid())) {
            progress = 0;
            setChanged();
            return;
        }
        if (fuelTime > 0 && fuelTemperature < plan.get().temperature()) {
            progress = 0;
            setChanged();
            return;
        }
        if (fuelTime <= 0 && !consumeFuel(plan.get().temperature())) {
            progress = 0;
            setChanged();
            return;
        }
        progress++;
        if (progress >= PROCESS_TIME) {
            Plan current = plan().orElse(null);
            if (current != null && canFill(current.fluid())) {
                FluidStack before = tank.getFluid().copy();
                int filled = tank.fill(current.fluid(), IFluidHandler.FluidAction.EXECUTE);
                boolean outputMatches = filled == current.fluid().getAmount()
                        && tank.getFluidAmount() == before.getAmount() + current.fluid().getAmount()
                        && FluidStack.isSameFluid(tank.getFluid(), current.fluid());
                if (outputMatches
                        && ++meltStage >= current.outputs().size()) {
                    inputs.removeItem(current.slot(), current.inputCount());
                    resetMeltProgress();
                } else if (!outputMatches && filled > 0) {
                    FluidStack rolledBack = tank.drain(filled,
                            IFluidHandler.FluidAction.EXECUTE);
                    if (!SmelteryFluidNetwork.isExactAmount(rolledBack, current.fluid(), filled)) {
                        setChanged();
                    }
                }
            }
            progress = 0;
        }
        setChanged();
    }

    private Optional<Plan> plan() {
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            ItemStack input = inputs.getItem(slot);
            TinkerRecipeManager.MeltingRecipe recipe = TinkerRecipeManager.findMelting(input);
            if (recipe != null && input.getCount() >= recipe.inputCount()) {
                MaterialFluids.FluidSet recipeFluid = MaterialFluids.get(recipe.fluidId());
                if (recipeFluid != null && recipeFluid.source().isBound()) {
                    prepareStage(slot, input);
                    FluidStack fluid = new FluidStack(recipeFluid.source().get(), recipe.amount());
                    return Optional.of(new Plan(slot,
                            List.of(fluid), fluid, recipe.inputCount(), recipe.temperature()));
                }
            }
            List<FluidStack> portions = MelterBlockEntity.meltingOutputs(input, 1);
            if (!portions.isEmpty()) {
                prepareStage(slot, input);
                if (meltStage >= portions.size()) {
                    resetMeltProgress();
                }
                FluidStack fluid = portions.get(Math.min(meltStage, portions.size() - 1));
                MaterialFluids.FluidSet set = MaterialFluids.findByFluid(fluid.getFluid());
                return Optional.of(new Plan(slot, portions, fluid, 1,
                        set == null ? 0 : set.temperature()));
            }
            Optional<MaterialManager.MaterialInput> found = MaterialManager.findInput(input);
            if (found.isEmpty()) {
                continue;
            }
            MaterialFluids.FluidSet fluid = MaterialFluids.get("molten_" + found.get().materialId());
            if (fluid != null && fluid.source().isBound()) {
                prepareStage(slot, input);
                FluidStack output = new FluidStack(fluid.source().get(), found.get().units() * 10);
                return Optional.of(new Plan(slot, List.of(output), output, 1,
                        fluid.temperature()));
            }
        }
        return Optional.empty();
    }

    public boolean isFormed() {
        return structure().formed();
    }

    public int structureRadius() {
        return structure().radius();
    }

    public int structureHeight() {
        return structure().height();
    }

    public int fluidCapacity() {
        return structure().fluidCapacity();
    }

    boolean owns(BlockPos target) {
        return structure().owns(worldPosition, target);
    }

    private SmelteryStructure structure() {
        if (level == null) {
            return structure;
        }
        long gameTime = level.getGameTime();
        if (gameTime - lastStructureCheck >= 10) {
            structure = SmelteryStructure.scan(level, worldPosition,
                    ((SmelteryControllerBlock) getBlockState().getBlock()).isScorched());
            lastStructureCheck = gameTime;
        }
        return structure;
    }

    private boolean canFill(FluidStack fluid) {
        if (fluid.isEmpty() || tank.getFluidAmount() + fluid.getAmount() > fluidCapacity()) {
            return false;
        }
        return tank.fill(fluid, IFluidHandler.FluidAction.SIMULATE) == fluid.getAmount();
    }

    private boolean consumeFuel(int requiredTemperature) {
        ItemStack fuel = inputs.getItem(FUEL_SLOT);
        int burn = MelterBlockEntity.burnTime(level, fuel);
        if (burn > 0 && MelterBlockEntity.fuelTemperature(fuel) >= requiredTemperature) {
            fuelTime = burn;
            fuelTotal = burn;
            fuelTemperature = MelterBlockEntity.fuelTemperature(fuel);
            Item remainder = fuel.is(Items.LAVA_BUCKET)
                    ? Items.BUCKET : fuel.getItem().getCraftingRemainingItem();
            fuel.shrink(1);
            if (fuel.isEmpty() && remainder != null) {
                inputs.setItem(FUEL_SLOT, new ItemStack(remainder));
            }
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

    public Container getInputInventory() {
        return inputs;
    }

    public FluidTank getFluidTank() {
        return tank;
    }

    public IFluidHandler getFluidHandler() {
        return handler;
    }

    void dropFluidContents() {
        if (level != null) {
            FluidContents.dropBuckets(level, worldPosition, tank);
        }
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

    public boolean isWorking() {
        return progress > 0;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.moderntinkers.smeltery");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new SmelteryMenu(id, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put("Items", inputs.createTag(provider));
        tag.put("Tank", tank.writeToNBT(provider, new CompoundTag()));
        tag.putInt("Progress", progress);
        tag.putInt("FuelTime", fuelTime);
        tag.putInt("FuelTotal", fuelTotal);
        tag.putInt("FuelTemperature", fuelTemperature);
        tag.putInt("MeltStage", meltStage);
        tag.putString("MeltSignature", meltSignature);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        inputs.fromTag(tag.getList("Items", Tag.TAG_COMPOUND), provider);
        if (tag.contains("Tank", Tag.TAG_COMPOUND)) {
            tank.readFromNBT(provider, tag.getCompound("Tank"));
            if (!isValidLoadedFluid(tank.getFluid())) {
                tank.drain(tank.getFluidAmount(), IFluidHandler.FluidAction.EXECUTE);
            }
        }
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            ItemStack input = inputs.getItem(slot);
            if (!input.isEmpty() && !MelterBlockEntity.isMeltable(input)) {
                inputs.setItem(slot, ItemStack.EMPTY);
            }
        }
        ItemStack loadedFuel = inputs.getItem(FUEL_SLOT);
        if (!loadedFuel.isEmpty() && !MelterBlockEntity.isFuel(level, loadedFuel)) {
            inputs.setItem(FUEL_SLOT, ItemStack.EMPTY);
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
        meltStage = Math.max(0, tag.getInt("MeltStage"));
        meltSignature = tag.getString("MeltSignature");
        if (meltSignature.length() > 512) {
            meltSignature = "";
            meltStage = 0;
        }
    }

    private void prepareStage(int slot, ItemStack input) {
        String signature = slot + "|" + input.getItem().toString() + "|" + input.getOrDefault(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if (!signature.equals(meltSignature)) {
            meltSignature = signature;
            meltStage = 0;
        }
    }

    private void resetMeltProgress() {
        meltStage = 0;
        meltSignature = "";
    }

    private static boolean isValidLoadedFluid(FluidStack stack) {
        return !stack.isEmpty() && MaterialFluids.findByFluid(stack.getFluid()) != null;
    }

    private record Plan(int slot, List<FluidStack> outputs, FluidStack fluid,
                        int inputCount, int temperature) {}

    private final class CallbackTank extends FluidTank {
        private CallbackTank() {
            super(TANK_CAPACITY,
                    stack -> MaterialFluids.findByFluid(stack.getFluid()) != null);
        }

        @Override
        protected void onContentsChanged() {
            SmelteryControllerBlockEntity.this.setChanged();
        }
    }

    private final class StructureFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tankIndex) {
            return tankIndex == 0 ? tank.getFluid() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tankIndex) {
            return tankIndex == 0 ? fluidCapacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tankIndex, FluidStack stack) {
            return tankIndex == 0 && MaterialFluids.findByFluid(stack.getFluid()) != null;
        }

        @Override
        public int fill(FluidStack stack, FluidAction action) {
            if (!isFluidValid(0, stack) || stack.isEmpty()) {
                return 0;
            }
            int accepted = Math.min(stack.getAmount(), Math.max(0,
                    fluidCapacity() - tank.getFluidAmount()));
            if (accepted <= 0) {
                return 0;
            }
            FluidStack portion = stack.copy();
            portion.setAmount(accepted);
            return tank.fill(portion, action);
        }

        @Override
        public FluidStack drain(FluidStack stack, FluidAction action) {
            return tank.drain(stack, action);
        }

        @Override
        public FluidStack drain(int amount, FluidAction action) {
            return tank.drain(amount, action);
        }
    }
}
