package com.pycoder.customtamingframework.client.screen;

import com.pycoder.customtamingframework.pet.DataVariables;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Full-screen editor for one data variable's total-value rules.
 */
public class DataTotalSettingsScreen extends Screen {
    private static final int VALID_TEXT_COLOR = 0xE0E0E0;
    private static final int INVALID_TEXT_COLOR = 0xFFFF3333;
    private static final int DISABLED_TEXT_COLOR = 0xFF888888;
    private static final int ROW_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 18;

    private final Screen returnScreen;
    private final Map<String, DataVariables.VariableState> data;
    private final String dataKey;
    private final boolean editable;

    private int contentX;
    private int contentY;
    private int contentW;
    private int contentH;
    private int leftW;
    private int rightX;
    private int rightW;
    private int row1Y;
    private int lowerY;
    private int stageHeaderY;
    private int listTop;
    private int listBottom;
    private int infiniteY;
    private int scrollOffset;

    private String message;
    private int messageTicks;

    private Button cumulativeModeButton;
    private Button stageModeButton;
    private Button reversibleButton;
    private Button countModeCumulativeButton;
    private Button countModeStageButton;
    private Button addStageButton;
    private Button minimumNoLowerBoundButton;
    private Button minimumConfirmButton;
    private Button minimumResetButton;
    private EditBox minimumValueBox;
    private EditBox infiniteNameBox;
    private Button infiniteEnabledButton;
    private final List<StageRowWidget> stageRows = new ArrayList<>();

    private record StageRowWidget(int index, EditBox valueBox, EditBox nameBox, Button confirmButton,
                                  Button deleteButton) {
    }

    public DataTotalSettingsScreen(Screen returnScreen, Map<String, DataVariables.VariableState> data, String dataKey, boolean editable) {
        super(Component.literal("总值设置"));
        this.returnScreen = returnScreen;
        this.data = data;
        this.dataKey = dataKey;
        this.editable = editable;
    }

    @Override
    protected void init() {
        clearWidgets();
        stageRows.clear();
        calculateLayout();

        DataVariables.VariableState state = data.get(dataKey);
        if (state == null) {
            closeToDataList();
            return;
        }

        int leftButtonX = contentX + 12;
        int leftButtonW = Math.max(70, leftW - 24);
        cumulativeModeButton = Button.builder(choiceLabel("累计", state.totalMode() == DataVariables.TotalMode.CUMULATIVE),
                b -> switchTotalMode(DataVariables.TotalMode.CUMULATIVE))
                .bounds(leftButtonX, contentY + 52, leftButtonW, 20)
                .build();
        stageModeButton = Button.builder(choiceLabel("阶段", state.totalMode() == DataVariables.TotalMode.STAGE),
                b -> switchTotalMode(DataVariables.TotalMode.STAGE))
                .bounds(leftButtonX, contentY + 80, leftButtonW, 20)
                .build();
        cumulativeModeButton.active = editable;
        stageModeButton.active = editable;
        addRenderableWidget(cumulativeModeButton);
        addRenderableWidget(stageModeButton);

        addRenderableWidget(Button.builder(Component.literal("关闭"), b -> closeToDataList())
                .bounds(width - 76, height - 28, 60, 20)
                .build());

        if (state.totalMode() == DataVariables.TotalMode.STAGE) {
            buildStageWidgets(state);
        }
    }

    private void calculateLayout() {
        int maxContentW = Math.min(width - 32, 900);
        contentW = Math.max(280, maxContentW);
        contentX = Math.max(16, (width - contentW) / 2);
        contentY = 28;
        contentH = Math.max(180, height - 66);
        leftW = Math.max(110, Math.min(170, contentW / 5));
        rightX = contentX + leftW + 14;
        rightW = Math.max(150, contentX + contentW - rightX);
        row1Y = contentY + 24;
        lowerY = row1Y + 30;
        stageHeaderY = lowerY + 30;
        listTop = stageHeaderY + 18;
        infiniteY = height - 58;
        listBottom = infiniteY - 8;
    }

