package com.moderntinkers.content.world;

import com.moderntinkers.ModernTinkers;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/** World-facing slime, wood, and cobalt acquisition content. */
public final class WorldContent {
    public static final TagKey<Block> SKY_SLIME_SPAWN = BlockTags.create(
            ResourceLocation.fromNamespaceAndPath(ModernTinkers.MOD_ID, "slime_spawn/sky"));
    public static final TagKey<Block> ENDER_SLIME_SPAWN = BlockTags.create(
            ResourceLocation.fromNamespaceAndPath(ModernTinkers.MOD_ID, "slime_spawn/ender"));
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, ModernTinkers.MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, ModernTinkers.MOD_ID);
    private static final List<DeferredHolder<Item, Item>> WORLD_ITEMS = new ArrayList<>();

    public static final DeferredHolder<Block, Block> SKY_SLIME = registerSlime(
            "sky_slime", MapColor.COLOR_LIGHT_BLUE, (state, other) -> true);
    public static final DeferredHolder<Block, Block> ICHOR_SLIME = registerSlime(
            "ichor_slime", MapColor.COLOR_ORANGE, (state, other) -> other.getBlock() != state.getBlock());
    public static final DeferredHolder<Block, Block> ENDER_SLIME = registerSlime(
            "ender_slime", MapColor.COLOR_PURPLE, (state, other) -> other.getBlock() == state.getBlock());
    public static final DeferredHolder<Block, Block> BLOOD_SLIME = registerSlime(
            "blood_slime", MapColor.COLOR_RED, (state, other) -> false);

    public static final DeferredHolder<Block, Block> EARTH_CONGEALED_SLIME = registerCongealed(
            "earth_congealed_slime", MapColor.COLOR_GREEN);
    public static final DeferredHolder<Block, Block> SKY_CONGEALED_SLIME = registerCongealed(
            "sky_congealed_slime", MapColor.COLOR_LIGHT_BLUE);
    public static final DeferredHolder<Block, Block> ENDER_CONGEALED_SLIME = registerCongealed(
            "ender_congealed_slime", MapColor.COLOR_PURPLE);
    public static final DeferredHolder<Block, Block> BLOOD_CONGEALED_SLIME = registerCongealed(
            "blood_congealed_slime", MapColor.COLOR_RED);
    public static final DeferredHolder<Block, Block> ICHOR_CONGEALED_SLIME = registerCongealed(
            "ichor_congealed_slime", MapColor.COLOR_ORANGE);

    public static final DeferredHolder<Block, Block> SLIMEWOOD_LOG = registerLog("slimewood_log");
    public static final DeferredHolder<Block, Block> GREENHEART_LOG = registerLog("greenheart_log");
    public static final DeferredHolder<Block, Block> SKYROOT_LOG = registerLog("skyroot_log");
    public static final DeferredHolder<Block, Block> BLOODSHROOM_LOG = registerLog("bloodshroom_log");
    public static final DeferredHolder<Block, Block> ENDERBARK_LOG = registerLog("enderbark_log");
    public static final DeferredHolder<Block, Block> SLIMEWOOD_LEAVES = registerLeaves("slimewood_leaves");
    public static final DeferredHolder<Block, Block> SKYROOT_LEAVES = registerLeaves("skyroot_leaves");
    public static final DeferredHolder<Block, Block> BLOODSHROOM_LEAVES = registerLeaves("bloodshroom_leaves");
    public static final DeferredHolder<Block, Block> ENDERBARK_LEAVES = registerLeaves("enderbark_leaves");
    public static final DeferredHolder<Block, Block> SLIMEWOOD_SAPLING = registerSapling(
            "slimewood_sapling", SLIMEWOOD_LOG, SLIMEWOOD_LEAVES);
    public static final DeferredHolder<Block, Block> SKYROOT_SAPLING = registerSapling(
            "skyroot_sapling", SKYROOT_LOG, SKYROOT_LEAVES);
    public static final DeferredHolder<Block, Block> BLOODSHROOM_SAPLING = registerSapling(
            "bloodshroom_sapling", BLOODSHROOM_LOG, BLOODSHROOM_LEAVES);
    public static final DeferredHolder<Block, Block> ENDERBARK_SAPLING = registerSapling(
            "enderbark_sapling", ENDERBARK_LOG, ENDERBARK_LEAVES);

    public static final DeferredHolder<Block, Block> COBALT_ORE = registerBlockWithItem(
            "cobalt_ore", BlockBehaviour.Properties.of().mapColor(MapColor.NETHER)
                    .sound(SoundType.NETHER_ORE).requiresCorrectToolForDrops().strength(3.0F));
    public static final DeferredHolder<Block, Block> RAW_COBALT_BLOCK = registerBlockWithItem(
            "raw_cobalt_block", BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE)
                    .sound(SoundType.NETHER_ORE).requiresCorrectToolForDrops().strength(5.0F));
    public static final DeferredHolder<Item, Item> RAW_COBALT = ITEMS.register(
            "raw_cobalt", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> STEEL_SHARD = ITEMS.register(
            "steel_shard", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> COBALT_SHARD = ITEMS.register(
            "cobalt_shard", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> KNIGHTMETAL_SHARD = ITEMS.register(
            "knightmetal_shard", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Block, Block> STEEL_CLUSTER = registerMetalCluster(
            "steel_cluster", MapColor.STONE, 5);
    public static final DeferredHolder<Block, Block> COBALT_CLUSTER = registerMetalCluster(
            "cobalt_cluster", MapColor.COLOR_BLUE, 8);
    public static final DeferredHolder<Block, Block> KNIGHTMETAL_CLUSTER = registerMetalCluster(
            "knightmetal_cluster", MapColor.GRASS, 12);
    public static final DeferredHolder<Item, Item> EARTH_SLIME_CRYSTAL = registerCrystal(
            "earth_slime_crystal");
    public static final DeferredHolder<Item, Item> SKY_SLIME_CRYSTAL = registerCrystal(
            "sky_slime_crystal");
    public static final DeferredHolder<Item, Item> ICHOR_SLIME_CRYSTAL = registerCrystal(
            "ichor_slime_crystal");
    public static final DeferredHolder<Item, Item> ENDER_SLIME_CRYSTAL = registerCrystal(
            "ender_slime_crystal");

    public static final DeferredHolder<Block, Block> EARTH_SLIME_CRYSTAL_BLOCK =
            registerCrystalBlock("earth_slime_crystal_block", MapColor.COLOR_LIGHT_GREEN);
    public static final DeferredHolder<Block, Block> BUDDING_EARTH_SLIME_CRYSTAL =
            registerBuddingCrystal("budding_earth_slime_crystal", MapColor.COLOR_LIGHT_GREEN);
    public static final DeferredHolder<Block, Block> SMALL_EARTH_SLIME_CRYSTAL_BUD =
            registerCrystalBud("small_earth_slime_crystal_bud", MapColor.COLOR_LIGHT_GREEN,
                    3.0F, 4.0F, SoundType.SMALL_AMETHYST_BUD);
    public static final DeferredHolder<Block, Block> MEDIUM_EARTH_SLIME_CRYSTAL_BUD =
            registerCrystalBud("medium_earth_slime_crystal_bud", MapColor.COLOR_LIGHT_GREEN,
                    4.0F, 3.0F, SoundType.MEDIUM_AMETHYST_BUD);
    public static final DeferredHolder<Block, Block> LARGE_EARTH_SLIME_CRYSTAL_BUD =
            registerCrystalBud("large_earth_slime_crystal_bud", MapColor.COLOR_LIGHT_GREEN,
                    5.0F, 3.0F, SoundType.LARGE_AMETHYST_BUD);
    public static final DeferredHolder<Block, Block> EARTH_SLIME_CRYSTAL_CLUSTER =
            registerCrystalBud("earth_slime_crystal_cluster", MapColor.COLOR_LIGHT_GREEN,
                    7.0F, 3.0F, SoundType.AMETHYST_CLUSTER);

    public static final DeferredHolder<Block, Block> SKY_SLIME_CRYSTAL_BLOCK =
            registerCrystalBlock("sky_slime_crystal_block", MapColor.COLOR_LIGHT_BLUE);
    public static final DeferredHolder<Block, Block> BUDDING_SKY_SLIME_CRYSTAL =
            registerBuddingCrystal("budding_sky_slime_crystal", MapColor.COLOR_LIGHT_BLUE);
    public static final DeferredHolder<Block, Block> SMALL_SKY_SLIME_CRYSTAL_BUD =
            registerCrystalBud("small_sky_slime_crystal_bud", MapColor.COLOR_LIGHT_BLUE,
                    3.0F, 4.0F, SoundType.SMALL_AMETHYST_BUD);
    public static final DeferredHolder<Block, Block> MEDIUM_SKY_SLIME_CRYSTAL_BUD =
            registerCrystalBud("medium_sky_slime_crystal_bud", MapColor.COLOR_LIGHT_BLUE,
                    4.0F, 3.0F, SoundType.MEDIUM_AMETHYST_BUD);
    public static final DeferredHolder<Block, Block> LARGE_SKY_SLIME_CRYSTAL_BUD =
            registerCrystalBud("large_sky_slime_crystal_bud", MapColor.COLOR_LIGHT_BLUE,
                    5.0F, 3.0F, SoundType.LARGE_AMETHYST_BUD);
    public static final DeferredHolder<Block, Block> SKY_SLIME_CRYSTAL_CLUSTER =
            registerCrystalBud("sky_slime_crystal_cluster", MapColor.COLOR_LIGHT_BLUE,
                    7.0F, 3.0F, SoundType.AMETHYST_CLUSTER);

    public static final DeferredHolder<Block, Block> ICHOR_SLIME_CRYSTAL_BLOCK =
            registerCrystalBlock("ichor_slime_crystal_block", MapColor.COLOR_ORANGE);
    public static final DeferredHolder<Block, Block> BUDDING_ICHOR_SLIME_CRYSTAL =
            registerBuddingCrystal("budding_ichor_slime_crystal", MapColor.COLOR_ORANGE);
    public static final DeferredHolder<Block, Block> SMALL_ICHOR_SLIME_CRYSTAL_BUD =
            registerCrystalBud("small_ichor_slime_crystal_bud", MapColor.COLOR_ORANGE,
                    3.0F, 4.0F, SoundType.SMALL_AMETHYST_BUD);
    public static final DeferredHolder<Block, Block> MEDIUM_ICHOR_SLIME_CRYSTAL_BUD =
            registerCrystalBud("medium_ichor_slime_crystal_bud", MapColor.COLOR_ORANGE,
                    4.0F, 3.0F, SoundType.MEDIUM_AMETHYST_BUD);
    public static final DeferredHolder<Block, Block> LARGE_ICHOR_SLIME_CRYSTAL_BUD =
            registerCrystalBud("large_ichor_slime_crystal_bud", MapColor.COLOR_ORANGE,
                    5.0F, 3.0F, SoundType.LARGE_AMETHYST_BUD);
    public static final DeferredHolder<Block, Block> ICHOR_SLIME_CRYSTAL_CLUSTER =
            registerCrystalBud("ichor_slime_crystal_cluster", MapColor.COLOR_ORANGE,
                    7.0F, 3.0F, SoundType.AMETHYST_CLUSTER);

    public static final DeferredHolder<Block, Block> ENDER_SLIME_CRYSTAL_BLOCK =
            registerCrystalBlock("ender_slime_crystal_block", MapColor.COLOR_PURPLE);
    public static final DeferredHolder<Block, Block> BUDDING_ENDER_SLIME_CRYSTAL =
            registerBuddingCrystal("budding_ender_slime_crystal", MapColor.COLOR_PURPLE);
    public static final DeferredHolder<Block, Block> SMALL_ENDER_SLIME_CRYSTAL_BUD =
            registerCrystalBud("small_ender_slime_crystal_bud", MapColor.COLOR_PURPLE,
                    3.0F, 4.0F, SoundType.SMALL_AMETHYST_BUD);
    public static final DeferredHolder<Block, Block> MEDIUM_ENDER_SLIME_CRYSTAL_BUD =
            registerCrystalBud("medium_ender_slime_crystal_bud", MapColor.COLOR_PURPLE,
                    4.0F, 3.0F, SoundType.MEDIUM_AMETHYST_BUD);
    public static final DeferredHolder<Block, Block> LARGE_ENDER_SLIME_CRYSTAL_BUD =
            registerCrystalBud("large_ender_slime_crystal_bud", MapColor.COLOR_PURPLE,
                    5.0F, 3.0F, SoundType.LARGE_AMETHYST_BUD);
    public static final DeferredHolder<Block, Block> ENDER_SLIME_CRYSTAL_CLUSTER =
            registerCrystalBud("ender_slime_crystal_cluster", MapColor.COLOR_PURPLE,
                    7.0F, 3.0F, SoundType.AMETHYST_CLUSTER);

    static {
        WORLD_ITEMS.add(RAW_COBALT);
        WORLD_ITEMS.add(STEEL_SHARD);
        WORLD_ITEMS.add(COBALT_SHARD);
        WORLD_ITEMS.add(KNIGHTMETAL_SHARD);
        WORLD_ITEMS.add(EARTH_SLIME_CRYSTAL);
        WORLD_ITEMS.add(SKY_SLIME_CRYSTAL);
        WORLD_ITEMS.add(ICHOR_SLIME_CRYSTAL);
        WORLD_ITEMS.add(ENDER_SLIME_CRYSTAL);
    }

    private WorldContent() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    public static void addItems(CreativeModeTab.Output output) {
        WORLD_ITEMS.forEach(item -> {
            if (item != null && item.isBound()) {
                output.accept(item.get());
            }
        });
    }

    private static DeferredHolder<Block, Block> registerSlime(String id, MapColor color,
                                                               java.util.function.BiPredicate<net.minecraft.world.level.block.state.BlockState,
                                                                       net.minecraft.world.level.block.state.BlockState> predicate) {
        return registerBlockWithItem(id, BlockBehaviour.Properties.of().mapColor(color)
                .sound(SoundType.SLIME_BLOCK).friction(0.8F).strength(0.6F).noOcclusion(),
                new SlimeFactory(predicate));
    }

    private static DeferredHolder<Block, Block> registerCongealed(String id, MapColor color) {
        return registerBlockWithItem(id, BlockBehaviour.Properties.of().mapColor(color)
                .sound(SoundType.SLIME_BLOCK).strength(0.5F).friction(0.5F),
                CongealedSlimeBlock::new);
    }

    private static DeferredHolder<Block, Block> registerLog(String id) {
        return registerBlockWithItem(id, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN)
                .sound(SoundType.WOOD).strength(2.0F).requiresCorrectToolForDrops(),
                RotatedPillarBlock::new);
    }

    private static DeferredHolder<Block, Block> registerLeaves(String id) {
        return registerBlockWithItem(id, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN)
                .sound(SoundType.GRASS).strength(0.2F).noOcclusion().randomTicks(),
                LeavesBlock::new);
    }

    private static DeferredHolder<Block, Block> registerSapling(String id,
                                                                 DeferredHolder<Block, Block> log,
                                                                 DeferredHolder<Block, Block> leaves) {
        return registerBlockWithItem(id, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN)
                .sound(SoundType.GRASS).strength(0.0F).noCollission().randomTicks(),
                properties -> new ThemedSaplingBlock(properties, log::get, leaves::get));
    }

    private static DeferredHolder<Block, Block> registerBlockWithItem(String id,
                                                                        BlockBehaviour.Properties properties) {
        return registerBlockWithItem(id, properties, Block::new);
    }

    private static DeferredHolder<Block, Block> registerBlockWithItem(String id,
                                                                        BlockBehaviour.Properties properties,
                                                                        java.util.function.Function<BlockBehaviour.Properties, Block> factory) {
        DeferredHolder<Block, Block> block = BLOCKS.register(id, () -> factory.apply(properties));
        DeferredHolder<Item, Item> item = ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
        WORLD_ITEMS.add(item);
        return block;
    }

    private static DeferredHolder<Item, Item> registerCrystal(String id) {
        return ITEMS.register(id, () -> new Item(new Item.Properties().rarity(
                net.minecraft.world.item.Rarity.UNCOMMON)));
    }

    private static DeferredHolder<Block, Block> registerCrystalBlock(String id, MapColor color) {
        return registerBlockWithItem(id, geodeProperties(color, SoundType.AMETHYST), Block::new);
    }

    private static DeferredHolder<Block, Block> registerBuddingCrystal(String id, MapColor color) {
        return registerBlockWithItem(id, geodeProperties(color, SoundType.AMETHYST).randomTicks(),
                properties -> switch (id) {
                    case "budding_earth_slime_crystal" -> new BuddingCrystalBlock(properties,
                            () -> SMALL_EARTH_SLIME_CRYSTAL_BUD.get(),
                            () -> MEDIUM_EARTH_SLIME_CRYSTAL_BUD.get(),
                            () -> LARGE_EARTH_SLIME_CRYSTAL_BUD.get(),
                            () -> EARTH_SLIME_CRYSTAL_CLUSTER.get());
                    case "budding_sky_slime_crystal" -> new BuddingCrystalBlock(properties,
                            () -> SMALL_SKY_SLIME_CRYSTAL_BUD.get(),
                            () -> MEDIUM_SKY_SLIME_CRYSTAL_BUD.get(),
                            () -> LARGE_SKY_SLIME_CRYSTAL_BUD.get(),
                            () -> SKY_SLIME_CRYSTAL_CLUSTER.get());
                    case "budding_ichor_slime_crystal" -> new BuddingCrystalBlock(properties,
                            () -> SMALL_ICHOR_SLIME_CRYSTAL_BUD.get(),
                            () -> MEDIUM_ICHOR_SLIME_CRYSTAL_BUD.get(),
                            () -> LARGE_ICHOR_SLIME_CRYSTAL_BUD.get(),
                            () -> ICHOR_SLIME_CRYSTAL_CLUSTER.get());
                    case "budding_ender_slime_crystal" -> new BuddingCrystalBlock(properties,
                            () -> SMALL_ENDER_SLIME_CRYSTAL_BUD.get(),
                            () -> MEDIUM_ENDER_SLIME_CRYSTAL_BUD.get(),
                            () -> LARGE_ENDER_SLIME_CRYSTAL_BUD.get(),
                            () -> ENDER_SLIME_CRYSTAL_CLUSTER.get());
                    default -> throw new IllegalArgumentException("Unknown crystal family: " + id);
                });
    }

    private static DeferredHolder<Block, Block> registerCrystalBud(String id, MapColor color,
                                                                    float height, float offset,
                                                                    SoundType sound) {
        return registerBlockWithItem(id, geodeProperties(color, sound).noOcclusion()
                        .forceSolidOn().randomTicks()
                        .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY),
                properties -> new CrystalClusterBlock((int) height, (int) offset, properties));
    }

    private static DeferredHolder<Block, Block> registerMetalCluster(String id, MapColor color,
                                                                      int light) {
        return registerBlockWithItem(id, BlockBehaviour.Properties.of().mapColor(color)
                        .sound(SoundType.METAL).strength(1.5F).requiresCorrectToolForDrops()
                        .noOcclusion().lightLevel(state -> light),
                properties -> new CrystalClusterBlock(7, 3, properties));
    }

    private static BlockBehaviour.Properties geodeProperties(MapColor color, SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(color).sound(sound)
                .strength(1.5F).requiresCorrectToolForDrops();
    }

    private static final class SlimeFactory implements java.util.function.Function<BlockBehaviour.Properties, Block> {
        private final java.util.function.BiPredicate<net.minecraft.world.level.block.state.BlockState,
                net.minecraft.world.level.block.state.BlockState> predicate;

        private SlimeFactory(java.util.function.BiPredicate<net.minecraft.world.level.block.state.BlockState,
                net.minecraft.world.level.block.state.BlockState> predicate) {
            this.predicate = predicate;
        }

        @Override
        public Block apply(BlockBehaviour.Properties properties) {
            return new StickySlimeBlock(properties, predicate);
        }
    }
}
