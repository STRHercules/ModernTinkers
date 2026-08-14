package com.moderntinkers.client.screen;

import com.moderntinkers.content.smeltery.MelterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Native Melter screen with progress/fuel indicators and capability hint. */
public final class MelterScreen extends AbstractContainerScreen<MelterMenu> {
    public MelterScreen(MelterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 178;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF2B2220);
        graphics.fill(leftPos + 40, topPos + 31, leftPos + 65, topPos + 56, 0xFF614B40);
        graphics.fill(leftPos + 76, topPos + 31, leftPos + 101, topPos + 56, 0xFF614B40);
        graphics.fill(leftPos + 112, topPos + 35, leftPos + 140, topPos + 51, 0xFF171313);
        int progress = Math.min(26, menu.progress() * 26 / 20);
        graphics.fill(leftPos + 113, topPos + 36, leftPos + 113 + progress, topPos + 50, 0xFFE56A2E);
        int fuel = menu.fuelTotal() <= 0 ? 0 : Math.min(24, menu.fuelTime() * 24 / menu.fuelTotal());
        graphics.fill(leftPos + 106, topPos + 57, leftPos + 110, topPos + 57 + fuel, 0xFFFFB52E);
        graphics.fill(leftPos + 5, topPos + 90, leftPos + 171, topPos + 170, 0xFF171313);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFFFFF);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFFFFF);
    }
}
