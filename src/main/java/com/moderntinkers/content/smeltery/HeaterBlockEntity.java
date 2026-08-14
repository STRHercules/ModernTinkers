package com.moderntinkers.content.smeltery;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Small persistent fuel buffer consumed by the melter above it. */
public final class HeaterBlockEntity extends BlockEntity {
    private static final int MAX_SAVED_TEMPERATURE = 10_000;
    private int fuelTemperature;
    private final SimpleContainer fuel = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            HeaterBlockEntity.this.setChanged();
        }
    };

    public HeaterBlockEntity(net.minecraft.core.BlockPos pos, BlockState state) {
        super(SmelteryContent.HEATER_BLOCK_ENTITY.get(), pos, state);
    }

    public Container getFuelInventory() {
        return fuel;
    }

    public boolean insertFuel(ItemStack stack) {
        if (stack.isEmpty() || !MelterBlockEntity.isFuel(level, stack)) {
            return false;
        }
        ItemStack stored = fuel.getItem(0);
        if (!stored.isEmpty() && !stored.is(stack.getItem())) {
            return false;
        }
        if (stored.isEmpty()) {
            fuel.setItem(0, stack.copyWithCount(1));
        } else if (stored.getCount() < stored.getMaxStackSize()) {
            stored.grow(1);
        } else {
            return false;
        }
        stack.shrink(1);
        setChanged();
        return true;
    }

    public ItemStack removeFuel() {
        ItemStack result = fuel.removeItem(0, fuel.getItem(0).getCount());
        setChanged();
        return result;
    }

    public int consumeFuel() {
        return consumeFuel(0);
    }

    public int consumeFuel(int requiredTemperature) {
        ItemStack stack = fuel.getItem(0);
        int burn = MelterBlockEntity.burnTime(level, stack);
        if (burn <= 0 || MelterBlockEntity.fuelTemperature(stack) < requiredTemperature) {
            return 0;
        }
        fuelTemperature = MelterBlockEntity.fuelTemperature(stack);
        ItemStack remainder = stack.is(Items.LAVA_BUCKET)
                ? new ItemStack(Items.BUCKET)
                : stack.getItem().getCraftingRemainingItem() == null
                ? ItemStack.EMPTY
                : new ItemStack(stack.getItem().getCraftingRemainingItem());
        stack.shrink(1);
        if (stack.isEmpty() && !remainder.isEmpty()) {
            fuel.setItem(0, remainder);
        }
        setChanged();
        return burn;
    }

    public int fuelTemperature() {
        return fuelTemperature;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put("Fuel", fuel.createTag(provider));
        tag.putInt("FuelTemperature", fuelTemperature);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        fuel.fromTag(tag.getList("Fuel", Tag.TAG_COMPOUND), provider);
        ItemStack loadedFuel = fuel.getItem(0);
        if (!loadedFuel.isEmpty() && !MelterBlockEntity.isFuel(level, loadedFuel)) {
            fuel.setItem(0, ItemStack.EMPTY);
        } else if (!loadedFuel.isEmpty()) {
            loadedFuel.setCount(Math.min(loadedFuel.getCount(), loadedFuel.getMaxStackSize()));
        }
        fuelTemperature = Math.max(0, Math.min(MAX_SAVED_TEMPERATURE,
                tag.getInt("FuelTemperature")));
        if (fuel.getItem(0).isEmpty()) {
            fuelTemperature = 0;
        }
    }
}
