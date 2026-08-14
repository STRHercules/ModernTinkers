package com.moderntinkers.client.screen;

import com.moderntinkers.content.smeltery.AlloyerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Capability-driven Alloyer status screen. */
public final class AlloyerScreen extends AbstractContainerScreen<AlloyerMenu> {
    public AlloyerScreen(AlloyerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 168;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF282020);
        graphics.fill(leftPos + 36, topPos + 30, leftPos + 61, topPos + 55, 0xFF665149);
        graphics.fill(leftPos + 72, topPos + 30, leftPos + 97, topPos + 55, 0xFF665149);
        graphics.fill(leftPos + 108, topPos + 30, leftPos + 136, topPos + 55, 0xFF56392C);
        int progress = Math.min(26, menu.progress() * 26 / 20);
        graphics.fill(leftPos + 109, topPos + 31, leftPos + 109 + progress, topPos + 54, 0xFFE56A2E);
        graphics.fill(leftPos + 5, topPos + 80, leftPos + 171, topPos + 160, 0xFF171313);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFFFFF);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFFFFF);
    }
}
