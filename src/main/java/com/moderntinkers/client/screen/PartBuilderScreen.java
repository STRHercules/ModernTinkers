package com.moderntinkers.client.screen;

import com.moderntinkers.content.partbuilder.PartBuilderMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Lightweight native screen for the server-authoritative Part Builder. */
public final class PartBuilderScreen extends AbstractContainerScreen<PartBuilderMenu> {
    public PartBuilderScreen(PartBuilderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 186;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF2B211D);
        graphics.fill(leftPos + 5, topPos + 39, leftPos + 49, topPos + 61, 0xFF5A453A);
        graphics.fill(leftPos + 145, topPos + 39, leftPos + 171, topPos + 65, 0xFF5A453A);
        graphics.fill(leftPos + 5, topPos + 98, leftPos + 171, topPos + 178, 0xFF171313);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFFFFF);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFFFFF);
    }
}
