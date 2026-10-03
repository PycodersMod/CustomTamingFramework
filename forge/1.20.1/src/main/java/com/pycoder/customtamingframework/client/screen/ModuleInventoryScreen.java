package com.pycoder.customtamingframework.client.screen;

import com.pycoder.customtamingframework.menu.ModuleInventoryMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ModuleInventoryScreen extends AbstractContainerScreen<ModuleInventoryMenu> {
    private static PetScreen pendingParent;

    private final PetScreen parentScreen;

    public static void prepareOpen(PetScreen parentScreen) {
        pendingParent = parentScreen;
    }

    public ModuleInventoryScreen(ModuleInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.parentScreen = pendingParent;
        pendingParent = null;
    }

    @Override
    protected void init() {
        super.init();
        this.imageWidth = Math.max(176, menu.cols() * 18 + 16);
        this.imageHeight = 18 + menu.rows() * 18 + 14 + 96 + 18;
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF20242C);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 1, 0xFF666C78);
        graphics.fill(leftPos, topPos + imageHeight - 1, leftPos + imageWidth, topPos + imageHeight, 0xFF666C78);
        graphics.fill(leftPos, topPos, leftPos + 1, topPos + imageHeight, 0xFF666C78);
        graphics.fill(leftPos + imageWidth - 1, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF666C78);

        int gridX = leftPos + 8;
        int gridY = topPos + 18;
        int gridW = menu.cols() * 18 + 8;
        int gridH = menu.rows() * 18 + 8;
        graphics.fill(gridX - 4, gridY - 4, gridX - 4 + gridW, gridY - 4 + gridH, 0xFF2B2F39);
        graphics.renderOutline(gridX - 4, gridY - 4, gridW, gridH, 0xFF5B6270);
        if (!menu.isEditable()) {
            graphics.fill(gridX - 4, gridY - 4, gridX - 4 + gridW, gridY - 4 + gridH, 0x55000000);
            graphics.drawCenteredString(font, Component.literal("只读"), gridX - 4 + gridW / 2, gridY + gridH / 2 - 4, 0xB0B0B0);
        }

        String name = menu.moduleConfig().getString("name");
        if (name == null || name.isBlank()) {
            name = "物品栏模块";
        }
        graphics.drawString(font, Component.literal(name), leftPos + 8, topPos + 6, 0xE0E0E0, false);

        int inventoryY = topPos + 18 + menu.rows() * 18 + 14;
        graphics.fill(leftPos + 8, inventoryY - 4, leftPos + 176, inventoryY + 96 + 4, 0xFF232833);
        graphics.renderOutline(leftPos + 8, inventoryY - 4, 168, 100, 0xFF515A68);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public void onClose() {
        if (parentScreen != null && menu != null) {
            List<ItemStack> stacks = menu.getModuleStacks();
            parentScreen.syncModuleInventoryPreview(menu.moduleId(), stacks);
            if (minecraft != null) {
                minecraft.setScreen(parentScreen);
                return;
            }
        }
        super.onClose();
    }
}