    private void buildStageWidgets(DataVariables.VariableState state) {
        int gap = rightW < 310 ? 4 : 8;
        int reversibleW = rightW < 310 ? 64 : 72;
        int countW = Math.max(76, Math.min(100, (rightW - reversibleW - gap * 2) / 2));
        int row1TotalW = reversibleW + countW * 2 + gap * 2;
        if (row1TotalW > rightW) {
            countW = Math.max(62, (rightW - reversibleW - gap * 2) / 2);
        }

        reversibleButton = Button.builder(checkboxLabel("可逆", state.reversible()), b -> {
            if (!editable) {
                return;
            }
            if (!tryCommitVisibleEdits()) {
                return;
            }
            DataVariables.VariableState next = DataVariables.setReversible(data.get(dataKey), !data.get(dataKey).reversible());
            data.put(dataKey, next);
            init();
        }).bounds(rightX, row1Y, reversibleW, BUTTON_HEIGHT).build();
        addRenderableWidget(reversibleButton);

        countModeCumulativeButton = Button.builder(choiceLabel("累计计数", state.countMode() == DataVariables.CountMode.CUMULATIVE), b -> {
            if (!editable) {
                return;
            }
            if (!tryCommitVisibleEdits()) {
                return;
            }
            DataVariables.VariableState next = DataVariables.setCountMode(data.get(dataKey), DataVariables.CountMode.CUMULATIVE);
            data.put(dataKey, next);
            init();
        }).bounds(rightX + reversibleW + gap, row1Y, countW, BUTTON_HEIGHT).build();
        addRenderableWidget(countModeCumulativeButton);

        countModeStageButton = Button.builder(choiceLabel("阶段计数", state.countMode() == DataVariables.CountMode.STAGE), b -> {
            if (!editable) {
                return;
            }
            if (!tryCommitVisibleEdits()) {
                return;
            }
            DataVariables.VariableState next = DataVariables.setCountMode(data.get(dataKey), DataVariables.CountMode.STAGE);
            data.put(dataKey, next);
            init();
        }).bounds(rightX + reversibleW + gap + countW + gap, row1Y, countW, BUTTON_HEIGHT).build();
        addRenderableWidget(countModeStageButton);

        int lowerLabelW = font.width("下限") + 8;
        int noLowerW = rightW < 310 ? 78 : 88;
        int smallButtonW = rightW < 310 ? 28 : 32;
        int minBoxW = Math.max(60, rightW - lowerLabelW - noLowerW - smallButtonW * 2 - gap * 5);
        int minBoxX = rightX + lowerLabelW + gap;
        int noLowerX = minBoxX + minBoxW + gap;
        int confirmX = noLowerX + noLowerW + gap;
        int resetX = confirmX + smallButtonW + gap;

        minimumValueBox = new EditBox(font, minBoxX, lowerY + 1, minBoxW, 16, Component.literal("下限"));
        minimumValueBox.setMaxLength(32);
        minimumValueBox.setValue(state.minimum());
        minimumValueBox.setResponder(val -> minimumValueBox.setTextColor(DataVariables.isNumericText(val)
                ? VALID_TEXT_COLOR : INVALID_TEXT_COLOR));
        minimumValueBox.setEditable(editable && !state.noLowerBound());
        minimumValueBox.setTextColor(!editable || state.noLowerBound() ? DISABLED_TEXT_COLOR
                : (DataVariables.isNumericText(state.minimum()) ? VALID_TEXT_COLOR : INVALID_TEXT_COLOR));
        addRenderableWidget(minimumValueBox);

        minimumNoLowerBoundButton = Button.builder(checkboxLabel("无下限", state.noLowerBound()), b -> {
            if (!editable) {
                return;
            }
            if (!tryCommitVisibleEdits()) {
                return;
            }
            DataVariables.VariableState next = DataVariables.setNoLowerBound(data.get(dataKey), !data.get(dataKey).noLowerBound());
            data.put(dataKey, next);
            init();
        }).bounds(noLowerX, lowerY, noLowerW, BUTTON_HEIGHT).build();
        addRenderableWidget(minimumNoLowerBoundButton);

        minimumConfirmButton = Button.builder(Component.literal("✓"), b -> {
            if (!editable) {
                return;
            }
            if (!tryCommitVisibleEdits()) {
                return;
            }
            init();
        }).bounds(confirmX, lowerY, smallButtonW, BUTTON_HEIGHT).build();
        minimumConfirmButton.active = editable && !state.noLowerBound();
        addRenderableWidget(minimumConfirmButton);

        minimumResetButton = Button.builder(Component.literal("✗"), b -> {
            if (!editable) {
                return;
            }
            DataVariables.VariableState next = DataVariables.resetMinimum(data.get(dataKey));
            data.put(dataKey, next);
            init();
        }).bounds(resetX, lowerY, smallButtonW, BUTTON_HEIGHT).build();
        minimumResetButton.active = editable && !state.noLowerBound();
        addRenderableWidget(minimumResetButton);

        addStageButton = Button.builder(Component.literal("+"), b -> {
            if (!editable) {
                return;
            }
            if (!tryCommitVisibleEdits()) {
                return;
            }
            DataVariables.VariableState next = DataVariables.addFiniteStage(data.get(dataKey));
            data.put(dataKey, next);
            scrollOffset = countFiniteStages(next.stages());
            init();
        }).bounds(rightX + rightW - 24, stageHeaderY, 22, BUTTON_HEIGHT).build();
        addRenderableWidget(addStageButton);

        buildStageRows(state, gap);
        buildInfiniteRow(state, gap);
    }

