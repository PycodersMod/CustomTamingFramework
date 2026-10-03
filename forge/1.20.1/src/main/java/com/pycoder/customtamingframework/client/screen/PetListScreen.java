package com.pycoder.customtamingframework.client.screen;

import com.pycoder.customtamingframework.config.CtfUiSettings;
import com.pycoder.customtamingframework.network.C2SSaveConfigScreenPacket;
import com.pycoder.customtamingframework.network.CtfNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Scrollable pet list with allowCTF checkboxes (OP) or read-only view (non-OP).
 * Data format: {pets: [{id, displayName, nickname, allowCTF}, ...]}
 */
public final class PetListScreen extends Screen {
    private static final int ROW_HEIGHT = 20;
    private static final int SEARCH_HEIGHT = 18;
    private static final int LIST_TOP_OFFSET = 50;

    private final CompoundTag originalData;
    private final String parentGuiSnbt;
    private final boolean isOp;
    private final CompoundTag uiSettings;
    private final List<PetEntry> allPets = new ArrayList<>();
    private final List<PetEntry> filteredPets = new ArrayList<>();
    private EditBox searchBox;
    private int scrollOffset;
    private boolean modified;

    private record PetEntry(String id, String displayName, String nickname, boolean originalAllow) {
    }

    // Mutable toggle state: index in allPets → boolean
    private final List<Boolean> toggleState = new ArrayList<>();

    public PetListScreen(CompoundTag data, String parentGuiSnbt, boolean isOp) {
        this(data, parentGuiSnbt, isOp, CtfUiSettings.defaults());
    }

    public PetListScreen(CompoundTag data, String parentGuiSnbt, boolean isOp, CompoundTag uiSettings) {
        super(Component.literal("宠物列表配置"));
        this.originalData = data;
        this.parentGuiSnbt = parentGuiSnbt;
        this.isOp = isOp;
        this.uiSettings = CtfUiSettings.sanitize(uiSettings);
        loadPets();
    }

    private void loadPets() {
        allPets.clear();
        toggleState.clear();
        ListTag pets = originalData.getList("pets", 10);
        for (int i = 0; i < pets.size(); i++) {
            CompoundTag entry = pets.getCompound(i);
            String id = entry.getString("id");
            String displayName = entry.getString("displayName");
            String nickname = entry.getString("nickname");
            boolean allowCTF = entry.getBoolean("allowCTF");
            allPets.add(new PetEntry(id, displayName, nickname, allowCTF));
            toggleState.add(allowCTF);
        }
        applyFilter();
    }

    private void applyFilter() {
        filteredPets.clear();
        String query = searchBox != null ? searchBox.getValue().toLowerCase() : "";
        for (int i = 0; i < allPets.size(); i++) {
            PetEntry pet = allPets.get(i);
            if (!isOp && !toggleState.get(i)) continue; // Non-OP: hide disabled
            if (!query.isEmpty()) {
                String lowerId = pet.id().toLowerCase();
                String lowerDisplay = pet.displayName().toLowerCase();
                if (!lowerId.contains(query) && !lowerDisplay.contains(query)) continue;
            }
            filteredPets.add(pet);
        }
    }

