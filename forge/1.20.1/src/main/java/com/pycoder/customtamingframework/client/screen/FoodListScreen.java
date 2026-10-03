package com.pycoder.customtamingframework.client.screen;

import com.pycoder.customtamingframework.network.C2SRequestConfigScreenPacket;
import com.pycoder.customtamingframework.network.C2SSaveConfigScreenPacket;
import com.pycoder.customtamingframework.network.CtfNetwork;
import com.pycoder.customtamingframework.config.CtfUiSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Food list GUI with 3-state type selection (普通/特殊/禁用).
 * Data format: {foods: [{id, type}, ...]}
 * type: 0=normal, 1=functional, 2=disabled
 */
public final class FoodListScreen extends Screen {
    private static final int ROW_HEIGHT = 22;
    private static final int SEARCH_HEIGHT = 18;
    private static final int LIST_TOP_OFFSET = 50;

    private final CompoundTag originalData;
    private final String parentGuiSnbt;
    private final boolean isOp;
    private final CompoundTag uiSettings;
    private final List<FoodEntry> allFoods = new ArrayList<>();
    private final List<FoodEntry> filteredFoods = new ArrayList<>();
    private EditBox searchBox;
    private int scrollOffset;
    private boolean modified;

    private record FoodEntry(String id, int originalType, String displayName) {
    }

    // Mutable state: index in allFoods → int (0=normal, 1=functional, 2=disabled)
    private final List<Integer> typeState = new ArrayList<>();

    public FoodListScreen(CompoundTag data, String parentGuiSnbt, boolean isOp) {
        this(data, parentGuiSnbt, isOp, CtfUiSettings.defaults());
    }

    public FoodListScreen(CompoundTag data, String parentGuiSnbt, boolean isOp, CompoundTag uiSettings) {
        super(Component.literal("食物列表配置"));
        this.originalData = data;
        this.parentGuiSnbt = parentGuiSnbt;
        this.isOp = isOp;
        this.uiSettings = CtfUiSettings.sanitize(uiSettings);
        loadFoods();
    }

    private void loadFoods() {
        allFoods.clear();
        typeState.clear();
        ListTag foods = originalData.getList("foods", 10);
        for (int i = 0; i < foods.size(); i++) {
            CompoundTag entry = foods.getCompound(i);
            String id = entry.getString("id");
            int type = entry.getInt("type");
            String displayName = getItemDisplayName(id);
            allFoods.add(new FoodEntry(id, type, displayName));
            typeState.add(type);
        }
        applyFilter();
    }

    private static String getItemDisplayName(String itemId) {
        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        if (rl == null || !ForgeRegistries.ITEMS.containsKey(rl)) return itemId;
        net.minecraft.world.item.Item item = ForgeRegistries.ITEMS.getValue(rl);
        if (item == null) return itemId;
        return Component.translatable(item.getDescriptionId()).getString();
    }

    private void applyFilter() {
        filteredFoods.clear();
        String query = searchBox != null ? searchBox.getValue().toLowerCase() : "";
        for (int i = 0; i < allFoods.size(); i++) {
            FoodEntry food = allFoods.get(i);
            if (!isOp && typeState.get(i) == 2) continue; // Non-OP: hide disabled
            if (!query.isEmpty() && !food.id().toLowerCase().contains(query)) continue;
            filteredFoods.add(food);
        }
    }

