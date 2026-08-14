package com.moderntinkers.client.screen;

import com.moderntinkers.content.smeltery.CastingMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Native casting screen; molten volume is supplied through the block tank. */
public final class CastingScreen extends AbstractContainerScreen<CastingMenu> {
    public CastingScreen(CastingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 186;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF2B2522);
        graphics.fill(leftPos + 76, topPos + 31, leftPos + 101, topPos + 57, 0xFF6C5545);
        graphics.fill(leftPos + 145, topPos + 31, leftPos + 171, topPos + 57, 0xFF6C5545);
        graphics.fill(leftPos + 5, topPos + 98, leftPos + 171, topPos + 178, 0xFF171313);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFFFFF);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFFFFF);
    }
}
