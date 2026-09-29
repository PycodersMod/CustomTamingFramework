package com.example.customtamingframework.client.screen;

import com.example.customtamingframework.network.C2SImportGeneralToPetPacket;
import com.example.customtamingframework.network.C2SRequestOpenPetPermissionScreenPacket;
import com.example.customtamingframework.network.C2SRequestOpenModuleInventoryPacket;
import com.example.customtamingframework.network.C2SSavePetSettingPacket;
import com.example.customtamingframework.network.C2SSetEntityBaseNamePacket;
import com.example.customtamingframework.network.C2SSyncPetInventoryOverlayPacket;
import com.example.customtamingframework.network.CtfNetwork;
import com.example.customtamingframework.config.CtfUiSettings;
import com.example.customtamingframework.pet.DataVariables;
import com.example.customtamingframework.pet.ModuleInventoryState;
import com.example.customtamingframework.pet.PetProgress;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.TreeMap;
import java.util.function.Consumer;

import static com.example.customtamingframework.client.screen.ModuleSystem.readModules;
import static com.example.customtamingframework.client.screen.ModuleSystem.writeModules;
import static com.example.customtamingframework.client.screen.ModuleSystem.renderModules;
import static com.example.customtamingframework.client.screen.ModuleSystem.hitTest;
import static com.example.customtamingframework.client.screen.ModuleSystem.ModuleType.PLAIN_TEXT;
import static com.example.customtamingframework.client.screen.ModuleSystem.MODULE_MIN_H;
import com.mojang.blaze3d.systems.RenderSystem;

public final class PetScreen extends Screen {
    private int dividerX() {
        return width / 3;  // 1:2 ratio (left 1/3, right 2/3)
    }

    private final int entityId;
    private final String entityTypeString;
    private final int evolutionPoints;
    private final List<String> unlockedSkills;
    private final String equipmentSummary;
    private final boolean isOp;
    private final String guiSnbt;
    private final CompoundTag uiSettings;
    private String ownerPlayerUuid;
    private String ownerPlayerName;
    private PetProgress.AccessMode accessMode;
    private final String nickname;

    private List<CompoundTag> tabs;
    private boolean tabsLoaded;
    private TabContainer tabContainer;
    private EditBox tabTitleEdit;
    private EditBox nicknameEdit;
    private EditBox nameTagEdit;
    private String saveMessage;
    private int saveMessageTicks;
    private boolean showImportConfirm;
    private String currentBaseName;
    private float currentHealth;
    private float maxHealth;
    private float armor;
    private Map<String, DataVariables.VariableState> dataVariables;
    private Map<String, List<ItemStack>> moduleInventories;
    private CompoundTag parsedGuiData;

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
    private ModuleSystem.Module editingModule; // reference to module being configured
    private InventoryModuleConfigPopup inventoryModulePopup;
    private String inventoryPopupSearchText = "";
    private String inventoryPopupAddText = "";
    private int inventoryPopupScrollOffset;
    private InventoryOverlayState inventoryOverlay;
    private int inventoryOverlayCandidate = -1;
    private boolean inventoryOverlayDragged;
    // ── DATA module config fields ──
    private boolean dataKeyValid;              // whether current data_key exists in data_variables
    private     boolean dataFormatIsTotal;
    private String savedDataKey;         // true = "当前值/总值", false = "当前值"

    private static final int MODULE_DRAG_THRESHOLD = 4;

    public PetScreen(int entityId, String entityTypeString, int evolutionPoints,
                     List<String> unlockedSkills, String equipmentSummary, boolean isOp,
                     String ownerPlayerUuid, String ownerPlayerName, String accessMode,
                     String guiSnbt, Map<String, DataVariables.VariableState> dataVariables,
                     CompoundTag moduleInventories,
                     CompoundTag uiSettings,
                     String nickname, String baseName,
                     float currentHealth, float maxHealth, float armor) {
        super(buildTitle(nickname, entityTypeString, accessMode));
        this.entityId = entityId;
        this.entityTypeString = entityTypeString;
        this.evolutionPoints = evolutionPoints;
        this.unlockedSkills = unlockedSkills;
        this.equipmentSummary = equipmentSummary;
        this.isOp = isOp;
        this.ownerPlayerUuid = ownerPlayerUuid != null ? ownerPlayerUuid : "";
        this.ownerPlayerName = ownerPlayerName != null ? ownerPlayerName : "";
        this.accessMode = PetProgress.AccessMode.fromString(accessMode);
        this.guiSnbt = guiSnbt;
        this.uiSettings = CtfUiSettings.sanitize(uiSettings);
        this.nickname = nickname;
        this.dataVariables = dataVariables != null ? new TreeMap<>(dataVariables) : new TreeMap<>();
        this.moduleInventories = parseModuleInventories(moduleInventories);
        this.showImportConfirm = false;
        this.currentBaseName = baseName != null ? baseName : "";
        this.currentHealth = currentHealth;
        this.maxHealth = maxHealth;
        this.armor = armor;
    }

    /** Called by the S2C sync packet to update real-time data without re-creating the screen. */
    public void syncData(float currentHealth, float maxHealth, float armor) {
        this.currentHealth = currentHealth;
        this.maxHealth = maxHealth;
        this.armor = armor;
    }

    public void syncDataVariables(Map<String, DataVariables.VariableState> vars) {
        this.dataVariables = vars != null ? new TreeMap<>(vars) : new TreeMap<>();
    }

    public void syncModuleInventories(CompoundTag inventories) {
        this.moduleInventories = parseModuleInventories(inventories);
    }

    public void syncModuleInventoryPreview(String moduleId, List<ItemStack> stacks) {
        if (moduleId == null || moduleId.isBlank()) {
            return;
        }
        List<ItemStack> copy = new ArrayList<>();
        if (stacks != null) {
            for (ItemStack stack : stacks) {
                copy.add(stack != null ? stack.copy() : ItemStack.EMPTY);
            }
        }
        moduleInventories.put(moduleId, copy);
    }

    private void syncModuleInventoryPreviewToConfig(ModuleSystem.Module module) {
        if (module == null || !ModuleSystem.isInventoryModule(module)) {
            return;
        }
        String moduleId = ModuleSystem.moduleId(module);
        int size = ModuleSystem.inventorySlotCount(module);
        List<ItemStack> current = moduleInventories.get(moduleId);
        List<ItemStack> next = new ArrayList<>(java.util.Collections.nCopies(size, ItemStack.EMPTY));
        if (current != null) {
            for (int i = 0; i < Math.min(size, current.size()); i++) {
                ItemStack stack = current.get(i);
                next.set(i, stack != null ? stack.copy() : ItemStack.EMPTY);
            }
        }
        moduleInventories.put(moduleId, next);
    }

    private Map<String, String> runtimeDataVariables() {
        if (isOp) {
            CompoundTag dataVars = getParsedGuiData().getCompound("data_variables");
            return DataVariables.toDisplayMap(dataVars);
        }
        return DataVariables.toDisplayMap(dataVariables);
    }

    private Map<String, List<ItemStack>> runtimeModuleInventories() {
        return moduleInventories != null ? moduleInventories : Map.of();
    }

