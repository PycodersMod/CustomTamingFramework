package com.example.customtamingframework.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared tab data model and widget factory used by GeneralConfigScreen and PetScreen.
 * Does NOT extend Screen — screens call its methods from their own init().
 * Supports scrolling via fixed 3-tab slots with left/right arrow buttons.
 */
public class TabContainer {
    private final List<CompoundTag> tabs;
    private final boolean isOp;
    private int activeTab;
    private int scrollOffset;
    private final int tabBarY;
    private final int separatorY;
    private final int leftEdgeX;
    private static final int TAB_HEIGHT = 20;
    private static final int ARROW_WIDTH = 18;
    private static final int DELETE_WIDTH = 14;
    private static final int PLUS_WIDTH = 20;
    private static final int MARGIN = 10;
    private static final int MAX_VISIBLE_TABS = 3;
    private int activeEditX, activeEditY, activeEditW, activeEditH;

    public TabContainer(List<CompoundTag> tabs, boolean isOp) {
        this(tabs, isOp, 30, 53, MARGIN);
    }

    public TabContainer(List<CompoundTag> tabs, boolean isOp, int tabBarY, int separatorY) {
        this(tabs, isOp, tabBarY, separatorY, MARGIN);
    }

    public TabContainer(List<CompoundTag> tabs, boolean isOp, int tabBarY, int separatorY, int leftEdgeX) {
        this.tabs = tabs;
        this.isOp = isOp;
        this.tabBarY = tabBarY;
        this.separatorY = separatorY;
        this.leftEdgeX = leftEdgeX;
        this.activeTab = 0;
        this.scrollOffset = 0;
        if (!tabs.isEmpty() && activeTab >= tabs.size()) {
            this.activeTab = tabs.size() - 1;
        }
    }

    public int getSeparatorY() {
        return separatorY;
    }

    /**
     * Creates tab bar buttons and returns them for the screen to add via addRenderableWidget.
     * Uses the instance's leftEdgeX for widget positioning.
     */
    public List<Button> createTabWidgets(Font font, int screenWidth, Runnable onBeforeSwitch, Runnable onInitRequested) {
        return createTabWidgets(font, screenWidth, leftEdgeX, onBeforeSwitch, onInitRequested);
    }

    /**
     * Creates tab bar buttons with a custom left edge (for right-panel-only use in PetScreen).
     */
    public List<Button> createTabWidgets(Font font, int screenWidth, int leftEdge, Runnable onBeforeSwitch, Runnable onInitRequested) {
        List<Button> widgets = new ArrayList<>();
        int rightEdge = screenWidth - MARGIN;
        boolean needsScroll = tabs.size() > MAX_VISIBLE_TABS;

        int x = leftEdge;

        // Left arrow
        if (needsScroll && scrollOffset > 0) {
            widgets.add(Button.builder(
                    Component.literal("<"),
                    b -> { onBeforeSwitch.run(); scrollOffset = Math.max(0, scrollOffset - 1); onInitRequested.run(); }
            ).bounds(x, tabBarY, ARROW_WIDTH, TAB_HEIGHT).build());
            x += ARROW_WIDTH + 2;
        } else if (needsScroll) {
            x += ARROW_WIDTH + 2;
        }

        // Visible tab buttons (up to MAX_VISIBLE_TABS)
        int visibleCount = needsScroll ? MAX_VISIBLE_TABS : tabs.size();
        for (int i = scrollOffset; i < Math.min(tabs.size(), scrollOffset + visibleCount); i++) {
            CompoundTag tab = tabs.get(i);
            String title = tab.getString("title");
            if (title.isBlank()) title = "未命名标签页";
            int btnWidth = Math.min(Math.max(60, font.width(title) + 12), (rightEdge - leftEdge) / 5);

            final int idx = i;
            // v0.1.4-alpha: OP active tab → no button (replaced by EditBox from createActiveTabTitleEdit)
            if (isOp && idx == activeTab) {
                activeEditX = x;
                activeEditY = tabBarY;
                activeEditW = btnWidth;
                activeEditH = TAB_HEIGHT;
                // advance x but don't create a button
                x += btnWidth + 2;
            } else {
                String displayName = (idx == activeTab) ? "§n" + title + "§r" : title;
                widgets.add(Button.builder(
                        Component.literal(displayName),
                        b -> { onBeforeSwitch.run(); switchTo(idx); onInitRequested.run(); }
                ).bounds(x, tabBarY, btnWidth, TAB_HEIGHT).build());
                x += btnWidth + 2;
            }

            if (isOp && tabs.size() > 1) {
                final int delIdx = i;
                widgets.add(Button.builder(
                        Component.literal("x"),
                        b -> { onBeforeSwitch.run(); removeTab(delIdx); onInitRequested.run(); }
                ).bounds(x, tabBarY, DELETE_WIDTH, TAB_HEIGHT).build());
                x += DELETE_WIDTH + 2;
            } else if (isOp) {
                // Reserve delete space for layout stability when only 1 tab
                x += DELETE_WIDTH + 2;
            }
        }

        // "+" button — at right edge before ">" when scrolling, after last tab otherwise
        if (isOp) {
            int plusX;
            if (needsScroll) {
                // Place "+" at right edge, just before ">" arrow
                plusX = rightEdge - ARROW_WIDTH - PLUS_WIDTH - 2;
            } else {
                plusX = x + 8;
            }
            widgets.add(Button.builder(
                    Component.literal("+"),
                    b -> { onBeforeSwitch.run(); addTab(); onInitRequested.run(); }
            ).bounds(plusX, tabBarY, PLUS_WIDTH, TAB_HEIGHT).build());
            if (!needsScroll) {
                x += PLUS_WIDTH + 2;
            }
        }

        // Right arrow
        if (needsScroll) {
            boolean canScrollRight = (scrollOffset + MAX_VISIBLE_TABS < tabs.size());
            if (canScrollRight) {
                int arrowX = rightEdge - ARROW_WIDTH;
                widgets.add(Button.builder(
                        Component.literal(">"),
                        b -> { onBeforeSwitch.run(); scrollOffset = Math.min(tabs.size() - 1, scrollOffset + 1); onInitRequested.run(); }
                ).bounds(arrowX, tabBarY, ARROW_WIDTH, TAB_HEIGHT).build());
            }
        }

        return widgets;
    }

