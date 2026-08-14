package com.moderntinkers.content;

import com.moderntinkers.ModernTinkers;
import com.moderntinkers.content.partbuilder.PartBuilderBlock;
import com.moderntinkers.content.partbuilder.PartBuilderBlockEntity;
import com.moderntinkers.content.partbuilder.PartBuilderMenu;
import com.moderntinkers.content.partbuilder.PatternItem;
import com.moderntinkers.content.anvil.TinkersAnvilBlock;
import com.moderntinkers.content.anvil.TinkersAnvilBlockEntity;
import com.moderntinkers.content.anvil.TinkersAnvilMenu;
import com.moderntinkers.content.modifier.ModifierWorktableBlock;
import com.moderntinkers.content.modifier.ModifierWorktableBlockEntity;
import com.moderntinkers.content.modifier.ModifierWorktableMenu;
import com.moderntinkers.content.modifier.DragonScaleItem;
import com.moderntinkers.content.modifier.ModifierCrystalItem;
import com.moderntinkers.content.modifier.ModifierManager;
import com.moderntinkers.content.fluid.MaterialFluids;
import com.moderntinkers.content.tinkerstation.TinkerStationBlock;
import com.moderntinkers.content.tinkerstation.TinkerStationBlockEntity;
import com.moderntinkers.content.tinkerstation.TinkerStationMenu;
import com.moderntinkers.content.tools.MaterialPartItem;
import com.moderntinkers.content.tools.TinkersArmorItem;
import com.moderntinkers.content.tools.TinkersArrowItem;
import com.moderntinkers.content.tools.TinkersShieldItem;
import com.moderntinkers.content.tools.TinkersToolItem;
import com.moderntinkers.content.world.WorldContent;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Static, dependency-light registrations from the next Tinkers modules.
 *
 * <p>These entries establish stable IDs and player-visible inventory content.
 * The table implementations use vanilla or custom server-authoritative paths
 * while the port fills in the remaining reference-specific presentation.</p>
 */
