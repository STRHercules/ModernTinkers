package com.moderntinkers.content.recipe;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small data-driven recipe boundary for the port's melting and alloying
 * machines. The fallback material lookup remains available for compatibility
 * with older worlds and datapacks that only provide material definitions.
 */
public final class TinkerRecipeManager {
    private static final Gson GSON = new Gson();
    private static volatile List<MeltingRecipe> melting = defaultMelting();
    private static volatile List<AlloyRecipe> alloys = defaultAlloys();

    private static final SimpleJsonResourceReloadListener MELTING_LISTENER =
            new SimpleJsonResourceReloadListener(GSON, "tinkering/melting") {
                @Override
                protected void apply(Map<ResourceLocation, JsonElement> resources,
                                     net.minecraft.server.packs.resources.ResourceManager manager,
                                     ProfilerFiller profiler) {
                    // Datapack entries override the built-in table by order,
                    // while a small add-on pack must not erase every default
                    // melt recipe that it did not restate.
                    List<MeltingRecipe> loaded = new ArrayList<>();
                    resources.forEach((location, element) -> appendMelting(loaded, element));
                    loaded.addAll(defaultMelting());
                    melting = List.copyOf(loaded);
                }
            };

    private static final SimpleJsonResourceReloadListener ALLOY_LISTENER =
            new SimpleJsonResourceReloadListener(GSON, "tinkering/alloying") {
                @Override
                protected void apply(Map<ResourceLocation, JsonElement> resources,
                                     net.minecraft.server.packs.resources.ResourceManager manager,
                                     ProfilerFiller profiler) {
                    Map<String, AlloyRecipe> loaded = new LinkedHashMap<>();
                    for (AlloyRecipe recipe : defaultAlloys()) {
                        loaded.put(recipe.resultFluid(), recipe);
                    }
                    resources.forEach((location, element) -> appendAlloys(loaded, element));
                    alloys = List.copyOf(loaded.values());
                }
            };

