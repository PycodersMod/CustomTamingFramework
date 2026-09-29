package com.example.customtamingframework.client.screen;

import com.example.customtamingframework.CustomTamingFramework;
import com.example.customtamingframework.config.CtfUiSettings;
import com.example.customtamingframework.network.C2SRequestConfigScreenPacket;
import com.example.customtamingframework.network.C2SSaveGeneralConfigPacket;
import com.example.customtamingframework.network.CtfNetwork;
import com.example.customtamingframework.pet.DataVariables;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import static com.example.customtamingframework.client.screen.ModuleSystem.readModules;
import static com.example.customtamingframework.client.screen.ModuleSystem.writeModules;
import static com.example.customtamingframework.client.screen.ModuleSystem.renderModules;
import static com.example.customtamingframework.client.screen.ModuleSystem.hitTest;
import static com.example.customtamingframework.client.screen.ModuleSystem.ModuleType.PLAIN_TEXT;
import static com.example.customtamingframework.client.screen.ModuleSystem.MODULE_MIN_H;
import com.mojang.blaze3d.systems.RenderSystem;

public final class GeneralConfigScreen extends Screen {
    private final String guiSnbt;
    private final boolean isOp;
    private final CompoundTag uiSettings;
    private List<CompoundTag> tabs;
    private boolean tabsLoaded;
    private TabContainer tabContainer;
    private EditBox tabTitleEdit;
    private String saveMessage;
    private int saveMessageTicks;

    // ── Module system (v0.1.4-alpha) ──
    private int hoveredModule = -1;
    private int draggedModule = -1;
    private int dragStartX, dragStartY;
    private boolean showModuleMenu;
    private int menuX, menuY;
    private boolean showModuleConfig;
    private int configModuleIdx = -1;
    private EditBox moduleTextEdit;
    private Button moduleSaveBtn;
    private ModuleSystem.Module editingModule;
    private InventoryModuleConfigPopup inventoryModulePopup;
    private String inventoryPopupSearchText = "";
    private String inventoryPopupAddText = "";
    private int inventoryPopupScrollOffset;

    // ── DATA module config fields ──
    private boolean dataKeyValid;
    private boolean dataFormatIsTotal;
    private String savedDataKey;

    private int entityId = -1;
    private CompoundTag parsedGuiData;

    public GeneralConfigScreen(String guiSnbt, boolean isOp) {
        this(guiSnbt, isOp, -1, CtfUiSettings.defaults());
    }

    public GeneralConfigScreen(String guiSnbt, boolean isOp, CompoundTag uiSettings) {
        this(guiSnbt, isOp, -1, uiSettings);
    }

    public GeneralConfigScreen(String guiSnbt, boolean isOp, int entityId) {
        this(guiSnbt, isOp, entityId, CtfUiSettings.defaults());
    }

    public GeneralConfigScreen(String guiSnbt, boolean isOp, int entityId, CompoundTag uiSettings) {
        super(buildTitle());
        this.guiSnbt = guiSnbt;
        this.isOp = isOp;
        this.entityId = entityId;
        this.uiSettings = CtfUiSettings.sanitize(uiSettings);
    }

    private static Component buildTitle() {
        MutableComponent title = Component.literal("");
        title.append(Component.literal("CTF").withStyle(ChatFormatting.BOLD, ChatFormatting.ITALIC, ChatFormatting.GOLD));
        title.append(Component.literal(" 通用配置界面"));
        return title;
    }

    private int dividerX() {
        return width / 3;  // 1:2 ratio (left 1/3, right 2/3)
    }

    private int tabLeftEdge() {
        return dividerX() + 10;
    }