    @Override
    protected void init() {
        clearWidgets();

        searchBox = new EditBox(font, 15, 20, width - 30, SEARCH_HEIGHT, Component.literal("搜索食物"));
        searchBox.setResponder(val -> {
            applyFilter();
            scrollOffset = 0;
        });
        addRenderableWidget(searchBox);

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

        if (isOp || CtfUiSettings.visibleForNonOp(uiSettings, CtfUiSettings.SHOW_NON_OP_MORE_FOOD_BUTTON)) {
            addRenderableWidget(Button.builder(
                    Component.literal("更多\"食物\""),
                    button -> openSpecialFoodScreen()
            ).bounds(width - 100, btnY, 90, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 6, 0xFFFFFF);
        graphics.drawString(font, Component.literal("§7搜索:"), 15, 8, 0xE0E0E0, false);

        int listLeft = 15;
        int listRight = width - 15;
        int listTop = LIST_TOP_OFFSET;
        int listBottom = height - 45;

        graphics.fill(listLeft - 2, listTop - 2, listRight + 2, listBottom + 2, 0x44000000);

        int visibleCount = Math.max(0, (listBottom - listTop) / ROW_HEIGHT);
        int totalHeight = filteredFoods.size() * ROW_HEIGHT;
        int maxScroll = Math.max(0, totalHeight - (listBottom - listTop));

        for (int i = 0; i < Math.min(visibleCount + 1, filteredFoods.size()); i++) {
            int idx = i + (maxScroll > 0 ? (int) ((float) scrollOffset / ROW_HEIGHT) : 0);
            if (idx < 0 || idx >= filteredFoods.size()) continue;

            FoodEntry food = filteredFoods.get(idx);
            int realIdx = allFoods.indexOf(food);
            int type = realIdx >= 0 ? typeState.get(realIdx) : 0;

            int rowY = listTop + i * ROW_HEIGHT;
            if (rowY + ROW_HEIGHT < listTop || rowY > listBottom) continue;

            boolean hovered = mouseX >= listLeft && mouseX <= listRight && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT;
            graphics.fill(listLeft, rowY, listRight, rowY + ROW_HEIGHT, hovered ? 0x33FFFFFF : 0x22000000);

            // Food item ID with type label and color
            String typeLabel;
            int textColor;
            String colorCode;
            if (type == 0) {
                typeLabel = "普通";
                textColor = 0xE0E0E0;
                colorCode = "§7";
            } else if (type == 1) {
                typeLabel = "特殊";
                textColor = 0xFFAA00;
                colorCode = "§6";
            } else {
                typeLabel = "禁用";
                textColor = 0x888888;
                colorCode = "§8";
            }
            String display = "§7" + food.displayName() + " §8[§7" + food.id() + "§8] §8[§7" + typeLabel + "§8]";
            graphics.drawString(font, Component.literal(display), listLeft + 4, rowY + 4, textColor, false);

            // Type buttons (right side, OP only)
            if (isOp) {
                int btnW = 42;
                int btnH = 16;
                int btnY0 = rowY + 2;
                int btnStartX = listRight - (3 * btnW + 6);

                drawTypeButton(graphics, btnStartX, btnY0, btnW, btnH, "普通", type == 0, mouseX, mouseY);
                drawTypeButton(graphics, btnStartX + btnW + 3, btnY0, btnW, btnH, "特殊", type == 1, mouseX, mouseY);
                drawTypeButton(graphics, btnStartX + 2 * (btnW + 3), btnY0, btnW, btnH, "禁用", type == 2, mouseX, mouseY);
            }
        }

        if (maxScroll > 0) {
            float barRatio = (float) (listBottom - listTop) / totalHeight;
            int barHeight = Math.max(10, (int) ((listBottom - listTop) * barRatio));
            int barY = listTop + (int) ((float) scrollOffset / maxScroll * ((listBottom - listTop) - barHeight));
            graphics.fill(listRight + 4, barY, listRight + 8, barY + barHeight, 0xFFAAAAAA);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawTypeButton(GuiGraphics graphics, int x, int y, int w, int h, String label, boolean active, int mouseX, int mouseY) {
        int bgColor = active ? 0xFF4CAF50 : 0xFF555555;
        if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
            bgColor = active ? 0xFF66BB6A : 0xFF777777;
        }
        graphics.fill(x, y, x + w, y + h, bgColor);
        graphics.drawCenteredString(font, Component.literal(label), x + w / 2, y + 3, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isOp) {
            int listLeft = 15;
            int listRight = width - 15;
            int listTop = LIST_TOP_OFFSET;
            int listBottom = height - 45;
            int totalHeight = filteredFoods.size() * ROW_HEIGHT;
            int maxScroll = Math.max(0, totalHeight - (listBottom - listTop));
            int visibleCount = Math.max(0, (listBottom - listTop) / ROW_HEIGHT);

            for (int i = 0; i < Math.min(visibleCount + 1, filteredFoods.size()); i++) {
                int idx = i + (maxScroll > 0 ? (int) ((float) scrollOffset / ROW_HEIGHT) : 0);
                if (idx < 0 || idx >= filteredFoods.size()) continue;

                FoodEntry food = filteredFoods.get(idx);
                int rowY = listTop + i * ROW_HEIGHT;
                int btnW = 42;
                int btnH = 16;
                int btnY0 = rowY + 2;
                int btnStartX = listRight - (3 * btnW + 6);

                if (mouseX >= btnStartX && mouseX <= btnStartX + btnW && mouseY >= btnY0 && mouseY <= btnY0 + btnH) {
                    setType(food, 0); // 普通
                    return true;
                }
                int funcX = btnStartX + btnW + 3;
                if (mouseX >= funcX && mouseX <= funcX + btnW && mouseY >= btnY0 && mouseY <= btnY0 + btnH) {
                    setType(food, 1); // 特殊
                    return true;
                }
                int disX = btnStartX + 2 * (btnW + 3);
                if (mouseX >= disX && mouseX <= disX + btnW && mouseY >= btnY0 && mouseY <= btnY0 + btnH) {
                    setType(food, 2);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void setType(FoodEntry food, int newType) {
        int realIdx = allFoods.indexOf(food);
        if (realIdx >= 0 && typeState.get(realIdx) != newType) {
            typeState.set(realIdx, newType);
            modified = true;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0) {
            int listTop = LIST_TOP_OFFSET;
            int listBottom = height - 45;
            int totalHeight = filteredFoods.size() * ROW_HEIGHT;
            int viewHeight = listBottom - listTop;
            int maxScroll = Math.max(0, totalHeight - viewHeight);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (delta * ROW_HEIGHT)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void openSpecialFoodScreen() {
        CtfNetwork.CHANNEL.sendToServer(new C2SRequestConfigScreenPacket(3));
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
        ListTag foodsOut = new ListTag();
        for (int i = 0; i < allFoods.size(); i++) {
            FoodEntry food = allFoods.get(i);
            CompoundTag entry = new CompoundTag();
            entry.putString("id", food.id());
            entry.putInt("type", typeState.get(i));
            foodsOut.add(entry);
        }
        output.put("foods", foodsOut);
        CtfNetwork.CHANNEL.sendToServer(new C2SSaveConfigScreenPacket(1, output));
        onClose();
    }
}
