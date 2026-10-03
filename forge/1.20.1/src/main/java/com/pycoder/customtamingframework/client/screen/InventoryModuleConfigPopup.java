package com.pycoder.customtamingframework.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.Font;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared inventory-module config popup for GeneralConfigScreen and PetScreen.
 * Keeps module framework/rules only; actual item stacks live on instantiated pets.
 */
public final class InventoryModuleConfigPopup {
    private static final int MIN_LEFT_COL_W = 122;
    private static final int RIGHT_ACTION_W = 68;
    private static final int RIGHT_ACTION_GAP = 8;
    private static final int ROW_H = 18;

    private final CompoundTag config;
    private final boolean editable;
    private final Runnable refresh;

    private final List<AbstractWidget> widgets = new ArrayList<>();
    private final List<String> filterIds = new ArrayList<>();
    private final List<Integer> visibleIndices = new ArrayList<>();
    private final List<String> itemCandidates = new ArrayList<>();
    private String pendingSearchText = "";
    private String pendingAddText = "";
    private int pendingScrollOffset;
    private List<String> suggestions = List.of();
    private int selectedSuggestion = -1;
    private boolean suppressAddResponder;
    private EditBox nameBox;
    private EditBox rowsBox;
    private EditBox colsBox;
    private EditBox searchBox;
    private EditBox addBox;
    private Button showNameButton;
    private Button deathDropButton;
    private Button stackNoButton;
    private Button stackVanillaButton;
    private Button stackInfiniteButton;
    private Button whiteListButton;
    private Button blackListButton;
    private Button addConfirmButton;
    private Button addClearButton;
    private int scrollOffset;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int leftColW;
    private int rightX;
    private int rightW;
    private int rightColX;
    private int rightColW;
    private int inputW;
    private int addX;
    private int addY;
    private int listTop;
    private int listBottom;

    public InventoryModuleConfigPopup(CompoundTag config, boolean editable, Runnable refresh) {
        this.config = config;
        this.editable = editable;
        this.refresh = refresh;
        loadFilters();
        ensureDefaults();
    }

