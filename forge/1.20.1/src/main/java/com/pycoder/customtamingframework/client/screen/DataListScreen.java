package com.pycoder.customtamingframework.client.screen;

import com.pycoder.customtamingframework.config.CtfUiSettings;
import com.pycoder.customtamingframework.network.C2SSaveDataVariablesPacket;
import com.pycoder.customtamingframework.network.CtfNetwork;
import com.pycoder.customtamingframework.pet.DataVariables;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * Data variable management GUI.
 * Supports editing the current value, total mode, and stage thresholds for each variable.
 */
public class DataListScreen extends Screen {
    private static final int VALID_TEXT_COLOR = 0xE0E0E0;
    private static final int INVALID_TEXT_COLOR = 0xFFFF3333;
    private static final int ROW_HEIGHT = 24;
    private static final int LIST_TOP_OFFSET = 96;
    private static final int BUTTON_HEIGHT = 18;

    private final int entityId;
    private final Map<String, DataVariables.VariableState> data;
    private final Consumer<Map<String, DataVariables.VariableState>> saveCallback;
    private final Screen returnScreen;
    private final CompoundTag uiSettings;
    private boolean editable;

    private int scrollOffset;
    private String searchFilter = "";
    private String saveMessage;
    private int saveMessageTicks;

    private EditBox searchEdit;
    private EditBox addKeyEdit;
    private EditBox addValueEdit;
    private int editingIndex = -1;
    private EditBox editingValueBox;
    private String editingKey;

    public DataListScreen(int entityId, Map<String, DataVariables.VariableState> data) {
        this(entityId, data, null, null, CtfUiSettings.defaults());
    }

    public DataListScreen(int entityId, Map<String, DataVariables.VariableState> data, Screen returnScreen) {
        this(entityId, data, null, returnScreen, CtfUiSettings.defaults());
    }

    /** Local-mode constructor: saves via callback instead of network packet. */
    public DataListScreen(Map<String, DataVariables.VariableState> data,
                          Consumer<Map<String, DataVariables.VariableState>> saveCallback) {
        this(-1, data, saveCallback, null, CtfUiSettings.defaults());
    }

    public DataListScreen(Map<String, DataVariables.VariableState> data,
                          Consumer<Map<String, DataVariables.VariableState>> saveCallback,
                          Screen returnScreen) {
        this(-1, data, saveCallback, returnScreen, CtfUiSettings.defaults());
    }

    public DataListScreen(Map<String, DataVariables.VariableState> data,
                          Consumer<Map<String, DataVariables.VariableState>> saveCallback,
                          Screen returnScreen, CompoundTag uiSettings) {
        this(-1, data, saveCallback, returnScreen, uiSettings);
    }

    private DataListScreen(int entityId, Map<String, DataVariables.VariableState> data,
                           Consumer<Map<String, DataVariables.VariableState>> saveCallback, Screen returnScreen,
                           CompoundTag uiSettings) {
        super(Component.literal("数据变量管理"));
        this.entityId = entityId;
        this.data = new TreeMap<>(data);
        this.saveCallback = saveCallback;
        this.returnScreen = returnScreen;
        this.uiSettings = CtfUiSettings.sanitize(uiSettings);
    }

