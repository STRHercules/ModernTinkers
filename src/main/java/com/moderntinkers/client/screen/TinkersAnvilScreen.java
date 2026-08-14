package com.moderntinkers.client.screen;

import com.moderntinkers.content.anvil.TinkersAnvilMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Compact client screen for the shared Tinkers anvil repair menu. */
public final class TinkersAnvilScreen extends AbstractContainerScreen<TinkersAnvilMenu> {
    public TinkersAnvilScreen(TinkersAnvilMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = (width - imageWidth) / 2;
        int top = (height - imageHeight) / 2;
        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFF2D2420);
        graphics.fill(left + 6, top + 6, left + imageWidth - 6, top + 78, 0xFF4B3A32);
        graphics.fill(left + 126, top + 32, left + 150, top + 56, 0xFF191313);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFFFFFF, false);
        graphics.drawString(font, playerInventoryTitle, 8, imageHeight - 94, 0xFFFFFF, false);
    }
}