    private void buildStageRows(DataVariables.VariableState state, int gap) {
        List<DataVariables.StageEntry> finiteStages = finiteStages(state.stages());
        int visibleCount = Math.max(0, (listBottom - listTop) / ROW_HEIGHT);
        if (scrollOffset > Math.max(0, finiteStages.size() - visibleCount)) {
            scrollOffset = Math.max(0, finiteStages.size() - visibleCount);
        }

        int indexW = rightW < 310 ? 28 : 34;
        int buttonW = rightW < 310 ? 26 : 30;
        int valueW = Math.max(58, Math.min(88, rightW / 5));
        int deleteX = rightX + rightW - buttonW;
        int confirmX = deleteX - gap - buttonW;
        int valueX = rightX + indexW;
        int nameX = valueX + valueW + gap;
        int nameW = Math.max(68, confirmX - gap - nameX);

        int visibleIndex = 0;
        for (int i = scrollOffset; i < Math.min(finiteStages.size(), scrollOffset + visibleCount); i++) {
            int rowY = listTop + visibleIndex * ROW_HEIGHT;
            int finalIndex = i;
            DataVariables.StageEntry entry = finiteStages.get(i);

            EditBox valueBox = new EditBox(font, valueX, rowY + 2, valueW, 16, Component.literal("阶段值"));
            valueBox.setMaxLength(32);
            valueBox.setValue(entry.value());
            valueBox.setResponder(val -> valueBox.setTextColor(DataVariables.isNumericText(val)
                    ? VALID_TEXT_COLOR : INVALID_TEXT_COLOR));
            valueBox.setTextColor(DataVariables.isNumericText(entry.value()) ? VALID_TEXT_COLOR : INVALID_TEXT_COLOR);
            valueBox.setEditable(editable);
            addRenderableWidget(valueBox);

            EditBox nameBox = new EditBox(font, nameX, rowY + 2, nameW, 16, Component.literal("阶段名称"));
            nameBox.setMaxLength(64);
            nameBox.setValue(entry.name().isBlank() ? defaultStageName(finalIndex + 1) : entry.name());
            nameBox.setEditable(editable);
            nameBox.setTextColor(editable ? VALID_TEXT_COLOR : DISABLED_TEXT_COLOR);
            addRenderableWidget(nameBox);

            Button confirmButton = Button.builder(Component.literal("✓"), b -> {
                if (!editable) {
                    return;
                }
                if (tryCommitVisibleEdits()) {
                    init();
                }
            }).bounds(confirmX, rowY + 1, buttonW, BUTTON_HEIGHT).build();
            addRenderableWidget(confirmButton);

            Button deleteButton = Button.builder(Component.literal("✗"), b -> {
                if (!editable) {
                    return;
                }
                DataVariables.VariableState next = DataVariables.removeFiniteStage(data.get(dataKey), finalIndex);
                data.put(dataKey, next);
                init();
            }).bounds(deleteX, rowY + 1, buttonW, BUTTON_HEIGHT).build();
            addRenderableWidget(deleteButton);

            stageRows.add(new StageRowWidget(finalIndex, valueBox, nameBox, confirmButton, deleteButton));
            visibleIndex++;
        }
    }