    private Map<String, List<ItemStack>> parseModuleInventories(CompoundTag inventoriesTag) {
        Map<String, List<ItemStack>> result = new TreeMap<>();
        if (inventoriesTag == null) {
            return result;
        }
        for (String moduleId : inventoriesTag.getAllKeys()) {
            if (moduleId == null || moduleId.isBlank() || !inventoriesTag.contains(moduleId, 10)) {
                continue;
            }
            CompoundTag inv = inventoriesTag.getCompound(moduleId);
            int size = Math.max(0, inv.contains("size") ? inv.getInt("size") : 0);
            List<ItemStack> stacks = new ArrayList<>(java.util.Collections.nCopies(size, ItemStack.EMPTY));
            if (inv.contains("items", 9)) {
                var list = inv.getList("items", 10);
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag slot = list.getCompound(i);
                    int index = slot.getInt("slot");
                    if (index < 0 || index >= stacks.size()) {
                        continue;
                    }
                    if (slot.contains("stack", 10)) {
                        stacks.set(index, ItemStack.of(slot.getCompound("stack")));
                    }
                }
            }
            result.put(moduleId, stacks);
        }
        return result;
    }

    private static Component buildTitle(String nickname, String entityTypeStr, String accessMode) {
        String translated = Component.translatable("entity." + entityTypeStr.replace(':', '.')).getString();
        PetProgress.AccessMode mode = PetProgress.AccessMode.fromString(accessMode);
        MutableComponent title = Component.literal("");
        title.append(Component.literal("CTF").withStyle(ChatFormatting.BOLD, ChatFormatting.ITALIC, ChatFormatting.GOLD));
        title.append(Component.literal(" 宠物界面"));
        title.append(Component.literal("（" + mode.displayName() + "）").withStyle(switch (mode) {
            case OWNER -> ChatFormatting.GOLD;
            case SECRET -> ChatFormatting.DARK_RED;
            case EDITABLE -> ChatFormatting.GREEN;
            case READ_ONLY -> ChatFormatting.GRAY;
        }));
        title.append(Component.literal("-"));
        title.append(Component.literal(nickname != null && !nickname.isBlank() ? nickname : "未命名").withStyle(ChatFormatting.AQUA));
        title.append(Component.literal("（" + translated + "）").withStyle(ChatFormatting.GRAY));
        return title;
    }

    private Component currentTitle() {
        return buildTitle(nickname, entityTypeString, accessMode.snbtKey());
    }

    public boolean matchesEntity(int id) {
        return this.entityId == id;
    }

    public void syncAccessState(String ownerPlayerUuid, String ownerPlayerName, String accessMode) {
        this.ownerPlayerUuid = ownerPlayerUuid != null ? ownerPlayerUuid : "";
        this.ownerPlayerName = ownerPlayerName != null ? ownerPlayerName : "";
        this.accessMode = PetProgress.AccessMode.fromString(accessMode);
    }

    public boolean isOpPlayer() {
        return isOp;
    }

    public void refreshLayout() {
        if (minecraft != null) {
            init();
        }
    }

    private String resolveOwnerDisplayName(String ownerUuid) {
        String storedName = ownerPlayerName == null ? "" : ownerPlayerName.trim();
        if (!storedName.isBlank() && !looksLikeUuid(storedName) && !storedName.equals(ownerUuid)) {
            return storedName;
        }
        String resolved = resolveOwnerDisplayNameByUuid(ownerUuid);
        if (!resolved.isBlank()) {
            return resolved;
        }
        if (!storedName.isBlank() && !looksLikeUuid(storedName)) {
            return storedName;
        }
        if (ownerUuid != null && !ownerUuid.isBlank() && !looksLikeUuid(ownerUuid)) {
            return ownerUuid;
        }
        return "未知";
    }

    private String resolveOwnerDisplayNameByUuid(String ownerUuid) {
        if (ownerUuid == null || ownerUuid.isBlank() || !looksLikeUuid(ownerUuid)) {
            return "";
        }
        try {
            UUID uuid = UUID.fromString(ownerUuid);
            if (minecraft != null) {
                if (minecraft.player != null && uuid.equals(minecraft.player.getUUID())) {
                    String localName = minecraft.player.getGameProfile().getName();
                    if (localName != null && !localName.isBlank()) {
                        return localName;
                    }
                }
                if (minecraft.level != null) {
                    var levelPlayer = minecraft.level.getPlayerByUUID(uuid);
                    if (levelPlayer != null) {
                        String levelName = levelPlayer.getGameProfile().getName();
                        if (levelName != null && !levelName.isBlank()) {
                            return levelName;
                        }
                    }
                    for (var onlinePlayer : minecraft.level.players()) {
                        if (onlinePlayer != null && uuid.equals(onlinePlayer.getUUID())) {
                            String levelName = onlinePlayer.getGameProfile().getName();
                            if (levelName != null && !levelName.isBlank()) {
                                return levelName;
                            }
                        }
                    }
                }
                var connection = minecraft.getConnection();
                if (connection != null) {
                    var info = connection.getPlayerInfo(uuid);
                    if (info != null && info.getProfile() != null) {
                        String networkName = info.getProfile().getName();
                        if (networkName != null && !networkName.isBlank()) {
                            return networkName;
                        }
                    }
                }
            }
        } catch (IllegalArgumentException ignored) {
            return "";
        }
        return "";
    }

    private boolean looksLikeUuid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            UUID.fromString(value.trim());
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private int tabLeftEdge() {
        return dividerX() + 10;
    }

    @Override
    protected void init() {
        clearWidgets();

        if (showImportConfirm) {
            initConfirmDialog();
            return;
        }

        ModuleSystem.setResourcesDir(com.example.customtamingframework.config.CtfSnbtConfig.resourcesDirectory());

        if (!tabsLoaded) {
            parseTabs();
            tabsLoaded = true;
        }
        if (tabs == null) tabs = new ArrayList<>();
        if (tabs.isEmpty()) {
            CompoundTag blankTab = new CompoundTag();
            blankTab.putString("id", "blank");
            blankTab.putString("title", "未命名标签页");
            blankTab.putBoolean("enabled", true);
            blankTab.putString("summary", "");
            blankTab.putInt("order", 0);
            tabs.add(blankTab);
        }

        // TabContainer confined to the right panel, at the top
        if (tabContainer == null) {
            tabContainer = new TabContainer(tabs, isOp, 52, 75, tabLeftEdge());
        }

        // Left panel: nametag edit (always accessible, non-OP too) then nickname edit
        int leftPanelX = 15;
        int leftEditWidth = dividerX() - 25;
        int editY = 58;

        if (isOp || accessMode != PetProgress.AccessMode.SECRET) {
            addRenderableWidget(Button.builder(Component.literal("宠物权限管理"), b -> {
                if (minecraft != null) {
                    CtfNetwork.CHANNEL.sendToServer(new C2SRequestOpenPetPermissionScreenPacket(entityId));
                }
            }).bounds(leftPanelX, 18, leftEditWidth, 16).build());
        }

        nameTagEdit = new EditBox(font, leftPanelX, editY, leftEditWidth, 18, Component.literal("命名牌名称"));
        nameTagEdit.setValue(currentBaseName);
        nameTagEdit.setResponder(val -> {
            currentBaseName = val;
            CtfNetwork.CHANNEL.sendToServer(new C2SSetEntityBaseNamePacket(entityId, val));
        });
        addRenderableWidget(nameTagEdit);
        editY += 40;

        if (isOp) {
            nicknameEdit = new EditBox(font, leftPanelX, editY, leftEditWidth, 18, Component.literal("宠物昵称"));
            nicknameEdit.setValue(nickname);
            nicknameEdit.setEditable(true);
            addRenderableWidget(nicknameEdit);
        } else {
            nicknameEdit = null;
        }

        // Right panel: tab widgets (confined to right side)
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

        // Bottom buttons: side by side
        int btnW = 90;
        int btnSpacing = 10;
        int btnY = height - 30;
        int opBtnCount = 4; // 宠物数据 + 导入 + 保存配置 + 关闭
        boolean showDataListButton = isOp || CtfUiSettings.visibleForNonOp(uiSettings, CtfUiSettings.SHOW_NON_OP_DATA_LIST_BUTTON);
        int nonOpBtnCount = (showDataListButton ? 1 : 0) + 1;
        int buttonCount = isOp ? opBtnCount : nonOpBtnCount;
        int totalW = buttonCount * btnW + (buttonCount - 1) * btnSpacing;
        int btnStartX = (width - totalW) / 2;

        if (isOp) {
            addRenderableWidget(Button.builder(
                    Component.literal("宠物数据"),
                    button -> openConfiguredDataList()
            ).bounds(btnStartX, btnY, btnW, 20).build());

            addRenderableWidget(Button.builder(
                    Component.literal("← 导入"),
                    button -> {
                        showImportConfirm = true;
                        init();
                    }
            ).bounds(btnStartX + btnW + btnSpacing, btnY, btnW, 20).build());

            addRenderableWidget(Button.builder(
                    Component.literal("保存配置"),
                    button -> save()
            ).bounds(btnStartX + 2 * (btnW + btnSpacing), btnY, btnW, 20).build());

            addRenderableWidget(Button.builder(Component.literal("关闭"), button -> onClose())
                    .bounds(btnStartX + 3 * (btnW + btnSpacing), btnY, btnW, 20).build());
        } else {
            int btnCurX = btnStartX;
            if (showDataListButton) {
                addRenderableWidget(Button.builder(
                        Component.literal("宠物数据"),
                        button -> openRuntimeDataList()
                ).bounds(btnCurX, btnY, btnW, 20).build());
                btnCurX += btnW + btnSpacing;
            }

            addRenderableWidget(Button.builder(Component.literal("关闭"), button -> onClose())
                    .bounds(btnCurX, btnY, btnW, 20).build());
        }
    }

    private void openConfiguredDataList() {
        CompoundTag dataTag = getParsedGuiData().getCompound("data_variables");
        Map<String, DataVariables.VariableState> vars = DataVariables.fromCompoundStates(dataTag);
        Consumer<Map<String, DataVariables.VariableState>> onSave = saved -> {
            CompoundTag root = getParsedGuiData();
            root.put("data_variables", DataVariables.toCompound(saved));
            parsedGuiData = root;
            dataVariables = new TreeMap<>(saved);
            save();
        };
        if (minecraft != null) {
            minecraft.setScreen(new DataListScreen(vars, onSave, this, uiSettings));
        }
    }

    private void openRuntimeDataList() {
        if (minecraft != null) {
            minecraft.setScreen(new DataListScreen(dataVariables, saved -> {
            }, this, uiSettings));
        }
    }

    private void initConfirmDialog() {
        int panelW = 260;
        int panelH = 100;
        int panelX = (width - panelW) / 2;
        int panelY = (height - panelH) / 2;

        addRenderableWidget(Button.builder(Component.literal("是"), b -> {
            showImportConfirm = false;
            CtfNetwork.CHANNEL.sendToServer(new C2SImportGeneralToPetPacket(entityId, entityTypeString));
            saveMessage = "导入请求已发送";
            saveMessageTicks = 0;
        }).bounds(panelX + 40, panelY + 60, 60, 20).build());

        addRenderableWidget(Button.builder(Component.literal("否"), b -> {
            showImportConfirm = false;
            init();
        }).bounds(panelX + panelW - 100, panelY + 60, 60, 20).build());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (inventoryOverlayOpen()) {
            if (keyCode == 256) {
                closeInventoryOverlay();
                return true;
            }
            return true;
        }
        if (showImportConfirm && keyCode == 256) {
            showImportConfirm = false;
            init();
            return true;
        }
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
                    flushModuleConfigEdit();
                    closeModuleConfig();
                }).bounds(panelX + panelW / 2 - 50, btY, 100, 20).build();
                addRenderableWidget(moduleSaveBtn);

                addRenderableWidget(Button.builder(Component.literal("删除"), b -> {
                    moduleInventories.remove(ModuleSystem.moduleId(m));
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

            int btY = panelY + panelH - 28;
            addRenderableWidget(Button.builder(Component.literal("确定"), b -> {
                flushModuleConfigEdit(); closeModuleConfig();
            }).bounds(panelX + panelW / 2 - 50, btY, 100, 20).build());
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
            dataKeyValid = isDataKeyAvailable(savedDataKey);
            addRenderableWidget(dataKeyEdit);
            dataKeyEdit.setResponder(val ->
                    dataKeyEdit.setTextColor(isDataKeyAvailable(val.trim()) ? 0xE0E0E0 : 0xFFFF3333));
            dataKeyEdit.setTextColor(dataKeyValid ? 0xE0E0E0 : 0xFFFF3333);

            addRenderableWidget(Button.builder(Component.literal("✓"), b -> {
                validateDataKey(activeTab, idx);
                dataKeyEdit.setTextColor(dataKeyValid ? 0xE0E0E0 : 0xFFFF3333);
            }).bounds(panelX + panelW - 55, rowY, 24, 18).build());
            addRenderableWidget(Button.builder(Component.literal("✗"), b -> {
                dataKeyEdit.setValue(savedDataKey != null ? savedDataKey : "");
                dataKeyValid = true;
                dataKeyEdit.setTextColor(0xE0E0E0);
            }).bounds(panelX + panelW - 28, rowY, 24, 18).build());

            int fmtY = rowY + 26;
            String curFmt = m.config().getString("data_format");
            if (curFmt == null || curFmt.isBlank()) {
                curFmt = "value";
                m.config().putString("data_format", curFmt);
            }
            dataFormatIsTotal = "value/total".equals(curFmt);

            int btY = panelY + panelH - 28;
            addRenderableWidget(Button.builder(Component.literal("确定"), b -> {
                validateDataKey(activeTab, idx);
                if (!dataKeyValid) return;
                flushModuleConfigEdit(); closeModuleConfig();
            }).bounds(panelX + panelW / 2 - 50, btY, 100, 20).build());
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
            addRenderableWidget(Button.builder(Component.literal("确定"), b -> {
                flushModuleConfigEdit(); closeModuleConfig();
            }).bounds(panelX + panelW / 2 - 50, btY, 100, 20).build());
            addRenderableWidget(Button.builder(Component.literal("删除"), b -> {
                List<ModuleSystem.Module> mods = readModules(activeTab);
                if (configModuleIdx >= 0 && configModuleIdx < mods.size()) {
                    mods.remove(configModuleIdx); writeModules(activeTab, mods);
                }
                closeModuleConfig(false);
            }).bounds(panelX + 20, btY, 60, 20).build());
        }
    }

    private void parseTabs() {
        tabs = new ArrayList<>();
        try {
            tabs = TabContainer.deserializeTabsFromCompound(getParsedGuiData());
        } catch (Exception e) {
            tabs = new ArrayList<>();
        }
    }

    private CompoundTag getParsedGuiData() {
        if (parsedGuiData == null) {
            try {
                parsedGuiData = guiSnbt == null || guiSnbt.isBlank()
                        ? new CompoundTag()
                        : net.minecraft.nbt.TagParser.parseTag(guiSnbt);
            } catch (Exception e) {
                parsedGuiData = new CompoundTag();
            }
        }
        return parsedGuiData;
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

        if (showImportConfirm) {
            graphics.fill(0, 0, width, height, 0x80000000);
            int panelW = 260;
            int panelH = 100;
            int panelX = (width - panelW) / 2;
            int panelY = (height - panelH) / 2;
            graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFFF0F0F0);
            graphics.renderOutline(panelX, panelY, panelW, panelH, 0xFF333333);
            graphics.drawCenteredString(font,
                    Component.literal("确定将通用配置导入此宠物？"),
                    width / 2, panelY + 20, 0x333333);
            graphics.drawCenteredString(font,
                    Component.literal("当前宠物标签页配置将被覆盖。"),
                    width / 2, panelY + 36, 0x555555);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

        graphics.drawCenteredString(font, currentTitle(), width / 2, 12, 0xFFFFFF);

        // Vertical divider line (separates left and right panels)
        graphics.fill(dividerX(), 30, dividerX() + 1, height - 40, 0xFF808080);

        // Left panel: pet info (always visible, not part of tab system)
        int leftX = 15;
        int ownerY = 34;
        int nameY = 47;
        int titleY = 84;
        int statsY = isOp ? 122 : 102;

        graphics.drawString(font,
                Component.literal("主人：").append(Component.literal(resolveOwnerDisplayName(ownerPlayerUuid)).withStyle(ChatFormatting.AQUA)),
                leftX, ownerY, 0xE0E0E0, false);

        graphics.drawString(font, Component.literal("命名:"), leftX, nameY, 0xE0E0E0, false);
        // nameTagEdit rendered by super.render()

        if (isOp) {
            graphics.drawString(font, Component.literal("头衔:").withStyle(ChatFormatting.AQUA), leftX, titleY, 0xE0E0E0, false);
            // nicknameEdit rendered by super.render()
        } else {
            graphics.drawString(font,
                    Component.literal("头衔: ").append(Component.literal(nickname != null && !nickname.isBlank() ? nickname : "未命名").withStyle(ChatFormatting.AQUA)),
                    leftX, titleY, 0xE0E0E0, false);
        }

        // Health and Armor display
        graphics.drawString(font,
                Component.literal("§c血量: §f" + String.format("%.0f", currentHealth) + " §7/ §c" + String.format("%.0f", maxHealth)),
                leftX, statsY, 0xE0E0E0, false);
        graphics.drawString(font,
                Component.literal("§7护甲: §f" + String.format("%.0f", armor)),
                leftX, statsY + 14, 0xE0E0E0, false);

        // Tab container (separator line only on the right panel side)
        tabContainer.render(graphics, width);

        // Tab body
        CompoundTag activeTab = tabContainer.getActiveTabData();
        if (activeTab != null && !showModuleConfig) {
            renderTabBody(graphics, mouseX, mouseY, activeTab);
        }

        if (saveMessage != null) {
            saveMessageTicks++;
            int alpha = Math.max(0, 255 - saveMessageTicks * 3);
            if (alpha > 0) {
                graphics.drawCenteredString(font, Component.literal("§a" + saveMessage), width / 2, height - 80, 0xFFFFFF | (alpha << 24));
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

        if (showModuleConfig) {
            renderModuleConfigPopup(graphics, mouseX, mouseY);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        if (!showModuleConfig && inventoryOverlayOpen() && inventoryOverlay != null) {
            inventoryOverlay.render(graphics, mouseX, mouseY);
        }
        if (showModuleConfig && editingModule != null && editingModule.type() == ModuleSystem.ModuleType.INVENTORY && inventoryModulePopup != null) {
            inventoryModulePopup.renderSuggestionPopup(graphics, font);
        }
    }

    private void renderModuleConfigPopup(GuiGraphics graphics, int mouseX, int mouseY) {
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
        graphics.fill(0, 0, width, height, 0xB0000000);
        int panelW = moduleConfigPanelWidth();
        int panelH = moduleConfigPanelHeight(editingModule);
        int panelX = (width - panelW) / 2;
        int panelY = (height - panelH) / 2;
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF444466);
        graphics.renderOutline(panelX, panelY, panelW, panelH, 0xFF888888);
        graphics.drawCenteredString(font, Component.literal(configTitle),
                width / 2, panelY + 10, 0xFFFFFF);

        if (editingModule == null) {
            return;
        }
        if (editingModule.type() == ModuleSystem.ModuleType.INVENTORY && inventoryModulePopup != null) {
            inventoryModulePopup.render(graphics, font, mouseX, mouseY, panelX, panelY, panelW, panelH);
            return;
        }
        if (editingModule.type() == ModuleSystem.ModuleType.IMAGE) {
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
                    RenderSystem.setShaderTexture(0, rl);
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
            return;
        }
        if (editingModule.type() == ModuleSystem.ModuleType.DATA) {
            graphics.drawString(font, Component.literal("§7数据键:"),
                    panelX + 20, panelY + 34, 0xE0E0E0, false);
            graphics.drawString(font, Component.literal("§8输入 data_variables 的 key，✓ 确认或按 Enter"),
                    panelX + 20, panelY + 58, 0x808080, false);
            int fmtY = panelY + 74;
            drawDataFormatButton(graphics, panelX + 20, fmtY, 90, 18,
                    "当前值", !dataFormatIsTotal, mouseX, mouseY);
            drawDataFormatButton(graphics, panelX + 110, fmtY, 110, 18,
                    "当前值/总值", dataFormatIsTotal, mouseX, mouseY);
            return;
        }
        graphics.drawString(font, Component.literal("§7内容:"),
                panelX + 20, panelY + 30, 0xE0E0E0, false);
    }

    // ── Module interaction ──

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (inventoryOverlayOpen()) {
            if (inventoryOverlay != null && inventoryOverlay.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
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
                            int newY = mods.isEmpty() ? 0 : mods.get(mods.size() - 1).y() + MODULE_MIN_H + 4;
                            mods.add(new ModuleSystem.Module(types[hitItem], 0, newY, ModuleSystem.Module.defaultConfig(types[hitItem])));
                            writeModules(tab, mods);
                            // Open config popup immediately for non-text modules
                            if (types[hitItem] != ModuleSystem.ModuleType.PLAIN_TEXT) {
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

        // Module interaction (confined to right panel tab body area)
        if (tabContainer != null) {
            CompoundTag activeTab = tabContainer.getActiveTabData();
            if (activeTab != null) {
                int originX = tabLeftEdge();
                int originY = tabContainer.getSeparatorY() + 5;
                if (mouseX < originX || mouseY < originY) {
                    return super.mouseClicked(mouseX, mouseY, button);
                }
                List<ModuleSystem.Module> modules = readModules(activeTab);
                int relX = (int) mouseX - originX;
                int relY = (int) mouseY - originY;
                int hit = hitTest(modules, relX, relY, font, runtimeDataVariables());

                if (hit >= 0 && hit < modules.size()) {
                    ModuleSystem.Module hitModule = modules.get(hit);
                    if (hitModule.type() == ModuleSystem.ModuleType.INVENTORY) {
                        if (button == 1 && isOp) {
                            openModuleConfig(hit);
                            return true;
                        }
                        if (!isOp) {
                            openInventoryOverlay();
                            return true;
                        }
                        if (button == 0) {
                            draggedModule = hit;
                            inventoryOverlayCandidate = hit;
                            inventoryOverlayDragged = false;
                            dragStartX = (int) mouseX;
                            dragStartY = (int) mouseY;
                            return true;
                        }
                    }
                }

                if (isOp) {
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
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (inventoryOverlayOpen()) {
            if (inventoryOverlay != null && inventoryOverlay.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                return true;
            }
        }
        if (button == 0 && draggedModule >= 0 && tabContainer != null && isOp) {
            CompoundTag activeTab = tabContainer.getActiveTabData();
            if (activeTab != null) {
                List<ModuleSystem.Module> modules = readModules(activeTab);
                if (draggedModule < modules.size()) {
                    ModuleSystem.Module m = modules.get(draggedModule);
                    if (ModuleSystem.isInventoryModule(m) && inventoryOverlayCandidate == draggedModule) {
                        int dx = Math.abs((int) mouseX - dragStartX);
                        int dy = Math.abs((int) mouseY - dragStartY);
                        if (Math.max(dx, dy) <= MODULE_DRAG_THRESHOLD) {
                            return true;
                        }
                        inventoryOverlayDragged = true;
                    }
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
        if (inventoryOverlayOpen()) {
            if (inventoryOverlay != null && inventoryOverlay.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        if (button == 0 && inventoryOverlayCandidate >= 0 && draggedModule == inventoryOverlayCandidate) {
            if (isOp && !inventoryOverlayDragged) {
                openInventoryOverlay();
            }
            inventoryOverlayCandidate = -1;
            inventoryOverlayDragged = false;
            draggedModule = -1;
            return true;
        }
        if (button == 0 && draggedModule >= 0) {
            draggedModule = -1;
            inventoryOverlayCandidate = -1;
            inventoryOverlayDragged = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (inventoryOverlayOpen()) {
            if (inventoryOverlay != null && inventoryOverlay.mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
        }
        if (showModuleConfig && editingModule != null && editingModule.type() == ModuleSystem.ModuleType.INVENTORY && inventoryModulePopup != null) {
            if (inventoryModulePopup.mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void renderTabBody(GuiGraphics graphics, int mouseX, int mouseY, CompoundTag tab) {
        // ── Module system v0.1.4-alpha ──
        List<ModuleSystem.Module> modules = readModules(tab);
        int originX = tabLeftEdge();
        int originY = tabContainer.getSeparatorY() + 5;
        renderModules(graphics, font, originX, originY, modules, hoveredModule,
                showModuleConfig || showModuleMenu ? -1 : draggedModule,
                runtimeDataVariables(), runtimeModuleInventories());
    }

    private boolean inventoryOverlayOpen() {
        return inventoryOverlay != null && inventoryOverlay.open;
    }

    private void openInventoryOverlay() {
        if (inventoryOverlayOpen()) {
            return;
        }
        inventoryOverlayCandidate = -1;
        inventoryOverlayDragged = false;
        if (tabContainer != null) {
            for (ModuleSystem.Module module : currentInventoryModules()) {
                syncModuleInventoryPreviewToConfig(module);
            }
        }
        inventoryOverlay = new InventoryOverlayState();
    }

    private void closeInventoryOverlay() {
        if (inventoryOverlay == null) {
            return;
        }
        inventoryOverlay.open = false;
        inventoryOverlay.syncToServer();
        inventoryOverlay = null;
        inventoryOverlayCandidate = -1;
        inventoryOverlayDragged = false;
        draggedModule = -1;
    }

    @Override
    public void onClose() {
        if (inventoryOverlayOpen()) {
            closeInventoryOverlay();
            return;
        }
        super.onClose();
    }

    private List<ModuleSystem.Module> currentInventoryModules() {
        if (tabContainer == null) {
            return List.of();
        }
        CompoundTag activeTab = tabContainer.getActiveTabData();
        if (activeTab == null) {
            return List.of();
        }
        List<ModuleSystem.Module> modules = readModules(activeTab);
        List<ModuleSystem.Module> result = new ArrayList<>();
        for (ModuleSystem.Module module : modules) {
            if (ModuleSystem.isInventoryModule(module)) {
                result.add(module);
            }
        }
        return result;
    }

    private boolean isRightPanel(double mouseX) {
        return mouseX >= dividerX();
    }

    private final class InventoryOverlayState {
        private static final int SLOT_SIZE = 12;
        private static final int PANEL_PAD = 6;
        private static final int BLOCK_GAP = 6;
        private static final int TITLE_HEIGHT = 18;
        private static final long DOUBLE_CLICK_WINDOW_MS = 250L;

        private final List<ItemStack> playerMain;
        private final List<ItemStack> armor;
        private final List<ItemStack> offhand;
        private final Container enderChest;
        private final int panelX;
        private final int panelY;
        private final int armorX;
        private final int armorY;
        private final int offhandX;
        private final int offhandY;
        private final int playerX;
        private final int playerY;
        private final int enderX;
        private final int enderY;
        private final int panelW;
        private final int panelH;
        private ItemStack carried = ItemStack.EMPTY;
        private long lastLeftClickTimeMs;
        private String lastLeftClickSignature = "";
        private boolean open = true;

        private InventoryOverlayState() {
            Inventory playerInv = minecraft != null && minecraft.player != null ? minecraft.player.getInventory() : null;
            this.playerMain = playerInv != null ? playerInv.items : List.of();
            this.armor = playerInv != null ? playerInv.armor : List.of();
            this.offhand = playerInv != null ? playerInv.offhand : List.of();
            Container chest = null;
            if (minecraft != null && minecraft.player != null) {
                try {
                    chest = minecraft.player.getEnderChestInventory();
                } catch (Throwable ignored) {
                }
            }
            this.enderChest = chest;

            int desiredPanelW = PANEL_PAD * 2 + (SLOT_SIZE * 2) + (BLOCK_GAP * 3) + (9 * SLOT_SIZE) * 2;
            this.panelX = dividerX();
            this.panelY = 0;
            this.panelW = Math.max(0, width - panelX);
            int contentY = panelY + TITLE_HEIGHT;
            this.armorX = panelX + PANEL_PAD;
            this.armorY = contentY;
            this.offhandX = armorX + SLOT_SIZE + BLOCK_GAP;
            this.offhandY = armorY + 3 * SLOT_SIZE;
            this.playerX = offhandX + SLOT_SIZE + BLOCK_GAP;
            this.playerY = armorY;
            this.enderX = playerX + 9 * SLOT_SIZE + BLOCK_GAP;
            this.enderY = armorY;
            int contentBottom = Math.max(armorY + 4 * SLOT_SIZE, enderY + 3 * SLOT_SIZE);
            this.panelH = tabContainer != null ? tabContainer.getSeparatorY() : Math.max(contentBottom + PANEL_PAD, 75);
        }

        private void render(GuiGraphics graphics, int mouseX, int mouseY) {
            graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF1B1B24);
            graphics.renderOutline(panelX, panelY, panelW, panelH, 0xFF8A8A8A);
            graphics.drawString(font, Component.literal("物品栏覆盖层"), panelX + 8, panelY + 4, 0xFFFFFF, false);
            graphics.drawString(font, Component.literal("Esc 关闭"),
                    panelX + panelW - font.width("Esc 关闭") - 8, panelY + 4, 0xC8C8C8, false);

            drawSlotColumn(graphics, mouseX, mouseY, armor, armorX, armorY, 4, true);
            drawSlot(graphics, mouseX, mouseY, offhand, 0, offhandX, offhandY, true, null);
            drawPlayerInventory(graphics, mouseX, mouseY);
            drawSlotGrid(graphics, mouseX, mouseY, snapshotContainer(enderChest), enderX, enderY, 9, 3, true, null);
            graphics.drawString(font, Component.literal("末影箱"), enderX, enderY + 3 * SLOT_SIZE + 4, 0xD0D0D0, false);

            if (!carried.isEmpty()) {
                renderScaledItem(graphics, carried, mouseX - 6, mouseY - 6, 0.75F);
            }
        }

        private boolean mouseClicked(double mouseX, double mouseY, int button) {
            OverlaySlotRef slot = findModuleSlot(mouseX, mouseY);
            if (slot != null) {
                return clickSlot(slot, button);
            }
            if (!containsPanel(mouseX, mouseY)) {
                return false;
            }
            slot = findPlayerSlot(mouseX, mouseY);
            if (slot != null) {
                return clickSlot(slot, button);
            }
            return true;
        }

        private boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            return false;
        }

        private boolean mouseReleased(double mouseX, double mouseY, int button) {
            return false;
        }

        private boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            return containsPanel(mouseX, mouseY);
        }

        private void syncToServer() {
            if (minecraft == null || minecraft.player == null) {
                return;
            }
            ItemStack carriedSnapshot = carried == null ? ItemStack.EMPTY : carried.copy();
            if (!carriedSnapshot.isEmpty()) {
                carriedSnapshot = insertIntoInventory(playerMain, carriedSnapshot);
                carried = carriedSnapshot.copy();
            }
            Map<String, List<ItemStack>> moduleCopy = copyModuleInventories(moduleInventories);
            CtfNetwork.CHANNEL.sendToServer(new C2SSyncPetInventoryOverlayPacket(
                    entityId,
                    copyStacks(playerMain),
                    copyStacks(armor),
                    copyStacks(offhand),
                    snapshotContainer(enderChest),
                    moduleCopy,
                    carriedSnapshot
            ));
        }

        private boolean clickSlot(OverlaySlotRef slot, int button) {
            if (slot == null) {
                return false;
            }
            ItemStack slotStack = getSlotStack(slot);
            ItemStack cursor = carried == null ? ItemStack.EMPTY : carried;
            boolean leftClick = button == 0;
            boolean rightClick = button == 1;
            if (!leftClick && !rightClick) {
                return false;
            }
            long now = System.currentTimeMillis();
            String signature = stackSignature(slotStack);
            String cursorSignature = stackSignature(cursor);
            if (leftClick
                    && !cursor.isEmpty()
                    && !lastLeftClickSignature.isBlank()
                    && cursorSignature.equals(lastLeftClickSignature)
                    && now - lastLeftClickTimeMs <= DOUBLE_CLICK_WINDOW_MS) {
                if (collectMatchingStacks(cursor)) {
                    lastLeftClickTimeMs = 0L;
                    lastLeftClickSignature = "";
                }
                lastLeftClickTimeMs = 0L;
                lastLeftClickSignature = "";
                return true;
            }

            if (cursor.isEmpty()) {
                if (slotStack.isEmpty()) {
                    return false;
                }
                if (rightClick) {
                    int take = (slotStack.getCount() + 1) / 2;
                    carried = slotStack.copy();
                    carried.setCount(take);
                    slotStack.shrink(take);
                    setSlotStack(slot, slotStack);
                    lastLeftClickTimeMs = 0L;
                    lastLeftClickSignature = "";
                    return true;
                }
                carried = slotStack.copy();
                setSlotStack(slot, ItemStack.EMPTY);
                if (leftClick) {
                    lastLeftClickTimeMs = now;
                    lastLeftClickSignature = signature;
                }
                return true;
            }

            if (slotStack.isEmpty()) {
                if (!canPlace(slot, cursor)) {
                    return false;
                }
                if (rightClick) {
                    ItemStack placed = cursor.copy();
                    placed.setCount(1);
                    setSlotStack(slot, placed);
                    cursor.shrink(1);
                    carried = cursor.isEmpty() ? ItemStack.EMPTY : cursor;
                    lastLeftClickTimeMs = 0L;
                    lastLeftClickSignature = "";
                    return true;
                }
                int move = Math.min(cursor.getCount(), maxStackSize(slot, cursor));
                ItemStack placed = cursor.copy();
                placed.setCount(move);
                setSlotStack(slot, placed);
                cursor.shrink(move);
                carried = cursor.isEmpty() ? ItemStack.EMPTY : cursor;
                lastLeftClickTimeMs = 0L;
                lastLeftClickSignature = "";
                return true;
            }

            if (!ItemStack.isSameItemSameTags(slotStack, cursor)) {
                if (!leftClick || !canPlace(slot, cursor)) {
                    return false;
                }
                ItemStack placed = cursor.copy();
                setSlotStack(slot, placed);
                carried = slotStack.copy();
                lastLeftClickTimeMs = 0L;
                lastLeftClickSignature = "";
                return true;
            }

            if (!canPlace(slot, cursor)) {
                return false;
            }
            int limit = maxStackSize(slot, cursor);
            if (rightClick) {
                if (slotStack.getCount() >= limit) {
                    return false;
                }
                ItemStack next = slotStack.copy();
                next.grow(1);
                setSlotStack(slot, next);
                cursor.shrink(1);
                carried = cursor.isEmpty() ? ItemStack.EMPTY : cursor;
                lastLeftClickTimeMs = 0L;
                lastLeftClickSignature = "";
                return true;
            }

            int space = Math.max(0, limit - slotStack.getCount());
            if (space <= 0) {
                return false;
            }
            int move = Math.min(space, cursor.getCount());
            if (move <= 0) {
                return false;
            }
            ItemStack next = slotStack.copy();
            next.grow(move);
            setSlotStack(slot, next);
            cursor.shrink(move);
            carried = cursor.isEmpty() ? ItemStack.EMPTY : cursor;
            lastLeftClickTimeMs = 0L;
            lastLeftClickSignature = "";
            return true;
        }

        private void drawPlayerInventory(GuiGraphics graphics, int mouseX, int mouseY) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    int index = 9 + row * 9 + col;
                    int x = playerX + col * SLOT_SIZE;
                    int y = playerY + row * SLOT_SIZE;
                    drawSlot(graphics, mouseX, mouseY, playerMain, index, x, y, true, null);
                }
            }
            int hotbarY = playerY + 3 * SLOT_SIZE;
            for (int col = 0; col < 9; col++) {
                int x = playerX + col * SLOT_SIZE;
                drawSlot(graphics, mouseX, mouseY, playerMain, col, x, hotbarY, true, null);
            }
        }

        private boolean containsPanel(double mouseX, double mouseY) {
            return mouseX >= panelX && mouseX <= panelX + panelW && mouseY >= panelY && mouseY <= panelY + panelH;
        }

        private OverlaySlotRef findPlayerSlot(double mouseX, double mouseY) {
            for (int i = 0; i < 4; i++) {
                int x = armorX;
                int y = armorY + i * SLOT_SIZE;
                if (hit(mouseX, mouseY, x, y)) {
                    return new OverlaySlotRef(OverlaySlotRef.Kind.ARMOR, i, null, x, y);
                }
            }
            if (hit(mouseX, mouseY, offhandX, offhandY)) {
                return new OverlaySlotRef(OverlaySlotRef.Kind.OFFHAND, 0, null, offhandX, offhandY);
            }
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    int index = 9 + row * 9 + col;
                    int x = playerX + col * SLOT_SIZE;
                    int y = playerY + row * SLOT_SIZE;
                    if (hit(mouseX, mouseY, x, y)) {
                        return new OverlaySlotRef(OverlaySlotRef.Kind.PLAYER_MAIN, index, null, x, y);
                    }
                }
            }
            int hotbarY = playerY + 3 * SLOT_SIZE;
            for (int col = 0; col < 9; col++) {
                int x = playerX + col * SLOT_SIZE;
                if (hit(mouseX, mouseY, x, hotbarY)) {
                    return new OverlaySlotRef(OverlaySlotRef.Kind.PLAYER_MAIN, col, null, x, hotbarY);
                }
            }
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    int index = row * 9 + col;
                    int x = enderX + col * SLOT_SIZE;
                    int y = enderY + row * SLOT_SIZE;
                    if (hit(mouseX, mouseY, x, y)) {
                        return new OverlaySlotRef(OverlaySlotRef.Kind.ENDER_CHEST, index, null, x, y);
                    }
                }
            }
            return null;
        }

        private OverlaySlotRef findModuleSlot(double mouseX, double mouseY) {
            if (tabContainer == null) {
                return null;
            }
            CompoundTag activeTab = tabContainer.getActiveTabData();
            if (activeTab == null) {
                return null;
            }
            List<ModuleSystem.Module> modules = readModules(activeTab);
            int originX = tabLeftEdge();
            int originY = tabContainer.getSeparatorY() + 5;
            if (mouseX < originX || mouseY < originY) {
                return null;
            }
            int relX = (int) mouseX - originX;
            int relY = (int) mouseY - originY;
            int hit = hitTest(modules, relX, relY, font, runtimeDataVariables());
            if (hit < 0 || hit >= modules.size()) {
                return null;
            }
            ModuleSystem.Module module = modules.get(hit);
            if (!ModuleSystem.isInventoryModule(module)) {
                return null;
            }
            int slotIndex = ModuleSystem.inventorySlotIndexAt(module, font, originX, originY, mouseX, mouseY);
            if (slotIndex < 0) {
                return null;
            }
            return new OverlaySlotRef(OverlaySlotRef.Kind.MODULE, slotIndex, module, -1, -1);
        }

        private ItemStack getSlotStack(OverlaySlotRef slot) {
            if (slot == null) {
                return ItemStack.EMPTY;
            }
            return switch (slot.kind) {
                case ARMOR -> slot.index >= 0 && slot.index < armor.size() ? armor.get(slot.index) : ItemStack.EMPTY;
                case OFFHAND -> slot.index >= 0 && slot.index < offhand.size() ? offhand.get(slot.index) : ItemStack.EMPTY;
                case PLAYER_MAIN -> slot.index >= 0 && slot.index < playerMain.size() ? playerMain.get(slot.index) : ItemStack.EMPTY;
                case ENDER_CHEST -> enderChest != null && slot.index >= 0 && slot.index < enderChest.getContainerSize() ? enderChest.getItem(slot.index) : ItemStack.EMPTY;
                case MODULE -> {
                    if (slot.module == null) {
                        yield ItemStack.EMPTY;
                    }
                    String moduleId = ModuleSystem.moduleId(slot.module);
                    List<ItemStack> stacks = moduleInventories.get(moduleId);
                    if (stacks == null || slot.index < 0 || slot.index >= stacks.size()) {
                        yield ItemStack.EMPTY;
                    }
                    yield stacks.get(slot.index);
                }
            };
        }

        private void setSlotStack(OverlaySlotRef slot, ItemStack stack) {
            ItemStack next = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
            switch (slot.kind) {
                case ARMOR -> {
                    if (slot.index >= 0 && slot.index < armor.size()) {
                        armor.set(slot.index, next);
                    }
                }
                case OFFHAND -> {
                    if (slot.index >= 0 && slot.index < offhand.size()) {
                        offhand.set(slot.index, next);
                    }
                }
                case PLAYER_MAIN -> {
                    if (slot.index >= 0 && slot.index < playerMain.size()) {
                        playerMain.set(slot.index, next);
                    }
                }
                case ENDER_CHEST -> {
                    if (enderChest != null && slot.index >= 0 && slot.index < enderChest.getContainerSize()) {
                        enderChest.setItem(slot.index, next);
                        enderChest.setChanged();
                    }
                }
                case MODULE -> {
                    if (slot.module != null) {
                        String moduleId = ModuleSystem.moduleId(slot.module);
                        List<ItemStack> stacks = moduleInventories.get(moduleId);
                        int size = ModuleSystem.inventorySlotCount(slot.module);
                        if (stacks == null || stacks.size() != size) {
                            syncModuleInventoryPreviewToConfig(slot.module);
                            stacks = moduleInventories.get(moduleId);
                        }
                        if (stacks != null && slot.index >= 0 && slot.index < stacks.size()) {
                            stacks.set(slot.index, next);
                        }
                    }
                }
            }
        }

        private boolean canPlace(OverlaySlotRef slot, ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                return false;
            }
            return switch (slot.kind) {
                case ARMOR -> {
                    EquipmentSlot equipmentSlot = switch (slot.index) {
                        case 0 -> EquipmentSlot.HEAD;
                        case 1 -> EquipmentSlot.CHEST;
                        case 2 -> EquipmentSlot.LEGS;
                        default -> EquipmentSlot.FEET;
                    };
                    yield minecraft != null && minecraft.player != null && stack.canEquip(equipmentSlot, minecraft.player);
                }
                case OFFHAND -> true;
                case MODULE -> slot.module != null && ModuleInventoryState.allowsItem(slot.module.config(), stack);
                default -> true;
            };
        }

        private int maxStackSize(OverlaySlotRef slot, ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                return 1;
            }
            return switch (slot.kind) {
                case ARMOR, OFFHAND -> 1;
                case MODULE -> slot.module != null ? ModuleInventoryState.maxStackSize(slot.module.config(), stack) : stack.getMaxStackSize();
                default -> stack.getMaxStackSize();
            };
        }

        private boolean hit(double mouseX, double mouseY, int x, int y) {
            return mouseX >= x && mouseX <= x + SLOT_SIZE && mouseY >= y && mouseY <= y + SLOT_SIZE;
        }

        private void drawSlotColumn(GuiGraphics graphics, int mouseX, int mouseY, List<ItemStack> stacks, int x, int y, int count, boolean renderItems) {
            for (int i = 0; i < count; i++) {
                drawSlot(graphics, mouseX, mouseY, stacks, i, x, y + i * SLOT_SIZE, renderItems, null);
            }
        }

        private void drawSlotGrid(GuiGraphics graphics, int mouseX, int mouseY, List<ItemStack> stacks, int x, int y,
                                  int cols, int rows, boolean renderItems, ModuleSystem.Module module) {
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    int index = row * cols + col;
                    drawSlot(graphics, mouseX, mouseY, stacks, index, x + col * SLOT_SIZE, y + row * SLOT_SIZE, renderItems, module);
                }
            }
        }

        private void drawSlot(GuiGraphics graphics, int mouseX, int mouseY, List<ItemStack> stacks, int index,
                              int x, int y, boolean renderItems, ModuleSystem.Module module) {
            boolean hovered = hit(mouseX, mouseY, x, y);
            graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, hovered ? 0xFF3A3A44 : 0xFF2B2B33);
            graphics.renderOutline(x, y, SLOT_SIZE, SLOT_SIZE, hovered ? 0xFFFFFFFF : 0xFF666677);
            if (!renderItems || stacks == null || index < 0 || index >= stacks.size()) {
                return;
            }
            ItemStack stack = stacks.get(index);
            if (stack != null && !stack.isEmpty()) {
                renderScaledItem(graphics, stack, x, y, 0.75F);
            }
        }

        private void renderScaledItem(GuiGraphics graphics, ItemStack stack, int x, int y, float scale) {
            if (stack == null || stack.isEmpty()) {
                return;
            }
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0.0);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.renderItem(stack, 0, 0);
            graphics.renderItemDecorations(font, stack, 0, 0);
            graphics.pose().popPose();
        }

        private boolean collectMatchingStacks(ItemStack targetStack) {
            if (targetStack == null || targetStack.isEmpty()) {
                return false;
            }
            ItemStack target = targetStack.copy();
            int originalCount = target.getCount();
            int limit = Math.max(originalCount, target.getMaxStackSize());
            if (limit <= originalCount) {
                return false;
            }
            int current = originalCount;
            current = collectFromStacks(playerMain, target, current, limit);
            current = collectFromStacks(armor, target, current, limit);
            current = collectFromStacks(offhand, target, current, limit);
            current = collectFromContainer(enderChest, target, current, limit);
            current = collectFromModuleInventories(target, current, limit);
            target.setCount(current);
            carried = target;
            return current > originalCount;
        }

        private int collectFromStacks(List<ItemStack> stacks, ItemStack target, int current, int limit) {
            if (stacks == null || target == null || target.isEmpty()) {
                return current;
            }
            for (int i = 0; i < stacks.size() && current < limit; i++) {
                ItemStack stack = stacks.get(i);
                if (stack == null || stack.isEmpty() || !ItemStack.isSameItemSameTags(stack, target)) {
                    continue;
                }
                int take = Math.min(limit - current, stack.getCount());
                if (take <= 0) {
                    continue;
                }
                ItemStack next = stack.copy();
                next.shrink(take);
                stacks.set(i, next.isEmpty() ? ItemStack.EMPTY : next);
                current += take;
            }
            return current;
        }

        private int collectFromContainer(Container container, ItemStack target, int current, int limit) {
            if (container == null || target == null || target.isEmpty()) {
                return current;
            }
            for (int i = 0; i < container.getContainerSize() && current < limit; i++) {
                ItemStack stack = container.getItem(i);
                if (stack == null || stack.isEmpty() || !ItemStack.isSameItemSameTags(stack, target)) {
                    continue;
                }
                int take = Math.min(limit - current, stack.getCount());
                if (take <= 0) {
                    continue;
                }
                ItemStack next = stack.copy();
                next.shrink(take);
                container.setItem(i, next.isEmpty() ? ItemStack.EMPTY : next);
                container.setChanged();
                current += take;
            }
            return current;
        }

        private int collectFromModuleInventories(ItemStack target, int current, int limit) {
            if (moduleInventories == null || moduleInventories.isEmpty() || target == null || target.isEmpty()) {
                return current;
            }
            for (Map.Entry<String, List<ItemStack>> entry : moduleInventories.entrySet()) {
                if (current >= limit) {
                    break;
                }
                List<ItemStack> stacks = entry.getValue();
                current = collectFromStacks(stacks, target, current, limit);
            }
            return current;
        }

        private String stackSignature(ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                return "";
            }
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
            String id = rl != null ? rl.toString() : stack.getItem().toString();
            CompoundTag tag = stack.getTag();
            return id + "|" + (tag != null ? tag.getAsString() : "");
        }

        private List<ItemStack> snapshotContainer(Container container) {
            if (container == null) {
                return List.of();
            }
            List<ItemStack> result = new ArrayList<>(container.getContainerSize());
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                result.add(stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
            }
            return result;
        }

        private ItemStack insertIntoInventory(List<ItemStack> target, ItemStack stack) {
            if (target == null || stack == null || stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack remaining = stack.copy();
            for (int i = 0; i < target.size() && !remaining.isEmpty(); i++) {
                ItemStack slot = target.get(i);
                if (slot == null || slot.isEmpty()) {
                    target.set(i, remaining.copy());
                    remaining = ItemStack.EMPTY;
                    break;
                }
                if (ItemStack.isSameItemSameTags(slot, remaining)) {
                    int max = Math.min(slot.getMaxStackSize(), remaining.getMaxStackSize());
                    int canMove = Math.min(max - slot.getCount(), remaining.getCount());
                    if (canMove > 0) {
                        ItemStack next = slot.copy();
                        next.grow(canMove);
                        target.set(i, next);
                        remaining.shrink(canMove);
                    }
                }
            }
            return remaining;
        }

        private List<ItemStack> copyStacks(List<ItemStack> source) {
            if (source == null) {
                return List.of();
            }
            List<ItemStack> result = new ArrayList<>(source.size());
            for (ItemStack stack : source) {
                result.add(stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
            }
            return result;
        }

        private Map<String, List<ItemStack>> copyModuleInventories(Map<String, List<ItemStack>> source) {
            Map<String, List<ItemStack>> result = new TreeMap<>();
            if (source == null) {
                return result;
            }
            for (Map.Entry<String, List<ItemStack>> entry : source.entrySet()) {
                String key = entry.getKey();
                if (key == null || key.isBlank()) {
                    continue;
                }
                result.put(key, copyStacks(entry.getValue()));
            }
            return result;
        }
    }

    private static final class OverlaySlotRef {
        private enum Kind {
            PLAYER_MAIN,
            ARMOR,
            OFFHAND,
            ENDER_CHEST,
            MODULE
        }

        private final Kind kind;
        private final int index;
        private final ModuleSystem.Module module;
        @SuppressWarnings("unused")
        private final int x;
        @SuppressWarnings("unused")
        private final int y;

        private OverlaySlotRef(Kind kind, int index, ModuleSystem.Module module, int x, int y) {
            this.kind = kind;
            this.index = index;
            this.module = module;
            this.x = x;
            this.y = y;
        }
    }

    private void saveCurrentEditValues() {
        if (tabContainer == null) return;
        CompoundTag activeTab = tabContainer.getActiveTabData();
        if (activeTab == null) return;
        // Persist module edits (saves the current in-flight config popup text as well)
        flushModuleConfigEdit();
    }

    /** If module config popup is open and has an edit box, save its content to the module data. */
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
                if (!isDataKeyAvailable(key)) {
                    dataKeyValid = false;
                    return;
                }
                m.config().putString("data_key", key);
                dataKeyValid = true;
                savedDataKey = key;
            }
        }
        writeModules(activeTab, modules);
        if (m.type() == ModuleSystem.ModuleType.INVENTORY) {
            syncModuleInventoryPreviewToConfig(m);
        }
    }

    /** Open native file picker on background thread and copy selected PNG to resources/ folder. */
    private void pickImageNative() {
        new Thread(() -> {
            String copiedName = NativeFilePicker.pickAndCopyPngToResources();
            if (copiedName != null && minecraft != null) {
                minecraft.execute(() -> {
                    if (editingModule != null) editingModule.config().putString("image_file", copiedName);
                });
            }
        }, "CTF-ImagePicker").start();
    }

    private void validateDataKey(CompoundTag activeTab, int idx) {
        if (activeTab == null) return;
        List<ModuleSystem.Module> mods = readModules(activeTab);
        if (idx < 0 || idx >= mods.size()) return;
        String key = moduleTextEdit != null ? moduleTextEdit.getValue().trim() : "";
        if (isDataKeyAvailable(key)) {
            mods.get(idx).config().putString("data_key", key);
            writeModules(activeTab, mods);
            dataKeyValid = true;
            savedDataKey = key;
        } else {
            dataKeyValid = false;
        }
    }

    private boolean isDataKeyAvailable(String key) {
        if (key == null || key.isBlank()) return false;
        if (isOp) {
            return getParsedGuiData().getCompound("data_variables").contains(key);
        }
        return dataVariables != null && dataVariables.containsKey(key);
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

        String newNickname = nicknameEdit != null ? nicknameEdit.getValue() : "";
        CompoundTag tabCompound = TabContainer.serializeTabsToCompound(tabs);
        CompoundTag dataVars = getParsedGuiData().getCompound("data_variables");
        if (!dataVars.isEmpty()) {
            tabCompound.put("data_variables", dataVars);
        }
        parsedGuiData = tabCompound;
        CtfNetwork.CHANNEL.sendToServer(new C2SSavePetSettingPacket(entityTypeString, newNickname, tabCompound.getAsString(), entityId));
        saveMessage = "保存请求已发送";
        saveMessageTicks = 0;
    }
}

