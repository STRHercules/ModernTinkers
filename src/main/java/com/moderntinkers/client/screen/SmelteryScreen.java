package com.moderntinkers.client.screen;

import com.moderntinkers.content.smeltery.SmelteryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Compact controller screen showing formed state and melting progress. */
public final class SmelteryScreen extends AbstractContainerScreen<SmelteryMenu> {
    public SmelteryScreen(SmelteryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = (width - imageWidth) / 2;
        int top = (height - imageHeight) / 2;
        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFF231A1A);
        graphics.fill(left + 6, top + 6, left + imageWidth - 6, top + 82, 0xFF4A3028);
        graphics.fill(left + 108, top + 34, left + 160, top + 46,
                menu.formed() ? 0xFFB85C31 : 0xFF573434);
        if (menu.progress() > 0) {
            int width = Math.min(48, menu.progress() * 48 / SmelteryControllerProgress.TIME);
            graphics.fill(left + 108, top + 34, left + 108 + width, top + 46, 0xFFE09B43);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFFFFFF, false);
        graphics.drawString(font, playerInventoryTitle, 8, imageHeight - 94, 0xFFFFFF, false);
        graphics.drawString(font, Component.translatable(menu.formed()
                ? "gui.moderntinkers.smeltery.formed"
                : "gui.moderntinkers.smeltery.unformed"), 108, 20, 0xFFFFFF, false);
    }

    private static final class SmelteryControllerProgress {
        private static final int TIME = 10;
    }
}
