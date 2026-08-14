package com.moderntinkers.content;

import com.moderntinkers.ModernTinkers;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * First port slice of Tinkers' Construct's shared material items.
 *
 * <p>The reference implementation registers these from TinkerMaterials. This
 * class adapts that item subset to NeoForge's current deferred-registration API
 * without importing the legacy Mantle registration layer.</p>
 */
public final class MaterialItems {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, ModernTinkers.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ModernTinkers.MOD_ID);

    public static final DeferredHolder<Item, Item> COPPER_NUGGET =
            ITEMS.register("copper_nugget", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> NETHERITE_NUGGET =
            ITEMS.register("netherite_nugget", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> DEBRIS_NUGGET =
            ITEMS.register("debris_nugget", () -> tooltipItem(
                    "item.moderntinkers.debris_nugget.tooltip"));

    public static final DeferredHolder<Item, Item> NECROTIC_BONE =
            ITEMS.register("necrotic_bone", () -> tooltipItem(
                    "item.moderntinkers.necrotic_bone.tooltip"));
    public static final DeferredHolder<Item, Item> VENOMBONE =
            ITEMS.register("venombone", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> BLAZING_BONE =
            ITEMS.register("blazing_bone", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> NECRONIUM_BONE =
            ITEMS.register("necronium_bone", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GENERAL_TAB =
            CREATIVE_TABS.register("general", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.moderntinkers.general"))
                    .icon(() -> new ItemStack(COPPER_NUGGET.get()))
                    .displayItems(MaterialItems::addTabItems)
                    .build());

    private MaterialItems() {}

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }

    private static Item tooltipItem(String translationKey) {
        return new PortTooltipItem(new Item.Properties(), translationKey);
    }

    private static void addTabItems(CreativeModeTab.ItemDisplayParameters parameters,
                                    CreativeModeTab.Output output) {
        output.accept(COPPER_NUGGET.get());
        output.accept(DEBRIS_NUGGET.get());
        output.accept(NETHERITE_NUGGET.get());
        output.accept(NECROTIC_BONE.get());
        output.accept(VENOMBONE.get());
        output.accept(BLAZING_BONE.get());
        output.accept(NECRONIUM_BONE.get());
    }
}