    private TinkerRecipeManager() {}

    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(MELTING_LISTENER);
        event.addListener(ALLOY_LISTENER);
    }

    public static MeltingRecipe findMelting(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        for (MeltingRecipe recipe : melting) {
            if (recipe.matches(stack)) {
                return recipe;
            }
        }
        return null;
    }

    public static List<AlloyRecipe> alloyRecipes() {
        return alloys;
    }

    private static void appendMelting(List<MeltingRecipe> target, JsonElement element) {
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (int index = 0; index < array.size(); index++) {
                appendMelting(target, array.get(index));
            }
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        try {
            JsonObject object = element.getAsJsonObject();
            List<ItemMatcher> matchers = new ArrayList<>();
            if (object.has("item")) {
                addItemMatcher(matchers, object.get("item").getAsString());
            }
            if (object.has("items") && object.get("items").isJsonArray()) {
                object.getAsJsonArray("items").forEach(value -> {
                    if (value.isJsonPrimitive()) {
                        addItemMatcher(matchers, value.getAsString());
                    }
                });
            }
            if (object.has("tag")) {
                addTagMatcher(matchers, object.get("tag").getAsString());
            }
            if (matchers.isEmpty()) {
                return;
            }
            String fluid = GsonHelper.getAsString(object, "fluid", "");
            int amount = Math.max(1, GsonHelper.getAsInt(object, "amount", 90));
            int time = Math.max(1, GsonHelper.getAsInt(object, "time", 20));
            int temperature = Math.max(0, GsonHelper.getAsInt(object, "temperature", 1000));
            int count = Math.max(1, GsonHelper.getAsInt(object, "count", 1));
            if (!fluid.isBlank()) {
                target.add(new MeltingRecipe(matchers, fluid, amount, time, temperature, count));
            }
        } catch (RuntimeException ignored) {
            // Optional datapack entries must not make a world unloadable.
        }
    }

    private static void appendAlloys(Map<String, AlloyRecipe> target, JsonElement element) {
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (int index = 0; index < array.size(); index++) {
                appendAlloys(target, array.get(index));
            }
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        try {
            JsonObject object = element.getAsJsonObject();
            String result = GsonHelper.getAsString(object, "result", "");
            int amount = Math.max(1, GsonHelper.getAsInt(object, "amount", 90));
            if (result.isBlank() || !object.has("inputs") || !object.get("inputs").isJsonArray()) {
                return;
            }
            List<FluidRequirement> inputs = new ArrayList<>();
            object.getAsJsonArray("inputs").forEach(value -> {
                if (!value.isJsonObject()) {
                    return;
                }
                JsonObject input = value.getAsJsonObject();
                String fluid = GsonHelper.getAsString(input, "fluid", "");
                int inputAmount = Math.max(1, GsonHelper.getAsInt(input, "amount", 1));
                if (!fluid.isBlank()) {
                    inputs.add(new FluidRequirement(fluid, inputAmount));
                }
            });
            if (!inputs.isEmpty()) {
                target.put(result, new AlloyRecipe(result, amount, List.copyOf(inputs)));
            }
        } catch (RuntimeException ignored) {
            // Optional datapack entries must not make a world unloadable.
        }
    }

    private static void addItemMatcher(List<ItemMatcher> matchers, String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location != null) {
            matchers.add(ItemMatcher.item(location));
        }
    }

    private static void addTagMatcher(List<ItemMatcher> matchers, String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location != null) {
            matchers.add(ItemMatcher.tag(TagKey.create(Registries.ITEM, location)));
        }
    }

    private static List<AlloyRecipe> defaultAlloys() {
        return List.of(
                alloy("molten_manyullyn", 360,
                        fluid("molten_cobalt", 270), fluid("molten_netherite", 90)),
                alloy("molten_rose_gold", 180,
                        fluid("molten_copper", 90), fluid("molten_gold", 90)),
                alloy("molten_amethyst_bronze", 90,
                        fluid("molten_copper", 90), fluid("molten_amethyst", 100)),
                alloy("molten_hepatizon", 180,
                        fluid("molten_copper", 180), fluid("molten_cobalt", 90),
                        fluid("molten_quartz", 100)),
                alloy("molten_slimesteel", 180,
                        fluid("molten_iron", 90), fluid("molten_sky_slime", 250),
                        fluid("seared_stone", 250)),
                alloy("molten_queens_slime", 180,
                        fluid("molten_cobalt", 90), fluid("molten_gold", 90),
                        fluid("molten_magma", 250)),
                alloy("molten_pig_iron", 180,
                        fluid("molten_iron", 90), fluid("molten_meat_soup", 500),
                        fluid("molten_honey", 250)),
                alloy("molten_knightslime", 180,
                        fluid("molten_cobalt", 90), fluid("molten_ender_slime", 250),
                        fluid("molten_obsidian", 250)),
                alloy("molten_cinderslime", 180,
                        fluid("molten_gold", 90), fluid("molten_ichor", 250),
                        fluid("scorched_stone", 250)));
    }

    private static List<MeltingRecipe> defaultMelting() {
        return List.of(
                melting("minecraft:iron_ingot", "molten_iron", 90, 20, 1000),
                melting("minecraft:iron_nugget", "molten_iron", 10, 8, 1000),
                melting("minecraft:gold_ingot", "molten_gold", 90, 20, 1100),
                melting("minecraft:gold_nugget", "molten_gold", 10, 8, 1100),
                melting("minecraft:copper_ingot", "molten_copper", 90, 20, 1080),
                melting("minecraft:copper_nugget", "molten_copper", 10, 8, 1080),
                melting("minecraft:netherite_ingot", "molten_netherite", 90, 28, 1600),
                melting("minecraft:netherite_scrap", "molten_debris", 10, 12, 1300),
                melting("minecraft:ancient_debris", "molten_debris", 90, 24, 1300),
                melting("minecraft:quartz", "molten_quartz", 10, 8, 1000),
                melting("minecraft:amethyst_shard", "molten_amethyst", 10, 8, 900),
                melting("minecraft:slime_ball", "molten_sky_slime", 10, 8, 900),
                melting("minecraft:magma_cream", "molten_magma", 10, 8, 1000),
                melting("minecraft:honey_bottle", "molten_honey", 100, 12, 600),
                melting("minecraft:obsidian", "molten_obsidian", 90, 24, 1200),
                melting("moderntinkers:steel_cluster", "molten_steel", 40, 20, 1200),
                melting("moderntinkers:cobalt_cluster", "molten_cobalt", 40, 20, 1200),
                melting("moderntinkers:knightmetal_cluster", "molten_knightmetal", 40, 20, 1150),
                taggedMelting("c:ingots/cobalt", "molten_cobalt", 90, 20, 1200),
                taggedMelting("c:ingots/steel", "molten_steel", 90, 20, 1200),
                taggedMelting("c:ingots/manyullyn", "molten_manyullyn", 90, 22, 1450),
                taggedMelting("c:ingots/rose_gold", "molten_rose_gold", 90, 18, 1000),
                taggedMelting("c:ingots/pig_iron", "molten_pig_iron", 90, 18, 950),
                taggedMelting("c:ingots/slimesteel", "molten_slimesteel", 90, 20, 1100),
                taggedMelting("c:ingots/amethyst_bronze", "molten_amethyst_bronze", 90, 18, 1050),
                taggedMelting("c:ingots/queens_slime", "molten_queens_slime", 90, 24, 1350),
                taggedMelting("c:ingots/hepatizon", "molten_hepatizon", 90, 22, 1400),
                taggedMelting("c:ingots/knightmetal", "molten_knightmetal", 90, 18, 1150),
                taggedMelting("c:ingots/knightslime", "molten_knightslime", 90, 20, 1250),
                taggedMelting("c:ingots/soulsteel", "molten_soulsteel", 90, 20, 1300));
    }

    private static MeltingRecipe melting(String itemId, String fluid, int amount,
                                         int time, int temperature) {
        return new MeltingRecipe(List.of(ItemMatcher.item(ResourceLocation.parse(itemId))),
                fluid, amount, time, temperature, 1);
    }

    private static MeltingRecipe taggedMelting(String tagId, String fluid, int amount,
                                               int time, int temperature) {
        return new MeltingRecipe(List.of(ItemMatcher.tag(TagKey.create(
                        Registries.ITEM, ResourceLocation.parse(tagId)))),
                fluid, amount, time, temperature, 1);
    }

    private static AlloyRecipe alloy(String result, int amount, FluidRequirement... inputs) {
        return new AlloyRecipe(result, amount, List.of(inputs));
    }

    private static FluidRequirement fluid(String id, int amount) {
        return new FluidRequirement(id, amount);
    }

    public record MeltingRecipe(List<ItemMatcher> matchers, String fluidId, int amount,
                                int time, int temperature, int inputCount) {
        public boolean matches(ItemStack stack) {
            return matchers.stream().anyMatch(matcher -> matcher.matches(stack));
        }
    }

    public record AlloyRecipe(String resultFluid, int resultAmount,
                              List<FluidRequirement> inputs) {}

    public record FluidRequirement(String fluidId, int amount) {}

    public record ItemMatcher(ResourceLocation item, TagKey<Item> tag) {
        public static ItemMatcher item(ResourceLocation location) {
            return new ItemMatcher(location, null);
        }

        public static ItemMatcher tag(TagKey<Item> tag) {
            return new ItemMatcher(null, tag);
        }

        public boolean matches(ItemStack stack) {
            if (item != null) {
                return stack.is(BuiltInRegistries.ITEM.get(item));
            }
            return tag != null && stack.is(tag);
        }
    }
}
