package com.moderntinkers.content.fluid;

import com.moderntinkers.ModernTinkers;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Common molten-fluid boundary for the port. Each material gets a real
 * source/flowing fluid pair and bucket; casting machines can consume these
 * stable IDs without depending on an old Forge fluid wrapper.
 */
public final class MaterialFluids {
    private static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, ModernTinkers.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, ModernTinkers.MOD_ID);
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, ModernTinkers.MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, ModernTinkers.MOD_ID);
    private static final Map<String, FluidSet> SETS = new LinkedHashMap<>();

    static {
        register("molten_iron", 0xFFD8D8D8, 1000, MapColor.METAL);
        register("molten_gold", 0xFFFFD83D, 1100, MapColor.GOLD);
        register("molten_copper", 0xFFB86C4B, 1080, MapColor.COLOR_ORANGE);
        register("molten_cobalt", 0xFF3569D6, 1200, MapColor.COLOR_BLUE);
        register("molten_steel", 0xFF6F747A, 1200, MapColor.STONE);
        register("molten_slimesteel", 0xFF6EB8A5, 1100, MapColor.WARPED_WART_BLOCK);
        register("molten_amethyst_bronze", 0xFFB38BDB, 1050, MapColor.COLOR_PURPLE);
        register("molten_rose_gold", 0xFFFFB283, 1000, MapColor.TERRACOTTA_WHITE);
        register("molten_pig_iron", 0xFFD18B84, 950, MapColor.COLOR_PINK);
        register("molten_cinderslime", 0xFFF28D43, 1300, MapColor.COLOR_ORANGE);
        register("molten_queens_slime", 0xFF8BD35C, 1350, MapColor.COLOR_GREEN);
        register("molten_manyullyn", 0xFF7B42A9, 1450, MapColor.COLOR_PURPLE);
        register("molten_hepatizon", 0xFF765B9A, 1400, MapColor.TERRACOTTA_BLUE);
        register("molten_knightmetal", 0xFF6E8754, 1150, MapColor.GRASS);
        register("molten_knightslime", 0xFF5A4F70, 1250, MapColor.COLOR_BLACK);
        register("molten_soulsteel", 0xFF5A3C3C, 1300, MapColor.COLOR_BROWN);
        register("molten_netherite", 0xFF443B45, 1600, MapColor.COLOR_GRAY);
        register("molten_debris", 0xFF4A3F45, 1300, MapColor.COLOR_GRAY);
        register("molten_amethyst", 0xFF9A72C8, 900, MapColor.COLOR_PURPLE);
        register("molten_quartz", 0xFFE8D7D1, 1000, MapColor.COLOR_LIGHT_GRAY);
        register("molten_sky_slime", 0xFF72B4D6, 900, MapColor.COLOR_LIGHT_BLUE);
        register("molten_blood", 0xFFA12D38, 950, MapColor.COLOR_RED);
        register("molten_magma", 0xFFE06028, 1000, MapColor.COLOR_ORANGE);
        register("molten_meat_soup", 0xFF9D3B2F, 850, MapColor.COLOR_RED);
        register("molten_honey", 0xFFFFB52E, 600, MapColor.COLOR_YELLOW);
        register("molten_ender_slime", 0xFF4E4A86, 1000, MapColor.COLOR_PURPLE);
        register("molten_obsidian", 0xFF241B37, 1200, MapColor.COLOR_BLACK);
        register("molten_ichor", 0xFFDEAE3A, 1200, MapColor.COLOR_YELLOW);
        register("seared_stone", 0xFF6B4C43, 1000, MapColor.COLOR_BROWN);
        register("scorched_stone", 0xFF2A2021, 1000, MapColor.COLOR_BLACK);
    }

    private MaterialFluids() {}

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }

    public static FluidSet get(String id) {
        return SETS.get(id);
    }

    public static FluidSet findByFluid(Fluid fluid) {
        if (fluid == null) {
            return null;
        }
        for (FluidSet set : SETS.values()) {
            if (set.source.isBound() && fluid == set.source.get()
                    || set.flowing.isBound() && fluid == set.flowing.get()) {
                return set;
            }
        }
        return null;
    }

    public static List<FluidSet> all() {
        return List.copyOf(SETS.values());
    }

    public static void addBuckets(CreativeModeTab.Output output) {
        SETS.values().forEach(set -> output.accept(set.bucket().get()));
    }

    private static void register(String id, int color, int temperature, MapColor mapColor) {
        FluidSet set = new FluidSet(id, color, temperature, mapColor);
        SETS.put(id, set);
        set.type = FLUID_TYPES.register(id, () -> new FluidType(FluidType.Properties.create()
                .descriptionId("fluid.moderntinkers." + id)
                .density(2000)
                .viscosity(10000)
                .temperature(temperature)
                .lightLevel(10)
                .canSwim(false)
                .canDrown(false)
                .canConvertToSource(true)));
        set.source = FLUIDS.register(id, () -> new BaseFlowingFluid.Source(set.properties()));
        set.flowing = FLUIDS.register(id + "_flowing",
                () -> new BaseFlowingFluid.Flowing(set.properties()));
        set.block = BLOCKS.register(id, () -> new LiquidBlock(
                (net.minecraft.world.level.material.FlowingFluid) set.source.get(),
                BlockBehaviour.Properties.of()
                        .mapColor(mapColor)
                        .replaceable()
                        .noCollission()
                        .strength(100.0F)
                        .sound(SoundType.EMPTY)));
        set.bucket = ITEMS.register(id + "_bucket",
                () -> new BucketItem(set.source.get(), new Item.Properties().stacksTo(1)));
    }

    public static final class FluidSet {
        private final String id;
        private final int color;
        private final int temperature;
        private final MapColor mapColor;
        private DeferredHolder<FluidType, FluidType> type;
        private DeferredHolder<Fluid, Fluid> source;
        private DeferredHolder<Fluid, Fluid> flowing;
        private DeferredHolder<Block, Block> block;
        private DeferredHolder<Item, Item> bucket;

        private FluidSet(String id, int color, int temperature, MapColor mapColor) {
            this.id = id;
            this.color = color;
            this.temperature = temperature;
            this.mapColor = mapColor;
        }

        private BaseFlowingFluid.Properties properties() {
            return new BaseFlowingFluid.Properties(type::get, flowing::get, source::get)
                    .bucket(bucket::get)
                    .block(() -> (LiquidBlock) block.get())
                    .levelDecreasePerBlock(2)
                    .slopeFindDistance(4)
                    .tickRate(10)
                    .explosionResistance(100.0F);
        }

        public String id() {
            return id;
        }

        public String materialId() {
            return id.startsWith("molten_") ? id.substring("molten_".length()) : "";
        }

        public int color() {
            return color;
        }

        public int temperature() {
            return temperature;
        }

        public DeferredHolder<FluidType, FluidType> type() {
            return type;
        }

        public DeferredHolder<Fluid, Fluid> source() {
            return source;
        }

        public DeferredHolder<Fluid, Fluid> flowing() {
            return flowing;
        }

        public DeferredHolder<Block, Block> block() {
            return block;
        }

        public DeferredHolder<Item, Item> bucket() {
            return bucket;
        }
    }
}
