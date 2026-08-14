package com.moderntinkers.content.smeltery;

import com.moderntinkers.content.fluid.MaterialFluids;
import com.moderntinkers.content.material.MaterialManager;
import com.moderntinkers.content.recipe.TinkerRecipeManager;
import com.moderntinkers.content.tools.MaterialPartItem;
import com.moderntinkers.content.tools.TinkersArmorItem;
import com.moderntinkers.content.tools.TinkersArrowItem;
import com.moderntinkers.content.tools.TinkersShieldItem;
import com.moderntinkers.content.tools.TinkersToolItem;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Fuelled one-item melter. Material parts and assembled Tinker items are
 * melted as their actual material portions, one fluid portion at a time.
 */
public final class MelterBlockEntity extends BlockEntity implements MenuProvider {
    public static final int TANK_CAPACITY = 4000;
    public static final int PROCESS_TIME = 20;
    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;

    private final SimpleContainer inputs = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            MelterBlockEntity.this.setChanged();
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            super.setItem(slot, stack);
            if (slot == INPUT_SLOT) {
                MelterBlockEntity.this.resetMeltProgress();
            }
        }
    };
    private final FluidTank tank = new CallbackTank(TANK_CAPACITY,
            stack -> MaterialFluids.findByFluid(stack.getFluid()) != null);
    private int progress;
    private int fuelTime;
    private int fuelTotal;
    private int fuelTemperature;
    private int meltStage;
    private String meltSignature = "";

    public MelterBlockEntity(BlockPos pos, BlockState state) {
        super(SmelteryContent.MELTER_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  MelterBlockEntity melter) {
        melter.tickServer();
    }

    private void tickServer() {
        if (fuelTime > 0) {
            fuelTime--;
        }
        Optional<Plan> plan = plan();
        if (plan.isEmpty() || !canFill(plan.get().fluid())) {
            progress = 0;
            setChanged();
            return;
        }
        if (fuelTime > 0 && fuelTemperature < plan.get().temperature()) {
            progress = 0;
            return;
        }
        if (fuelTime <= 0 && !consumeFuel(plan.get().temperature())) {
            progress = 0;
            return;
        }
        progress++;
        if (progress >= plan.get().time()) {
            Plan current = plan().orElse(null);
            if (current != null && canFill(current.fluid())) {
                tank.fill(current.fluid(), IFluidHandler.FluidAction.EXECUTE);
                if (++meltStage >= current.outputs().size()) {
                    inputs.removeItem(INPUT_SLOT, current.inputCount());
                    resetMeltProgress();
                }
            }
            progress = 0;
        }
        setChanged();
    }

    private Optional<Plan> plan() {
        ItemStack input = inputs.getItem(INPUT_SLOT);
        if (input.isEmpty()) {
            resetMeltProgress();
            return Optional.empty();
        }

        TinkerRecipeManager.MeltingRecipe recipe = TinkerRecipeManager.findMelting(input);
        if (recipe != null) {
            MaterialFluids.FluidSet fluid = MaterialFluids.get(recipe.fluidId());
            if (fluid != null && fluid.source().isBound()
                && input.getCount() >= recipe.inputCount()) {
                prepareStage(input);
                FluidStack output = new FluidStack(fluid.source().get(), recipe.amount());
                return Optional.of(new Plan(
                        List.of(output), output, recipe.time(), recipe.temperature(),
                        recipe.inputCount()));
            }
        }

        List<FluidStack> portions = meltingOutputs(input, 1);
        if (!portions.isEmpty()) {
            prepareStage(input);
            if (meltStage >= portions.size()) {
                resetMeltProgress();
            }
            FluidStack fluid = portions.get(Math.min(meltStage, portions.size() - 1));
            MaterialFluids.FluidSet set = MaterialFluids.findByFluid(fluid.getFluid());
            return Optional.of(new Plan(portions, fluid,
                    Math.max(8, Math.min(40, fluid.getAmount() / 5)),
                    set == null ? 0 : set.temperature(), 1));
        }

        return MaterialManager.findInput(input).flatMap(found -> {
            MaterialFluids.FluidSet fluid = MaterialFluids.get("molten_" + found.materialId());
            if (fluid == null || !fluid.source().isBound()) {
                return Optional.empty();
            }
            prepareStage(input);
            FluidStack output = new FluidStack(fluid.source().get(), found.units() * 10);
            return Optional.of(new Plan(
                    List.of(output), output, PROCESS_TIME, fluid.temperature(), 1));
        });
    }

    private boolean canFill(FluidStack fluid) {
        return tank.fill(fluid, IFluidHandler.FluidAction.SIMULATE) == fluid.getAmount();
    }

    private boolean consumeFuel(int requiredTemperature) {
        ItemStack fuel = inputs.getItem(FUEL_SLOT);
        int burn = burnTime(fuel);
        if (burn > 0 && fuelTemperature(fuel) < requiredTemperature) {
            burn = 0;
        }
        if (burn <= 0) {
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
        fuelTime = burn;
        fuelTotal = burn;
        fuelTemperature = fuelTemperature(fuel);
        Item remainder = fuel.is(Items.LAVA_BUCKET)
                ? Items.BUCKET : fuel.getItem().getCraftingRemainingItem();
        fuel.shrink(1);
        if (fuel.isEmpty() && remainder != null) {
            inputs.setItem(FUEL_SLOT, new ItemStack(remainder));
        }
        return true;
    }

    static int burnTime(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.is(Items.LAVA_BUCKET)) {
            return 20000;
        }
        if (stack.is(Items.BLAZE_ROD)) {
            return 2400;
        }
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) {
            return 1600;
        }
        if (stack.is(Items.BLAZE_POWDER)) {
            return 1200;
        }
        return 0;
    }

    static int fuelTemperature(ItemStack stack) {
        if (stack.is(Items.LAVA_BUCKET)) {
            return 2000;
        }
        if (stack.is(Items.BLAZE_ROD) || stack.is(Items.BLAZE_POWDER)) {
            return 1400;
        }
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) {
            return 1000;
        }
        return 0;
    }

    public Container getInputInventory() {
        return inputs;
    }

    public FluidTank getFluidTank() {
        return tank;
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

    public static boolean isFuel(ItemStack stack) {
        return burnTime(stack) > 0;
    }

    public static boolean isMeltable(ItemStack stack) {
        TinkerRecipeManager.MeltingRecipe recipe = TinkerRecipeManager.findMelting(stack);
        if (recipe != null) {
            MaterialFluids.FluidSet fluid = MaterialFluids.get(recipe.fluidId());
            if (fluid != null && fluid.source().isBound()) {
                return stack.getCount() >= recipe.inputCount();
            }
        }
        if (!meltingOutputs(stack, 1).isEmpty()) {
            return true;
        }
        return MaterialManager.findInput(stack)
                .map(found -> MaterialFluids.get("molten_" + found.materialId()) != null)
                .orElse(false);
    }

    public boolean isWorking() {
        return progress > 0;
    }

    public IFluidHandler getFluidHandler() {
        return tank;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.moderntinkers.melter");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new MelterMenu(id, playerInventory, this);
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
        }
        progress = tag.getInt("Progress");
        fuelTime = tag.getInt("FuelTime");
        fuelTotal = tag.getInt("FuelTotal");
        fuelTemperature = tag.getInt("FuelTemperature");
        meltStage = tag.getInt("MeltStage");
        meltSignature = tag.getString("MeltSignature");
    }

    private void prepareStage(ItemStack input) {
        String signature = input.getItem().toString() + "|"
                + input.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
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

    public static List<FluidStack> meltingOutputs(ItemStack stack) {
        return meltingOutputs(stack, Math.max(1, stack.getCount()));
    }

    /**
     * Returns the material portions for a bounded number of input items.
     * Machine plans intentionally request one item at a time; the public
     * stack-sized overload remains useful for inspection and tooltip logic.
     */
    public static List<FluidStack> meltingOutputs(ItemStack stack, int itemCount) {
        int count = Math.max(1, itemCount);
        List<TinkersToolItem.MaterialAmount> amounts = new ArrayList<>();
        if (TinkersToolItem.isTool(stack)) {
            amounts.addAll(TinkersToolItem.meltingMaterials(stack));
        } else if (TinkersArmorItem.isArmor(stack)) {
            amounts.addAll(TinkersArmorItem.meltingMaterials(stack));
        } else if (stack.getItem() instanceof TinkersShieldItem) {
            amounts.addAll(TinkersShieldItem.meltingMaterials(stack));
        } else if (stack.getItem() instanceof TinkersArrowItem) {
            amounts.addAll(TinkersArrowItem.meltingMaterials(stack, count));
        } else if (MaterialPartItem.isPart(stack)) {
            String material = MaterialPartItem.getMaterial(stack);
            int units = MaterialPartItem.materialUnits(stack) * count;
            if (!material.isEmpty() && units > 0) {
                amounts.add(new TinkersToolItem.MaterialAmount(material, units));
            }
        }
        List<FluidStack> outputs = new ArrayList<>();
        for (TinkersToolItem.MaterialAmount amount : amounts) {
            MaterialFluids.FluidSet fluid = MaterialFluids.get("molten_" + amount.materialId());
            if (fluid == null || !fluid.source().isBound()) {
                return List.of();
            }
            outputs.add(new FluidStack(fluid.source().get(), amount.units() * 10));
        }
        return outputs;
    }

    private record Plan(List<FluidStack> outputs, FluidStack fluid, int time,
                        int temperature, int inputCount) {}

    private final class CallbackTank extends FluidTank {
        private CallbackTank(int capacity, java.util.function.Predicate<FluidStack> validator) {
            super(capacity, validator);
        }

        @Override
        protected void onContentsChanged() {
            MelterBlockEntity.this.setChanged();
        }
    }
}
