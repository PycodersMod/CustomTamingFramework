package com.pycoder.customtamingframework.client.screen;

import com.pycoder.customtamingframework.config.CtfUiSettings;
import com.pycoder.customtamingframework.network.C2SSaveConfigScreenPacket;
import com.pycoder.customtamingframework.network.CtfNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Taming core recipe editor.
 * Data format: {recipe: "...", displayName, consumeOnSuccess}
 * Shows a 3x3 grid of editable slots.
 */
public final class TamingCoreScreen extends Screen {
    private static final int GRID_SIZE = 3;
    private static final int SLOT_SIZE = 36;
    private static final int SLOT_GAP = 4;

    private final CompoundTag originalData;
    private final String parentGuiSnbt;
    private final boolean isOp;
    private final CompoundTag uiSettings;

    private final String[] recipeSlots = new String[9];
    private String displayName;
    private boolean consumeOnSuccess;

    private int selectedSlot = -1;
    private EditBox slotEditBox;
    private boolean modified;

    // Autocomplete state
    private List<ResourceLocation> suggestions = List.of();
    private int selectedSuggestion = -1;

    public TamingCoreScreen(CompoundTag data, String parentGuiSnbt, boolean isOp) {
        this(data, parentGuiSnbt, isOp, CtfUiSettings.defaults());
    }

    public TamingCoreScreen(CompoundTag data, String parentGuiSnbt, boolean isOp, CompoundTag uiSettings) {
        super(Component.literal("驯兽核心配方"));
        this.originalData = data;
        this.parentGuiSnbt = parentGuiSnbt;
        this.isOp = isOp;
        this.uiSettings = CtfUiSettings.sanitize(uiSettings);
        loadData();
    }

    private void loadData() {
        String recipe = originalData.getString("recipe");
        java.util.Arrays.fill(recipeSlots, "");
        if (recipe.contains(",")) {
            String[] parts = recipe.split(",", 9);
            for (int i = 0; i < 9 && i < parts.length; i++) {
                recipeSlots[i] = parts[i].trim();
            }
        } else if (recipe.contains("/")) {
            String[] rows = recipe.split("/", 3);
            for (int r = 0; r < GRID_SIZE; r++) {
                for (int c = 0; c < GRID_SIZE; c++) {
                    int idx = r * GRID_SIZE + c;
                    if (r < rows.length && c < rows[r].length()) {
                        char ch = rows[r].charAt(c);
                        recipeSlots[idx] = (ch == '.' || ch == ' ' || ch == '_') ? "" : String.valueOf(ch);
                    }
                }
            }
        }
        this.displayName = originalData.getString("displayName");
        if (displayName.isBlank()) displayName = "驯兽核心";
        this.consumeOnSuccess = originalData.getBoolean("consumeOnSuccess");
    }