    public void init(Screen screen, Font font, int panelX, int panelY, int panelW, int panelH) {
        widgets.clear();
        this.panelX = panelX;
        this.panelY = panelY;
        this.panelW = panelW;
        this.panelH = panelH;
        this.leftColW = Math.max(MIN_LEFT_COL_W, (panelW - 24) / 4);
        this.rightX = panelX + leftColW + 12;
        this.rightW = panelW - leftColW - 24;
        this.listTop = panelY + 124;
        this.listBottom = panelY + panelH - 40;

        int leftX = panelX + 12;
        int leftW = leftColW - 18;
        this.rightColX = rightX;
        this.rightColW = rightW;
        this.inputW = Math.max(120, rightColW - RIGHT_ACTION_W - RIGHT_ACTION_GAP);
        int actionX = rightColX + inputW + RIGHT_ACTION_GAP;
        this.addX = rightColX;
        this.addY = panelY + 96;
        int filterBtnY = panelY + 50;
        int filterButtonGap = 2;
        int filterBtnH = 18;

        nameBox = new EditBox(font, leftX + 38, panelY + 30, leftW - 44, 18, Component.literal("命名"));
        nameBox.setValue(config.getString("name"));
        nameBox.setEditable(editable);
        nameBox.setResponder(val -> config.putString("name", val));
        nameBox.setTextColor(editable ? 0xE0E0E0 : 0x888888);
        register(nameBox);

        showNameButton = Button.builder(flagLabel("显示名称", config.getBoolean("show_name")), b -> {
            if (!editable) return;
            config.putBoolean("show_name", !config.getBoolean("show_name"));
            refresh.run();
        }).bounds(leftX, panelY + 54, leftW, 18).build();
        showNameButton.active = editable;
        register(showNameButton);

        rowsBox = new EditBox(font, leftX + 38, panelY + 84, (leftW - 50) / 2, 18, Component.literal("行"));
        rowsBox.setValue(String.valueOf(Math.max(1, config.getInt("rows") == 0 ? 1 : config.getInt("rows"))));
        rowsBox.setEditable(editable);
        rowsBox.setResponder(val -> rowsBox.setTextColor(isPositiveInteger(val) ? 0xE0E0E0 : 0xFF5555));
        rowsBox.setTextColor(isPositiveInteger(rowsBox.getValue()) ? 0xE0E0E0 : 0xFF5555);
        register(rowsBox);
        colsBox = new EditBox(font, leftX + 38 + (leftW - 50) / 2 + 12, panelY + 84, (leftW - 50) / 2, 18, Component.literal("列"));
        colsBox.setValue(String.valueOf(Math.max(1, config.getInt("cols") == 0 ? 1 : config.getInt("cols"))));
        colsBox.setEditable(editable);
        colsBox.setResponder(val -> colsBox.setTextColor(isPositiveInteger(val) ? 0xE0E0E0 : 0xFF5555));
        colsBox.setTextColor(isPositiveInteger(colsBox.getValue()) ? 0xE0E0E0 : 0xFF5555);
        register(colsBox);

        deathDropButton = Button.builder(flagLabel("死亡掉落", config.getBoolean("death_drop")), b -> {
            if (!editable) return;
            config.putBoolean("death_drop", !config.getBoolean("death_drop"));
            refresh.run();
        }).bounds(leftX, panelY + 110, leftW, 18).build();
        deathDropButton.active = editable;
        register(deathDropButton);

        String mode = stackMode();
        stackNoButton = Button.builder(choiceLabel("无法堆叠", "no_stack".equalsIgnoreCase(mode)), b -> {
            if (!editable) return;
            config.putString("stack_mode", "no_stack");
            refresh.run();
        }).bounds(leftX, panelY + 136, leftW, 18).build();
        stackNoButton.active = editable;
        register(stackNoButton);

        stackVanillaButton = Button.builder(choiceLabel("原版堆叠", "vanilla".equalsIgnoreCase(mode)), b -> {
            if (!editable) return;
            config.putString("stack_mode", "vanilla");
            refresh.run();
        }).bounds(leftX, panelY + 158, leftW, 18).build();
        stackVanillaButton.active = editable;
        register(stackVanillaButton);

        stackInfiniteButton = Button.builder(choiceLabel("无限堆叠", "infinite".equalsIgnoreCase(mode)), b -> {
            if (!editable) return;
            config.putString("stack_mode", "infinite");
            refresh.run();
        }).bounds(leftX, panelY + 180, leftW, 18).build();
        stackInfiniteButton.active = editable;
        register(stackInfiniteButton);

        whiteListButton = Button.builder(choiceLabel("白名单", !"blacklist".equalsIgnoreCase(filterMode())), b -> {
            if (!editable) return;
            config.putString("filter_mode", "whitelist");
            refresh.run();
        }).bounds(actionX, filterBtnY, RIGHT_ACTION_W, filterBtnH).build();
        whiteListButton.active = editable;
        register(whiteListButton);

        blackListButton = Button.builder(choiceLabel("黑名单", "blacklist".equalsIgnoreCase(filterMode())), b -> {
            if (!editable) return;
            config.putString("filter_mode", "blacklist");
            refresh.run();
        }).bounds(actionX, filterBtnY + filterBtnH + filterButtonGap, RIGHT_ACTION_W, filterBtnH).build();
        blackListButton.active = editable;
        register(blackListButton);

        searchBox = new EditBox(font, rightColX, panelY + 50, inputW, 18, Component.literal("搜索物品"));
        searchBox.setMaxLength(Integer.MAX_VALUE);
        searchBox.setValue(pendingSearchText == null ? "" : pendingSearchText);
        searchBox.setEditable(editable);
        searchBox.setResponder(val -> {
            scrollOffset = 0;
            refresh.run();
        });
        register(searchBox);

        addBox = new EditBox(font, addX, addY, inputW, 18, Component.literal("添加物品"));
        addBox.setMaxLength(Integer.MAX_VALUE);
        addBox.setEditable(editable);
        addBox.setValue(pendingAddText == null ? "" : pendingAddText);
        addBox.setResponder(val -> {
            if (suppressAddResponder) {
                return;
            }
            addBox.setTextColor(isValidItemId(val.trim()) ? 0xE0E0E0 : 0xFF5555);
            refreshSuggestions(val);
        });
        addBox.setTextColor(isValidItemId(addBox.getValue().trim()) ? 0xE0E0E0 : 0xFF5555);
        register(addBox);

        addConfirmButton = Button.builder(Component.literal("✓"), b -> {
            if (!editable) return;
            String input = addBox.getValue().trim();
            if (!isValidItemId(input) || filterIds.contains(input)) {
                addBox.setTextColor(0xFF5555);
                return;
            }
            filterIds.add(input);
            addBox.setValue("");
            clearSuggestions();
            commitToConfig();
            refreshVisibleIndices();
            refresh.run();
        }).bounds(actionX + 8, addY, 22, 18).build();
        addConfirmButton.active = editable;
        register(addConfirmButton);

        addClearButton = Button.builder(Component.literal("✗"), b -> {
            if (!editable) return;
            addBox.setValue("");
            addBox.setTextColor(0xE0E0E0);
            clearSuggestions();
        }).bounds(actionX + 34, addY, 22, 18).build();
        addClearButton.active = editable;
        register(addClearButton);

        scrollOffset = Math.max(0, pendingScrollOffset);
        refreshVisibleIndices();
        refreshSuggestions(addBox.getValue());
        buildFilterRows(screen);
        updateWidgetStates();
    }

    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, int panelX, int panelY, int panelW, int panelH) {
        graphics.drawString(font, Component.literal("物品栏配置"), panelX + 12, panelY + 8, 0xFFFFFF, false);
        graphics.drawString(font, Component.literal("允许放入的物品类型"), rightX, panelY + 8, 0xE0E0E0, false);

        graphics.drawString(font, Component.literal("命名:"), panelX + 12, panelY + 32, 0xE0E0E0, false);
        graphics.drawString(font, Component.literal("行×列"), panelX + 12, panelY + 86, 0xE0E0E0, false);
        graphics.drawString(font, Component.literal("×"), panelX + 38 + (leftColW - 50) / 2 + 2, panelY + 87, 0xE0E0E0, false);

        int listW = rightW;
        graphics.fill(rightX - 2, listTop - 2, rightX + listW + 2, listBottom + 2, 0x22000000);

        List<String> items = visibleItems();
        int visibleCount = Math.max(0, (listBottom - listTop) / ROW_H);
        int maxScroll = Math.max(0, items.size() - visibleCount);
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }
        int rowIndex = 0;
        for (int i = scrollOffset; i < items.size() && rowIndex < visibleCount; i++) {
            String id = items.get(i);
            int rowY = listTop + rowIndex * ROW_H;
            boolean hovered = false;
            graphics.fill(rightX, rowY, rightX + listW, rowY + ROW_H, hovered ? 0x33FFFFFF : 0x22000000);
            String display = displayText(id);
            graphics.drawString(font, Component.literal(display), rightX + 4, rowY + 4,
                    0xE0E0E0, false);
            rowIndex++;
        }

        if (items.isEmpty()) {
            graphics.drawCenteredString(font, Component.literal("暂无过滤项"),
                    rightX + listW / 2, listTop + 8, 0x808080);
        }
    }

    public void renderSuggestionPopup(GuiGraphics graphics, Font font) {
        if (!showAddSuggestions()) {
            return;
        }
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
        int popX = addX;
        int popY = addY + 20;
        int popW = inputW;
        int popItemH = 11;
        int popH = Math.min(suggestions.size(), 8) * popItemH + 4;

        graphics.pose().pushPose();
        graphics.pose().translate(0.0, 0.0, 500.0);
        graphics.fill(popX, popY, popX + popW, popY + popH, 0xFF222222);
        graphics.renderOutline(popX, popY, popW, popH, 0xFF888888);
        for (int i = 0; i < Math.min(suggestions.size(), 8); i++) {
            String id = suggestions.get(i);
            boolean hovered = false;
            boolean sel = i == selectedSuggestion;
            if (sel || hovered) {
                graphics.fill(popX + 2, popY + 2 + i * popItemH, popX + popW - 2, popY + 2 + (i + 1) * popItemH,
                        sel ? 0xFF4CAF50 : 0xFF555555);
            }
            graphics.drawString(font, Component.literal(id), popX + 4, popY + 3 + i * popItemH, 0xE0E0E0, false);
        }
        graphics.pose().popPose();
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < rightX || mouseX > rightX + rightW || mouseY < listTop || mouseY > listBottom) {
            return false;
        }
        List<String> items = visibleItems();
        int visibleCount = Math.max(0, (listBottom - listTop) / ROW_H);
        int maxScroll = Math.max(0, items.size() - visibleCount);
        if (maxScroll <= 0) {
            return true;
        }
        if (delta < 0) {
            scrollOffset = Math.min(maxScroll, scrollOffset + 1);
        } else if (delta > 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
        }
        refresh.run();
        return true;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (searchBox != null && searchBox.isMouseOver(mouseX, mouseY)) {
            searchBox.setFocused(true);
            if (addBox != null) {
                addBox.setFocused(false);
            }
            return searchBox.mouseClicked(mouseX, mouseY, button);
        }
        if (addBox != null && addBox.isMouseOver(mouseX, mouseY)) {
            addBox.setFocused(true);
            if (searchBox != null) {
                searchBox.setFocused(false);
            }
            return addBox.mouseClicked(mouseX, mouseY, button);
        }
        if (button != 0 || !showAddSuggestions()) {
            return false;
        }
        int popX = addX;
        int popY = addY + 20;
        int popW = inputW;
        int popItemH = 11;
        int maxVisible = Math.min(suggestions.size(), 8);
        int popH = maxVisible * popItemH + 4;
        if (mouseX < popX || mouseX > popX + popW || mouseY < popY || mouseY > popY + popH) {
            return false;
        }
        int idx = (int) ((mouseY - popY - 2) / popItemH);
        if (idx >= 0 && idx < maxVisible) {
            applySuggestion(idx);
            return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode) {
        if (!showAddSuggestions() || addBox == null || !addBox.isFocused()) {
            return false;
        }
        if (keyCode == 258) {
            cycleSuggestion();
            return true;
        }
        if (keyCode == 257 || keyCode == 335) {
            if (!suggestions.isEmpty()) {
                if (selectedSuggestion < 0) {
                    selectedSuggestion = 0;
                }
                applySuggestion(selectedSuggestion);
                return true;
            }
            return false;
        }
        if (keyCode == 256) {
            clearSuggestions();
            return true;
        }
        return false;
    }

    public void commitToConfig() {
        config.putString("name", nameBox != null ? nameBox.getValue() : config.getString("name"));
        config.putBoolean("show_name", config.getBoolean("show_name"));
        config.putInt("rows", Math.max(1, parseInt(rowsBox != null ? rowsBox.getValue() : String.valueOf(config.getInt("rows")), 1)));
        config.putInt("cols", Math.max(1, parseInt(colsBox != null ? colsBox.getValue() : String.valueOf(config.getInt("cols")), 1)));
        config.putBoolean("death_drop", config.getBoolean("death_drop"));
        config.putString("stack_mode", stackMode());
        config.putString("filter_mode", filterMode());
        ListTag filters = new ListTag();
        for (String id : filterIds) {
            if (id != null && !id.isBlank()) {
                filters.add(net.minecraft.nbt.StringTag.valueOf(id));
            }
        }
        config.put("filters", filters);
        ensureDefaults();
    }

    public boolean inputsValid() {
        return isPositiveInteger(rowsBox != null ? rowsBox.getValue() : "1")
                && isPositiveInteger(colsBox != null ? colsBox.getValue() : "1")
                && (addBox == null || addBox.getValue().isBlank() || isValidItemId(addBox.getValue().trim()));
    }

    public List<String> currentFilters() {
        return List.copyOf(filterIds);
    }

    public boolean isEditable() {
        return editable;
    }

    public void setTransientState(String searchText, String addText, int scrollOffset) {
        this.pendingSearchText = searchText != null ? searchText : "";
        this.pendingAddText = addText != null ? addText : "";
        this.pendingScrollOffset = Math.max(0, scrollOffset);
    }

    public String currentSearchText() {
        return searchBox != null ? searchBox.getValue() : pendingSearchText;
    }

    public String currentAddText() {
        return addBox != null ? addBox.getValue() : pendingAddText;
    }

    public int currentScrollOffset() {
        return scrollOffset;
    }

    public void focusSearchBox() {
        if (searchBox != null) {
            searchBox.setFocused(true);
        }
    }

    public void focusAddBox() {
        if (addBox != null) {
            addBox.setFocused(true);
        }
    }

    public List<AbstractWidget> widgets() {
        return List.copyOf(widgets);
    }

    private void ensureDefaults() {
        if (config.getString("name").isBlank()) {
            config.putString("name", "物品栏模块");
        }
        if (!config.contains("rows")) {
            config.putInt("rows", 1);
        }
        if (!config.contains("cols")) {
            config.putInt("cols", 1);
        }
        if (!config.contains("show_name")) {
            config.putBoolean("show_name", true);
        }
        if (!config.contains("death_drop")) {
            config.putBoolean("death_drop", true);
        }
        if (!config.contains("stack_mode")) {
            config.putString("stack_mode", "vanilla");
        }
        if (!config.contains("filter_mode")) {
            config.putString("filter_mode", "blacklist");
        }
        if (!config.contains("filters", 9)) {
            config.put("filters", new ListTag());
        }
        if (config.contains("all_items")) {
            boolean legacyAllowAll = config.getBoolean("all_items");
            config.remove("all_items");
            if (legacyAllowAll) {
                config.putString("filter_mode", "blacklist");
                config.put("filters", new ListTag());
            }
        }
    }

    private void loadFilters() {
        filterIds.clear();
        if (!config.contains("filters", 9)) {
            return;
        }
        ListTag list = config.getList("filters", 8);
        for (int i = 0; i < list.size(); i++) {
            String id = list.getString(i);
            if (id != null && !id.isBlank()) {
                filterIds.add(id);
            }
        }
    }

    private void buildFilterRows(Screen screen) {
        List<String> items = visibleItems();
        int visibleCount = Math.max(0, (listBottom - listTop) / ROW_H);
        int start = Math.min(scrollOffset, Math.max(0, items.size() - visibleCount));
        for (int row = 0; row < visibleCount; row++) {
            int idx = start + row;
            if (idx < 0 || idx >= items.size()) {
                continue;
            }
            String id = items.get(idx);
            int rowY = listTop + row * ROW_H;
            int delX = rightX + rightW - 44;
            Button del = Button.builder(Component.literal("删除"), b -> {
                if (!editable) return;
                filterIds.remove(id);
                commitToConfig();
                refreshVisibleIndices();
                refresh.run();
            }).bounds(delX, rowY + 1, 40, 16).build();
            del.active = editable;
            register(del);
        }
    }

    private void updateWidgetStates() {
        boolean locked = !editable;
        if (searchBox != null) {
            searchBox.setEditable(!locked);
            searchBox.setTextColor(locked ? 0x888888 : 0xE0E0E0);
        }
        if (addBox != null) {
            addBox.setEditable(!locked);
            addBox.setTextColor(locked ? 0x888888 : 0xE0E0E0);
        }
        if (addConfirmButton != null) {
            addConfirmButton.active = !locked;
        }
        if (addClearButton != null) {
            addClearButton.active = !locked;
        }
        if (whiteListButton != null) {
            whiteListButton.active = editable;
        }
        if (blackListButton != null) {
            blackListButton.active = editable;
        }
    }

    private void refreshSuggestions(String prefix) {
        if (!editable) {
            clearSuggestions();
            return;
        }
        String query = prefix == null ? "" : prefix.trim().toLowerCase(Locale.ROOT);
        if (query.isBlank()) {
            clearSuggestions();
            return;
        }
        ensureItemCandidates();
        List<String> matches = new ArrayList<>();
        for (String id : itemCandidates) {
            if (matchesCandidate(id, query)) {
                matches.add(id);
            }
        }
        suggestions = matches.size() > 8 ? List.copyOf(matches.subList(0, 8)) : List.copyOf(matches);
        selectedSuggestion = -1;
    }

    private boolean matchesCandidate(String id, String query) {
        if (id == null || id.isBlank() || query == null || query.isBlank()) {
            return false;
        }
        String lower = id.toLowerCase(Locale.ROOT);
        if (lower.startsWith(query)) {
            return true;
        }
        int colon = lower.indexOf(':');
        if (colon >= 0 && colon + 1 < lower.length() && lower.substring(colon + 1).startsWith(query)) {
            return true;
        }
        return lower.contains(query);
    }

    private void ensureItemCandidates() {
        if (!itemCandidates.isEmpty()) {
            return;
        }
        for (ResourceLocation rl : ForgeRegistries.ITEMS.getKeys()) {
            if (rl != null) {
                itemCandidates.add(rl.toString());
            }
        }
        itemCandidates.sort(String::compareToIgnoreCase);
    }

    private boolean showAddSuggestions() {
        return editable && addBox != null && addBox.isFocused() && !suggestions.isEmpty();
    }

    private void clearSuggestions() {
        suggestions = List.of();
        selectedSuggestion = -1;
    }

    private void cycleSuggestion() {
        if (suggestions.isEmpty()) {
            return;
        }
        selectedSuggestion = (selectedSuggestion + 1) % suggestions.size();
        applySuggestionText(suggestions.get(selectedSuggestion));
    }

    private void applySuggestion(int index) {
        if (index < 0 || index >= suggestions.size()) {
            return;
        }
        selectedSuggestion = index;
        applySuggestionText(suggestions.get(index));
        clearSuggestions();
    }

    private void applySuggestionText(String text) {
        if (addBox == null) {
            return;
        }
        suppressAddResponder = true;
        addBox.setValue(text);
        addBox.setTextColor(0xE0E0E0);
        suppressAddResponder = false;
    }

    private void refreshVisibleIndices() {
        visibleIndices.clear();
        String query = searchBox != null ? searchBox.getValue().trim().toLowerCase(Locale.ROOT) : "";
        for (String id : filterIds) {
            if (id == null || id.isBlank()) {
                continue;
            }
            if (!query.isBlank() && !displayText(id).toLowerCase(Locale.ROOT).contains(query) && !id.toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            visibleIndices.add(filterIds.indexOf(id));
        }
    }

    private List<String> visibleItems() {
        List<String> items = new ArrayList<>();
        String query = searchBox != null ? searchBox.getValue().trim().toLowerCase(Locale.ROOT) : "";
        for (String id : filterIds) {
            if (id == null || id.isBlank()) {
                continue;
            }
            if (!query.isBlank() && !displayText(id).toLowerCase(Locale.ROOT).contains(query) && !id.toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            items.add(id);
        }
        return items;
    }

    private String stackMode() {
        String mode = config.getString("stack_mode");
        return mode == null || mode.isBlank() ? "vanilla" : mode;
    }

    private String filterMode() {
        String mode = config.getString("filter_mode");
        return mode == null || mode.isBlank() ? "blacklist" : mode;
    }

    private String displayText(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || !ForgeRegistries.ITEMS.containsKey(rl)) {
            return "未知物品";
        }
        Item item = ForgeRegistries.ITEMS.getValue(rl);
        if (item == null) {
            return "未知物品";
        }
        String translated = Component.translatable(item.getDescriptionId()).getString();
        if (translated == null || translated.isBlank()) {
            return "未知物品";
        }
        return translated;
    }

    private boolean isValidItemId(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl != null && ForgeRegistries.ITEMS.containsKey(rl);
    }

    private static boolean isPositiveInteger(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return Integer.parseInt(trimmed) > 0;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private Component flagLabel(String label, boolean checked) {
        return Component.literal((checked ? "☑ " : "☐ ") + label)
                .withStyle(checked ? ChatFormatting.GREEN : ChatFormatting.WHITE);
    }

    private Component choiceLabel(String label, boolean selected) {
        return Component.literal((selected ? "● " : "○ ") + label)
                .withStyle(selected ? ChatFormatting.AQUA : ChatFormatting.WHITE);
    }

    private <T extends AbstractWidget> T register(T widget) {
        widgets.add(widget);
        return widget;
    }
}