    /**
     * Draws the separator line from leftEdgeX to the screen edge.
     */
    public void render(GuiGraphics graphics, int screenWidth) {
        graphics.fill(leftEdgeX, separatorY, screenWidth, separatorY + 1, 0xFF808080);
    }

    public void switchTo(int index) {
        if (index < 0 || index >= tabs.size()) return;
        activeTab = index;
    }

    public void addTab() {
        CompoundTag newTab = new CompoundTag();
        newTab.putString("id", "tab_" + System.currentTimeMillis());
        newTab.putString("title", "未命名标签页");
        newTab.putBoolean("enabled", true);
        newTab.putString("summary", "");
        newTab.putInt("order", tabs.size());
        ModuleSystem.initModules(newTab);
        tabs.add(newTab);
        activeTab = tabs.size() - 1;
        // Scroll so new tab is visible (rightmost visible slot)
        if (tabs.size() > MAX_VISIBLE_TABS) {
            scrollOffset = tabs.size() - MAX_VISIBLE_TABS;
        }
    }

    public void removeTab(int index) {
        if (tabs.size() <= 1) return;
        tabs.remove(index);
        if (activeTab >= tabs.size()) activeTab = tabs.size() - 1;
        int maxOffset = Math.max(0, tabs.size() - MAX_VISIBLE_TABS);
        if (scrollOffset > maxOffset) scrollOffset = maxOffset;
    }

    public int getActiveTab() { return activeTab; }

    public CompoundTag getActiveTabData() {
        return activeTab >= 0 && activeTab < tabs.size() ? tabs.get(activeTab) : null;
    }

    /**
     * v0.1.4-alpha: Creates an EditBox at the active tab's position (OP mode only).
     * The returned EditBox replaces the active tab button for inline title editing.
     */
    public EditBox createActiveTabTitleEdit(Font font) {
        String curTitle = activeTab >= 0 && activeTab < tabs.size()
                ? tabs.get(activeTab).getString("title") : "";
        if (curTitle.isBlank()) curTitle = "未命名标签页";
        EditBox edit = new EditBox(font, activeEditX, activeEditY, activeEditW, activeEditH,
                Component.literal("标签名"));
        edit.setValue(curTitle);
        edit.setResponder(val -> {
            if (activeTab >= 0 && activeTab < tabs.size()) {
                tabs.get(activeTab).putString("title", val.isBlank() ? "未命名标签页" : val);
            }
        });
        return edit;
    }

    public List<CompoundTag> getTabs() { return tabs; }

    public static ListTag serializeTabs(List<CompoundTag> tabList) {
        ListTag result = new ListTag();
        for (CompoundTag tab : tabList) {
            result.add(tab.copy());
        }
        return result;
    }

    public static List<CompoundTag> deserializeTabs(ListTag list) {
        List<CompoundTag> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            result.add(list.getCompound(i).copy());
        }
        return result;
    }

    /** Serialize tabs to a CompoundTag keyed by each tab's "id", preserving order. */
    public static CompoundTag serializeTabsToCompound(List<CompoundTag> tabList) {
        CompoundTag result = new CompoundTag();
        for (int i = 0; i < tabList.size(); i++) {
            CompoundTag tab = tabList.get(i);
            String id = tab.getString("id");
            if (!id.isBlank()) {
                CompoundTag tabData = tab.copy();
                tabData.remove("id");
                tabData.putInt("order", i);
                result.put(id, tabData);
            }
        }
        return result;
    }

    /** Deserialize tabs from a CompoundTag where each key is a tab ID, sorted by order. */
    public static List<CompoundTag> deserializeTabsFromCompound(CompoundTag compound) {
        List<CompoundTag> result = new ArrayList<>();
        for (String key : compound.getAllKeys()) {
            if (key.equals("data_variables")) continue;
            if (compound.contains(key, 10)) {
                CompoundTag tab = compound.getCompound(key).copy();
                tab.putString("id", key);
                if (!tab.contains("title")) tab.putString("title", key);
                if (!tab.contains("enabled")) tab.putBoolean("enabled", true);
                if (!tab.contains("summary")) tab.putString("summary", "");
                if (!tab.contains("order")) tab.putInt("order", 999);
                result.add(tab);
            }
        }
        result.sort((a, b) -> Integer.compare(a.getInt("order"), b.getInt("order")));
        return result;
    }
}