    private String buildRecipeString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            if (i > 0) sb.append(",");
            String val = recipeSlots[i];
            sb.append(val != null && !val.isBlank() ? val : "");
        }
        return sb.toString();
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

    @Override
    protected void init() {
        clearWidgets();

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

        // Create slot edit box (initially hidden)
        slotEditBox = new EditBox(font, 0, 0, 80, 18, Component.literal("物品ID"));
        slotEditBox.setVisible(false);
        slotEditBox.setMaxLength(64);
        slotEditBox.setResponder(val -> {
            if (slotEditBox.isVisible()) refreshSuggestions(val.trim());
        });
        addRenderableWidget(slotEditBox);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 6, 0xFFFFFF);

        // Display name
        graphics.drawString(font,
                Component.literal(displayName),
                width / 2 - font.width(displayName) / 2, 22, 0xFFAA00, false);

        // Restart hint (OP only, below grid)
        if (isOp) {
            graphics.drawCenteredString(font,
                    Component.literal("§7§o配方保存后需要重启游戏才能生效"),
                    width / 2, height - 50, 0x888888);
        }

        // Grid area
        int gridLeft = (width - (GRID_SIZE * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP)) / 2;
        int gridTop = 50;

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                int idx = r * GRID_SIZE + c;
                int x = gridLeft + c * (SLOT_SIZE + SLOT_GAP);
                int y = gridTop + r * (SLOT_SIZE + SLOT_GAP);

                boolean isSelected = (idx == selectedSlot);
                int bg = isSelected ? 0xFF4CAF50 : 0xFF333333;
                if (!isSelected && mouseX >= x && mouseX <= x + SLOT_SIZE && mouseY >= y && mouseY <= y + SLOT_SIZE) {
                    bg = 0xFF555555;
                }
                graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, bg);
                graphics.renderOutline(x, y, SLOT_SIZE, SLOT_SIZE, 0xFF888888);

                String val = recipeSlots[idx];
                if (val != null && !val.isBlank()) {
                    ResourceLocation rl = ResourceLocation.tryParse(val);
                    if (rl != null && ForgeRegistries.ITEMS.containsKey(rl)) {
                        ItemStack stack = new ItemStack(ForgeRegistries.ITEMS.getValue(rl));
                        graphics.renderItem(stack, x + (SLOT_SIZE - 16) / 2, y + (SLOT_SIZE - 16) / 2);
                        graphics.renderItemDecorations(font, stack, x + (SLOT_SIZE - 16) / 2, y + (SLOT_SIZE - 16) / 2);
                    } else {
                        String display = val.length() > 4 ? val.substring(0, 4) + ".." : val;
                        graphics.drawCenteredString(font, Component.literal("§c" + display), x + SLOT_SIZE / 2, y + SLOT_SIZE / 2 - 4, 0xFF5555);
                    }
                }
            }
        }

        // No-recipe message (non-OP)
        boolean allEmpty = true;
        for (String s : recipeSlots) {
            if (s != null && !s.isBlank()) {
                allEmpty = false;
                break;
            }
        }
        if (!isOp && allEmpty) {
            graphics.drawCenteredString(font,
                    Component.literal("§c无配方，请让管理员使用指令添加配方"),
                    width / 2, gridTop + GRID_SIZE * (SLOT_SIZE + SLOT_GAP) + 20, 0xFF5555);
        }

        // Edit box with autocomplete
        if (slotEditBox.isVisible()) {
            int editX = gridLeft;
            int editY = gridTop - 30;
            slotEditBox.setPosition(editX, editY);
            int boxW = GRID_SIZE * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP - 24;
            slotEditBox.setWidth(boxW);

            int btnX = editX + boxW + 4;
            graphics.fill(btnX, editY, btnX + 18, editY + 18, 0xFF4CAF50);
            graphics.drawCenteredString(font, Component.literal("✓"), btnX + 9, editY + 3, 0xFFFFFF);
            graphics.fill(btnX + 20, editY, btnX + 38, editY + 18, 0xFFE53935);
            graphics.drawCenteredString(font, Component.literal("✗"), btnX + 29, editY + 3, 0xFFFFFF);

        }

        super.render(graphics, mouseX, mouseY, partialTick);

        // ── Autocomplete suggestions popup ──
        // Uses GuiGraphics default fill + drawString with a separate endBatch()
        // to ensure the popup content is flushed as an independent draw call on GPU.
        // The popup uses graphics.pose translation to raise its z-level above items/widgets.
        if (slotEditBox.isVisible() && !suggestions.isEmpty()) {
            // Flush ALL pending GuiGraphics data (items, widgets) to GPU first
            Minecraft.getInstance().renderBuffers().bufferSource().endBatch();

            int gridLeft2 = (width - (GRID_SIZE * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP)) / 2;
            int gridTop2 = 50;
            int editX2 = gridLeft2;
            int editY2 = gridTop2 - 30;
            int boxW2 = GRID_SIZE * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP - 24;

            int popX = editX2;
            int popY = editY2 + 18 + 2;
            int popW = boxW2 + 38;
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
                graphics.drawString(font, Component.literal("§7" + id), popX + 4, popY + 3 + i * popItemH, 0xE0E0E0, false);
            }

            graphics.pose().popPose();

            // Flush popup as a separate GPU draw pass (after items/widgets, so it renders on top)
            Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int gridLeft = (width - (GRID_SIZE * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP)) / 2;
            int gridTop = 50;

            if (slotEditBox.isVisible()) {
                int editX = gridLeft;
                int editY = gridTop - 30;
                int boxW = GRID_SIZE * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP - 24;
                int btnX = editX + boxW + 4;

                // Check autocomplete suggestion clicks
                int popY = editY + 18 + 2;
                int popW = boxW + 38;
                int popItemH = 11;
                if (!suggestions.isEmpty() && mouseY >= popY && mouseY <= popY + suggestions.size() * popItemH + 4) {
                    int si = (int) ((mouseY - popY - 2) / popItemH);
                    if (si >= 0 && si < suggestions.size()) {
                        slotEditBox.setValue(suggestions.get(si).toString());
                        slotEditBox.setFocused(true);
                        suggestions = List.of();
                    }
                    return true;
                }

                if (mouseX >= btnX && mouseX <= btnX + 18 && mouseY >= editY && mouseY <= editY + 18) {
                    applySlotEdit();
                    return true;
                }
                int cancelX = btnX + 20;
                if (mouseX >= cancelX && mouseX <= cancelX + 18 && mouseY >= editY && mouseY <= editY + 18) {
                    cancelSlotEdit();
                    return true;
                }
            }

            if (isOp) {
                for (int r = 0; r < GRID_SIZE; r++) {
                    for (int c = 0; c < GRID_SIZE; c++) {
                        int idx = r * GRID_SIZE + c;
                        int x = gridLeft + c * (SLOT_SIZE + SLOT_GAP);
                        int y = gridTop + r * (SLOT_SIZE + SLOT_GAP);
                        if (mouseX >= x && mouseX <= x + SLOT_SIZE && mouseY >= y && mouseY <= y + SLOT_SIZE) {
                            if (selectedSlot >= 0 && selectedSlot != idx) {
                                applySlotEdit();
                            }
                            selectSlot(idx);
                            return true;
                        }
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (slotEditBox.isVisible() && slotEditBox.isFocused()) {
            // Tab = cycle through suggestions
            if (keyCode == 258 && !suggestions.isEmpty()) {
                if (selectedSuggestion < 0) {
                    selectedSuggestion = 0;
                } else {
                    selectedSuggestion = (selectedSuggestion + 1) % suggestions.size();
                }
                return true;
            }
            // Enter = confirm (or accept current suggestion)
            if (keyCode == 257) {
                if (selectedSuggestion >= 0 && selectedSuggestion < suggestions.size()) {
                    slotEditBox.setValue(suggestions.get(selectedSuggestion).toString());
                    suggestions = List.of();
                    selectedSuggestion = -1;
                    return true;
                }
                applySlotEdit();
                return true;
            }
            if (keyCode == 256) {
                if (!suggestions.isEmpty()) {
                    suggestions = List.of();
                    selectedSuggestion = -1;
                    return true;
                }
                cancelSlotEdit();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void selectSlot(int idx) {
        selectedSlot = idx;
        String currentVal = recipeSlots[idx];
        slotEditBox.setValue(currentVal != null ? currentVal : "");
        slotEditBox.setVisible(true);
        slotEditBox.setFocused(true);
        refreshSuggestions(currentVal != null ? currentVal.trim() : "");
    }

    private void applySlotEdit() {
        if (selectedSlot < 0 || selectedSlot >= 9) return;
        String newVal = slotEditBox.getValue().trim();

        if (!newVal.isEmpty()) {
            ResourceLocation rl = ResourceLocation.tryParse(newVal);
            if (rl == null || !ForgeRegistries.ITEMS.containsKey(rl)) {
                slotEditBox.setTextColor(0xFF5555);
                return;
            }
        }
        slotEditBox.setTextColor(0xFFFFFF);

        String oldVal = recipeSlots[selectedSlot] != null ? recipeSlots[selectedSlot] : "";
        if (!newVal.equals(oldVal)) {
            recipeSlots[selectedSlot] = newVal;
            modified = true;
        }
        slotEditBox.setVisible(false);
        slotEditBox.setFocused(false);
        selectedSlot = -1;
        suggestions = List.of();
    }

    private void cancelSlotEdit() {
        slotEditBox.setVisible(false);
        slotEditBox.setFocused(false);
        selectedSlot = -1;
        suggestions = List.of();
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
        output.putString("recipe", buildRecipeString());
        output.putString("displayName", displayName);
        output.putBoolean("consumeOnSuccess", consumeOnSuccess);
        CtfNetwork.CHANNEL.sendToServer(new C2SSaveConfigScreenPacket(2, output));
        onClose();
    }
}
