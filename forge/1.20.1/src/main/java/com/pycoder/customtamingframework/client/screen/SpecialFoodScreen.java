package com.pycoder.customtamingframework.client.screen;

import com.pycoder.customtamingframework.config.CtfUiSettings;
import com.pycoder.customtamingframework.network.C2SSaveConfigScreenPacket;
import com.pycoder.customtamingframework.network.CtfNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * "更多食物" screen: manage special items (non-food items that can be fed to pets).
 * Data format: {items: [{id, displayName}], used_ids: [...]}
 */
public final class SpecialFoodScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int SEARCH_HEIGHT = 18;
    private static final int INPUT_HEIGHT = 18;
    private static final int SEARCH_Y = 22;
    private static final int INPUT_Y = 56;
    private static final int LIST_TOP = 88;

    private final CompoundTag originalData;
    private final String parentGuiSnbt;
    private final boolean isOp;
    private final CompoundTag uiSettings;
    private final List<String> allIds = new ArrayList<>();
    private final List<String> allNames = new ArrayList<>();
    private final List<Integer> filteredIndices = new ArrayList<>();
    private final Set<String> usedIds;

    private EditBox searchBox;
    private EditBox itemEditBox;
    private int scrollOffset;
    private boolean modified;

    // Autocomplete state
    private List<ResourceLocation> suggestions = List.of();
    private int selectedSuggestion = -1;

    public SpecialFoodScreen(CompoundTag data, String parentGuiSnbt, boolean isOp) {
        this(data, parentGuiSnbt, isOp, CtfUiSettings.defaults());
    }

    public SpecialFoodScreen(CompoundTag data, String parentGuiSnbt, boolean isOp, CompoundTag uiSettings) {
        super(Component.literal("更多\"食物\"管理"));
        this.originalData = data;
        this.parentGuiSnbt = parentGuiSnbt;
        this.isOp = isOp;
        this.uiSettings = CtfUiSettings.sanitize(uiSettings);
        ListTag usedList = data.getList("used_ids", 8);
        this.usedIds = new java.util.LinkedHashSet<>();
        for (int i = 0; i < usedList.size(); i++) {
            usedIds.add(usedList.getString(i));
        }
        loadItems();
    }

    private void loadItems() {
        allIds.clear();
        allNames.clear();
        ListTag items = originalData.getList("items", 10);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag entry = items.getCompound(i);
            allIds.add(entry.getString("id"));
            allNames.add(entry.getString("displayName"));
        }
        applyFilter();
    }

    private void applyFilter() {
        filteredIndices.clear();
        String query = searchBox != null ? searchBox.getValue().toLowerCase() : "";
        for (int i = 0; i < allIds.size(); i++) {
            if (!query.isEmpty() && !allIds.get(i).toLowerCase().contains(query) && !allNames.get(i).toLowerCase().contains(query))
                continue;
            filteredIndices.add(i);
        }
    }

    /** Rebuild suggestion list from registry matching current edit box text. */
    private void refreshSuggestions(String prefix) {
        if (prefix.isEmpty()) {
            suggestions = List.of();
            selectedSuggestion = -1;
            return;
        }
        String lower = prefix.toLowerCase();
        List<ResourceLocation> all = new ArrayList<>();
        for (ResourceLocation id : ForgeRegistries.ITEMS.getKeys()) {
            if (id.toString().toLowerCase().contains(lower)) {
                all.add(id);
            }
        }
        all.sort(Comparator.comparing(ResourceLocation::toString));
        suggestions = all.subList(0, Math.min(10, all.size()));
        selectedSuggestion = -1;
    }

    /** Get the item ID from the currently selected suggestion, or empty string. */
    private String selectedSuggestionId() {
        if (selectedSuggestion >= 0 && selectedSuggestion < suggestions.size()) {
            return suggestions.get(selectedSuggestion).toString();
        }
        return "";
    }

    @Override
    protected void init() {
        clearWidgets();

        // Search box (always visible)
        searchBox = new EditBox(font, 15, SEARCH_Y, width - 30, SEARCH_HEIGHT, Component.literal("搜索物品"));
        searchBox.setResponder(val -> {
            applyFilter();
            scrollOffset = 0;
        });
        addRenderableWidget(searchBox);

        // Input edit box (OP only)
        if (isOp) {
            itemEditBox = new EditBox(font, 15, INPUT_Y, width - 170, INPUT_HEIGHT, Component.literal("添加物品ID"));
            itemEditBox.setMaxLength(64);
            itemEditBox.setResponder(val -> refreshSuggestions(val.trim()));
            addRenderableWidget(itemEditBox);
        }

        // Bottom buttons
        int btnY = height - 25;
        addRenderableWidget(Button.builder(
                Component.literal("\u2190 返回"),
                button -> onClose()
        ).bounds(width - 90, btnY, 80, 20).build());

        if (isOp) {
            addRenderableWidget(Button.builder(
                    Component.literal("保存"),
                    button -> save()
            ).bounds(width - 180, btnY, 80, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 6, 0xFFFFFF);

        // Labels
        graphics.drawString(font, Component.literal("§7搜索:"), 15, SEARCH_Y - 10, 0xE0E0E0, false);

        if (isOp) {
            graphics.drawString(font, Component.literal("§7添加物品ID:"), 15, INPUT_Y - 10, 0xE0E0E0, false);

            // Input bar buttons (✓ / ✗) — right-aligned with generous margin
            int editW = width - 170 - 15;
            int btnX = 15 + editW + 50;

            // Add button (✓)
            graphics.fill(btnX, INPUT_Y, btnX + 18, INPUT_Y + INPUT_HEIGHT, 0xFF4CAF50);
            graphics.drawCenteredString(font, Component.literal("\u2714"), btnX + 9, INPUT_Y + 3, 0xFFFFFF);

            // Cancel/clear button (✗)
            int cancelX = btnX + 24;
            graphics.fill(cancelX, INPUT_Y, cancelX + 18, INPUT_Y + INPUT_HEIGHT, 0xFFE53935);
            graphics.drawCenteredString(font, Component.literal("\u2718"), cancelX + 9, INPUT_Y + 3, 0xFFFFFF);
        }

        // List area
        int listLeft = 15;
        int listRight = width - 15;
        int listTop = LIST_TOP;
        int listBottom = height - 45;

        graphics.fill(listLeft - 2, listTop - 2, listRight + 2, listBottom + 2, 0x44000000);

        int visibleCount = Math.max(0, (listBottom - listTop) / ROW_HEIGHT);
        int totalHeight = filteredIndices.size() * ROW_HEIGHT;
        int maxScroll = Math.max(0, totalHeight - (listBottom - listTop));

        for (int i = 0; i < Math.min(visibleCount + 1, filteredIndices.size()); i++) {
            int idx = i + (maxScroll > 0 ? (int) ((float) scrollOffset / ROW_HEIGHT) : 0);
            if (idx < 0 || idx >= filteredIndices.size()) continue;

            int realIdx = filteredIndices.get(idx);
            int rowY = listTop + i * ROW_HEIGHT;
            if (rowY + ROW_HEIGHT < listTop || rowY > listBottom) continue;

            boolean hovered = mouseX >= listLeft && mouseX <= listRight && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT;
            graphics.fill(listLeft, rowY, listRight, rowY + ROW_HEIGHT, hovered ? 0x33FFFFFF : 0x22000000);

            String display = "§7" + allIds.get(realIdx) + " §8[§7" + allNames.get(realIdx) + "§8]";
            graphics.drawString(font, Component.literal(display), listLeft + 4, rowY + 4, 0xE0E0E0, false);

            if (isOp) {
                int delX = listRight - 60;
                int delY = rowY + 3;
                graphics.fill(delX, delY, delX + 54, delY + 16, 0xFFE53935);
                graphics.drawCenteredString(font, Component.literal("删除"), delX + 27, delY + 3, 0xFFFFFF);
            }
        }

        if (maxScroll > 0) {
            float barRatio = (float) (listBottom - listTop) / totalHeight;
            int barHeight = Math.max(10, (int) ((listBottom - listTop) * barRatio));
            int barY = listTop + (int) ((float) scrollOffset / maxScroll * ((listBottom - listTop) - barHeight));
            graphics.fill(listRight + 4, barY, listRight + 8, barY + barHeight, 0xFFAAAAAA);
        }

        super.render(graphics, mouseX, mouseY, partialTick);

        // ── Autocomplete suggestions popup ──
        // Uses GuiGraphics default fill + drawString with a separate endBatch()
        // to ensure the popup content is flushed as an independent draw call on GPU.
        // The popup uses graphics.pose translation to raise its z-level above items/widgets.
        if (isOp && !suggestions.isEmpty() && itemEditBox != null && itemEditBox.isFocused()) {
            // Flush ALL pending GuiGraphics data (items, widgets) to GPU first
            Minecraft.getInstance().renderBuffers().bufferSource().endBatch();

            int popX = 15;
            int popY = INPUT_Y + INPUT_HEIGHT + 2;
            int popW = width - 30;
            int popItemH = 11;
            int popH = suggestions.size() * popItemH + 4;

            // Raise z-level so fill/drawString has higher priority in the buffer
            graphics.pose().pushPose();
            graphics.pose().translate(0.0, 0.0, 500.0);

            graphics.fill(popX, popY, popX + popW, popY + popH, 0xFF222222);
            graphics.renderOutline(popX, popY, popW, popH, 0xFF888888);
            for (int i = 0; i < suggestions.size(); i++) {
                String id = suggestions.get(i).toString();
                boolean hovered = mouseX >= popX && mouseX <= popX + popW && mouseY >= popY + 2 + i * popItemH && mouseY <= popY + 2 + (i + 1) * popItemH;
                boolean sel = i == selectedSuggestion;
                if (sel || hovered) {
                    graphics.fill(popX + 2, popY + 2 + i * popItemH, popX + popW - 2, popY + 2 + (i + 1) * popItemH, sel ? 0xFF4CAF50 : 0xFF555555);
                }
                String color = usedIds.contains(id) ? "§8" : "§7";
                graphics.drawString(font, Component.literal(color + id), popX + 4, popY + 3 + i * popItemH, 0xE0E0E0, false);
            }

            graphics.pose().popPose();

            // Flush popup as a separate GPU draw pass (after items/widgets, so it renders on top)
            Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isOp) {
            int listLeft = 15;
            int listRight = width - 15;
            int listTop = LIST_TOP;
            int listBottom = height - 45;
            int totalHeight = filteredIndices.size() * ROW_HEIGHT;
            int maxScroll = Math.max(0, totalHeight - (listBottom - listTop));
            int visibleCount = Math.max(0, (listBottom - listTop) / ROW_HEIGHT);

            // Check autocomplete suggestion clicks
            int popY = INPUT_Y + INPUT_HEIGHT + 2;
            int popW = width - 30;
            int popItemH = 11;
            if (!suggestions.isEmpty() && mouseY >= popY && mouseY <= popY + suggestions.size() * popItemH + 4) {
                int si = (int) ((mouseY - popY - 2) / popItemH);
                if (si >= 0 && si < suggestions.size()) {
                    if (itemEditBox != null) {
                        itemEditBox.setValue(suggestions.get(si).toString());
                        itemEditBox.setFocused(true);
                        suggestions = List.of();
                    }
                }
                return true;
            }

            // Check input area buttons (must match render() positioning)
            int editW = width - 170 - 15;
            int btnX = 15 + editW + 50;

            if (mouseX >= btnX && mouseX <= btnX + 18 && mouseY >= INPUT_Y && mouseY <= INPUT_Y + INPUT_HEIGHT) {
                addItem();
                return true;
            }
            int cancelX = btnX + 24;
            if (mouseX >= cancelX && mouseX <= cancelX + 18 && mouseY >= INPUT_Y && mouseY <= INPUT_Y + INPUT_HEIGHT) {
                if (itemEditBox != null) {
                    itemEditBox.setValue("");
                    itemEditBox.setTextColor(0xFFFFFF);
                    suggestions = List.of();
                }
                return true;
            }

            // Check delete buttons
            for (int i = 0; i < Math.min(visibleCount + 1, filteredIndices.size()); i++) {
                int idx = i + (maxScroll > 0 ? (int) ((float) scrollOffset / ROW_HEIGHT) : 0);
                if (idx < 0 || idx >= filteredIndices.size()) continue;
                int realIdx = filteredIndices.get(idx);
                int rowY = listTop + i * ROW_HEIGHT;
                int delX = listRight - 60;
                int delY = rowY + 3;
                if (mouseX >= delX && mouseX <= delX + 54 && mouseY >= delY && mouseY <= delY + 16) {
                    deleteItem(realIdx);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (itemEditBox != null && itemEditBox.isFocused()) {
            // Tab = cycle through suggestions
            if (keyCode == 258 && !suggestions.isEmpty()) {
                if (selectedSuggestion < 0) {
                    selectedSuggestion = 0;
                } else {
                    selectedSuggestion = (selectedSuggestion + 1) % suggestions.size();
                }
                return true;
            }
            // Enter = confirm add (or accept current suggestion if one is selected)
            if (keyCode == 257) {
                if (selectedSuggestion >= 0 && selectedSuggestion < suggestions.size()) {
                    itemEditBox.setValue(suggestions.get(selectedSuggestion).toString());
                    suggestions = List.of();
                    selectedSuggestion = -1;
                    return true;
                }
                addItem();
                return true;
            }
            // Escape = close suggestions
            if (keyCode == 256 && !suggestions.isEmpty()) {
                suggestions = List.of();
                selectedSuggestion = -1;
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0) {
            int listTop = LIST_TOP;
            int listBottom = height - 45;
            int totalHeight = filteredIndices.size() * ROW_HEIGHT;
            int viewHeight = listBottom - listTop;
            int maxScroll = Math.max(0, totalHeight - viewHeight);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (delta * ROW_HEIGHT)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void addItem() {
        if (itemEditBox == null) return;
        String input = itemEditBox.getValue().trim();
        if (input.isEmpty()) return;

        ResourceLocation rl = ResourceLocation.tryParse(input);
        if (rl == null || !ForgeRegistries.ITEMS.containsKey(rl)) {
            itemEditBox.setTextColor(0xFF5555);
            return;
        }
        if (usedIds.contains(input) || allIds.contains(input)) {
            itemEditBox.setTextColor(0xFF5555);
            return;
        }

        ItemStack stack = new ItemStack(ForgeRegistries.ITEMS.getValue(rl));
        String displayName = stack.getHoverName().getString();

        allIds.add(input);
        allNames.add(displayName);
        modified = true;
        applyFilter();

        itemEditBox.setValue("");
        itemEditBox.setTextColor(0xFFFFFF);
        suggestions = List.of();
    }

    private void deleteItem(int idx) {
        if (idx < 0 || idx >= allIds.size()) return;
        allIds.remove(idx);
        allNames.remove(idx);
        modified = true;
        applyFilter();
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
        ListTag itemsOut = new ListTag();
        for (String id : allIds) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", id);
            itemsOut.add(entry);
        }
        output.put("items", itemsOut);
        CtfNetwork.CHANNEL.sendToServer(new C2SSaveConfigScreenPacket(3, output));
        onClose();
    }
}