    @Override
    protected void init() {
        clearWidgets();
        editable = minecraft != null && minecraft.player != null && minecraft.player.hasPermissions(2);

        int cx = width / 2;
        int panelL = Math.max(60, cx - 180);
        int panelR = Math.min(width - 10, cx + 180);
        int panelW = panelR - panelL;

        searchEdit = new EditBox(font, panelL, 28, panelW - 50, 16, Component.literal("搜索"));
        searchEdit.setValue(searchFilter);
        searchEdit.setResponder(val -> searchFilter = val.toLowerCase());
        addRenderableWidget(searchEdit);
        addRenderableWidget(Button.builder(Component.literal("搜索"), b -> {
            searchFilter = searchEdit.getValue().toLowerCase();
            scrollOffset = 0;
            init();
        }).bounds(panelR - 46, 27, 46, 18).build());

        if (editable) {
            int addY = 52;
            int addKeyW = Math.max(90, (panelW - 110) / 2);
            int valueLabelW = font.width("初始值：") + 4;
            int addValueX = panelL + addKeyW + 5 + valueLabelW;
            int addValueW = panelR - 52 - addValueX - 6;

            addKeyEdit = new EditBox(font, panelL, addY, addKeyW, 16, Component.literal("键"));
            addKeyEdit.setMaxLength(64);
            addRenderableWidget(addKeyEdit);

            addValueEdit = new EditBox(font, addValueX, addY, Math.max(70, addValueW), 16, Component.literal("初始值"));
            addValueEdit.setMaxLength(64);
            addValueEdit.setResponder(val -> addValueEdit.setTextColor(DataVariables.isNumericText(val) ? VALID_TEXT_COLOR : INVALID_TEXT_COLOR));
            addValueEdit.setTextColor(VALID_TEXT_COLOR);
            addRenderableWidget(addValueEdit);

            addRenderableWidget(Button.builder(Component.literal("添加"), b -> addVariable())
                    .bounds(panelR - 46, addY, 46, 18).build());
        } else {
            addKeyEdit = null;
            addValueEdit = null;
        }

        // List area
        List<Map.Entry<String, DataVariables.VariableState>> entries = getFilteredEntries();
        int listY = LIST_TOP_OFFSET;
        int listBottom = height - 45;
        int maxVisible = Math.max(0, (listBottom - listY) / ROW_HEIGHT);
        if (scrollOffset > Math.max(0, entries.size() - maxVisible)) {
            scrollOffset = Math.max(0, entries.size() - maxVisible);
        }

        if (editingIndex >= 0 && editingIndex < entries.size()) {
            int visibleIndex = editingIndex - scrollOffset;
            if (visibleIndex >= 0 && visibleIndex < maxVisible) {
                Map.Entry<String, DataVariables.VariableState> entry = entries.get(editingIndex);
                editingKey = entry.getKey();
                int rowY = listY + visibleIndex * ROW_HEIGHT;
                int editX = panelL + 122;
                int editW = Math.max(90, panelR - editX - 124);
                editingValueBox = new EditBox(font, editX, rowY + 3, editW, 16, Component.literal("初始值"));
                editingValueBox.setMaxLength(64);
                editingValueBox.setValue(entry.getValue().current());
                editingValueBox.setResponder(val -> editingValueBox.setTextColor(DataVariables.isNumericText(val) ? VALID_TEXT_COLOR : INVALID_TEXT_COLOR));
                editingValueBox.setTextColor(DataVariables.isNumericText(entry.getValue().current()) ? VALID_TEXT_COLOR : INVALID_TEXT_COLOR);
                if (editable) {
                    addRenderableWidget(editingValueBox);

                    addRenderableWidget(Button.builder(Component.literal("✓"), b -> {
                        if (confirmCurrentEditValue()) {
                            init();
                        }
                    }).bounds(panelR - 84, rowY + 2, 38, BUTTON_HEIGHT).build());

                    addRenderableWidget(Button.builder(Component.literal("✗"), b -> {
                        if (editingKey != null) {
                            data.remove(editingKey);
                        }
                        editingIndex = -1;
                        editingKey = null;
                        editingValueBox = null;
                        init();
                    }).bounds(panelR - 44, rowY + 2, 38, BUTTON_HEIGHT).build());
                }
            } else {
                editingIndex = -1;
                editingKey = null;
                editingValueBox = null;
            }
        }

        // Bottom buttons
        int btnY = height - 30;
        int btnW = 70;
        int btnSpacing = 12;
        if (editable) {
            int totalW = 2 * btnW + btnSpacing;
            int btnStartX = (width - totalW) / 2;
            addRenderableWidget(Button.builder(Component.literal("保存"), b -> saveAndReturn())
                    .bounds(btnStartX, btnY, btnW, 20).build());
            addRenderableWidget(Button.builder(Component.literal("取消"), b -> onClose())
                    .bounds(btnStartX + btnW + btnSpacing, btnY, btnW, 20).build());
        } else {
            int closeX = (width - btnW) / 2;
            addRenderableWidget(Button.builder(Component.literal("关闭"), b -> onClose())
                    .bounds(closeX, btnY, btnW, 20).build());
        }
    }

