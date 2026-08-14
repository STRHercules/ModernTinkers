package com.moderntinkers.client.screen;

import com.moderntinkers.content.tinkerstation.TinkerStationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Lightweight native screen for Tinker Station assembly. */
public final class TinkerStationScreen extends AbstractContainerScreen<TinkerStationMenu> {
    public TinkerStationScreen(TinkerStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 186;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF29292A);
        graphics.fill(leftPos + 40, topPos + 21, leftPos + 65, topPos + 47, 0xFF5B5B5D);
        graphics.fill(leftPos + 76, topPos + 39, leftPos + 101, topPos + 65, 0xFF5B5B5D);
        graphics.fill(leftPos + 112, topPos + 57, leftPos + 137, topPos + 83, 0xFF5B5B5D);
        graphics.fill(leftPos + 145, topPos + 39, leftPos + 171, topPos + 65, 0xFF5B5B5D);
        graphics.fill(leftPos + 5, topPos + 98, leftPos + 171, topPos + 178, 0xFF171717);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFFFFF);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFFFFF);
    }
}
