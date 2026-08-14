package com.moderntinkers.client.screen;

import com.moderntinkers.content.modifier.ModifierWorktableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Lightweight native screen for modifier application. */
public final class ModifierWorktableScreen extends AbstractContainerScreen<ModifierWorktableMenu> {
    public ModifierWorktableScreen(ModifierWorktableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 186;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF272A2B);
        graphics.fill(leftPos + 40, topPos + 39, leftPos + 65, topPos + 65, 0xFF555C60);
        graphics.fill(leftPos + 76, topPos + 39, leftPos + 101, topPos + 65, 0xFF555C60);
        graphics.fill(leftPos + 145, topPos + 39, leftPos + 171, topPos + 65, 0xFF555C60);
        graphics.fill(leftPos + 5, topPos + 98, leftPos + 171, topPos + 178, 0xFF151717);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFFFFF);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFFFFF);
    }
}