    private List<Map.Entry<String, DataVariables.VariableState>> getFilteredEntries() {
        List<Map.Entry<String, DataVariables.VariableState>> result = new ArrayList<>();
        if (searchFilter.isEmpty()) {
            result.addAll(data.entrySet());
            return result;
        }
        for (Map.Entry<String, DataVariables.VariableState> e : data.entrySet()) {
            DataVariables.VariableState state = e.getValue();
            String key = e.getKey().toLowerCase();
            String current = state != null ? state.current().toLowerCase() : "";
            String mode = state != null ? state.totalMode().snbtKey() : "";
            if (key.contains(searchFilter) || current.contains(searchFilter) || mode.contains(searchFilter)) {
                result.add(e);
            }
        }
        return result;
    }

    private void addVariable() {
        String key = addKeyEdit.getValue().trim();
        String value = addValueEdit.getValue().trim();
        if (key.isEmpty()) {
            return;
        }
        if (!DataVariables.isNumericText(value)) {
            addValueEdit.setTextColor(INVALID_TEXT_COLOR);
            saveMessage = "初始值必须是数字";
            saveMessageTicks = 0;
            return;
        }

        DataVariables.VariableState existing = data.get(key);
        DataVariables.VariableState next;
        if (existing == null) {
            next = DataVariables.createState(value);
        } else {
            next = DataVariables.setInitialValue(existing, value);
        }
        data.put(key, next);
        addKeyEdit.setValue("");
        addValueEdit.setValue("");
        addValueEdit.setTextColor(VALID_TEXT_COLOR);
        editingIndex = -1;
        editingKey = null;
        editingValueBox = null;
        init();
    }

    private boolean confirmCurrentEditValue() {
        if (editingKey == null || editingValueBox == null) {
            return true;
        }
        String value = editingValueBox.getValue().trim();
        if (!DataVariables.isNumericText(value)) {
            editingValueBox.setTextColor(INVALID_TEXT_COLOR);
            saveMessage = "初始值必须是数字";
            saveMessageTicks = 0;
            return false;
        }
        DataVariables.VariableState existing = data.get(editingKey);
        if (existing == null) {
            existing = DataVariables.createState(value);
        } else {
            existing = DataVariables.setInitialValue(existing, value);
        }
        data.put(editingKey, existing);
        editingIndex = -1;
        editingKey = null;
        editingValueBox = null;
        return true;
    }

    private void openTotalSettings(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        editingIndex = -1;
        editingKey = null;
        editingValueBox = null;
        if (minecraft != null) {
            minecraft.setScreen(new DataTotalSettingsScreen(this, data, key, editable));
        }
    }

    private Map<String, DataVariables.VariableState> buildSaveSnapshot() {
        Map<String, DataVariables.VariableState> snapshot = new TreeMap<>(data);

        if (editingIndex >= 0 && editingValueBox != null && editingKey != null) {
            String value = editingValueBox.getValue().trim();
            if (!DataVariables.isNumericText(value)) {
                editingValueBox.setTextColor(INVALID_TEXT_COLOR);
                return null;
            }
            DataVariables.VariableState existing = snapshot.get(editingKey);
            if (existing == null) {
                existing = DataVariables.createState(value);
            } else {
                existing = DataVariables.setInitialValue(existing, value);
            }
            snapshot.put(editingKey, existing);
        }

        return snapshot;
    }

    private void saveAndReturn() {
        Map<String, DataVariables.VariableState> snapshot = buildSaveSnapshot();
        if (snapshot == null) {
            saveMessage = "存在无效数值，无法保存";
            saveMessageTicks = 0;
            return;
        }

        data.clear();
        data.putAll(snapshot);

        if (saveCallback != null) {
            saveCallback.accept(new TreeMap<>(snapshot));
        } else {
            CompoundTag tag = DataVariables.toCompound(snapshot);
            CtfNetwork.CHANNEL.sendToServer(new C2SSaveDataVariablesPacket(entityId, tag));
        }
        onClose();
    }

