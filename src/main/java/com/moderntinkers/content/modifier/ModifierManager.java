package com.moderntinkers.content.modifier;

import com.moderntinkers.content.tools.TinkersArmorItem;
import com.moderntinkers.content.tools.TinkersShieldItem;
import com.moderntinkers.content.tools.TinkersToolItem;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The server-side contract for the bundled modifier items.
 *
 * <p>The reference has a registry of data-driven modifier objects.  The
 * bundle stores the same player-facing state in the stack custom-data
 * component, so this small registry owns the rules that must agree between
 * the worktable, tooltips, and runtime hooks: target type, level cap, and
 * modifier-slot cost.</p>
 */
public final class ModifierManager {
    private static final Map<String, Definition> DEFINITIONS = new LinkedHashMap<>();

    static {
        tool("silky_cloth", 1);
        tool("autosmelt", 1);
        tool("fortune", 3);
        tool("luck", 3);
        tool("looting", 3);
        tool("sharpness", 5);
        tool("smite", 5);
        tool("bane_of_arthropods", 5);
        tool("beheading", 5);
        tool("mending", 1);
        tool("experience", 1);
        tool("redstone", 5);
        tool("quartz", 5);
        tool("fiery", 2);
        tool("necrotic", 5);
        tool("knockback", 2);
        tool("haste", 5);
        tool("expanded", 5);
        tool("spitting", 3);
        any("spilling", 1);
        any("overshield", 5);

        any("dragon_scale", 1);
        any("emerald_reinforcement", 5);
        any("slimesteel_reinforcement", 5);
        any("seared_reinforcement", 5);
        any("iron_reinforcement", 5);
        any("obsidian_reinforcement", 5);
        any("gold_reinforcement", 5);
        any("cobalt_reinforcement", 5);
    }

    private ModifierManager() {}

    public static Definition get(String id) {
        return DEFINITIONS.get(id);
    }

    public static boolean isKnown(String id) {
        return get(id) != null;
    }

    public static List<String> ids() {
        return List.copyOf(DEFINITIONS.keySet());
    }

    public static int maxLevel(String id) {
        Definition definition = get(id);
        return definition == null ? 0 : definition.maxLevel();
    }

    public static int slotCost(String id) {
        Definition definition = get(id);
        return definition == null ? 1 : definition.slotsPerLevel();
    }

    public static boolean canApply(String id, ItemStack stack) {
        Definition definition = get(id);
        if (definition == null || stack.isEmpty()) {
            return false;
        }
        if ("spitting".equals(id)
                && stack.getItem() instanceof TinkersToolItem tool
                && tool.kind().requiresAmmo()) {
            return false;
        }
        return switch (definition.target()) {
            case TOOL -> TinkersToolItem.isTool(stack);
            case ANY -> TinkersToolItem.isTool(stack)
                    || TinkersArmorItem.isArmor(stack)
                    || stack.getItem() instanceof TinkersShieldItem;
        };
    }

    private static void tool(String id, int maxLevel) {
        DEFINITIONS.put(id, new Definition(id, maxLevel, 1, Target.TOOL));
    }

    private static void any(String id, int maxLevel) {
        DEFINITIONS.put(id, new Definition(id, maxLevel, 1, Target.ANY));
    }

    public record Definition(String id, int maxLevel, int slotsPerLevel, Target target) {
        public Definition {
            maxLevel = Math.max(1, maxLevel);
            slotsPerLevel = Math.max(1, slotsPerLevel);
        }
    }

    public enum Target {
        TOOL,
        ANY
    }
}
