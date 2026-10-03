package com.pycoder.customtamingframework.client.screen;

import com.pycoder.customtamingframework.network.C2STransferPetOwnerPacket;
import com.pycoder.customtamingframework.network.CtfNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PetPermissionTransferConfirmScreen extends Screen {
    private final PetPermissionScreen parentScreen;
    private final String targetPlayerUuid;
    private final String targetPlayerName;
    private final int entityId;

    public PetPermissionTransferConfirmScreen(PetPermissionScreen parentScreen, String targetPlayerUuid,
                                              String targetPlayerName, int entityId) {
        super(Component.literal("转赠确认"));
        this.parentScreen = parentScreen;
        this.targetPlayerUuid = targetPlayerUuid;
        this.targetPlayerName = targetPlayerName;
        this.entityId = entityId;
    }

    @Override
    protected void init() {
        clearWidgets();
        int panelW = 380;
        int panelH = 132;
        int panelX = (width - panelW) / 2;
        int panelY = (height - panelH) / 2;

        int buttonW = 104;
        int buttonGap = 8;
        int totalW = buttonW * 3 + buttonGap * 2;
        int startX = panelX + (panelW - totalW) / 2;
        int buttonY = panelY + 88;

        addRenderableWidget(Button.builder(Component.literal("确认并全服播报"), b -> confirm(true))
                .bounds(startX, buttonY, buttonW, 20).build());
        addRenderableWidget(Button.builder(Component.literal("确认但不播报"), b -> confirm(false))
                .bounds(startX + buttonW + buttonGap, buttonY, buttonW, 20).build());
        addRenderableWidget(Button.builder(Component.literal("取消"), b -> closeToParent())
                .bounds(startX + (buttonW + buttonGap) * 2, buttonY, buttonW, 20).build());
    }

    private void confirm(boolean broadcast) {
        if (minecraft == null) {
            return;
        }
        minecraft.setScreen(parentScreen);
        CtfNetwork.CHANNEL.sendToServer(new C2STransferPetOwnerPacket(entityId, targetPlayerUuid, broadcast));
    }

    private void closeToParent() {
        if (minecraft != null) {
            minecraft.setScreen(parentScreen);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int panelW = 380;
        int panelH = 132;
        int panelX = (width - panelW) / 2;
        int panelY = (height - panelH) / 2;
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xCC111111);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + 1, 0xFF4A4A4A);
        graphics.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, 0xFF4A4A4A);
        graphics.fill(panelX, panelY, panelX + 1, panelY + panelH, 0xFF4A4A4A);
        graphics.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, 0xFF4A4A4A);
        graphics.drawCenteredString(font, title, width / 2, panelY + 10, 0xFFFFFF);
        graphics.drawCenteredString(font,
                Component.literal("确认把主人身份转赠给（" + (targetPlayerName == null || targetPlayerName.isBlank() ? targetPlayerUuid : targetPlayerName) + "）？"),
                width / 2, panelY + 42, 0xE0E0E0);
        graphics.drawCenteredString(font, Component.literal("请选择转赠后的播报方式"), width / 2, panelY + 58, 0xA8A8A8);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            closeToParent();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