    @Override
    protected void init() {
        clearWidgets();
        ModuleSystem.setResourcesDir(com.example.customtamingframework.config.CtfSnbtConfig.resourcesDirectory());
        if (!tabsLoaded) {
            parseTabs();
            tabsLoaded = true;
        }
        if (tabs == null || tabs.isEmpty()) {
            tabs = new ArrayList<>();
            CompoundTag defaultTab = new CompoundTag();
            defaultTab.putString("id", "default");
            defaultTab.putString("title", "通用配置");
            defaultTab.putBoolean("enabled", true);
            defaultTab.putString("summary", "");
            defaultTab.putInt("order", 0);
            tabs.add(defaultTab);
        }

        if (tabContainer == null) {
            tabContainer = new TabContainer(tabs, isOp, 52, 75, tabLeftEdge());
        }
        for (Button btn : tabContainer.createTabWidgets(font, width, tabLeftEdge(),
                () -> saveCurrentEditValues(),
                () -> init())) {
            addRenderableWidget(btn);
        }
        // v0.1.4-alpha: OP tab title EditBox (replaces the active tab button)
        if (isOp && tabContainer.getActiveTabData() != null) {
            tabTitleEdit = tabContainer.createActiveTabTitleEdit(font);
            addRenderableWidget(tabTitleEdit);
        } else {
            tabTitleEdit = null;
        }
        createBodyWidgets();

        // Left panel: 3 config sub-screen navigation buttons
        int leftPanelX = 15;
        int leftBtnW = dividerX() - 30;
        int leftY = 44;
        if (isOp || CtfUiSettings.visibleForNonOp(uiSettings, CtfUiSettings.SHOW_NON_OP_PET_LIST_BUTTON)) {
            addRenderableWidget(Button.builder(
                    Component.literal("宠物列表"),
                    button -> CtfNetwork.CHANNEL.sendToServer(new C2SRequestConfigScreenPacket(0))
            ).bounds(leftPanelX, leftY, leftBtnW, 20).build());
            leftY += 24;
        }
        if (isOp || CtfUiSettings.visibleForNonOp(uiSettings, CtfUiSettings.SHOW_NON_OP_FOOD_LIST_BUTTON)) {
            addRenderableWidget(Button.builder(
                    Component.literal("食物列表"),
                    button -> CtfNetwork.CHANNEL.sendToServer(new C2SRequestConfigScreenPacket(1))
            ).bounds(leftPanelX, leftY, leftBtnW, 20).build());
            leftY += 24;
        }
        if (isOp || CtfUiSettings.visibleForNonOp(uiSettings, CtfUiSettings.SHOW_NON_OP_TAMING_CORE_BUTTON)) {
            addRenderableWidget(Button.builder(
                    Component.literal("驯兽核心"),
                    button -> CtfNetwork.CHANNEL.sendToServer(new C2SRequestConfigScreenPacket(2))
            ).bounds(leftPanelX, leftY, leftBtnW, 20).build());
            leftY += 24;
        }
        if (isOp) {
            addRenderableWidget(Button.builder(
                    Component.literal("用户设置"),
                    button -> {
                        if (minecraft != null) {
                            minecraft.setScreen(new UserSettingsScreen(this, guiSnbt, isOp, uiSettings));
                        }
                    }
            ).bounds(leftPanelX, leftY, leftBtnW, 20).build());
        }

        // Bottom buttons: data list + save + close
        int btnW = 90;
        int btnSpacing = 10;
        int btnY = height - 30;
        boolean showDataListButton = isOp || CtfUiSettings.visibleForNonOp(uiSettings, CtfUiSettings.SHOW_NON_OP_DATA_LIST_BUTTON);
        int btnCount = (showDataListButton ? 1 : 0) + (isOp ? 1 : 0) + 1;
        int totalW = btnCount * btnW + (btnCount - 1) * btnSpacing;
        int btnStartX = (width - totalW) / 2;
        int btnCurX = btnStartX;

        if (showDataListButton) {
            addRenderableWidget(Button.builder(
                    Component.literal("数据列表"),
                    button -> {
                        try {
                            CompoundTag dataTag = getParsedGuiData().getCompound("data_variables");
                            Map<String, DataVariables.VariableState> vars = DataVariables.fromCompoundStates(dataTag);
                            Consumer<Map<String, DataVariables.VariableState>> onSave = saved -> {
                                CompoundTag root = getParsedGuiData();
                                root.put("data_variables", DataVariables.toCompound(saved));
                                parsedGuiData = root;
                                save();
                            };
                            if (minecraft != null) {
                                minecraft.setScreen(new DataListScreen(vars, onSave, this, uiSettings));
                            }
                        } catch (Exception e) {
                            CustomTamingFramework.LOGGER.error("[CTF] Data list button error", e);
                        }
                    }
            ).bounds(btnCurX, btnY, btnW, 20).build());
            btnCurX += btnW + btnSpacing;
        }

        if (isOp) {
            addRenderableWidget(Button.builder(
                    Component.literal("保存配置"),
                    button -> save()
            ).bounds(btnCurX, btnY, btnW, 20).build());
            btnCurX += btnW + btnSpacing;
        }

        addRenderableWidget(Button.builder(Component.literal("关闭"), button -> onClose())
                .bounds(btnCurX, btnY, btnW, 20).build());
    }

    private void parseTabs() {
        try {
            CompoundTag data = net.minecraft.nbt.TagParser.parseTag(guiSnbt);
            tabs = TabContainer.deserializeTabsFromCompound(data);
        } catch (Exception e) {
            tabs = new ArrayList<>();
        }
    }