    private void buildInfiniteRow(DataVariables.VariableState state, int gap) {
        DataVariables.StageEntry infiniteEntry = findInfiniteStage(state);
        boolean infiniteEnabled = infiniteEntry == null || infiniteEntry.enabled();

        int indexW = rightW < 310 ? 28 : 34;
        int buttonW = rightW < 310 ? 74 : 84;
        int valueW = Math.max(58, Math.min(88, rightW / 5));
        int valueX = rightX + indexW;
        int nameX = valueX + valueW + gap;
        int buttonX = rightX + rightW - buttonW;
        int nameW = Math.max(80, buttonX - gap - nameX);

        infiniteNameBox = new EditBox(font, nameX, infiniteY + 2, nameW, 16, Component.literal("无穷大名称"));
        infiniteNameBox.setMaxLength(64);
        infiniteNameBox.setValue(infiniteEntry != null && !infiniteEntry.name().isBlank()
                ? infiniteEntry.name() : defaultInfiniteStageName());
        infiniteNameBox.setEditable(editable);
        infiniteNameBox.setTextColor(editable ? VALID_TEXT_COLOR : DISABLED_TEXT_COLOR);
        addRenderableWidget(infiniteNameBox);

        infiniteEnabledButton = Button.builder(checkboxLabel("启用", infiniteEnabled), b -> {
            if (!editable) {
                return;
            }
            if (!tryCommitVisibleEdits()) {
                return;
            }
            DataVariables.StageEntry currentInfinite = findInfiniteStage(data.get(dataKey));
            boolean nextEnabled = currentInfinite == null || !currentInfinite.enabled();
            DataVariables.VariableState next = DataVariables.setInfiniteStageEnabled(data.get(dataKey), nextEnabled);
            data.put(dataKey, next);
            init();
        }).bounds(buttonX, infiniteY + 1, buttonW, BUTTON_HEIGHT).build();
        addRenderableWidget(infiniteEnabledButton);
    }

    private void switchTotalMode(DataVariables.TotalMode mode) {
        DataVariables.VariableState current = data.get(dataKey);
        if (!editable || current == null || current.totalMode() == mode) {
            return;
        }
        if (current.totalMode() == DataVariables.TotalMode.STAGE && !tryCommitVisibleEdits()) {
            return;
        }
        DataVariables.VariableState next = DataVariables.setTotalMode(data.get(dataKey), mode);
        data.put(dataKey, next);
        scrollOffset = 0;
        init();
    }

    private boolean tryCommitVisibleEdits() {
        DataVariables.VariableState state = data.get(dataKey);
        if (!editable || state == null || state.totalMode() != DataVariables.TotalMode.STAGE) {
            return true;
        }

        if (minimumValueBox != null && !DataVariables.isNumericText(minimumValueBox.getValue())) {
            minimumValueBox.setTextColor(INVALID_TEXT_COLOR);
            showMessage("下限必须是数字");
            return false;
        }

        List<DataVariables.StageEntry> finite = new ArrayList<>(finiteStages(state.stages()));
        for (StageRowWidget row : stageRows) {
            if (row.index() < 0 || row.index() >= finite.size()) {
                continue;
            }
            String value = row.valueBox().getValue().trim();
            if (!DataVariables.isNumericText(value)) {
                row.valueBox().setTextColor(INVALID_TEXT_COLOR);
                showMessage("阶段值必须是数字");
                return false;
            }
            DataVariables.StageEntry current = finite.get(row.index());
            finite.set(row.index(), new DataVariables.StageEntry(value, row.nameBox().getValue(), false, current.enabled()));
        }

        if (!areStagesStrictlyIncreasing(finite)) {
            refreshStageRowColors();
            showMessage("阶段值必须从上到下递增");
            return false;
        }

        List<DataVariables.StageEntry> merged = new ArrayList<>(finite);
        DataVariables.StageEntry infinite = findInfiniteStage(state);
        if (infinite != null) {
            String name = infiniteNameBox != null ? infiniteNameBox.getValue() : infinite.name();
            merged.add(new DataVariables.StageEntry("", name, true, infinite.enabled()));
        } else if (infiniteNameBox != null) {
            merged.add(new DataVariables.StageEntry("", infiniteNameBox.getValue(), true, true));
        }

        DataVariables.VariableState next = state;
        if (minimumValueBox != null) {
            next = DataVariables.setMinimum(next, minimumValueBox.getValue());
        }
        next = DataVariables.setStages(next, merged);
        data.put(dataKey, next);
        return true;
    }

