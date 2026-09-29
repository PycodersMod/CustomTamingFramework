package com.example.customtamingframework.client.screen;

import com.example.customtamingframework.config.CtfUiSettings;
import com.example.customtamingframework.network.C2SSaveUiSettingsPacket;
import com.example.customtamingframework.network.CtfNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class UserSettingsScreen extends Screen {
    private static final int ROW_HEIGHT = 22;

    private final Screen returnScreen;
    private final String generalGuiSnbt;
    private final boolean isOp;
    private final CompoundTag initialSettings;
    private final CompoundTag settings;
    private final List<Row> rows = new ArrayList<>();
    private EditBox searchBox;
    private String searchFilter = "";
    private int scrollOffset;

    private record Row(String key, String label, String parentKey, int indent, String description) {
    }

    public UserSettingsScreen(Screen returnScreen, String generalGuiSnbt, boolean isOp, CompoundTag settings) {
        super(Component.literal("用户设置"));
        this.returnScreen = returnScreen;
        this.generalGuiSnbt = generalGuiSnbt;
        this.isOp = isOp;
        this.initialSettings = CtfUiSettings.sanitize(settings);
        this.settings = this.initialSettings.copy();
        rows.add(new Row(CtfUiSettings.SHOW_NON_OP_GENERAL_CONFIG_SCREEN, "通用配置界面", null, 0, "非 OP 玩家是否可读通用配置界面"));
        rows.add(new Row(CtfUiSettings.SHOW_NON_OP_PET_LIST_BUTTON, "宠物列表", CtfUiSettings.SHOW_NON_OP_GENERAL_CONFIG_SCREEN, 1, "非 OP 玩家是否可读宠物列表"));
        rows.add(new Row(CtfUiSettings.SHOW_NON_OP_FOOD_LIST_BUTTON, "食物列表", CtfUiSettings.SHOW_NON_OP_GENERAL_CONFIG_SCREEN, 1, "非 OP 玩家是否可读食物列表"));
        rows.add(new Row(CtfUiSettings.SHOW_NON_OP_MORE_FOOD_BUTTON, "更多“食物”", CtfUiSettings.SHOW_NON_OP_FOOD_LIST_BUTTON, 2, "非 OP 玩家是否可读更多“食物”列表"));
        rows.add(new Row(CtfUiSettings.SHOW_NON_OP_TAMING_CORE_BUTTON, "驯兽核心", CtfUiSettings.SHOW_NON_OP_GENERAL_CONFIG_SCREEN, 1, "非 OP 玩家是否可读驯兽核心配方"));
        rows.add(new Row(CtfUiSettings.SHOW_NON_OP_DATA_LIST_BUTTON, "数据列表", null, 0, "非 OP 玩家是否可读数据列表"));
        rows.add(new Row(CtfUiSettings.SHOW_NON_OP_TOTAL_SETTINGS_BUTTON, "总值设置", CtfUiSettings.SHOW_NON_OP_DATA_LIST_BUTTON, 1, "非 OP 玩家是否可读总值设置"));
    }

    @Override
    protected void init() {
        clearWidgets();
        searchBox = new EditBox(font, 18, 24, width - 36, 18, Component.literal("搜索设置"));
        searchBox.setValue(searchFilter);
        searchBox.setResponder(val -> searchFilter = val.toLowerCase());
        addRenderableWidget(searchBox);

        int btnW = 76;
        int btnSpacing = 10;
        int btnY = height - 30;
        int totalW = 2 * btnW + btnSpacing;
        int startX = (width - totalW) / 2;
        addRenderableWidget(Button.builder(Component.literal("保存"), b -> save()).bounds(startX, btnY, btnW, 20).build());
        addRenderableWidget(Button.builder(Component.literal("取消"), b -> onClose()).bounds(startX + btnW + btnSpacing, btnY, btnW, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        graphics.fill(16, 48, width - 16, height - 44, 0x44000000);
        int rowY = 54;
        int listBottom = height - 52;
        int visible = Math.max(0, (listBottom - rowY) / ROW_HEIGHT);
        List<Row> filtered = filteredRows();
        if (scrollOffset > Math.max(0, filtered.size() - visible)) {
            scrollOffset = Math.max(0, filtered.size() - visible);
        }
        for (int i = scrollOffset; i < Math.min(filtered.size(), scrollOffset + visible); i++) {
            Row row = filtered.get(i);
            int drawY = rowY + (i - scrollOffset) * ROW_HEIGHT;
            boolean hovered = mouseX >= 18 && mouseX <= width - 18 && mouseY >= drawY && mouseY <= drawY + ROW_HEIGHT;
            graphics.fill(18, drawY, width - 18, drawY + ROW_HEIGHT, hovered ? 0x33FFFFFF : 0x22000000);
            graphics.drawString(font, Component.literal(row.label()), 28 + row.indent() * 14, drawY + 6, 0xE0E0E0, false);
            graphics.drawString(font, Component.literal(row.description()), 154, drawY + 6, 0x909090, false);
            addToggle(graphics, row, drawY);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void addToggle(GuiGraphics graphics, Row row, int y) {
        boolean checked = settings.getBoolean(row.key());
        boolean locked = isLocked(row);
        int x = width - 88;
        int w = 70;
        int bg = locked ? 0xFF555555 : (checked ? 0xFF4CAF50 : 0xFF4A4A5A);
        graphics.fill(x, y + 3, x + w, y + 19, bg);
        graphics.renderOutline(x, y + 3, w, 16, locked ? 0xFF777777 : 0xFFFFFFFF);
        graphics.drawCenteredString(font, Component.literal((checked ? "☑ " : "☐ ") + "可读"), x + w / 2, y + 6, locked ? 0xFFAAAAAA : 0xFFFFFFFF);
    }

    private boolean isLocked(Row row) {
        String parent = row.parentKey();
        while (parent != null) {
            if (!settings.getBoolean(parent)) {
                return true;
            }
            Row parentRow = findRow(parent);
            if (parentRow == null) {
                break;
            }
            parent = parentRow.parentKey();
        }
        return false;
    }

    private Row findRow(String key) {
        for (Row row : rows) {
            if (row.key().equals(key)) {
                return row;
            }
        }
        return null;
    }

    private List<Row> filteredRows() {
        if (searchFilter.isEmpty()) {
            return rows;
        }
        List<Row> filtered = new ArrayList<>();
        for (Row row : rows) {
            if (row.label().toLowerCase().contains(searchFilter) || row.description().toLowerCase().contains(searchFilter)) {
                filtered.add(row);
            }
        }
        return filtered;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int rowY = 54;
            int listBottom = height - 52;
            int visible = Math.max(0, (listBottom - rowY) / ROW_HEIGHT);
            List<Row> filtered = filteredRows();
            for (int i = scrollOffset; i < Math.min(filtered.size(), scrollOffset + visible); i++) {
                Row row = filtered.get(i);
                int drawY = rowY + (i - scrollOffset) * ROW_HEIGHT;
                int x = width - 88;
                if (mouseX >= x && mouseX <= x + 70 && mouseY >= drawY + 3 && mouseY <= drawY + 19) {
                    boolean locked = isLocked(row);
                    if (!locked) {
                        settings.putBoolean(row.key(), !settings.getBoolean(row.key()));
                        if (!settings.getBoolean(row.key())) {
                            if (CtfUiSettings.SHOW_NON_OP_FOOD_LIST_BUTTON.equals(row.key())) {
                                settings.putBoolean(CtfUiSettings.SHOW_NON_OP_MORE_FOOD_BUTTON, false);
                            }
                            if (CtfUiSettings.SHOW_NON_OP_DATA_LIST_BUTTON.equals(row.key())) {
                                settings.putBoolean(CtfUiSettings.SHOW_NON_OP_TOTAL_SETTINGS_BUTTON, false);
                            }
                        }
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
            int rowY = 54;
            int listBottom = height - 52;
            int visible = Math.max(0, (listBottom - rowY) / ROW_HEIGHT);
            int maxScroll = Math.max(0, filteredRows().size() - visible);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.signum(delta)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (minecraft != null && returnScreen != null) {
            minecraft.setScreen(returnScreen);
            return;
        }
        super.onClose();
    }

    private void save() {
        CtfNetwork.CHANNEL.sendToServer(new C2SSaveUiSettingsPacket(settings.copy()));
        if (minecraft != null) {
            minecraft.setScreen(new GeneralConfigScreen(generalGuiSnbt, isOp, settings.copy()));
        }
    }
}