    private void createBodyWidgets() {
        CompoundTag activeTab = tabContainer.getActiveTabData();
        if (activeTab == null) return;

        int bodyY = tabContainer.getSeparatorY() + 5;
        int leftX = tabLeftEdge();
        int editWidth = Math.min(280, width - leftX - 20);

        if (isOp) {
            // v0.1.4-alpha: summary/slots edit removed (modules replace summary/slots)
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);

        // Vertical divider line (1:3 split)
        graphics.fill(dividerX(), 30, dividerX() + 1, height - 40, 0xFF808080);

        tabContainer.render(graphics, width);

        CompoundTag activeTab = tabContainer.getActiveTabData();
        if (activeTab != null) {
            renderTabBody(graphics, activeTab);
        }

        if (saveMessage != null) {
            saveMessageTicks++;
            int alpha = Math.max(0, 255 - saveMessageTicks * 3);
            if (alpha > 0) {
                graphics.drawCenteredString(font, Component.literal("§a" + saveMessage), width / 2, height - 75, 0xFFFFFF | (alpha << 24));
            } else {
                saveMessage = null;
                saveMessageTicks = 0;
            }
        }

        // ── Module context menu ──
        if (showModuleMenu) {
            graphics.fill(0, 0, width, height, 0x40000000);
            int menuW = 140;
            int menuItemH = 25;
            String[] items = {"  + 添加纯文本模块", "  + 添加图片模块", "  + 添加数据模块", "  + 添加物品栏模块"};
            int menuH = items.length * menuItemH;
            int mx2 = Math.min(menuX, width - menuW);
            int my2 = Math.min(menuY, height - menuH);
            for (int mi = 0; mi < items.length; mi++) {
                int myOff = my2 + mi * menuItemH;
                boolean hovered = mouseX >= mx2 && mouseX <= mx2 + menuW
                        && mouseY >= myOff && mouseY <= myOff + menuItemH;
                graphics.fill(mx2, myOff, mx2 + menuW, myOff + menuItemH,
                        hovered ? 0xFF555566 : 0xFF333344);
                graphics.drawString(font, Component.literal(items[mi]),
                        mx2 + 4, myOff + 6, 0xE0E0E0, false);
            }
            graphics.renderOutline(mx2, my2, menuW, menuH, 0xFF888888);
        }

        // ── Module config popup ──
        if (showModuleConfig) {
            String configTitle = "编辑模块";
            if (tabContainer != null) {
                CompoundTag tab = tabContainer.getActiveTabData();
                if (tab != null && configModuleIdx >= 0) {
                    List<ModuleSystem.Module> mods = readModules(tab);
                    if (configModuleIdx < mods.size()) {
                        ModuleSystem.ModuleType mt = mods.get(configModuleIdx).type();
                        if (mt == ModuleSystem.ModuleType.PLAIN_TEXT) configTitle = "编辑纯文本模块";
                        else if (mt == ModuleSystem.ModuleType.IMAGE) configTitle = "编辑图片模块";
                        else if (mt == ModuleSystem.ModuleType.DATA) configTitle = "编辑数据模块";
                        else if (mt == ModuleSystem.ModuleType.INVENTORY) configTitle = "编辑物品栏模块";
                    }
                }
            }
            graphics.fill(0, 0, width, height, 0x80000000);
            int panelW = moduleConfigPanelWidth();
            int panelH = moduleConfigPanelHeight(editingModule);
            int panelX = (width - panelW) / 2;
            int panelY = (height - panelH) / 2;
            graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF444466);
            graphics.renderOutline(panelX, panelY, panelW, panelH, 0xFF888888);
            graphics.drawCenteredString(font, Component.literal(configTitle),
                    width / 2, panelY + 10, 0xFFFFFF);

            if (editingModule != null && editingModule.type() == ModuleSystem.ModuleType.INVENTORY && inventoryModulePopup != null) {
                inventoryModulePopup.render(graphics, font, mouseX, mouseY, panelX, panelY, panelW, panelH);
            } else if (editingModule != null && editingModule.type() == ModuleSystem.ModuleType.IMAGE) {
                int previewX = panelX + 10;
                int previewY = panelY + 34;
                int previewW = Math.min(150, Math.max(120, panelW - 210));
                int previewH = panelH - 64;
                graphics.fill(previewX, previewY, previewX + previewW, previewY + previewH, 0xFF222222);
                graphics.renderOutline(previewX, previewY, previewW, previewH, 0xFFAAAAAA);

                String imgFile = editingModule.config().getString("image_file");
                int maxWidth = Math.max(1, previewW - 8);
                int maxHeight = Math.max(1, previewH - 8);
                int cfgW = clampSliderValue(editingModule.config().contains("image_width")
                        ? editingModule.config().getInt("image_width") : 80, maxWidth);
                int cfgH = clampSliderValue(editingModule.config().contains("image_height")
                        ? editingModule.config().getInt("image_height") : 80, maxHeight);
                int boxW = Math.max(1, Math.min(maxWidth, cfgW));
                int boxH = Math.max(1, Math.min(maxHeight, cfgH));
                int boxX = previewX + (previewW - boxW) / 2;
                int boxY = previewY + (previewH - boxH) / 2;
                graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xFF333344);
                graphics.renderOutline(boxX, boxY, boxW, boxH, 0xFF9999AA);

                if (!imgFile.isEmpty()) {
                    ResourceLocation rl = ModuleSystem.getOrLoadImageCached(imgFile);
                    if (rl != null) {
                        int natW = ModuleSystem.getImageWidth(imgFile);
                        int natH = ModuleSystem.getImageHeight(imgFile);
                        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, rl);
                        graphics.blit(rl, boxX, boxY, boxW, boxH, 0.0F, 0.0F, natW, natH, natW, natH);
                    } else {
                        graphics.drawCenteredString(font, Component.literal("§8[加载失败]"),
                                boxX + boxW / 2, boxY + boxH / 2 - 4, 0x808080);
                    }
                }

