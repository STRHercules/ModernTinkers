package com.moderntinkers.content.material;

import java.util.List;

/**
 * The small, data-driven material contract shared by part building and tools.
 * Values intentionally mirror the useful subset of Tinkers' head and handle
 * stats while keeping the persisted tool format independent of registry IDs.
 */
public record MaterialDefinition(
        String id,
        int tier,
        int headDurability,
        float miningSpeed,
        float headAttackDamage,
        float handleDurability,
        float handleMiningSpeed,
        float handleAttackDamage,
        float handleAttackSpeed,
        int color,
        List<String> traits,
        List<MaterialInput> inputs) {

    public MaterialDefinition {
        traits = List.copyOf(traits);
        inputs = List.copyOf(inputs);
    }

    public record MaterialInput(String itemId, int units) {
        public MaterialInput {
            units = Math.max(1, units);
        }
    }
}
