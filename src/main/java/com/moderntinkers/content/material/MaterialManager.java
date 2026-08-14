package com.moderntinkers.content.material;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.moderntinkers.ModernTinkers;
import com.moderntinkers.content.tools.MaterialPartItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Reloadable material definitions used by the port's tool and table systems.
 * The fallback set keeps tools usable before the first server resource reload;
 * datapacks can override individual definitions without code changes.
 */
public final class MaterialManager {
    private static final com.google.gson.Gson GSON = new com.google.gson.Gson();
    private static volatile Map<String, MaterialDefinition> definitions = defaults();
    private static final SimpleJsonResourceReloadListener RELOAD_LISTENER =
            new SimpleJsonResourceReloadListener(GSON, "tinkering/materials") {
                @Override
                protected void apply(Map<ResourceLocation, JsonElement> resources,
                                     net.minecraft.server.packs.resources.ResourceManager manager,
                                     ProfilerFiller profiler) {
                    Map<String, MaterialDefinition> loaded = new LinkedHashMap<>(defaults());
                    resources.forEach((location, element) -> {
                        if (!element.isJsonObject()) {
                            return;
                        }
                        try {
                            String id = location.getPath();
                            int slash = id.lastIndexOf('/');
                            if (slash >= 0) {
                                id = id.substring(slash + 1);
                            }
                            loaded.put(id, parse(id, element.getAsJsonObject()));
                        } catch (RuntimeException ignored) {
                            // A malformed optional definition should not take down a world reload.
                        }
                    });
                    definitions = Map.copyOf(loaded);
                }
            };