                int fileLabelX = previewX + previewW + 10;
                int fileLabelMaxW = Math.max(40, panelX + panelW - 110 - fileLabelX);
                String fileLabel = "文件: " + (imgFile.isEmpty() ? "未选择" : imgFile);
                graphics.drawString(font,
                        Component.literal("§7" + font.plainSubstrByWidth(fileLabel, fileLabelMaxW)),
                        fileLabelX, panelY + 18, 0xE0E0E0, false);
            } else if (editingModule.type() == ModuleSystem.ModuleType.DATA) {
                graphics.drawString(font, Component.literal("§7数据键:"),
                        panelX + 20, panelY + 34, 0xE0E0E0, false);
                graphics.drawString(font, Component.literal("§8输入 data_variables 的 key，✓ 确认或按 Enter"),
                        panelX + 20, panelY + 58, 0x808080, false);
                int fmtY = panelY + 74;
                drawDataFormatButton(graphics, panelX + 20, fmtY, 90, 18,
                        "当前值", !dataFormatIsTotal, mouseX, mouseY);
                drawDataFormatButton(graphics, panelX + 110, fmtY, 110, 18,
                        "当前值/总值", dataFormatIsTotal, mouseX, mouseY);
            } else {
                graphics.drawString(font, Component.literal("§7内容:"),
                        panelX + 20, panelY + 30, 0xE0E0E0, false);
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        if (showModuleConfig && editingModule != null && editingModule.type() == ModuleSystem.ModuleType.INVENTORY && inventoryModulePopup != null) {
            inventoryModulePopup.renderSuggestionPopup(graphics, font);
        }
    }

    private void renderTabBody(GuiGraphics graphics, CompoundTag tab) {
        // ── Module system v0.1.4-alpha ──
        List<ModuleSystem.Module> modules = readModules(tab);
        int originX = tabLeftEdge();
        int originY = tabContainer.getSeparatorY() + 5;
        renderModules(graphics, font, originX, originY, modules, hoveredModule,
                showModuleConfig || showModuleMenu ? -1 : draggedModule, guiDataVariables());
    }

    private void saveCurrentEditValues() {
        if (tabContainer == null) return;
        CompoundTag activeTab = tabContainer.getActiveTabData();
        if (activeTab == null) return;
        flushModuleConfigEdit();
    }

    private void flushModuleConfigEdit() {
        if (!showModuleConfig || configModuleIdx < 0 || tabContainer == null) return;
        CompoundTag activeTab = tabContainer.getActiveTabData();
        if (activeTab == null) return;
        List<ModuleSystem.Module> modules = readModules(activeTab);
        if (configModuleIdx >= modules.size()) return;
        if (editingModule != null && configModuleIdx < modules.size()) {
            modules.set(configModuleIdx, new ModuleSystem.Module(
                    editingModule.type(),
                    editingModule.x(),
                    editingModule.y(),
                    editingModule.config().copy()));
        }
        ModuleSystem.Module m = modules.get(configModuleIdx);
        if (m.type() == ModuleSystem.ModuleType.INVENTORY && inventoryModulePopup != null) {
            if (!inventoryModulePopup.inputsValid()) {
                return;
            }
            inventoryModulePopup.commitToConfig();
        }
        if (moduleTextEdit != null) {
            if (m.type() == PLAIN_TEXT) {
                m.config().putString("text", moduleTextEdit.getValue());
            } else if (m.type() == ModuleSystem.ModuleType.DATA) {
                String key = moduleTextEdit.getValue().trim();
                if (!isDataKeyAvailable(activeTab, key)) {
                    dataKeyValid = false;
                    return;
                }
                m.config().putString("data_key", key);
                dataKeyValid = true;
                savedDataKey = key;
            }
        }
        writeModules(activeTab, modules);
    }

    /** Open native file picker on background thread and copy selected PNG to resources/ folder. */
    private void pickImageNative() {
        new Thread(() -> {
            String filename = NativeFilePicker.pickAndCopyPngToResources();
            if (filename != null && minecraft != null) {
                minecraft.execute(() -> {
                    if (editingModule != null) editingModule.config().putString("image_file", filename);
                });
            }
        }, "CTF-ImagePicker").start();
    }

    private void closeModuleConfig() {
        closeModuleConfig(true);
    }

    private void closeModuleConfig(boolean flush) {
        if (flush) {
            flushModuleConfigEdit();
        }
        showModuleConfig = false;
        configModuleIdx = -1;
        moduleTextEdit = null;
        moduleSaveBtn = null;
        editingModule = null;
        inventoryModulePopup = null;
        inventoryPopupSearchText = "";
        inventoryPopupAddText = "";
        inventoryPopupScrollOffset = 0;
        dataKeyValid = true;
        savedDataKey = null;
        init();
    }

    private void refreshInventoryModulePopup() {
        if (!showModuleConfig || editingModule == null || editingModule.type() != ModuleSystem.ModuleType.INVENTORY || inventoryModulePopup == null) {
            return;
        }
        inventoryPopupSearchText = inventoryModulePopup.currentSearchText();
        inventoryPopupAddText = inventoryModulePopup.currentAddText();
        inventoryPopupScrollOffset = inventoryModulePopup.currentScrollOffset();
        saveCurrentEditValues();
        openModuleConfig(configModuleIdx);
        if (inventoryModulePopup != null) {
            inventoryModulePopup.focusSearchBox();
        }
    }

    /** Validate data_key against available data_variables from gui data. */
    private void validateDataKey(CompoundTag activeTab, int idx) {
        if (activeTab == null) return;
        var modules = readModules(activeTab);
        if (idx < 0 || idx >= modules.size()) return;
        String key = moduleTextEdit != null ? moduleTextEdit.getValue().trim() : "";
        if (isDataKeyAvailable(activeTab, key)) {
            modules.get(idx).config().putString("data_key", key);
            writeModules(activeTab, modules);
            dataKeyValid = true;
            savedDataKey = key;
        } else {
            dataKeyValid = false;
        }
    }

    private void openModuleConfig(int idx) {
        if (tabContainer == null) return;
        CompoundTag activeTab = tabContainer.getActiveTabData();
        if (activeTab == null) return;
        List<ModuleSystem.Module> modules = readModules(activeTab);
        if (idx < 0 || idx >= modules.size()) return;

        showModuleMenu = false;
        showModuleConfig = true;
        configModuleIdx = idx;

        clearWidgets();
        ModuleSystem.Module m = modules.get(idx);
        editingModule = m;

            int panelW = moduleConfigPanelWidth();
            int panelX = (width - panelW) / 2;
            int panelH = moduleConfigPanelHeight(m);
            int panelY = (height - panelH) / 2;

        if (m.type() == ModuleSystem.ModuleType.INVENTORY) {
            moduleTextEdit = null;
            inventoryModulePopup = new InventoryModuleConfigPopup(m.config(), isOp, this::refreshInventoryModulePopup);
            inventoryModulePopup.setTransientState(inventoryPopupSearchText, inventoryPopupAddText, inventoryPopupScrollOffset);
            inventoryModulePopup.init(this, font, panelX, panelY, panelW, panelH);
            for (var widget : inventoryModulePopup.widgets()) {
                addRenderableWidget(widget);
            }

            int btY = panelY + panelH - 28;
            if (isOp) {
                moduleSaveBtn = Button.builder(Component.literal("确定"), b -> {
                    if (inventoryModulePopup != null) {
                        inventoryModulePopup.commitToConfig();
                    }
                    flushModuleConfigEdit(); closeModuleConfig();
                }).bounds(panelX + panelW / 2 - 50, btY, 100, 20).build();
                addRenderableWidget(moduleSaveBtn);

                addRenderableWidget(Button.builder(Component.literal("删除"), b -> {
                    List<ModuleSystem.Module> mods = readModules(activeTab);
                    if (configModuleIdx >= 0 && configModuleIdx < mods.size()) {
                        mods.remove(configModuleIdx); writeModules(activeTab, mods);
                    }
                    closeModuleConfig(false);
                }).bounds(panelX + 20, btY, 60, 20).build());
            } else {
                addRenderableWidget(Button.builder(Component.literal("关闭"), b -> closeModuleConfig())
                        .bounds(panelX + panelW / 2 - 30, btY, 60, 20).build());
            }
            return;
        } else if (m.type() == ModuleSystem.ModuleType.IMAGE) {
            moduleTextEdit = null;
            int chooseY = panelY + 34;
            addRenderableWidget(Button.builder(Component.literal("选择文件"), b -> pickImageNative())
                    .bounds(panelX + panelW - 100, chooseY, 80, 18).build());

            int previewW = imagePreviewWidth(panelW);
            int previewH = imagePreviewHeight(panelH);
            int maxWidth = Math.max(1, previewW - 8);
            int maxHeight = Math.max(1, previewH - 8);
            int initWidth = clampSliderValue(m.config().contains("image_width") ? m.config().getInt("image_width") : 80, maxWidth);
            int initHeight = clampSliderValue(m.config().contains("image_height") ? m.config().getInt("image_height") : 80, maxHeight);
            editingModule.config().putInt("image_width", initWidth);
            editingModule.config().putInt("image_height", initHeight);
            int sliderX = panelX + 172;
            int sliderW = Math.max(100, panelW - 192);

            addRenderableWidget(new AbstractSliderButton(sliderX, panelY + 72, sliderW, 20,
                    Component.literal("宽度: " + initWidth), maxWidth > 0 ? initWidth / (double) maxWidth : 0.0D) {
                @Override
                protected void updateMessage() {
                    setMessage(Component.literal("宽度: " + clampSliderValue((int) Math.round(value * maxWidth), maxWidth)));
                }

                @Override
                protected void applyValue() {
                    if (editingModule != null) {
                        editingModule.config().putInt("image_width", clampSliderValue((int) Math.round(value * maxWidth), maxWidth));
                    }
                }
            });

            addRenderableWidget(new AbstractSliderButton(sliderX, panelY + 108, sliderW, 20,
                    Component.literal("高度: " + initHeight), maxHeight > 0 ? initHeight / (double) maxHeight : 0.0D) {
                @Override
                protected void updateMessage() {
                    setMessage(Component.literal("高度: " + clampSliderValue((int) Math.round(value * maxHeight), maxHeight)));
                }

                @Override
                protected void applyValue() {
                    if (editingModule != null) {
                        editingModule.config().putInt("image_height", clampSliderValue((int) Math.round(value * maxHeight), maxHeight));
                    }
                }
            });

            // 确定 + 删除 at bottom
            int btY = panelY + panelH - 28;
            moduleSaveBtn = Button.builder(Component.literal("确定"), b -> {
                flushModuleConfigEdit(); closeModuleConfig();
            }).bounds(panelX + panelW / 2 - 50, btY, 100, 20).build();
            addRenderableWidget(moduleSaveBtn);

            addRenderableWidget(Button.builder(Component.literal("删除"), b -> {
                List<ModuleSystem.Module> mods = readModules(activeTab);
                if (configModuleIdx >= 0 && configModuleIdx < mods.size()) {
                    mods.remove(configModuleIdx); writeModules(activeTab, mods);
                }
                closeModuleConfig(false);
            }).bounds(panelX + 20, btY, 60, 20).build());

        } else if (m.type() == ModuleSystem.ModuleType.DATA) {
            int rowY = panelY + 48;
            EditBox dataKeyEdit = new EditBox(font, panelX + 20, rowY, panelW - 80, 18,
                    Component.literal("数据键名称"));
            moduleTextEdit = dataKeyEdit;
            dataKeyEdit.setMaxLength(Integer.MAX_VALUE);
            dataKeyEdit.setValue(m.config().getString("data_key"));
            savedDataKey = m.config().getString("data_key");
            dataKeyValid = isDataKeyAvailable(activeTab, savedDataKey);
            addRenderableWidget(dataKeyEdit);
            dataKeyEdit.setResponder(val ->
                    dataKeyEdit.setTextColor(isDataKeyAvailable(activeTab, val.trim()) ? 0xE0E0E0 : 0xFFFF3333));
            dataKeyEdit.setTextColor(dataKeyValid ? 0xE0E0E0 : 0xFFFF3333);

            addRenderableWidget(Button.builder(Component.literal("✓"), b -> {
                validateDataKey(activeTab, configModuleIdx);
                dataKeyEdit.setTextColor(dataKeyValid ? 0xE0E0E0 : 0xFFFF3333);
            }).bounds(panelX + panelW - 55, rowY, 24, 18).build());

            addRenderableWidget(Button.builder(Component.literal("✗"), b -> {
                dataKeyEdit.setValue(savedDataKey != null ? savedDataKey : "");
                dataKeyValid = true;
                dataKeyEdit.setTextColor(0xE0E0E0);
            }).bounds(panelX + panelW - 28, rowY, 24, 18).build());

            String curFmt2 = m.config().getString("data_format");
            if (curFmt2 == null || curFmt2.isBlank()) {
                curFmt2 = "value";
                m.config().putString("data_format", curFmt2);
            }
            dataFormatIsTotal = "value/total".equals(curFmt2);

            // 确定 + 删除 at bottom
            int btY = panelY + panelH - 28;
            moduleSaveBtn = Button.builder(Component.literal("确定"), b -> {
                validateDataKey(activeTab, configModuleIdx);
                if (!dataKeyValid) return;
                flushModuleConfigEdit(); closeModuleConfig();
            }).bounds(panelX + panelW / 2 - 50, btY, 100, 20).build();
            addRenderableWidget(moduleSaveBtn);

            addRenderableWidget(Button.builder(Component.literal("删除"), b -> {
                List<ModuleSystem.Module> mods = readModules(activeTab);
                if (configModuleIdx >= 0 && configModuleIdx < mods.size()) {
                    mods.remove(configModuleIdx); writeModules(activeTab, mods);
                }
                closeModuleConfig(false);
            }).bounds(panelX + 20, btY, 60, 20).build());

        } else {
            moduleTextEdit = new EditBox(font, panelX + 20, panelY + 48, panelW - 40, 18,
                    Component.literal("模块内容"));
            moduleTextEdit.setMaxLength(Integer.MAX_VALUE);
            moduleTextEdit.setValue(m.config().getString("text"));
            addRenderableWidget(moduleTextEdit);

            int btY = panelY + 80;
            moduleSaveBtn = Button.builder(Component.literal("确定"), b -> {
                flushModuleConfigEdit(); closeModuleConfig();
            }).bounds(panelX + panelW / 2 - 50, btY, 100, 20).build();
            addRenderableWidget(moduleSaveBtn);

            addRenderableWidget(Button.builder(Component.literal("删除"), b -> {
                List<ModuleSystem.Module> mods = readModules(activeTab);
                if (configModuleIdx >= 0 && configModuleIdx < mods.size()) {
                    mods.remove(configModuleIdx); writeModules(activeTab, mods);
                }
                closeModuleConfig(false);
            }).bounds(panelX + 20, btY, 60, 20).build());
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (showModuleConfig && editingModule != null && editingModule.type() == ModuleSystem.ModuleType.INVENTORY) {
            if (inventoryModulePopup != null && inventoryModulePopup.keyPressed(keyCode)) {
                return true;
            }
            if (keyCode == 256) {
                closeModuleConfig();
                return true;
            }
            if ((keyCode == 257 || keyCode == 335) && inventoryModulePopup != null) {
                if (inventoryModulePopup.inputsValid()) {
                    flushModuleConfigEdit();
                    closeModuleConfig();
                }
                return true;
            }
        }
        if (showModuleConfig && (keyCode == 257 || keyCode == 335) && editingModule != null
                && editingModule.type() == ModuleSystem.ModuleType.DATA) {
            CompoundTag activeTab = tabContainer != null ? tabContainer.getActiveTabData() : null;
            validateDataKey(activeTab, configModuleIdx);
            if (dataKeyValid) {
                flushModuleConfigEdit();
                closeModuleConfig();
            }
            return true;
        }
        if (showModuleConfig && keyCode == 256) {
            closeModuleConfig();
            return true;
        }
        if (showModuleMenu && keyCode == 256) {
            showModuleMenu = false;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ── Module interaction ──

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showModuleConfig) {
            if (super.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (editingModule != null && editingModule.type() == ModuleSystem.ModuleType.INVENTORY
                    && inventoryModulePopup != null && inventoryModulePopup.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            return handleModuleConfigClick(mouseX, mouseY, button);
        }

        if (showModuleMenu) {
            int menuW = 140;
            int menuItemH = 25;
            int menuItemCount = 4;
            int mx2 = Math.min(menuX, width - menuW);
            int my2 = Math.min(menuY, height - menuItemCount * menuItemH);
            if (button == 0 && mouseX >= mx2 && mouseX <= mx2 + menuW) {
                int relY = (int) mouseY - my2;
                int hitItem = relY / menuItemH;
                if (relY >= 0 && hitItem >= 0 && hitItem < menuItemCount) {
                    if (tabContainer != null) {
                        CompoundTag tab = tabContainer.getActiveTabData();
                        if (tab != null) {
                            ModuleSystem.ModuleType[] types = {PLAIN_TEXT, ModuleSystem.ModuleType.IMAGE, ModuleSystem.ModuleType.DATA, ModuleSystem.ModuleType.INVENTORY};
                            List<ModuleSystem.Module> mods = readModules(tab);
                            ModuleSystem.ModuleType newType = types[hitItem];
                            int newY = mods.isEmpty() ? 0 : mods.get(mods.size() - 1).y() + MODULE_MIN_H + 4;
                            mods.add(new ModuleSystem.Module(newType, 0, newY, ModuleSystem.Module.defaultConfig(newType)));
                            writeModules(tab, mods);
                            if (newType != ModuleSystem.ModuleType.PLAIN_TEXT) {
                                openModuleConfig(mods.size() - 1);
                            }
                            showModuleMenu = false;
                        }
                    }
                    return true;
                }
            }
            showModuleMenu = false;
            return true;
        }

        if (tabContainer != null) {
            CompoundTag activeTab = tabContainer.getActiveTabData();
            if (activeTab != null && isOp) {
                int originX = tabLeftEdge();
                int originY = tabContainer.getSeparatorY() + 5;
                // Only interact when clicking within the right panel tab body (not left panel, not tab bar)
                if (mouseX < originX || mouseY < originY) {
                    return super.mouseClicked(mouseX, mouseY, button);
                }
                List<ModuleSystem.Module> modules = readModules(activeTab);
                int relX = (int) mouseX - originX;
                int relY = (int) mouseY - originY;
                int hit = hitTest(modules, relX, relY, font, guiDataVariables());

                if (button == 1) {
                    if (hit >= 0) {
                        openModuleConfig(hit);
                    } else {
                        showModuleMenu = true;
                        menuX = (int) mouseX;
                        menuY = (int) mouseY;
                    }
                    return true;
                }

                if (button == 0 && hit >= 0) {
                    draggedModule = hit;
                    dragStartX = (int) mouseX;
                    dragStartY = (int) mouseY;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggedModule >= 0 && tabContainer != null && isOp) {
            CompoundTag activeTab = tabContainer.getActiveTabData();
            if (activeTab != null) {
                List<ModuleSystem.Module> modules = readModules(activeTab);
                if (draggedModule < modules.size()) {
                    ModuleSystem.Module m = modules.get(draggedModule);
                    int originX = tabLeftEdge();
                    int originY = tabContainer.getSeparatorY() + 5;
                    int newX = (int) mouseX - originX - (dragStartX - m.x() - originX);
                    int newY = (int) mouseY - originY - (dragStartY - m.y() - originY);
                    newX = Math.max(0, newX);
                    newY = Math.max(0, newY);
                    modules.set(draggedModule, new ModuleSystem.Module(m.type(), newX, newY, m.config().copy()));
                    writeModules(activeTab, modules);
                    dragStartX = (int) mouseX;
                    dragStartY = (int) mouseY;
                }
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggedModule >= 0) {
            draggedModule = -1;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (showModuleConfig && editingModule != null && editingModule.type() == ModuleSystem.ModuleType.INVENTORY && inventoryModulePopup != null) {
            if (inventoryModulePopup.mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private CompoundTag getParsedGuiData() {
        if (parsedGuiData == null) {
            try {
                parsedGuiData = net.minecraft.nbt.TagParser.parseTag(guiSnbt);
            } catch (Exception e) {
                parsedGuiData = new CompoundTag();
            }
        }
        return parsedGuiData;
    }

    private Map<String, String> guiDataVariables() {
        CompoundTag dataVars = getParsedGuiData().getCompound("data_variables");
        return DataVariables.toDisplayMap(dataVars);
    }

    private int moduleConfigPanelWidth() {
        return ModuleConfigLayout.panelWidth(width);
    }

    private int moduleConfigPanelHeight(ModuleSystem.Module module) {
        return ModuleConfigLayout.panelHeight(module);
    }

    private int imagePreviewWidth(int panelW) {
        return ModuleConfigLayout.imagePreviewWidth(panelW);
    }

    private int imagePreviewHeight(int panelH) {
        return ModuleConfigLayout.imagePreviewHeight(panelH);
    }

    private int clampSliderValue(int value) {
        return ModuleConfigLayout.clampSliderValue(value);
    }

    private int clampSliderValue(int value, int max) {
        return ModuleConfigLayout.clampSliderValue(value, max);
    }

    private boolean isDataKeyAvailable(CompoundTag activeTab, String key) {
        if (activeTab == null || key == null || key.isBlank()) {
            return false;
        }
        CompoundTag dataVars = getParsedGuiData().getCompound("data_variables");
        return dataVars.contains(key);
    }

    private void drawDataFormatButton(GuiGraphics graphics, int x, int y, int w, int h,
                                      String label, boolean active, int mouseX, int mouseY) {
        int bgColor = active ? 0xFF4CAF50 : 0xFF4A4A5A;
        if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
            bgColor = active ? 0xFF66BB6A : 0xFF666677;
        }
        graphics.fill(x, y, x + w, y + h, bgColor);
        graphics.renderOutline(x, y, w, h, active ? 0xFFFFFFFF : 0xFFB0B0B0);
        graphics.drawCenteredString(font, Component.literal(label), x + w / 2, y + 4,
                active ? 0xFFB2FF59 : 0xFFFFFFFF);
    }

    private boolean handleModuleConfigClick(double mouseX, double mouseY, int button) {
        if (!showModuleConfig || button != 0 || editingModule == null) return false;
        if (editingModule.type() != ModuleSystem.ModuleType.DATA) return false;
        int panelW = moduleConfigPanelWidth();
        int panelH = moduleConfigPanelHeight(editingModule);
        int panelX = (width - panelW) / 2;
        int panelY = (height - panelH) / 2;
        int fmtY = panelY + 74;
        if (mouseX >= panelX + 20 && mouseX <= panelX + 110 && mouseY >= fmtY && mouseY <= fmtY + 18) {
            dataFormatIsTotal = false;
            editingModule.config().putString("data_format", "value");
            return true;
        }
        if (mouseX >= panelX + 110 && mouseX <= panelX + 220 && mouseY >= fmtY && mouseY <= fmtY + 18) {
            dataFormatIsTotal = true;
            editingModule.config().putString("data_format", "value/total");
            return true;
        }
        return false;
    }

    private void save() {
        if (!isOp) return;

        saveCurrentEditValues();

        CompoundTag output = TabContainer.serializeTabsToCompound(tabs);
        CompoundTag dataVars = getParsedGuiData().getCompound("data_variables");
        if (!dataVars.isEmpty()) {
            output.put("data_variables", dataVars);
        }

        CtfNetwork.CHANNEL.sendToServer(new C2SSaveGeneralConfigPacket(output.getAsString()));
        saveMessage = "保存请求已发送";
        saveMessageTicks = 0;
    }
}

