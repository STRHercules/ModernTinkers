package com.moderntinkers.content.smeltery;

import com.moderntinkers.content.MaterialItems;
import com.moderntinkers.content.StaticContent;
import com.moderntinkers.content.material.MaterialManager;
import com.moderntinkers.content.tools.MaterialPartItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Reusable sand/gold cast used by the compact casting machines. */
public final class CastingCastItem extends Item {
    public enum Form {
        INGOT("ingot", 90),
        NUGGET("nugget", 10),
        PART("part", 90);

        private final String id;
        private final int amount;

        Form(String id, int amount) {
            this.id = id;
            this.amount = amount;
        }

        public String id() {
            return id;
        }

        public int amount() {
            return amount;
        }
    }

    private final Form form;
    private final String partId;
    private final boolean reusable;

    public CastingCastItem(Properties properties, Form form, String partId) {
        this(properties, form, partId, true);
    }

    public CastingCastItem(Properties properties, Form form, String partId, boolean reusable) {
        super(properties.stacksTo(1));
        this.form = form;
        this.partId = partId;
        this.reusable = reusable;
    }

    public Form form() {
        return form;
    }

    public String partId() {
        return partId;
    }

    public int amount() {
        if (form != Form.PART) {
            return form.amount();
        }
        return com.moderntinkers.content.tools.MaterialPartItem.partUnits(partId) * 10;
    }

    public boolean reusable() {
        return reusable;
    }

    public ItemStack createOutput(String materialId) {
        if (form == Form.INGOT) {
            return MaterialManager.outputStack(materialId, "ingot");
        }
        if (form == Form.NUGGET) {
            ItemStack output = MaterialManager.outputStack(materialId, "nugget");
            if (!output.isEmpty()) {
                return output;
            }
            if ("copper".equals(materialId)) {
                return new ItemStack(MaterialItems.COPPER_NUGGET.get());
            }
            return ItemStack.EMPTY;
        }
        if (partId.isEmpty()) {
            return ItemStack.EMPTY;
        }
        var part = StaticContent.toolPart(partId);
        return part == null || !part.isBound()
                ? ItemStack.EMPTY
                : MaterialPartItem.withMaterial(part.get(), materialId);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.moderntinkers.cast.form", form.id));
        if (!partId.isEmpty()) {
            tooltip.add(Component.translatable("item.moderntinkers.cast.part", partId));
        }
        if (!reusable) {
            tooltip.add(Component.translatable("item.moderntinkers.cast.disposable"));
        }
    }
}