    private MaterialManager() {}

    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(RELOAD_LISTENER);
    }

    public static MaterialDefinition get(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        MaterialDefinition definition = definitions.get(id);
        if (definition != null) {
            return definition;
        }
        ResourceLocation location = ResourceLocation.tryParse(id);
        return location == null ? null : definitions.get(location.getPath());
    }

    public static List<MaterialDefinition> all() {
        return List.copyOf(definitions.values());
    }

    public static boolean hasTrait(String id, String trait) {
        MaterialDefinition definition = get(id);
        return definition != null && definition.traits().contains(trait);
    }

    public static Optional<MaterialInput> findInput(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        if (MaterialPartItem.isPart(stack)) {
            String material = MaterialPartItem.getMaterial(stack);
            int units = MaterialPartItem.materialUnits(stack);
            if (get(material) != null && units > 0) {
                return Optional.of(new MaterialInput(material, units));
            }
        }
        for (MaterialDefinition definition : definitions.values()) {
            for (MaterialDefinition.MaterialInput input : definition.inputs()) {
                String inputId = input.itemId();
                ResourceLocation itemId = ResourceLocation.tryParse(
                        inputId.startsWith("#") ? inputId.substring(1) : inputId);
                if (itemId == null) {
                    continue;
                }
                if (matchesInput(stack, inputId, itemId)) {
                    return Optional.of(new MaterialInput(definition.id(), input.units()));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Finds the registered output item for a material form.  The first
     * matching input is intentional: material JSON keeps the canonical ingot
     * and nugget before scrap, dust, or compatibility inputs.
     */
    public static Item outputItem(String materialId, String form) {
        MaterialDefinition definition = get(materialId);
        if (definition == null) {
            return Items.AIR;
        }
        String suffix = "ingot".equals(form) ? "_ingot" : "_nugget";
        for (MaterialDefinition.MaterialInput input : definition.inputs()) {
            ResourceLocation itemId = ResourceLocation.tryParse(input.itemId());
            if (itemId == null || !itemId.getPath().endsWith(suffix)) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item != Items.AIR) {
                return item;
            }
        }
        return Items.AIR;
    }

    public static ItemStack outputStack(String materialId, String form) {
        Item item = outputItem(materialId, form);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static MaterialDefinition parse(String id, JsonObject object) {
        List<String> traits = new ArrayList<>();
        if (object.has("traits") && object.get("traits").isJsonArray()) {
            JsonArray array = object.getAsJsonArray("traits");
            array.forEach(value -> {
                if (value.isJsonPrimitive()) {
                    traits.add(value.getAsString());
                }
            });
        }

        List<MaterialDefinition.MaterialInput> inputs = new ArrayList<>();
        addInput(inputs, object, "ingot", 9);
        addInput(inputs, object, "nugget", 1);
        addInput(inputs, object, "item", GsonHelper.getAsInt(object, "item_units", 1));
        if (object.has("additional_inputs") && object.get("additional_inputs").isJsonArray()) {
            object.getAsJsonArray("additional_inputs").forEach(value -> {
                if (value.isJsonPrimitive()) {
                    inputs.add(new MaterialDefinition.MaterialInput(value.getAsString(), 1));
                }
            });
        }
        return new MaterialDefinition(
                id,
                tier(object, GsonHelper.getAsString(object, "harvest_tier", "stone")),
                Math.max(1, GsonHelper.getAsInt(object, "head_durability", 100)),
                Math.max(0.1F, GsonHelper.getAsFloat(object, "mining_speed", 2.0F)),
                GsonHelper.getAsFloat(object, "head_attack_damage", 1.0F),
                GsonHelper.getAsFloat(object, "handle_durability", 0.0F),
                GsonHelper.getAsFloat(object, "handle_mining_speed", 0.0F),
                GsonHelper.getAsFloat(object, "handle_attack_damage", 0.0F),
                GsonHelper.getAsFloat(object, "handle_attack_speed", 0.0F),
                color(GsonHelper.getAsString(object, "color", "#FFFFFF")),
                traits,
                inputs);
    }

    private static void addInput(List<MaterialDefinition.MaterialInput> inputs,
                                 JsonObject object, String key, int units) {
        if (object.has(key) && object.get(key).isJsonPrimitive()) {
            inputs.add(new MaterialDefinition.MaterialInput(
                    object.get(key).getAsString(), units));
        }
    }

    private static boolean matchesInput(ItemStack stack, String inputId,
                                        ResourceLocation itemId) {
        if (inputId.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(inputId.substring(1));
            return tagId != null && stack.is(net.minecraft.tags.TagKey.create(
                    Registries.ITEM, tagId));
        }
        Item item = BuiltInRegistries.ITEM.get(itemId);
        return item != Items.AIR && stack.is(item);
    }

    private static int tier(JsonObject object, String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "netherite" -> 4;
            case "diamond" -> 3;
            case "iron" -> 2;
            case "stone" -> 1;
            default -> 0;
        };
    }

    private static int color(String value) {
        try {
            String normalized = value.startsWith("#") ? value.substring(1) : value;
            return Integer.parseInt(normalized, 16) | 0xFF000000;
        } catch (NumberFormatException ignored) {
            return 0xFFFFFFFF;
        }
    }

    private static Map<String, MaterialDefinition> defaults() {
        Map<String, MaterialDefinition> result = new LinkedHashMap<>();
        add(result, "wood", 0, 200, 2.0F, 0.5F, 0.05F, 0.0F, 0.0F, 0.0F,
                0xFF9A6A3A, "minecraft:oak_planks");
        add(result, "stone", 1, 100, 4.0F, 1.5F, 0.0F, 0.0F, 0.0F, 0.0F,
                0xFF898989, "minecraft:cobblestone");
        add(result, "flint", 1, 150, 5.0F, 1.5F, 0.0F, 0.0F, 0.0F, 0.0F,
                0xFF6E6E6E, "minecraft:flint");
        add(result, "cactus", 0, 180, 3.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F,
                0xFF5E9C45, "minecraft:cactus");
        add(result, "bone", 0, 100, 2.5F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F,
                0xFFE8E1C8, "minecraft:bone");
        add(result, "feather", 0, 80, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                0xFFEFEFEF, "minecraft:feather");
        add(result, "paper", 0, 50, 3.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                0xFFF4F0DC, "minecraft:paper");
        add(result, "iron", 2, 250, 6.0F, 2.0F, 0.10F, 0.0F, 0.0F, 0.0F,
                0xFFD8D8D8, "minecraft:iron_ingot", "minecraft:iron_nugget");
        add(result, "gold", 1, 175, 9.0F, 1.0F, -0.30F, 0.10F, 0.0F, 0.0F,
                0xFFFFD83D, "minecraft:gold_ingot", "minecraft:gold_nugget");
        add(result, "copper", 1, 210, 5.0F, 1.5F, 0.05F, 0.0F, 0.0F, 0.0F,
                0xFFB86C4B, "minecraft:copper_ingot", "moderntinkers:copper_nugget");
        add(result, "obsidian", 3, 1200, 5.5F, 3.0F, 0.10F, 0.0F, 0.0F, 0.0F,
                0xFF241B37, "minecraft:obsidian");
        add(result, "prismarine", 2, 500, 4.5F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                0xFF72A59A, "minecraft:prismarine_shard");
        add(result, "endstone", 2, 400, 4.0F, 1.5F, 0.0F, 0.0F, 0.0F, 0.0F,
                0xFFE5D9A2, "minecraft:end_stone");
        add(result, "slimewood", 1, 300, 3.5F, 0.75F, 0.10F, 0.0F, 0.0F, 0.0F,
                0xFF8BC98B, "minecraft:slime_ball");
        add(result, "blood", 2, 150, 4.0F, 1.0F, 0.10F, 0.0F, 0.0F, 0.0F,
                0xFFA12D38, "moderntinkers:blood_slime");
        add(result, "netherite", 4, 800, 8.0F, 3.5F, 0.15F, 0.0F, 0.0F, 0.0F,
                0xFF443B45, "minecraft:netherite_ingot", "minecraft:netherite_scrap",
                "moderntinkers:netherite_nugget", "moderntinkers:debris_nugget");
        add(result, "cobalt", 3, 800, 6.5F, 2.25F, 0.05F, 0.05F, 0.0F, 0.05F,
                0xFF3569D6, "moderntinkers:cobalt_ingot", "moderntinkers:cobalt_nugget",
                "moderntinkers:cobalt_shard");
        add(result, "steel", 3, 750, 6.5F, 2.0F, 0.10F, 0.0F, 0.0F, 0.0F,
                0xFF6F747A, "moderntinkers:steel_ingot", "moderntinkers:steel_nugget",
                "moderntinkers:steel_shard");
        add(result, "slimesteel", 3, 850, 6.0F, 2.0F, 0.10F, 0.0F, 0.0F, 0.05F,
                0xFF6EB8A5, "moderntinkers:slimesteel_ingot", "moderntinkers:slimesteel_nugget");
        add(result, "amethyst_bronze", 3, 720, 7.0F, 1.5F, 0.0F, 0.10F, 0.0F, 0.05F,
                0xFFB38BDB, "moderntinkers:amethyst_bronze_ingot", "moderntinkers:amethyst_bronze_nugget");
        add(result, "rose_gold", 2, 580, 6.0F, 2.5F, 0.10F, -0.15F, 0.0F, 0.0F,
                0xFFFFB283, "moderntinkers:rose_gold_ingot", "moderntinkers:rose_gold_nugget");
        add(result, "pig_iron", 2, 540, 5.0F, 2.0F, 0.10F, 0.0F, 0.05F, 0.0F,
                0xFFD18B84, "moderntinkers:pig_iron_ingot", "moderntinkers:pig_iron_nugget");
        add(result, "cinderslime", 3, 720, 7.0F, 1.5F, 0.0F, 0.10F, 0.0F, 0.0F,
                0xFFF28D43, "moderntinkers:cinderslime_ingot", "moderntinkers:cinderslime_nugget");
        add(result, "queens_slime", 4, 1150, 7.0F, 3.0F, 0.15F, 0.0F, 0.0F, 0.0F,
                0xFF8BD35C, "moderntinkers:queens_slime_ingot", "moderntinkers:queens_slime_nugget");
        add(result, "manyullyn", 4, 1250, 6.5F, 3.5F, 0.10F, 0.20F, -0.05F, 0.0F,
                0xFF7B42A9, "moderntinkers:manyullyn_ingot", "moderntinkers:manyullyn_nugget");
        add(result, "hepatizon", 3, 1047, 7.5F, 3.25F, -0.05F, -0.05F, -0.05F, 0.15F,
                0xFF765B9A, "moderntinkers:hepatizon_ingot", "moderntinkers:hepatizon_nugget");
        add(result, "knightmetal", 3, 580, 6.0F, 2.5F, 0.10F, -0.15F, 0.0F, 0.0F,
                0xFF6E8754, "moderntinkers:knightmetal_ingot", "moderntinkers:knightmetal_nugget",
                "moderntinkers:knightmetal_shard");
        add(result, "knightslime", 3, 775, 6.0F, 2.75F, 0.05F, 0.05F, 0.05F, 0.0F,
                0xFF5A4F70, "moderntinkers:knightslime_ingot", "moderntinkers:knightslime_nugget");
        add(result, "soulsteel", 3, 700, 6.5F, 2.75F, 0.10F, 0.0F, 0.0F, 0.0F,
                0xFF5A3C3C, "moderntinkers:soulsteel_ingot", "moderntinkers:soulsteel_nugget");
        add(result, "nahuatl", 0, 200, 2.0F, 0.5F, 0.05F, 0.0F, 0.0F, 0.0F,
                0xFF8F5B92, "moderntinkers:nahuatl");
        add(result, "blazewood", 1, 350, 4.5F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                0xFFCB603B, "moderntinkers:blazewood");
        return result;
    }

    private static void add(Map<String, MaterialDefinition> target, String id, int tier,
                            int durability, float miningSpeed, float attackDamage,
                            float handleDurability, float handleMiningSpeed,
                            float handleAttackDamage, float handleAttackSpeed,
                            int color, String... inputs) {
        List<MaterialDefinition.MaterialInput> materialInputs = new ArrayList<>();
        for (String input : inputs) {
            int units = input.endsWith("_nugget") || input.endsWith("_scrap")
                    || input.endsWith("_shard")
                    || input.endsWith(":nahuatl") || input.endsWith(":blazewood")
                    || input.endsWith(":copper_nugget") || input.endsWith(":netherite_nugget")
                    || input.endsWith(":debris_nugget") ? 1 : 9;
            materialInputs.add(new MaterialDefinition.MaterialInput(input, units));
        }
        target.put(id, new MaterialDefinition(id, tier, durability, miningSpeed, attackDamage,
                handleDurability, handleMiningSpeed, handleAttackDamage, handleAttackSpeed,
                color, defaultTraits(id), materialInputs));
    }

    private static List<String> defaultTraits(String id) {
        return switch (id) {
            case "cactus" -> List.of("spiny");
            case "endstone" -> List.of("enderference");
            case "prismarine" -> List.of("aquatic");
            case "slimewood" -> List.of("slimey");
            case "obsidian" -> List.of("durable");
            case "cobalt" -> List.of("lightspeed");
            case "hepatizon" -> List.of("soulspeed");
            case "knightslime" -> List.of("crumbling", "overshield");
            case "manyullyn" -> List.of("insatiable");
            case "pig_iron" -> List.of("bloodbound");
            case "queens_slime" -> List.of("slime");
            case "rose_gold" -> List.of("established");
            case "soulsteel" -> List.of("necrotic");
            case "blazewood", "cinderslime" -> List.of("flamewake");
            default -> List.of();
        };
    }

    public record MaterialInput(String materialId, int units) {}
}