    @Override
    protected void init() {
        clearWidgets();

        // Search box at top
        searchBox = new EditBox(font, 15, 20, width - 30, SEARCH_HEIGHT, Component.literal("搜索宠物"));
        searchBox.setResponder(val -> {
            applyFilter();
            scrollOffset = 0;
        });
        addRenderableWidget(searchBox);

        // Bottom buttons
        int btnW = 90;
        int btnSpacing = 10;
        int btnY = height - 30;
        int totalW = (isOp ? 2 : 1) * btnW + (isOp ? 1 : 0) * btnSpacing;
        int btnStartX = (width - totalW) / 2;

        if (isOp) {
            addRenderableWidget(Button.builder(
                    Component.literal("保存"),
                    button -> save()
            ).bounds(btnStartX, btnY, btnW, 20).build());
            addRenderableWidget(Button.builder(
                    Component.literal("取消"),
                    button -> onClose()
            ).bounds(btnStartX + btnW + btnSpacing, btnY, btnW, 20).build());
        } else {
            addRenderableWidget(Button.builder(
                    Component.literal("关闭"),
                    button -> onClose()
            ).bounds(btnStartX, btnY, btnW, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 6, 0xFFFFFF);

        // Search box label
        graphics.drawString(font, Component.literal("§7搜索:"), 15, 8, 0xE0E0E0, false);

        // List area
        int listLeft = 15;
        int listRight = width - 15;
        int listTop = LIST_TOP_OFFSET;
        int listBottom = height - 45;
        int listWidth = listRight - listLeft;

        // Clip area background
        graphics.fill(listLeft - 2, listTop - 2, listRight + 2, listBottom + 2, 0x44000000);

        // Determine visible range
        int visibleCount = Math.max(0, (listBottom - listTop) / ROW_HEIGHT);
        int totalHeight = filteredPets.size() * ROW_HEIGHT;
        int maxScroll = Math.max(0, totalHeight - (listBottom - listTop));

        // Draw rows
        for (int i = 0; i < Math.min(visibleCount + 1, filteredPets.size()); i++) {
            int idx = i + (maxScroll > 0 ? (int) ((float) scrollOffset / ROW_HEIGHT) : 0);
            if (idx < 0 || idx >= filteredPets.size()) continue;

            PetEntry pet = filteredPets.get(idx);
            int realIdx = allPets.indexOf(pet);
            boolean checked = realIdx >= 0 && toggleState.get(realIdx);

            int rowY = listTop + i * ROW_HEIGHT;
            if (rowY + ROW_HEIGHT < listTop || rowY > listBottom) continue;

            // Row background
            boolean hovered = mouseX >= listLeft && mouseX <= listRight && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT;
            graphics.fill(listLeft, rowY, listRight, rowY + ROW_HEIGHT, hovered ? 0x33FFFFFF : 0x22000000);

            // Pet info text
            String label = pet.displayName() + " §8(" + pet.id() + ")";
            if (!pet.nickname().isBlank()) {
                label = "§b" + pet.nickname() + "§r " + label;
            }
            graphics.drawString(font, Component.literal(label), listLeft + 4, rowY + 4, 0xE0E0E0, false);

            // Checkbox (right side)
            int cbX = listRight - 22;
            int cbY = rowY + 2;
            if (isOp) {
                if (checked) {
                    graphics.fill(cbX, cbY, cbX + 16, cbY + 16, 0xFF4CAF50);
                    graphics.drawString(font, Component.literal("✓"), cbX + 3, cbY + 3, 0xFFFFFF, false);
                } else {
                    graphics.fill(cbX, cbY, cbX + 16, cbY + 16, 0xFF888888);
                }
            } else {
                // Non-OP: show indicator only
                if (checked) {
                    graphics.drawString(font, Component.literal("✓").withStyle(ChatFormatting.GREEN), cbX + 3, cbY + 2, 0xFFFFFF, false);
                }
            }
        }

        // Scroll indicator
        if (maxScroll > 0) {
            float barRatio = (float) (listBottom - listTop) / totalHeight;
            int barHeight = Math.max(10, (int) ((listBottom - listTop) * barRatio));
            int barY = listTop + (int) ((float) scrollOffset / maxScroll * ((listBottom - listTop) - barHeight));
            graphics.fill(listRight + 4, barY, listRight + 8, barY + barHeight, 0xFFAAAAAA);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isOp) {
            int listLeft = 15;
            int listRight = width - 15;
            int listTop = LIST_TOP_OFFSET;
            int listBottom = height - 45;
            int totalHeight = filteredPets.size() * ROW_HEIGHT;
            int maxScroll = Math.max(0, totalHeight - (listBottom - listTop));
            int visibleCount = Math.max(0, (listBottom - listTop) / ROW_HEIGHT);

            for (int i = 0; i < Math.min(visibleCount + 1, filteredPets.size()); i++) {
                int idx = i + (maxScroll > 0 ? (int) ((float) scrollOffset / ROW_HEIGHT) : 0);
                if (idx < 0 || idx >= filteredPets.size()) continue;

                PetEntry pet = filteredPets.get(idx);
                int rowY = listTop + i * ROW_HEIGHT;

                // Checkbox click area
                int cbX = listRight - 22;
                int cbY = rowY + 2;
                if (mouseX >= cbX && mouseX <= cbX + 16 && mouseY >= cbY && mouseY <= cbY + 16) {
                    int realIdx = allPets.indexOf(pet);
                    if (realIdx >= 0) {
                        toggleState.set(realIdx, !toggleState.get(realIdx));
                        modified = true;
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0) {
            int listTop = LIST_TOP_OFFSET;
            int listBottom = height - 45;
            int totalHeight = filteredPets.size() * ROW_HEIGHT;
            int viewHeight = listBottom - listTop;
            int maxScroll = Math.max(0, totalHeight - viewHeight);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (delta * ROW_HEIGHT)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (parentGuiSnbt != null && !parentGuiSnbt.isEmpty()) {
            minecraft.setScreen(new GeneralConfigScreen(parentGuiSnbt, isOp, uiSettings));
        } else {
            super.onClose();
        }
    }

    private void save() {
        if (!isOp || !modified) {
            onClose();
            return;
        }

        CompoundTag output = new CompoundTag();
        ListTag petsOut = new ListTag();
        for (int i = 0; i < allPets.size(); i++) {
            PetEntry pet = allPets.get(i);
            CompoundTag entry = new CompoundTag();
            entry.putString("id", pet.id());
            entry.putBoolean("allowCTF", toggleState.get(i));
            petsOut.add(entry);
        }
        output.put("pets", petsOut);
        CtfNetwork.CHANNEL.sendToServer(new C2SSaveConfigScreenPacket(0, output));
        onClose();
    }
}