    private boolean areStagesStrictlyIncreasing(List<DataVariables.StageEntry> finite) {
        for (int i = 0; i < finite.size(); i++) {
            DataVariables.StageEntry current = finite.get(i);
            if (!DataVariables.isNumericText(current.value())) {
                return false;
            }
            if (i > 0) {
                DataVariables.StageEntry previous = finite.get(i - 1);
                if (DataVariables.parseDecimal(current.value()).compareTo(DataVariables.parseDecimal(previous.value())) <= 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private void closeToDataList() {
        if (!tryCommitVisibleEdits()) {
            return;
        }
        if (minecraft != null && returnScreen != null) {
            minecraft.setScreen(returnScreen);
            return;
        }
        super.onClose();
    }

    @Override
    public void onClose() {
        closeToDataList();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            closeToDataList();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        DataVariables.VariableState state = data.get(dataKey);
        if (state == null || state.totalMode() != DataVariables.TotalMode.STAGE || delta == 0) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        if (mouseX < rightX || mouseX > rightX + rightW || mouseY < listTop || mouseY > listBottom) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        if (!tryCommitVisibleEdits()) {
            return true;
        }
        int visibleCount = Math.max(0, (listBottom - listTop) / ROW_HEIGHT);
        int maxScroll = Math.max(0, countFiniteStages(state.stages()) - visibleCount);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.signum(delta)));
        init();
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        calculateLayout();
        renderBackground(graphics);
        graphics.drawCenteredString(font, Component.literal("总值设置 - " + dataKey), width / 2, 12, 0xFFFFFF);

        DataVariables.VariableState state = data.get(dataKey);
        if (state == null) {
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        int rightBoundary = contentX + contentW;
        graphics.fill(contentX, contentY, rightBoundary, contentY + contentH, 0x66000000);
        graphics.fill(contentX + leftW, contentY + 8, contentX + leftW + 1, contentY + contentH - 8, 0xFF777788);

        graphics.drawString(font, Component.literal("模式"), contentX + 12, contentY + 24, 0xE0E0E0, false);

        if (state.totalMode() == DataVariables.TotalMode.STAGE) {
            renderStageArea(graphics, mouseX, mouseY, state);
            updateWidgetState();
        } else {
            drawWrappedSection(graphics, "累计模式：",
                    "当前值每次增加时，总值也会同步累加；当前值减少时，总值不会跟着降低。这个模式适合记录累计获得量、累计经验、历史货币收入、总成长值等不断沉淀的统计值。",
                    rightX, row1Y + 2, rightW - 12);
            drawWrappedSection(graphics, "阶段模式：",
                    "当前值按阶段阈值决定总值和阶段名称。可逆决定数值回落时能否掉回低阶段；累计计数按绝对累计值判断阶段；阶段计数会在进入下一阶段后从本阶段重新计数；下限限制当前值最低能降到哪里；无穷大决定超过最高有限阶段后是否进入无限阶段。",
                    rightX, row1Y + 82, rightW - 12);
        }

        super.render(graphics, mouseX, mouseY, partialTick);

        if (message != null) {
            messageTicks++;
            int alpha = Math.max(0, 255 - messageTicks * 3);
            if (alpha > 0) {
                graphics.drawCenteredString(font, Component.literal("§e" + message), width / 2, height - 48,
                        0xFFFFFF | (alpha << 24));
            } else {
                message = null;
                messageTicks = 0;
            }
        }
    }

    private void renderStageArea(GuiGraphics graphics, int mouseX, int mouseY, DataVariables.VariableState state) {
        graphics.drawString(font, Component.literal("阶段规则"), rightX, contentY + 6, 0xE0E0E0, false);

        graphics.fill(rightX, lowerY - 2, rightX + rightW, lowerY + 20, 0x22000000);
        graphics.drawString(font, Component.literal("下限"), rightX + 4, lowerY + 5, 0xA0A0A0, false);

        graphics.fill(rightX, stageHeaderY - 2, rightX + rightW, stageHeaderY + 20, 0x22000000);
        graphics.drawString(font, Component.literal("阶段列表"), rightX + 4, stageHeaderY + 5, 0xE0E0E0, false);
        graphics.drawString(font, Component.literal(editable ? "点击 + 添加条目" : "只读查看"), rightX + 76, stageHeaderY + 5, 0xA0A0A0, false);

        List<DataVariables.StageEntry> finiteStages = finiteStages(state.stages());
        int visibleCount = Math.max(0, (listBottom - listTop) / ROW_HEIGHT);
        int visibleIndex = 0;
        for (int i = scrollOffset; i < Math.min(finiteStages.size(), scrollOffset + visibleCount); i++) {
            int rowY = listTop + visibleIndex * ROW_HEIGHT;
            boolean hovered = mouseX >= rightX && mouseX <= rightX + rightW && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT;
            graphics.fill(rightX, rowY, rightX + rightW, rowY + ROW_HEIGHT, hovered ? 0x33FFFFFF : 0x22000000);
            graphics.drawString(font, Component.literal("§8#" + (i + 1)), rightX + 6, rowY + 4, 0xA0A0A0, false);
            if (editable) {
                graphics.drawString(font, Component.literal("✓"), rightX + rightW - 46, rowY + 4, 0xE0E0E0, false);
                graphics.drawString(font, Component.literal("✗"), rightX + rightW - 24, rowY + 4, 0xE0E0E0, false);
            }
            visibleIndex++;
        }
        if (finiteStages.isEmpty()) {
            graphics.drawCenteredString(font, Component.literal("点击 + 添加阶段"),
                    rightX + rightW / 2, listTop + 12, 0x808080);
        }

        graphics.fill(rightX, infiniteY, rightX + rightW, infiniteY + ROW_HEIGHT, 0x22000000);
        graphics.drawString(font, Component.literal("无穷大"), rightX + (rightW < 310 ? 30 : 36), infiniteY + 5,
                0xE0E0E0, false);
        if (editable) {
            graphics.drawString(font, Component.literal("☑"), rightX + rightW - 24, infiniteY + 4, 0xE0E0E0, false);
        }
    }

    private void updateWidgetState() {
        DataVariables.VariableState state = data.get(dataKey);
        if (state == null || state.totalMode() != DataVariables.TotalMode.STAGE) {
            return;
        }
        boolean totalEditable = editable && state.totalMode() == DataVariables.TotalMode.STAGE;
        if (addStageButton != null) {
            addStageButton.active = totalEditable;
            addStageButton.visible = totalEditable;
        }
        if (reversibleButton != null) {
            reversibleButton.active = totalEditable;
        }
        if (countModeCumulativeButton != null) {
            countModeCumulativeButton.active = totalEditable;
        }
        if (countModeStageButton != null) {
            countModeStageButton.active = totalEditable;
        }
        boolean minimumEditable = totalEditable && !state.noLowerBound();
        if (minimumValueBox != null) {
            minimumValueBox.setEditable(minimumEditable);
            minimumValueBox.setTextColor(minimumEditable
                    ? (DataVariables.isNumericText(minimumValueBox.getValue()) ? VALID_TEXT_COLOR : INVALID_TEXT_COLOR)
                    : DISABLED_TEXT_COLOR);
        }
        if (minimumConfirmButton != null) {
            minimumConfirmButton.active = minimumEditable;
            minimumConfirmButton.visible = totalEditable;
        }
        if (minimumResetButton != null) {
            minimumResetButton.active = minimumEditable;
            minimumResetButton.visible = totalEditable;
        }
        if (minimumNoLowerBoundButton != null) {
            minimumNoLowerBoundButton.active = totalEditable;
            minimumNoLowerBoundButton.visible = true;
        }
        if (infiniteEnabledButton != null) {
            infiniteEnabledButton.active = totalEditable;
            infiniteEnabledButton.visible = true;
        }
        if (infiniteNameBox != null) {
            infiniteNameBox.setEditable(totalEditable);
            infiniteNameBox.setTextColor(totalEditable ? VALID_TEXT_COLOR : DISABLED_TEXT_COLOR);
        }
        for (StageRowWidget row : stageRows) {
            row.valueBox().setEditable(totalEditable);
            row.nameBox().setEditable(totalEditable);
            row.nameBox().setTextColor(totalEditable ? VALID_TEXT_COLOR : DISABLED_TEXT_COLOR);
            row.confirmButton().active = totalEditable;
            row.confirmButton().visible = totalEditable;
            row.deleteButton().active = totalEditable;
            row.deleteButton().visible = totalEditable;
        }
        refreshStageRowColors();
    }

    private void refreshStageRowColors() {
        DataVariables.VariableState state = data.get(dataKey);
        if (state == null) {
            return;
        }
        List<DataVariables.StageEntry> values = new ArrayList<>(finiteStages(state.stages()));
        for (StageRowWidget row : stageRows) {
            if (row.index() >= 0 && row.index() < values.size()) {
                DataVariables.StageEntry current = values.get(row.index());
                values.set(row.index(), new DataVariables.StageEntry(row.valueBox().getValue().trim(),
                        current.name(), false, current.enabled()));
            }
        }
        for (StageRowWidget row : stageRows) {
            row.valueBox().setTextColor(editable ? stageRowColor(row.index(), values) : DISABLED_TEXT_COLOR);
        }
    }

    private int stageRowColor(int index, List<DataVariables.StageEntry> values) {
        if (index < 0 || index >= values.size()) {
            return INVALID_TEXT_COLOR;
        }
        DataVariables.StageEntry current = values.get(index);
        if (!DataVariables.isNumericText(current.value())) {
            return INVALID_TEXT_COLOR;
        }
        if (index > 0) {
            DataVariables.StageEntry previous = values.get(index - 1);
            if (!DataVariables.isNumericText(previous.value())
                    || DataVariables.parseDecimal(current.value()).compareTo(DataVariables.parseDecimal(previous.value())) <= 0) {
                return INVALID_TEXT_COLOR;
            }
        }
        if (index + 1 < values.size()) {
            DataVariables.StageEntry next = values.get(index + 1);
            if (!DataVariables.isNumericText(next.value())
                    || DataVariables.parseDecimal(current.value()).compareTo(DataVariables.parseDecimal(next.value())) >= 0) {
                return INVALID_TEXT_COLOR;
            }
        }
        return VALID_TEXT_COLOR;
    }

    private List<DataVariables.StageEntry> finiteStages(List<DataVariables.StageEntry> stages) {
        List<DataVariables.StageEntry> result = new ArrayList<>();
        if (stages == null) {
            return result;
        }
        for (DataVariables.StageEntry entry : stages) {
            if (entry != null && !entry.infinite()) {
                result.add(entry);
            }
        }
        return result;
    }

    private int countFiniteStages(List<DataVariables.StageEntry> stages) {
        return finiteStages(stages).size();
    }

    private DataVariables.StageEntry findInfiniteStage(DataVariables.VariableState state) {
        if (state == null) {
            return null;
        }
        for (int i = state.stages().size() - 1; i >= 0; i--) {
            DataVariables.StageEntry stage = state.stages().get(i);
            if (stage != null && stage.infinite()) {
                return stage;
            }
        }
        return null;
    }

    private void showMessage(String value) {
        message = value;
        messageTicks = 0;
    }

    private void drawWrappedSection(GuiGraphics graphics, String title, String body, int x, int y, int width) {
        graphics.drawString(font, Component.literal(title).withStyle(Style.EMPTY.withBold(true)), x, y, 0xFFFFFF, false);
        int bodyY = y + 14;
        for (FormattedCharSequence line : font.split(Component.literal(body), Math.max(40, width))) {
            graphics.drawString(font, line, x, bodyY, 0xB8B8B8, false);
            bodyY += 10;
        }
    }

    private Component choiceLabel(String label, boolean active) {
        return Component.literal((active ? "● " : "○ ") + label)
                .withStyle(active ? ChatFormatting.AQUA : ChatFormatting.WHITE);
    }

    private Component checkboxLabel(String label, boolean checked) {
        return Component.literal((checked ? "☑ " : "☐ ") + label)
                .withStyle(checked ? ChatFormatting.GREEN : ChatFormatting.WHITE);
    }

    private String defaultStageName(int index) {
        return switch (index) {
            case 1 -> "一阶";
            case 2 -> "二阶";
            case 3 -> "三阶";
            case 4 -> "四阶";
            case 5 -> "五阶";
            case 6 -> "六阶";
            case 7 -> "七阶";
            case 8 -> "八阶";
            case 9 -> "九阶";
            case 10 -> "十阶";
            default -> index + "阶";
        };
    }

    private String defaultInfiniteStageName() {
        return "无穷大";
    }
}
