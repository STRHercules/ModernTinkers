package com.moderntinkers.content.smeltery;

import com.moderntinkers.content.fluid.MaterialFluids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidUtil;

import java.util.List;

/** Stores a non-bucket-sized machine remainder without discarding fluid. */
public final class FluidRemainderItem extends Item {
    private static final String FLUID_KEY = "moderntinkers:fluid";
    private static final String AMOUNT_KEY = "moderntinkers:amount";
    private static final int CAPACITY = 1000;

    public FluidRemainderItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        FluidStack stored = fluid(context.getItemInHand());
        if (stored.isEmpty() || context.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        FluidActionResult result = FluidUtil.tryPlaceFluid(context.getPlayer(), context.getLevel(),
                context.getHand(), context.getClickedPos().relative(context.getClickedFace()),
                context.getItemInHand(), stored);
        if (!result.isSuccess()) {
            return InteractionResult.PASS;
        }
        if (!context.getPlayer().getAbilities().instabuild) {
            context.getPlayer().setItemInHand(context.getHand(), result.getResult());
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    public static ItemStack create(FluidStack fluid) {
        if (fluid == null || fluid.isEmpty() || fluid.getAmount() <= 0
                || fluid.getFluid() == net.minecraft.world.level.material.Fluids.EMPTY) {
            return ItemStack.EMPTY;
        }
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
        if (id == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = new ItemStack(SmelteryContent.FLUID_REMAINDER.get());
        CustomData.update(DataComponents.CUSTOM_DATA, result, tag -> {
            tag.putString(FLUID_KEY, id.toString());
            tag.putInt(AMOUNT_KEY, Math.min(CAPACITY, fluid.getAmount()));
        });
        return result;
    }

    public static FluidStack fluid(ItemStack stack) {
        if (!(stack.getItem() instanceof FluidRemainderItem)) {
            return FluidStack.EMPTY;
        }
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(FLUID_KEY));
        int amount = Math.max(0, Math.min(CAPACITY, tag.getInt(AMOUNT_KEY)));
        if (id == null || amount <= 0) {
            return FluidStack.EMPTY;
        }
        var fluid = BuiltInRegistries.FLUID.get(id);
        return fluid == null || fluid == net.minecraft.world.level.material.Fluids.EMPTY
                ? FluidStack.EMPTY : new FluidStack(fluid, amount);
    }

    static IFluidHandlerItem handler(ItemStack stack) {
        return new IFluidHandlerItem() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return tank == 0 ? fluid(stack) : FluidStack.EMPTY;
            }

            @Override
            public int getTankCapacity(int tank) {
                return tank == 0 ? CAPACITY : 0;
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack candidate) {
                return tank == 0 && candidate != null && !candidate.isEmpty()
                        && candidate.getFluid() != net.minecraft.world.level.material.Fluids.EMPTY;
            }

            @Override
            public int fill(FluidStack candidate, FluidAction action) {
                if (!isFluidValid(0, candidate)) {
                    return 0;
                }
                FluidStack stored = fluid(stack);
                if (!stored.isEmpty() && stored.getFluid() != candidate.getFluid()) {
                    return 0;
                }
                int accepted = Math.min(candidate.getAmount(), CAPACITY - stored.getAmount());
                if (accepted > 0 && action.execute()) {
                    FluidStack result = candidate.copy();
                    result.setAmount(accepted + stored.getAmount());
                    write(stack, result);
                }
                return Math.max(0, accepted);
            }

            @Override
            public FluidStack drain(FluidStack request, FluidAction action) {
                if (request == null || request.isEmpty()) {
                    return FluidStack.EMPTY;
                }
                FluidStack stored = fluid(stack);
                if (stored.isEmpty() || stored.getFluid() != request.getFluid()) {
                    return FluidStack.EMPTY;
                }
                return drain(Math.min(request.getAmount(), stored.getAmount()), action);
            }

            @Override
            public FluidStack drain(int amount, FluidAction action) {
                FluidStack stored = fluid(stack);
                if (stored.isEmpty() || amount <= 0) {
                    return FluidStack.EMPTY;
                }
                FluidStack result = stored.copy();
                result.setAmount(Math.min(amount, stored.getAmount()));
                if (action.execute()) {
                    int remaining = stored.getAmount() - result.getAmount();
                    if (remaining <= 0) {
                        clear(stack);
                    } else {
                        stored.setAmount(remaining);
                        write(stack, stored);
                    }
                }
                return result;
            }

            @Override
            public ItemStack getContainer() {
                return stack;
            }
        };
    }

    private static void write(ItemStack stack, FluidStack fluid) {
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
        if (id == null || fluid.getAmount() <= 0) {
            clear(stack);
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(FLUID_KEY, id.toString());
            tag.putInt(AMOUNT_KEY, Math.min(CAPACITY, fluid.getAmount()));
        });
    }

    private static void clear(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove(FLUID_KEY);
            tag.remove(AMOUNT_KEY);
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        FluidStack stored = fluid(stack);
        if (!stored.isEmpty()) {
            ResourceLocation id = BuiltInRegistries.FLUID.getKey(stored.getFluid());
            tooltip.add(Component.translatable("item.moderntinkers.fluid_remainder.contents",
                    stored.getAmount(), id == null ? "unknown" : id.toString()));
        }
    }
}
