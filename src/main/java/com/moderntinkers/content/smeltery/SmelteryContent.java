package com.moderntinkers.content.smeltery;

import com.moderntinkers.ModernTinkers;
import com.moderntinkers.content.StaticContent;
import com.moderntinkers.content.tools.TinkerItemFluidHandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/** Registrations for the common Melter, casting, and alloying loop. */
public final class SmelteryContent {
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, ModernTinkers.MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, ModernTinkers.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ModernTinkers.MOD_ID);
    private static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ModernTinkers.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ModernTinkers.MOD_ID);
    private static final List<DeferredHolder<Item, Item>> CASTS = new ArrayList<>();

    public static final DeferredHolder<Block, Block> MELTER = registerBlock("melter", BlockKind.MELTER);
    public static final DeferredHolder<Block, Block> CASTING_TABLE = registerBlock(
            "casting_table", BlockKind.TABLE);
    public static final DeferredHolder<Block, Block> CASTING_BASIN = registerBlock(
            "casting_basin", BlockKind.BASIN);
    public static final DeferredHolder<Block, Block> ALLOYER = registerBlock("alloyer", BlockKind.ALLOYER);
    public static final DeferredHolder<Block, Block> SEARED_TANK = registerBlock(
            "seared_tank", BlockKind.SEARED_TANK);
    public static final DeferredHolder<Block, Block> SCORCHED_TANK = registerBlock(
            "scorched_tank", BlockKind.SCORCHED_TANK);
    public static final DeferredHolder<Block, Block> SEARED_DRAIN = registerBlock(
            "seared_drain", BlockKind.DRAIN);
    public static final DeferredHolder<Block, Block> SCORCHED_DRAIN = registerBlock(
            "scorched_drain", BlockKind.DRAIN);
    public static final DeferredHolder<Block, Block> SEARED_DUCT = registerBlock(
            "seared_duct", BlockKind.DUCT);
    public static final DeferredHolder<Block, Block> SCORCHED_DUCT = registerBlock(
            "scorched_duct", BlockKind.DUCT);
    public static final DeferredHolder<Block, Block> SEARED_CHANNEL = registerBlock(
            "seared_channel", BlockKind.CHANNEL);
    public static final DeferredHolder<Block, Block> SCORCHED_CHANNEL = registerBlock(
            "scorched_channel", BlockKind.CHANNEL);
    public static final DeferredHolder<Block, Block> COPPER_GAUGE = registerBlock(
            "copper_gauge", BlockKind.GAUGE);
    public static final DeferredHolder<Block, Block> OBSIDIAN_GAUGE = registerBlock(
            "obsidian_gauge", BlockKind.GAUGE);
    public static final DeferredHolder<Block, Block> SEARED_CASTING_TANK = registerBlock(
            "seared_casting_tank", BlockKind.CASTING_TANK);
    public static final DeferredHolder<Block, Block> HEATER = registerBlock("heater", BlockKind.HEATER);
    public static final DeferredHolder<Block, Block> FAUCET = registerBlock("faucet", BlockKind.FAUCET);
    public static final DeferredHolder<Block, Block> SEARED_BRICKS = registerBlock(
            "seared_bricks", BlockKind.SEARED_BRICKS);
    public static final DeferredHolder<Block, Block> SCORCHED_BRICKS = registerBlock(
            "scorched_bricks", BlockKind.SCORCHED_BRICKS);
    public static final DeferredHolder<Block, Block> SMELTERY_CONTROLLER = registerBlock(
            "smeltery_controller", BlockKind.SMELTERY_CONTROLLER);
    public static final DeferredHolder<Block, Block> FOUNDRY_CONTROLLER = registerBlock(
            "foundry_controller", BlockKind.FOUNDRY_CONTROLLER);

    public static final DeferredHolder<Item, Item> MELTER_ITEM = registerBlockItem("melter", MELTER);
    public static final DeferredHolder<Item, Item> CASTING_TABLE_ITEM = registerBlockItem(
            "casting_table", CASTING_TABLE);
    public static final DeferredHolder<Item, Item> CASTING_BASIN_ITEM = registerBlockItem(
            "casting_basin", CASTING_BASIN);
    public static final DeferredHolder<Item, Item> ALLOYER_ITEM = registerBlockItem("alloyer", ALLOYER);
    public static final DeferredHolder<Item, Item> SEARED_TANK_ITEM = registerBlockItem(
            "seared_tank", SEARED_TANK);
    public static final DeferredHolder<Item, Item> SCORCHED_TANK_ITEM = registerBlockItem(
            "scorched_tank", SCORCHED_TANK);
    public static final DeferredHolder<Item, Item> SEARED_DRAIN_ITEM = registerBlockItem(
            "seared_drain", SEARED_DRAIN);
    public static final DeferredHolder<Item, Item> SCORCHED_DRAIN_ITEM = registerBlockItem(
            "scorched_drain", SCORCHED_DRAIN);
    public static final DeferredHolder<Item, Item> SEARED_DUCT_ITEM = registerBlockItem(
            "seared_duct", SEARED_DUCT);
    public static final DeferredHolder<Item, Item> SCORCHED_DUCT_ITEM = registerBlockItem(
            "scorched_duct", SCORCHED_DUCT);
    public static final DeferredHolder<Item, Item> SEARED_CHANNEL_ITEM = registerBlockItem(
            "seared_channel", SEARED_CHANNEL);
    public static final DeferredHolder<Item, Item> SCORCHED_CHANNEL_ITEM = registerBlockItem(
            "scorched_channel", SCORCHED_CHANNEL);
    public static final DeferredHolder<Item, Item> COPPER_GAUGE_ITEM = registerBlockItem(
            "copper_gauge", COPPER_GAUGE);
    public static final DeferredHolder<Item, Item> OBSIDIAN_GAUGE_ITEM = registerBlockItem(
            "obsidian_gauge", OBSIDIAN_GAUGE);
    public static final DeferredHolder<Item, Item> SEARED_CASTING_TANK_ITEM = registerBlockItem(
            "seared_casting_tank", SEARED_CASTING_TANK);
    public static final DeferredHolder<Item, Item> HEATER_ITEM = registerBlockItem("heater", HEATER);
    public static final DeferredHolder<Item, Item> FAUCET_ITEM = registerBlockItem("faucet", FAUCET);
    public static final DeferredHolder<Item, Item> SEARED_BRICKS_ITEM = registerBlockItem(
            "seared_bricks", SEARED_BRICKS);
    public static final DeferredHolder<Item, Item> SCORCHED_BRICKS_ITEM = registerBlockItem(
            "scorched_bricks", SCORCHED_BRICKS);
    public static final DeferredHolder<Item, Item> SMELTERY_CONTROLLER_ITEM = registerBlockItem(
            "smeltery_controller", SMELTERY_CONTROLLER);
    public static final DeferredHolder<Item, Item> FOUNDRY_CONTROLLER_ITEM = registerBlockItem(
            "foundry_controller", FOUNDRY_CONTROLLER);
    public static final DeferredHolder<Item, Item> FLUID_REMAINDER = ITEMS.register(
            "fluid_remainder", () -> new FluidRemainderItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> BLANK_SAND_CAST = ITEMS.register(
            "blank_sand_cast", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> BLANK_RED_SAND_CAST = ITEMS.register(
            "blank_red_sand_cast", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> INGOT_CAST = registerCast(
            "ingot_cast", CastingCastItem.Form.INGOT, "");
    public static final DeferredHolder<Item, Item> NUGGET_CAST = registerCast(
            "nugget_cast", CastingCastItem.Form.NUGGET, "");
    public static final DeferredHolder<Item, Item> INGOT_SAND_CAST = registerCast(
            "ingot_sand_cast", CastingCastItem.Form.INGOT, "", false);
    public static final DeferredHolder<Item, Item> INGOT_RED_SAND_CAST = registerCast(
            "ingot_red_sand_cast", CastingCastItem.Form.INGOT, "", false);
    public static final DeferredHolder<Item, Item> NUGGET_SAND_CAST = registerCast(
            "nugget_sand_cast", CastingCastItem.Form.NUGGET, "", false);
    public static final DeferredHolder<Item, Item> NUGGET_RED_SAND_CAST = registerCast(
            "nugget_red_sand_cast", CastingCastItem.Form.NUGGET, "", false);

    static {
        for (String part : List.of("repair_kit", "fake_ingot", "pick_head", "hammer_head",
                "small_axe_head", "broad_axe_head", "small_blade", "broad_blade", "adze_head",
                "large_plate", "bow_limb", "bow_grip", "bowstring", "arrow_head", "arrow_shaft",
                "fletching", "tool_binding", "tough_binding", "tool_handle", "tough_handle",
                "plating_helmet", "plating_chestplate", "plating_leggings", "plating_boots",
                "maille", "shield_core", "skull", "ribcage", "shell", "laces", "slime")) {
            registerCast(part + "_cast", CastingCastItem.Form.PART, part);
            registerCast(part + "_sand_cast", CastingCastItem.Form.PART, part, false);
            registerCast(part + "_red_sand_cast", CastingCastItem.Form.PART, part, false);
        }
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MelterBlockEntity>>
            MELTER_BLOCK_ENTITY = BLOCK_ENTITIES.register("melter", () -> BlockEntityType.Builder
                    .of(MelterBlockEntity::new, MELTER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CastingBlockEntity>>
            CASTING_TABLE_BLOCK_ENTITY = BLOCK_ENTITIES.register("casting_table", () -> BlockEntityType.Builder
                    .of((pos, state) -> new CastingBlockEntity(pos, state, false), CASTING_TABLE.get())
                    .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CastingBlockEntity>>
            CASTING_BASIN_BLOCK_ENTITY = BLOCK_ENTITIES.register("casting_basin", () -> BlockEntityType.Builder
                    .of((pos, state) -> new CastingBlockEntity(pos, state, true), CASTING_BASIN.get())
                    .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlloyerBlockEntity>>
            ALLOYER_BLOCK_ENTITY = BLOCK_ENTITIES.register("alloyer", () -> BlockEntityType.Builder
                    .of(AlloyerBlockEntity::new, ALLOYER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidTankBlockEntity>>
            SEARED_TANK_BLOCK_ENTITY = BLOCK_ENTITIES.register("seared_tank", () -> BlockEntityType.Builder
                    .of(FluidTankBlockEntity::new, SEARED_TANK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidTankBlockEntity>>
            SCORCHED_TANK_BLOCK_ENTITY = BLOCK_ENTITIES.register("scorched_tank", () -> BlockEntityType.Builder
                    .of(FluidTankBlockEntity::new, SCORCHED_TANK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidTankBlockEntity>>
            COMPONENT_BLOCK_ENTITY = BLOCK_ENTITIES.register("fluid_component", () -> BlockEntityType.Builder
                    .of((pos, state) -> new FluidTankBlockEntity(
                                    BuiltInRegistries.BLOCK_ENTITY_TYPE.get(
                                            ResourceLocation.fromNamespaceAndPath(
                                                    ModernTinkers.MOD_ID, "fluid_component")),
                                    pos, state),
                            SEARED_DRAIN.get(), SCORCHED_DRAIN.get(), SEARED_DUCT.get(),
                            SCORCHED_DUCT.get(), SEARED_CHANNEL.get(), SCORCHED_CHANNEL.get(),
                            COPPER_GAUGE.get(), OBSIDIAN_GAUGE.get(), SEARED_CASTING_TANK.get())
                    .build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeaterBlockEntity>>
            HEATER_BLOCK_ENTITY = BLOCK_ENTITIES.register("heater", () -> BlockEntityType.Builder
                    .of(HeaterBlockEntity::new, HEATER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FaucetBlockEntity>>
            FAUCET_BLOCK_ENTITY = BLOCK_ENTITIES.register("faucet", () -> BlockEntityType.Builder
                    .of(FaucetBlockEntity::new, FAUCET.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmelteryControllerBlockEntity>>
            SMELTERY_CONTROLLER_BLOCK_ENTITY = BLOCK_ENTITIES.register("smeltery_controller",
                    () -> BlockEntityType.Builder.of(SmelteryControllerBlockEntity::new,
                            SMELTERY_CONTROLLER.get(), FOUNDRY_CONTROLLER.get()).build(null));

    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>,
            net.minecraft.world.inventory.MenuType<MelterMenu>> MELTER_MENU =
            MENUS.register("melter", () -> IMenuTypeExtension.create(MelterMenu::new));
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>,
            net.minecraft.world.inventory.MenuType<CastingMenu>> CASTING_TABLE_MENU =
            MENUS.register("casting_table", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new CastingMenu(id, inventory, buffer, false)));
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>,
            net.minecraft.world.inventory.MenuType<CastingMenu>> CASTING_BASIN_MENU =
            MENUS.register("casting_basin", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new CastingMenu(id, inventory, buffer, true)));
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>,
            net.minecraft.world.inventory.MenuType<AlloyerMenu>> ALLOYER_MENU =
            MENUS.register("alloyer", () -> IMenuTypeExtension.create(AlloyerMenu::new));
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>,
            net.minecraft.world.inventory.MenuType<SmelteryMenu>> SMELTERY_MENU =
            MENUS.register("smeltery", () -> IMenuTypeExtension.create(SmelteryMenu::new));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SMELTERY_TAB = TABS.register(
            "smeltery", () -> CreativeModeTab.builder()
                    .title(net.minecraft.network.chat.Component.translatable(
                            "itemGroup.moderntinkers.smeltery"))
                    .icon(() -> new ItemStack(MELTER_ITEM.get()))
                    .displayItems((parameters, output) -> addItems(output))
                    .build());

    private SmelteryContent() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        TABS.register(bus);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, MELTER_BLOCK_ENTITY.get(),
                (be, side) -> be.getFluidHandler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CASTING_TABLE_BLOCK_ENTITY.get(),
                (be, side) -> be.getFluidHandler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CASTING_BASIN_BLOCK_ENTITY.get(),
                (be, side) -> be.getFluidHandler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ALLOYER_BLOCK_ENTITY.get(),
                AlloyerBlockEntity::getFluidHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, SEARED_TANK_BLOCK_ENTITY.get(),
                (be, side) -> be.getFluidHandler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, SCORCHED_TANK_BLOCK_ENTITY.get(),
                (be, side) -> be.getFluidHandler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, COMPONENT_BLOCK_ENTITY.get(),
                (be, side) -> be.getFluidHandler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, SMELTERY_CONTROLLER_BLOCK_ENTITY.get(),
                (be, side) -> be.getFluidHandler());
        event.registerItem(Capabilities.FluidHandler.ITEM,
                (stack, context) -> FluidRemainderItem.handler(stack), FLUID_REMAINDER.get());
        event.registerItem(Capabilities.FluidHandler.ITEM,
                (stack, context) -> new TinkerItemFluidHandler(stack),
                StaticContent.fluidTankItems().toArray(new Item[0]));
    }

    private static void addItems(CreativeModeTab.Output output) {
        output.accept(MELTER_ITEM.get());
        output.accept(CASTING_TABLE_ITEM.get());
        output.accept(CASTING_BASIN_ITEM.get());
        output.accept(ALLOYER_ITEM.get());
        output.accept(SEARED_TANK_ITEM.get());
        output.accept(SCORCHED_TANK_ITEM.get());
        output.accept(SEARED_DRAIN_ITEM.get());
        output.accept(SCORCHED_DRAIN_ITEM.get());
        output.accept(SEARED_DUCT_ITEM.get());
        output.accept(SCORCHED_DUCT_ITEM.get());
        output.accept(SEARED_CHANNEL_ITEM.get());
        output.accept(SCORCHED_CHANNEL_ITEM.get());
        output.accept(COPPER_GAUGE_ITEM.get());
        output.accept(OBSIDIAN_GAUGE_ITEM.get());
        output.accept(SEARED_CASTING_TANK_ITEM.get());
        output.accept(HEATER_ITEM.get());
        output.accept(FAUCET_ITEM.get());
        output.accept(SEARED_BRICKS_ITEM.get());
        output.accept(SCORCHED_BRICKS_ITEM.get());
        output.accept(SMELTERY_CONTROLLER_ITEM.get());
        output.accept(FOUNDRY_CONTROLLER_ITEM.get());
        output.accept(BLANK_SAND_CAST.get());
        output.accept(BLANK_RED_SAND_CAST.get());
        CASTS.forEach(cast -> output.accept(cast.get()));
    }

    private static DeferredHolder<Block, Block> registerBlock(String id, BlockKind kind) {
        return BLOCKS.register(id, () -> switch (kind) {
            case MELTER -> new MelterBlock(machineProperties(MapColor.COLOR_GRAY));
            case TABLE -> new CastingBlock(machineProperties(MapColor.COLOR_GRAY), CastingBlock.Mode.TABLE);
            case BASIN -> new CastingBlock(machineProperties(MapColor.COLOR_GRAY), CastingBlock.Mode.BASIN);
            case ALLOYER -> new AlloyerBlock(machineProperties(MapColor.TERRACOTTA_BROWN));
            case SEARED_TANK, SCORCHED_TANK -> new FluidTankBlock(machineProperties(
                    kind == BlockKind.SCORCHED_TANK ? MapColor.TERRACOTTA_BROWN : MapColor.COLOR_GRAY));
            case DRAIN -> new FluidTankBlock(machineProperties(MapColor.COLOR_GRAY),
                    () -> COMPONENT_BLOCK_ENTITY.get());
            case DUCT -> new FluidTankBlock(machineProperties(MapColor.COLOR_GRAY),
                    () -> COMPONENT_BLOCK_ENTITY.get());
            case CHANNEL -> new FluidTankBlock(machineProperties(MapColor.COLOR_GRAY),
                    () -> COMPONENT_BLOCK_ENTITY.get());
            case GAUGE -> new FluidTankBlock(machineProperties(MapColor.COLOR_GRAY),
                    () -> COMPONENT_BLOCK_ENTITY.get());
            case CASTING_TANK -> new FluidTankBlock(machineProperties(MapColor.COLOR_GRAY),
                    () -> COMPONENT_BLOCK_ENTITY.get());
            case HEATER -> new HeaterBlock(machineProperties(MapColor.COLOR_GRAY));
            case FAUCET -> new FaucetBlock(machineProperties(MapColor.COLOR_GRAY));
            case SEARED_BRICKS -> new Block(machineProperties(MapColor.COLOR_BROWN));
            case SCORCHED_BRICKS -> new Block(machineProperties(MapColor.TERRACOTTA_BROWN));
            case SMELTERY_CONTROLLER -> new SmelteryControllerBlock(
                    machineProperties(MapColor.COLOR_BROWN), false);
            case FOUNDRY_CONTROLLER -> new SmelteryControllerBlock(
                    machineProperties(MapColor.TERRACOTTA_BROWN), true);
        });
    }

    private static BlockBehaviour.Properties machineProperties(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).sound(SoundType.METAL)
                .requiresCorrectToolForDrops().strength(3.0F, 9.0F);
    }

    private static DeferredHolder<Item, Item> registerBlockItem(String id,
                                                                  DeferredHolder<Block, Block> block) {
        return ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static DeferredHolder<Item, Item> registerCast(String id, CastingCastItem.Form form,
                                                            String partId) {
        return registerCast(id, form, partId, true);
    }

    private static DeferredHolder<Item, Item> registerCast(String id, CastingCastItem.Form form,
                                                            String partId, boolean reusable) {
        DeferredHolder<Item, Item> cast = ITEMS.register(id,
                () -> new CastingCastItem(new Item.Properties(), form, partId, reusable));
        CASTS.add(cast);
        return cast;
    }

    private enum BlockKind {
        MELTER,
        TABLE,
        BASIN,
        ALLOYER,
        SEARED_TANK,
        SCORCHED_TANK,
        DRAIN,
        DUCT,
        CHANNEL,
        GAUGE,
        CASTING_TANK,
        HEATER,
        FAUCET,
        SEARED_BRICKS,
        SCORCHED_BRICKS,
        SMELTERY_CONTROLLER
        ,FOUNDRY_CONTROLLER
    }

}