    @Override
    public void onClose() {
        if (minecraft != null && returnScreen != null) {
            minecraft.setScreen(returnScreen);
            return;
        }
        super.onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && editingIndex < 0) {
            List<Map.Entry<String, DataVariables.VariableState>> entries = getFilteredEntries();
            for (int i = scrollOffset; i < Math.min(entries.size(), scrollOffset + getVisibleRowCount()); i++) {
                int rowY = LIST_TOP_OFFSET + (i - scrollOffset) * ROW_HEIGHT;
                int totalBtnX = width - 166;
                int editBtnX = width - 86;
                if (showTotalSettingsButton() && mouseX >= totalBtnX && mouseX <= totalBtnX + 72
                        && mouseY >= rowY + 2 && mouseY <= rowY + 20) {
                    openTotalSettings(entries.get(i).getKey());
                    return true;
                }
                if (editable && mouseX >= editBtnX && mouseX <= editBtnX + 44 && mouseY >= rowY + 2 && mouseY <= rowY + 20) {
                    editingIndex = i;
                    editingKey = entries.get(i).getKey();
                    init();
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editingIndex >= 0 && (keyCode == 257 || keyCode == 335)) {
            if (confirmCurrentEditValue()) {
                init();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0) {
            int maxScroll = Math.max(0, getFilteredEntries().size() - getVisibleRowCount());
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.signum(delta)));
            init();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private int getVisibleRowCount() {
        return Math.max(0, (height - 45 - LIST_TOP_OFFSET) / ROW_HEIGHT);
    }

    private boolean showTotalSettingsButton() {
        return editable || CtfUiSettings.visibleForNonOp(uiSettings, CtfUiSettings.SHOW_NON_OP_TOTAL_SETTINGS_BUTTON);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);

        graphics.fill(15, LIST_TOP_OFFSET - 2, width - 15, height - 45 + 2, 0x44000000);
        List<Map.Entry<String, DataVariables.VariableState>> entries = getFilteredEntries();
        for (int i = scrollOffset; i < Math.min(entries.size(), scrollOffset + getVisibleRowCount()); i++) {
            Map.Entry<String, DataVariables.VariableState> entry = entries.get(i);
            DataVariables.VariableState state = entry.getValue();
            int rowY = LIST_TOP_OFFSET + (i - scrollOffset) * ROW_HEIGHT;
            boolean hovered = mouseX >= 15 && mouseX <= width - 15 && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT;
            graphics.fill(15, rowY, width - 15, rowY + ROW_HEIGHT, hovered ? 0x33FFFFFF : 0x22000000);

            if (editingIndex == i && editingValueBox != null) {
                graphics.drawString(font, Component.literal("§7" + entry.getKey()), 18, rowY + 5, 0xE0E0E0, false);
                graphics.drawString(font, Component.literal("初始值："), 118, rowY + 5, 0xE0E0E0, false);
            } else {
                String modeLabel = state.totalMode() == DataVariables.TotalMode.CUMULATIVE ? "累计" : "阶段";
                String summary = "初始值: " + state.current() + "  [" + modeLabel + "]";
                int textMaxW = Math.max(30, width - 15 - 176 - 18);
                String keyPart = entry.getKey();
                String summaryPart = font.plainSubstrByWidth(summary, textMaxW);
                graphics.drawString(font, Component.literal("§e" + keyPart + " §7" + summaryPart), 18, rowY + 5, 0xE0E0E0, false);

                if (showTotalSettingsButton()) {
                    drawRowButton(graphics, width - 166, rowY + 2, 72, BUTTON_HEIGHT, "总值设置",
                            mouseX, mouseY, hovered);
                }
                if (editable) {
                    drawRowButton(graphics, width - 86, rowY + 2, 44, BUTTON_HEIGHT, "编辑",
                            mouseX, mouseY, hovered);
                }
            }
        }

        if (entries.isEmpty()) {
            graphics.drawCenteredString(font,
                    Component.literal("暂无数据。在上方输入键和值后点击“添加”。"),
                    width / 2, LIST_TOP_OFFSET + 20, 0x808080);
        }

        if (saveMessage != null) {
            saveMessageTicks++;
            int alpha = Math.max(0, 255 - saveMessageTicks * 3);
            if (alpha > 0) {
                graphics.drawCenteredString(font, Component.literal("§e" + saveMessage),
                        width / 2, height - 70, 0xFFFFFF | (alpha << 24));
            } else {
                saveMessage = null;
                saveMessageTicks = 0;
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawRowButton(GuiGraphics graphics, int x, int y, int w, int h, String label,
                               int mouseX, int mouseY, boolean hoveredRow) {
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        int bgColor = hovered ? 0xFF555566 : 0xFF333344;
        graphics.fill(x, y, x + w, y + h, bgColor);
        graphics.renderOutline(x, y, w, h, hovered ? 0xFFFFFFFF : 0xFF888888);
        graphics.drawCenteredString(font, Component.literal(label), x + w / 2, y + 4, hovered ? 0xFFFFFF : 0xE0E0E0);
    }

}