public final class StaticContent {
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, ModernTinkers.MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, ModernTinkers.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ModernTinkers.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ModernTinkers.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ModernTinkers.MOD_ID);

    private static final List<MetalSet> METALS = new ArrayList<>();
    private static final Map<String, DeferredHolder<Item, Item>> TOOL_PARTS =
            new LinkedHashMap<>();
    private static final Map<TinkersToolItem.ToolKind, DeferredHolder<Item, Item>> TOOLS =
            new LinkedHashMap<>();
    private static final Map<ArmorItem.Type, DeferredHolder<Item, Item>> ARMOR =
            new LinkedHashMap<>();
    private static final Map<TinkersArmorItem.Family, Map<ArmorItem.Type,
            DeferredHolder<Item, Item>>> ARMOR_FAMILIES = new LinkedHashMap<>();
    private static final List<DeferredHolder<Item, Item>> MODIFIER_ITEMS = new ArrayList<>();
    private static final Map<String, DeferredHolder<Item, Item>> MODIFIERS =
            new LinkedHashMap<>();
    private static final List<DeferredHolder<Item, Item>> TABLE_ITEMS = new ArrayList<>();

    public static final MetalSet COBALT = registerMetal("cobalt", MapColor.COLOR_BLUE);
    public static final MetalSet STEEL = registerMetal("steel", MapColor.STONE);
    public static final MetalSet SLIMESTEEL = registerMetal("slimesteel", MapColor.WARPED_WART_BLOCK);
    public static final MetalSet AMETHYST_BRONZE = registerMetal("amethyst_bronze", MapColor.COLOR_PURPLE);
    public static final MetalSet ROSE_GOLD = registerMetal("rose_gold", MapColor.TERRACOTTA_WHITE);
    public static final MetalSet PIG_IRON = registerMetal("pig_iron", MapColor.COLOR_PINK);
    public static final MetalSet CINDERSLIME = registerMetal("cinderslime", MapColor.COLOR_ORANGE);
    public static final MetalSet QUEENS_SLIME = registerMetal("queens_slime", MapColor.COLOR_GREEN);
    public static final MetalSet MANYULLYN = registerMetal("manyullyn", MapColor.COLOR_PURPLE);
    public static final MetalSet HEPATIZON = registerMetal("hepatizon", MapColor.TERRACOTTA_BLUE);
    public static final MetalSet KNIGHTMETAL = registerMetal("knightmetal", MapColor.GRASS);
    public static final MetalSet KNIGHTSLIME = registerMetal("knightslime", MapColor.COLOR_BLACK);
    public static final MetalSet SOULSTEEL = registerMetal("soulsteel", MapColor.COLOR_BROWN);

    public static final DeferredHolder<Block, Block> NAHUATL =
            registerWoodBlock("nahuatl", MapColor.COLOR_PURPLE);
    public static final DeferredHolder<Item, Item> NAHUATL_ITEM =
            registerBlockItem("nahuatl", NAHUATL);
    public static final DeferredHolder<Block, Block> BLAZEWOOD =
            registerWoodBlock("blazewood", MapColor.TERRACOTTA_RED);
    public static final DeferredHolder<Item, Item> BLAZEWOOD_ITEM =
            registerBlockItem("blazewood", BLAZEWOOD);

    public static final DeferredHolder<Block, Block> FAKE_STORAGE_BLOCK =
            registerBlock("fake_storage_block", metalProperties(MapColor.COLOR_GRAY));
    public static final DeferredHolder<Item, Item> FAKE_STORAGE_BLOCK_ITEM =
            registerBlockItem("fake_storage_block", FAKE_STORAGE_BLOCK);

    public static final DeferredHolder<Item, Item> SLIME_WINGS;

    static {
        registerToolPart("repair_kit");
        registerToolPart("fake_ingot");
        registerToolPart("pick_head");
        registerToolPart("hammer_head");
        registerToolPart("small_axe_head");
        registerToolPart("broad_axe_head");
        registerToolPart("small_blade");
        registerToolPart("broad_blade");
        registerToolPart("adze_head");
        registerToolPart("large_plate");
        registerToolPart("bow_limb");
        registerToolPart("bow_grip");
        registerToolPart("bowstring");
        registerToolPart("arrow_head");
        registerToolPart("arrow_shaft");
        registerToolPart("fletching");
        registerToolPart("tool_binding");
        registerToolPart("tough_binding");
        registerToolPart("tool_handle");
        registerToolPart("tough_handle");
        registerToolPart("plating_helmet");
        registerToolPart("plating_chestplate");
        registerToolPart("plating_leggings");
        registerToolPart("plating_boots");
        registerToolPart("maille");
        registerToolPart("shield_core");
        registerToolPart("skull");
        registerToolPart("ribcage");
        registerToolPart("shell");
        registerToolPart("laces");
        registerToolPart("slime");
    }

    static {
        registerTool("pickaxe", TinkersToolItem.ToolKind.PICKAXE);
        registerTool("sledge_hammer", TinkersToolItem.ToolKind.SLEDGE_HAMMER);
        registerTool("vein_hammer", TinkersToolItem.ToolKind.VEIN_HAMMER);
        registerTool("pickadze", TinkersToolItem.ToolKind.PICKADZE);
        registerTool("excavator", TinkersToolItem.ToolKind.EXCAVATOR);
        registerTool("hand_axe", TinkersToolItem.ToolKind.HAND_AXE);
        registerTool("broad_axe", TinkersToolItem.ToolKind.BROAD_AXE);
        registerTool("throwing_axe", TinkersToolItem.ToolKind.THROWING_AXE);
        registerTool("mattock", TinkersToolItem.ToolKind.MATTOCK);
        registerTool("kama", TinkersToolItem.ToolKind.KAMA);
        registerTool("scythe", TinkersToolItem.ToolKind.SCYTHE);
        registerTool("dagger", TinkersToolItem.ToolKind.DAGGER);
        registerTool("sword", TinkersToolItem.ToolKind.SWORD);
        registerTool("cleaver", TinkersToolItem.ToolKind.CLEAVER);
        registerTool("longbow", TinkersToolItem.ToolKind.LONGBOW);
        registerTool("crossbow", TinkersToolItem.ToolKind.CROSSBOW);
        registerTool("javelin", TinkersToolItem.ToolKind.JAVELIN);
        registerTool("shuriken", TinkersToolItem.ToolKind.SHURIKEN);
        registerTool("fishing_rod", TinkersToolItem.ToolKind.FISHING_ROD);
        registerTool("staff", TinkersToolItem.ToolKind.STAFF);
        registerTool("flint_and_brick", TinkersToolItem.ToolKind.FLINT_AND_BRICK);
        registerTool("sky_staff", TinkersToolItem.ToolKind.SKY_STAFF);
        registerTool("earth_staff", TinkersToolItem.ToolKind.EARTH_STAFF);
        registerTool("ichor_staff", TinkersToolItem.ToolKind.ICHOR_STAFF);
        registerTool("ender_staff", TinkersToolItem.ToolKind.ENDER_STAFF);
        registerTool("melting_pan", TinkersToolItem.ToolKind.MELTING_PAN);
        registerTool("war_pick", TinkersToolItem.ToolKind.WAR_PICK);
        registerTool("battlesign", TinkersToolItem.ToolKind.BATTLESIGN);
        registerTool("swasher", TinkersToolItem.ToolKind.SWASHER);
        registerTool("minotaur_axe", TinkersToolItem.ToolKind.MINOTAUR_AXE);
    }

    static {
        registerArmorFamily(TinkersArmorItem.Family.TINKERS, type -> "tinkers_" + type.getName());
        registerArmorFamily(TinkersArmorItem.Family.TRAVELERS, type -> "travelers_" + type.getName());
        registerArmorFamily(TinkersArmorItem.Family.PLATE, type -> "plate_" + type.getName());
        registerArmorFamily(TinkersArmorItem.Family.SLIME, type -> switch (type) {
            case HELMET -> "slime_helmet";
            case CHESTPLATE -> "slimy_chestplate";
            case LEGGINGS -> "slime_leggings";
            case BOOTS -> "slime_boots";
            default -> "slime_" + type.getName();
        });
        Map<ArmorItem.Type, DeferredHolder<Item, Item>> wings = new LinkedHashMap<>();
        SLIME_WINGS = ITEMS.register("slime_wings",
                () -> new TinkersArmorItem(new Item.Properties(), ArmorItem.Type.CHESTPLATE,
                        TinkersArmorItem.Family.WINGS));
        wings.put(ArmorItem.Type.CHESTPLATE, SLIME_WINGS);
        ARMOR_FAMILIES.put(TinkersArmorItem.Family.WINGS, wings);
    }

    public static final DeferredHolder<Item, Item> TRAVELERS_SHIELD = ITEMS.register(
            "travelers_shield", () -> new TinkersShieldItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> PLATE_SHIELD = ITEMS.register(
            "plate_shield", () -> new TinkersShieldItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> TINKERS_ARROW = ITEMS.register(
            "arrow", () -> new TinkersArrowItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> PATTERN = registerTableItem("pattern");

    static {
        registerModifierItem("silky_cloth", "item.moderntinkers.silky_cloth.tooltip", false);
        registerModifierItem("dragon_scale", "item.moderntinkers.dragon_scale.tooltip", true);
        registerModifierItem("emerald_reinforcement", "item.moderntinkers.emerald_reinforcement.tooltip", false);
        registerModifierItem("slimesteel_reinforcement", "item.moderntinkers.slimesteel_reinforcement.tooltip", false);
        registerModifierItem("seared_reinforcement", "item.moderntinkers.seared_reinforcement.tooltip", false);
        registerModifierItem("iron_reinforcement", "item.moderntinkers.iron_reinforcement.tooltip", false);
        registerModifierItem("obsidian_reinforcement", "item.moderntinkers.obsidian_reinforcement.tooltip", false);
        registerModifierItem("gold_reinforcement", "item.moderntinkers.gold_reinforcement.tooltip", false);
        registerModifierItem("cobalt_reinforcement", "item.moderntinkers.cobalt_reinforcement.tooltip", false);
        registerModifierItem("modifier_crystal", "item.moderntinkers.modifier_crystal.tooltip", false);
        registerModifierItem("redstone", "item.moderntinkers.redstone.tooltip", false);
        registerModifierItem("quartz", "item.moderntinkers.quartz.tooltip", false);
        registerModifierItem("fiery", "item.moderntinkers.fiery.tooltip", false);
        registerModifierItem("necrotic", "item.moderntinkers.necrotic.tooltip", false);
        registerModifierItem("knockback", "item.moderntinkers.knockback.tooltip", false);
        registerModifierItem("autosmelt", "item.moderntinkers.autosmelt.tooltip", false);
        registerModifierItem("haste", "item.moderntinkers.haste.tooltip", false);
        registerModifierItem("fortune", "item.moderntinkers.fortune.tooltip", false);
        registerModifierItem("luck", "item.moderntinkers.luck.tooltip", false);
        registerModifierItem("looting", "item.moderntinkers.looting.tooltip", false);
        registerModifierItem("sharpness", "item.moderntinkers.sharpness.tooltip", false);
        registerModifierItem("smite", "item.moderntinkers.smite.tooltip", false);
        registerModifierItem("bane_of_arthropods", "item.moderntinkers.bane_of_arthropods.tooltip", false);
        registerModifierItem("beheading", "item.moderntinkers.beheading.tooltip", false);
        registerModifierItem("mending", "item.moderntinkers.mending.tooltip", false);
        registerModifierItem("experience", "item.moderntinkers.experience.tooltip", false);
        registerModifierItem("expanded", "item.moderntinkers.expanded.tooltip", false);
        registerModifierItem("spitting", "item.moderntinkers.spitting.tooltip", false);
        registerModifierItem("spilling", "item.moderntinkers.spilling.tooltip", false);
        registerModifierItem("overshield", "item.moderntinkers.overshield.tooltip", false);
    }

    public static final DeferredHolder<Block, Block> TINKER_STATION;
    public static final DeferredHolder<Block, Block> PART_BUILDER;
    public static final DeferredHolder<Block, Block> MODIFIER_WORKTABLE;
    public static final DeferredHolder<Block, Block> TINKERS_ANVIL;
    public static final DeferredHolder<Block, Block> SCORCHED_ANVIL;

    static {
        registerTableBlock("crafting_station", MapColor.WOOD, SoundType.WOOD);
        TINKER_STATION = registerTableBlock("tinker_station", MapColor.WOOD, SoundType.WOOD);
        PART_BUILDER = registerTableBlock("part_builder", MapColor.WOOD, SoundType.WOOD);
        MODIFIER_WORKTABLE = registerTableBlock("modifier_worktable", MapColor.STONE, SoundType.WOOD);
        TINKERS_ANVIL = registerTableBlock("tinkers_anvil", MapColor.METAL, SoundType.METAL);
        SCORCHED_ANVIL = registerTableBlock("scorched_anvil", MapColor.COLOR_BLACK, SoundType.METAL);
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PartBuilderBlockEntity>>
            PART_BUILDER_BLOCK_ENTITY = BLOCK_ENTITIES.register("part_builder", () -> BlockEntityType.Builder
                    .of(PartBuilderBlockEntity::new, PART_BUILDER.get())
                    .build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<PartBuilderMenu>> PART_BUILDER_MENU =
            MENUS.register("part_builder", () -> IMenuTypeExtension.create(PartBuilderMenu::new));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TinkerStationBlockEntity>>
            TINKER_STATION_BLOCK_ENTITY = BLOCK_ENTITIES.register("tinker_station", () -> BlockEntityType.Builder
                    .of(TinkerStationBlockEntity::new, TINKER_STATION.get())
                    .build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<TinkerStationMenu>> TINKER_STATION_MENU =
            MENUS.register("tinker_station", () -> IMenuTypeExtension.create(TinkerStationMenu::new));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ModifierWorktableBlockEntity>>
            MODIFIER_WORKTABLE_BLOCK_ENTITY = BLOCK_ENTITIES.register("modifier_worktable", () -> BlockEntityType.Builder
                    .of(ModifierWorktableBlockEntity::new, MODIFIER_WORKTABLE.get())
                    .build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<ModifierWorktableMenu>> MODIFIER_WORKTABLE_MENU =
            MENUS.register("modifier_worktable", () -> IMenuTypeExtension.create(ModifierWorktableMenu::new));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TinkersAnvilBlockEntity>>
            TINKERS_ANVIL_BLOCK_ENTITY = BLOCK_ENTITIES.register("tinkers_anvil", () -> BlockEntityType.Builder
                    .of(TinkersAnvilBlockEntity::new, TINKERS_ANVIL.get(), SCORCHED_ANVIL.get())
                    .build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<TinkersAnvilMenu>> TINKERS_ANVIL_MENU =
            MENUS.register("tinkers_anvil", () -> IMenuTypeExtension.create(TinkersAnvilMenu::new));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MATERIALS_TAB =
            CREATIVE_TABS.register("materials", () -> CreativeModeTab.builder()
                    .title(net.minecraft.network.chat.Component.translatable(
                            "itemGroup.moderntinkers.materials"))
                    .icon(() -> new ItemStack(COBALT.ingot().get()))
                    .displayItems(StaticContent::addMaterialItems)
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TOOL_PARTS_TAB =
            CREATIVE_TABS.register("tool_parts", () -> CreativeModeTab.builder()
                    .title(net.minecraft.network.chat.Component.translatable(
                            "itemGroup.moderntinkers.tool_parts"))
                    .icon(() -> new ItemStack(TOOL_PARTS.get("pick_head").get()))
                    .displayItems(StaticContent::addToolParts)
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TOOLS_TAB =
            CREATIVE_TABS.register("tools", () -> CreativeModeTab.builder()
                    .title(net.minecraft.network.chat.Component.translatable(
                            "itemGroup.moderntinkers.tools"))
                    .icon(() -> new ItemStack(tool(TinkersToolItem.ToolKind.PICKAXE).get()))
                    .displayItems(StaticContent::addTools)
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MODIFIERS_TAB =
            CREATIVE_TABS.register("modifiers", () -> CreativeModeTab.builder()
                    .title(net.minecraft.network.chat.Component.translatable(
                            "itemGroup.moderntinkers.modifiers"))
                    .icon(() -> new ItemStack(MODIFIER_ITEMS.get(0).get()))
                    .displayItems(StaticContent::addModifierItems)
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TABLES_TAB =
            CREATIVE_TABS.register("tables", () -> CreativeModeTab.builder()
                    .title(net.minecraft.network.chat.Component.translatable(
                            "itemGroup.moderntinkers.tables"))
                    .icon(() -> new ItemStack(TABLE_ITEMS.get(0).get()))
                    .displayItems(StaticContent::addTableItems)
                    .build());

    private StaticContent() {}

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
    }

    public static DeferredHolder<Item, Item> toolPart(String id) {
        return TOOL_PARTS.get(id);
    }

    public static DeferredHolder<Item, Item> tool(TinkersToolItem.ToolKind kind) {
        return TOOLS.get(kind);
    }

    public static DeferredHolder<Item, Item> armor(ArmorItem.Type type) {
        return ARMOR.get(type);
    }

    public static DeferredHolder<Item, Item> armor(TinkersArmorItem.Family family,
                                                    ArmorItem.Type type) {
        Map<ArmorItem.Type, DeferredHolder<Item, Item>> familyArmor = ARMOR_FAMILIES.get(family);
        return familyArmor == null ? null : familyArmor.get(type);
    }

    public static List<String> toolPartIds() {
        return List.copyOf(TOOL_PARTS.keySet());
    }

    public static List<MetalSet> metals() {
        return List.copyOf(METALS);
    }

    /** Items that expose the shared modifier fluid tank to NeoForge. */
    public static List<Item> fluidTankItems() {
        List<Item> result = new ArrayList<>();
        TOOLS.values().forEach(holder -> result.add(holder.get()));
        ARMOR_FAMILIES.values().forEach(family -> family.values()
                .forEach(holder -> result.add(holder.get())));
        result.add(TRAVELERS_SHIELD.get());
        result.add(PLATE_SHIELD.get());
        return List.copyOf(result);
    }

    public static String modifierId(ItemStack stack) {
        if (stack.getItem() instanceof ModifierCrystalItem) {
            return ModifierCrystalItem.getModifier(stack);
        }
        for (Map.Entry<String, DeferredHolder<Item, Item>> entry : MODIFIERS.entrySet()) {
            if ("modifier_crystal".equals(entry.getKey())) {
                continue;
            }
            if (stack.is(entry.getValue().get())) {
                return entry.getKey();
            }
        }
        return "";
    }

    public static boolean isModifierItem(ItemStack stack) {
        return stack.getItem() instanceof ModifierCrystalItem || !modifierId(stack).isEmpty();
    }

    public static DeferredHolder<Item, Item> modifierItem(String id) {
        return MODIFIERS.get(id);
    }

    private static MetalSet registerMetal(String id, MapColor color) {
        DeferredHolder<Block, Block> block = registerBlock(id + "_block", metalProperties(color));
        DeferredHolder<Item, Item> blockItem = registerBlockItem(id + "_block", block);
        DeferredHolder<Item, Item> ingot = ITEMS.register(id + "_ingot",
                () -> new Item(new Item.Properties()));
        DeferredHolder<Item, Item> nugget = ITEMS.register(id + "_nugget",
                () -> new Item(new Item.Properties()));
        MetalSet metal = new MetalSet(id, block, blockItem, ingot, nugget);
        METALS.add(metal);
        return metal;
    }

    private static DeferredHolder<Block, Block> registerWoodBlock(String id, MapColor color) {
        return registerBlock(id, BlockBehaviour.Properties.of()
                .mapColor(color)
                .sound(SoundType.WOOD)
                .strength(2.0F, 7.0F)
                .ignitedByLava());
    }

    private static DeferredHolder<Block, Block> registerBlock(
            String id, BlockBehaviour.Properties properties) {
        return BLOCKS.register(id, () -> new Block(properties));
    }

    private static DeferredHolder<Item, Item> registerBlockItem(
            String id, DeferredHolder<Block, Block> block) {
        return ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static DeferredHolder<Item, Item> registerToolPart(String id) {
        DeferredHolder<Item, Item> item = ITEMS.register(id,
                () -> new MaterialPartItem(new Item.Properties(), id));
        TOOL_PARTS.put(id, item);
        return item;
    }

    private static DeferredHolder<Item, Item> registerTool(String id, TinkersToolItem.ToolKind kind) {
        DeferredHolder<Item, Item> item = ITEMS.register(id,
                () -> new TinkersToolItem(new Item.Properties(), kind));
        TOOLS.put(kind, item);
        return item;
    }

    private static void registerArmorFamily(TinkersArmorItem.Family family,
                                            Function<ArmorItem.Type, String> idFactory) {
        Map<ArmorItem.Type, DeferredHolder<Item, Item>> entries = new LinkedHashMap<>();
        for (ArmorItem.Type type : List.of(ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE,
                ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS)) {
            DeferredHolder<Item, Item> item = ITEMS.register(idFactory.apply(type),
                    () -> new TinkersArmorItem(new Item.Properties(), type, family));
            entries.put(type, item);
            if (family == TinkersArmorItem.Family.TINKERS) {
                ARMOR.put(type, item);
            }
        }
        ARMOR_FAMILIES.put(family, entries);
    }

    private static DeferredHolder<Item, Item> registerModifierItem(
            String id, String tooltipKey, boolean rare) {
        DeferredHolder<Item, Item> item = ITEMS.register(id, () -> {
            Item.Properties properties = new Item.Properties()
                    .rarity(rare ? Rarity.RARE : Rarity.COMMON);
            if ("dragon_scale".equals(id)) {
                return new DragonScaleItem(properties, tooltipKey);
            }
            if ("modifier_crystal".equals(id)) {
                return new ModifierCrystalItem(properties);
            }
            return new PortTooltipItem(properties, tooltipKey);
        });
        MODIFIER_ITEMS.add(item);
        MODIFIERS.put(id, item);
        return item;
    }

    private static DeferredHolder<Item, Item> registerTableItem(String id) {
        DeferredHolder<Item, Item> item = ITEMS.register(id,
                () -> "pattern".equals(id)
                        ? new PatternItem(new Item.Properties())
                        : new Item(new Item.Properties()));
        TABLE_ITEMS.add(item);
        return item;
    }

    private static DeferredHolder<Block, Block> registerTableBlock(String id, MapColor color, SoundType sound) {
        DeferredHolder<Block, Block> block = registerBlock(id, BlockBehaviour.Properties.of()
                .mapColor(color)
                .sound(sound)
                .strength(2.5F), "crafting_station".equals(id));
        TABLE_ITEMS.add(registerBlockItem(id, block));
        return block;
    }

    private static DeferredHolder<Block, Block> registerBlock(
            String id, BlockBehaviour.Properties properties, boolean craftingTable) {
        return BLOCKS.register(id, () -> craftingTable
                ? new CraftingTableBlock(properties)
                : "part_builder".equals(id)
                        ? new PartBuilderBlock(properties)
                        : "tinker_station".equals(id)
                                ? new TinkerStationBlock(properties)
                        : "modifier_worktable".equals(id)
                                ? new ModifierWorktableBlock(properties)
                        : "tinkers_anvil".equals(id)
                                ? new TinkersAnvilBlock(properties, false)
                        : "scorched_anvil".equals(id)
                                ? new TinkersAnvilBlock(properties, true)
                        : new Block(properties));
    }

    private static BlockBehaviour.Properties metalProperties(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops()
                .strength(5.0F);
    }

    private static void addMaterialItems(CreativeModeTab.ItemDisplayParameters parameters,
                                          CreativeModeTab.Output output) {
        for (MetalSet metal : METALS) {
            output.accept(metal.blockItem().get());
            output.accept(metal.ingot().get());
            output.accept(metal.nugget().get());
        }
        output.accept(NAHUATL_ITEM.get());
        output.accept(BLAZEWOOD_ITEM.get());
        output.accept(FAKE_STORAGE_BLOCK_ITEM.get());
        WorldContent.addItems(output);
        MaterialFluids.addBuckets(output);
    }

    private static void addToolParts(CreativeModeTab.ItemDisplayParameters parameters,
                                     CreativeModeTab.Output output) {
        TOOL_PARTS.values().forEach(item -> output.accept(item.get()));
        output.accept(PATTERN.get());
    }

    private static void addTools(CreativeModeTab.ItemDisplayParameters parameters,
                                 CreativeModeTab.Output output) {
        for (TinkersToolItem.ToolKind kind : TinkersToolItem.ToolKind.values()) {
            ItemStack[] parts = kind.parts().stream()
                    .map(part -> MaterialPartItem.withMaterial(toolPart(part).get(), "iron"))
                    .toArray(ItemStack[]::new);
            output.accept(TinkersToolItem.assemble(tool(kind).get(), kind, parts));
        }
        for (ArmorItem.Type type : ARMOR.keySet()) {
            ItemStack plating = MaterialPartItem.withMaterial(
                    toolPart("plating_" + type.getName()).get(), "iron");
            output.accept(TinkersArmorItem.assemble(armor(type).get(), type, plating,
                    ItemStack.EMPTY));
        }
        for (ArmorItem.Type type : List.of(ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE,
                ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS)) {
            ItemStack plating = MaterialPartItem.withMaterial(
                    toolPart("plating_" + type.getName()).get(), "iron");
            output.accept(TinkersArmorItem.assemble(
                    armor(TinkersArmorItem.Family.TRAVELERS, type).get(), type, plating,
                    ItemStack.EMPTY));
            ItemStack maille = MaterialPartItem.withMaterial(toolPart("maille").get(), "iron");
            output.accept(TinkersArmorItem.assemble(
                    armor(TinkersArmorItem.Family.PLATE, type).get(), type, plating, maille));

            String slimePart = switch (type) {
                case HELMET -> "skull";
                case CHESTPLATE -> "ribcage";
                case LEGGINGS -> "shell";
                case BOOTS -> "laces";
                default -> "";
            };
            ItemStack slimeCore = MaterialPartItem.withMaterial(toolPart(slimePart).get(), "iron");
            ItemStack slimeCoating = MaterialPartItem.withMaterial(toolPart("slime").get(), "slimewood");
            output.accept(TinkersArmorItem.assemble(
                    armor(TinkersArmorItem.Family.SLIME, type).get(), type,
                    slimeCore, slimeCoating));
        }
        output.accept(TinkersArmorItem.assembleWings(SLIME_WINGS.get(), "blood"));
        ItemStack shieldCore = MaterialPartItem.withMaterial(
                toolPart("shield_core").get(), "iron");
        output.accept(TinkersShieldItem.assemble(TRAVELERS_SHIELD.get(), shieldCore,
                ItemStack.EMPTY));
        ItemStack shieldPlating = MaterialPartItem.withMaterial(
                toolPart("large_plate").get(), "iron");
        output.accept(TinkersShieldItem.assemblePlate(PLATE_SHIELD.get(), shieldCore,
                shieldPlating));
        output.accept(TinkersArrowItem.assemble(TINKERS_ARROW.get(),
                MaterialPartItem.withMaterial(toolPart("arrow_head").get(), "iron"),
                MaterialPartItem.withMaterial(toolPart("arrow_shaft").get(), "wood"),
                MaterialPartItem.withMaterial(toolPart("fletching").get(), "feather")));
    }

    private static void addModifierItems(CreativeModeTab.ItemDisplayParameters parameters,
                                         CreativeModeTab.Output output) {
        MODIFIER_ITEMS.forEach(item -> {
            output.accept(item.get());
            if (item.get() instanceof ModifierCrystalItem) {
                ModifierManager.ids().forEach(id -> output.accept(
                        ModifierCrystalItem.withModifier(item.get(), id)));
            }
        });
    }

    private static void addTableItems(CreativeModeTab.ItemDisplayParameters parameters,
                                      CreativeModeTab.Output output) {
        TABLE_ITEMS.forEach(item -> output.accept(item.get()));
    }

    public record MetalSet(String id, DeferredHolder<Block, Block> block,
                           DeferredHolder<Item, Item> blockItem,
                           DeferredHolder<Item, Item> ingot,
                           DeferredHolder<Item, Item> nugget) {}
}
