package com.example.customtamingframework.client.screen;

import com.example.customtamingframework.network.C2SSavePetPermissionsPacket;
import com.example.customtamingframework.network.C2STransferPetOwnerPacket;
import com.example.customtamingframework.network.CtfNetwork;
import com.example.customtamingframework.network.S2CPetPermissionSnapshotPacket;
import com.example.customtamingframework.pet.PetPermissionEntry;
import com.example.customtamingframework.pet.PetProgress;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class PetPermissionScreen extends Screen {
    private static final int ROW_HEIGHT = 20;
    private static final int HEADER_HEIGHT = 16;
    private static final String HEADER_ONLINE = "__HEADER_ONLINE__";
    private static final String HEADER_OFFLINE = "__HEADER_OFFLINE__";

    private final PetScreen parentScreen;
    private final boolean isOp;
    private int entityId;

    private String ownerPlayerUuid;
    private String ownerPlayerName;
    private String viewerPermission;
    private List<PetPermissionEntry> snapshotEntries = List.of();
    private final Map<String, PetProgress.AccessMode> draftPermissions = new TreeMap<>();

    private String searchFilter = "";
    private EditBox searchEdit;
    private int scrollOffset;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int listStartY;
    private int listEndY;

    public PetPermissionScreen(PetScreen parentScreen, boolean isOp, S2CPetPermissionSnapshotPacket packet) {
        super(Component.literal("宠物权限管理"));
        this.parentScreen = parentScreen;
        this.isOp = isOp;
        applySnapshotInternal(packet);
    }

    public void applySnapshot(S2CPetPermissionSnapshotPacket packet) {
        applySnapshotInternal(packet);
        if (parentScreen != null) {
            parentScreen.syncAccessState(ownerPlayerUuid, ownerPlayerName, viewerPermission);
            parentScreen.refreshLayout();
        }
        init();
    }

    public boolean matchesEntity(int id) {
        return this.entityId == id;
    }

    private void applySnapshotInternal(S2CPetPermissionSnapshotPacket packet) {
        this.entityId = packet.entityId();
        this.ownerPlayerUuid = packet.ownerPlayerUuid() != null ? packet.ownerPlayerUuid() : "";
        this.ownerPlayerName = packet.ownerPlayerName() != null ? packet.ownerPlayerName() : "";
        this.viewerPermission = packet.viewerPermission() != null ? packet.viewerPermission() : PetProgress.AccessMode.READ_ONLY.snbtKey();
        this.snapshotEntries = packet.entries() != null ? List.copyOf(packet.entries()) : List.of();
        this.draftPermissions.clear();
        for (PetPermissionEntry entry : this.snapshotEntries) {
            if (entry.playerUuid() != null && !entry.playerUuid().isBlank()) {
                this.draftPermissions.put(entry.playerUuid(), entry.permission() == null ? PetProgress.AccessMode.SECRET : entry.permission());
            }
        }
    }

    @Override
    protected void init() {
        clearWidgets();

        panelW = Math.min(720, width - 24);
        panelH = Math.min(460, height - 24);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        int contentLeft = panelX + 12;
        int contentRight = panelX + panelW - 12;
        int searchY = panelY + 28;
        int ownerY = panelY + 58;
        listStartY = panelY + 92;
        listEndY = panelY + panelH - 44;

        searchEdit = new EditBox(font, contentLeft, searchY, panelW - 92, 18, Component.literal("搜索玩家"));
        searchEdit.setValue(searchFilter);
        searchEdit.setResponder(val -> searchFilter = val);
        addRenderableWidget(searchEdit);
        addRenderableWidget(Button.builder(Component.literal("搜索"), b -> applySearch())
                .bounds(contentRight - 68, searchY - 1, 68, 20).build());

        buildRowWidgets();
        buildBottomButtons();
    }

    private void buildRowWidgets() {
        List<PetPermissionEntry> visibleItems = buildVisibleItems();
        int visibleRows = Math.max(0, (listEndY - listStartY) / ROW_HEIGHT);
        int maxScroll = Math.max(0, visibleItems.size() - visibleRows);
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }

        int rowNameX = panelX + 18;
        int transferW = 58;
        int transferX = panelX + (panelW - transferW) / 2;
        int rightBtnW = 64;
        int gap = 4;
        int rightGroupW = rightBtnW * 3 + gap * 2;
        int rightStartX = panelX + panelW - 12 - rightGroupW;

        int rowIndex = 0;
        for (int i = scrollOffset; i < visibleItems.size() && rowIndex < visibleRows; i++) {
            PetPermissionEntry row = visibleItems.get(i);
            int rowY = listStartY + rowIndex * ROW_HEIGHT;
            if (rowY + ROW_HEIGHT > listEndY) {
                break;
            }
            if (isHeaderRow(row)) {
                rowIndex++;
                continue;
            }
            if (row.owner()) {
                rowIndex++;
                continue;
            }

            PetProgress.AccessMode effective = currentPermission(row);
            boolean fullControl = canManageAll();
            boolean editableViewer = canEditNonAdminRows() && !fullControl;
            boolean rowLocked = !fullControl && editableViewer && (effective == PetProgress.AccessMode.EDITABLE || effective == PetProgress.AccessMode.OWNER);
            boolean rowMayEdit = fullControl || (editableViewer && !rowLocked && (effective == PetProgress.AccessMode.READ_ONLY || effective == PetProgress.AccessMode.SECRET));
            boolean canTransfer = canTransfer(row);

            if (canTransfer) {
                addRenderableWidget(actionButton(Component.literal("转赠"), true, b -> openTransferConfirm(row),
                        transferX, rowY + 1, transferW, 18));
            }

            boolean allowReadOnly = fullControl || (editableViewer && !rowLocked);
            boolean allowEditable = fullControl;
            boolean allowSecret = fullControl || (editableViewer && !rowLocked);

            addRenderableWidget(actionButton(permissionLabel("只读", effective == PetProgress.AccessMode.READ_ONLY, allowReadOnly),
                    allowReadOnly, b -> setDraftPermission(row.playerUuid(), PetProgress.AccessMode.READ_ONLY),
                    rightStartX, rowY + 1, rightBtnW, 18));
            addRenderableWidget(actionButton(permissionLabel("可编辑", effective == PetProgress.AccessMode.EDITABLE, allowEditable),
                    allowEditable, b -> setDraftPermission(row.playerUuid(), PetProgress.AccessMode.EDITABLE),
                    rightStartX + rightBtnW + gap, rowY + 1, rightBtnW, 18));
            addRenderableWidget(actionButton(permissionLabel("保密", effective == PetProgress.AccessMode.SECRET, allowSecret),
                    allowSecret, b -> setDraftPermission(row.playerUuid(), PetProgress.AccessMode.SECRET),
                    rightStartX + (rightBtnW + gap) * 2, rowY + 1, rightBtnW, 18));

            rowIndex++;
        }
    }

    private void buildBottomButtons() {
        int btnY = panelY + panelH - 30;
        int btnW = 80;
        int btnSpacing = 10;
        boolean editable = canManageAll() || canEditNonAdminRows();
        if (editable) {
            int totalW = btnW * 2 + btnSpacing;
            int startX = (width - totalW) / 2;
            addRenderableWidget(Button.builder(Component.literal("保存"), b -> saveAndReturn())
                    .bounds(startX, btnY, btnW, 20).build());
            addRenderableWidget(Button.builder(Component.literal("取消"), b -> closeToParent())
                    .bounds(startX + btnW + btnSpacing, btnY, btnW, 20).build());
        } else {
            int startX = (width - btnW) / 2;
            addRenderableWidget(Button.builder(Component.literal("关闭"), b -> closeToParent())
                    .bounds(startX, btnY, btnW, 20).build());
        }
    }

    private void applySearch() {
        searchFilter = searchEdit != null ? searchEdit.getValue().trim() : "";
        scrollOffset = 0;
        init();
    }

    private boolean canManageAll() {
        return isOp || PetProgress.AccessMode.OWNER.name().equalsIgnoreCase(viewerPermission);
    }

    private boolean canEditNonAdminRows() {
        return !canManageAll() && PetProgress.AccessMode.EDITABLE.snbtKey().equalsIgnoreCase(viewerPermission);
    }

    private boolean canTransfer(PetPermissionEntry row) {
        return (isOp || PetProgress.AccessMode.OWNER.name().equalsIgnoreCase(viewerPermission)) && !row.owner();
    }

    private PetProgress.AccessMode currentPermission(PetPermissionEntry row) {
        if (row == null || row.playerUuid() == null) {
            return PetProgress.AccessMode.SECRET;
        }
        return draftPermissions.getOrDefault(row.playerUuid(), row.permission());
    }

    private void setDraftPermission(String playerUuid, PetProgress.AccessMode permission) {
        if (playerUuid == null || playerUuid.isBlank() || permission == null) {
            return;
        }
        if (ownerPlayerUuid != null && ownerPlayerUuid.equals(playerUuid)) {
            draftPermissions.put(playerUuid, PetProgress.AccessMode.OWNER);
            init();
            return;
        }
        PetPermissionEntry row = findEntry(playerUuid);
        if (row == null) {
            return;
        }
        PetProgress.AccessMode current = currentPermission(row);
        if (canManageAll()) {
            if (permission != PetProgress.AccessMode.OWNER) {
                draftPermissions.put(playerUuid, permission);
                init();
            }
            return;
        }
        if (canEditNonAdminRows()) {
            if (current == PetProgress.AccessMode.OWNER || current == PetProgress.AccessMode.EDITABLE) {
                return;
            }
            if (permission == PetProgress.AccessMode.READ_ONLY || permission == PetProgress.AccessMode.SECRET) {
                draftPermissions.put(playerUuid, permission);
                init();
            }
        }
    }

    private PetPermissionEntry findEntry(String playerUuid) {
        if (playerUuid == null) {
            return null;
        }
        for (PetPermissionEntry entry : snapshotEntries) {
            if (playerUuid.equals(entry.playerUuid())) {
                return entry;
            }
        }
        return null;
    }

    private void openTransferConfirm(PetPermissionEntry row) {
        if (minecraft == null || row == null || row.owner()) {
            return;
        }
        minecraft.setScreen(new PetPermissionTransferConfirmScreen(this, row.playerUuid(), row.playerName(), entityId));
    }

    private Button actionButton(Component text, boolean enabled, Button.OnPress onPress, int x, int y, int w, int h) {
        Button button = Button.builder(text, onPress).bounds(x, y, w, h).build();
        button.active = enabled;
        return button;
    }

    private void saveAndReturn() {
        if (minecraft == null) {
            return;
        }
        Map<String, String> payload = new TreeMap<>();
        draftPermissions.forEach((key, value) -> payload.put(key, value.snbtKey()));
        CtfNetwork.CHANNEL.sendToServer(new C2SSavePetPermissionsPacket(entityId, payload));
        minecraft.setScreen(parentScreen);
    }

    private void closeToParent() {
        if (minecraft != null) {
            minecraft.setScreen(parentScreen);
        }
    }

    private List<PetPermissionEntry> buildVisibleItems() {
        String filter = searchFilter == null ? "" : searchFilter.trim().toLowerCase(Locale.ROOT);
        List<PetPermissionEntry> online = new ArrayList<>();
        List<PetPermissionEntry> offline = new ArrayList<>();
        for (PetPermissionEntry entry : snapshotEntries) {
            if (entry == null || entry.owner()) {
                continue;
            }
            if (!filter.isBlank()) {
                String name = entry.playerName() == null ? "" : entry.playerName().toLowerCase(Locale.ROOT);
                String uuid = entry.playerUuid() == null ? "" : entry.playerUuid().toLowerCase(Locale.ROOT);
                if (!name.contains(filter) && !uuid.contains(filter)) {
                    continue;
                }
            }
            if (entry.online()) {
                online.add(entry);
            } else {
                offline.add(entry);
            }
        }
        List<PetPermissionEntry> items = new ArrayList<>();
        if (!online.isEmpty()) {
            items.add(new PetPermissionEntry("", HEADER_ONLINE, false, PetProgress.AccessMode.READ_ONLY, false));
            items.addAll(online);
        }
        if (!offline.isEmpty()) {
            items.add(new PetPermissionEntry("", HEADER_OFFLINE, false, PetProgress.AccessMode.READ_ONLY, false));
            items.addAll(offline);
        }
        return items;
    }

    private boolean isHeaderRow(PetPermissionEntry entry) {
        return entry != null && entry.playerUuid() != null && entry.playerUuid().isBlank()
                && (HEADER_ONLINE.equals(entry.playerName()) || HEADER_OFFLINE.equals(entry.playerName()));
    }

    private String headerLabel(PetPermissionEntry entry) {
        if (entry != null && HEADER_OFFLINE.equals(entry.playerName())) {
            return "离线玩家";
        }
        return "在线玩家";
    }

    private Component permissionLabel(String label, boolean selected, boolean enabled) {
        ChatFormatting color;
        if (!enabled) {
            color = ChatFormatting.GRAY;
        } else if (selected) {
            color = ChatFormatting.AQUA;
        } else {
            color = ChatFormatting.WHITE;
        }
        return Component.literal((selected ? "● " : "○ ") + label).withStyle(color);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xCC111111);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + 1, 0xFF4A4A4A);
        graphics.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, 0xFF4A4A4A);
        graphics.fill(panelX, panelY, panelX + 1, panelY + panelH, 0xFF4A4A4A);
        graphics.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, 0xFF4A4A4A);
        graphics.drawCenteredString(font, title, width / 2, panelY + 8, 0xFFFFFF);

        graphics.drawString(font, Component.literal("主人"), panelX + 18, panelY + 58 + 6, 0xB8B8B8, false);
        graphics.drawString(font, resolveOwnerDisplayName(),
                panelX + 56, panelY + 58 + 6, 0x55FFFF, false);

        List<PetPermissionEntry> items = buildVisibleItems();
        int visibleRows = Math.max(0, (listEndY - listStartY) / ROW_HEIGHT);
        int maxScroll = Math.max(0, items.size() - visibleRows);
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }

        int y = listStartY;
        int rowIndex = 0;
        int contentRight = panelX + panelW - 12;
        int rightBtnW = 64;
        int gap = 4;
        int rightGroupW = rightBtnW * 3 + gap * 2;
        int rightStartX = contentRight - rightGroupW;
        int transferW = 58;
        int transferX = panelX + (panelW - transferW) / 2;
        int rowNameX = panelX + 18;

        for (int i = scrollOffset; i < items.size() && rowIndex < visibleRows; i++) {
            PetPermissionEntry entry = items.get(i);
            if (isHeaderRow(entry)) {
                graphics.drawString(font, Component.literal(headerLabel(entry)).withStyle(ChatFormatting.GRAY), panelX + 18, y + 4, 0xCCCCCC, false);
                graphics.fill(panelX + 18, y + 15, contentRight - 18, y + 16, 0xFF3A3A3A);
            } else {
                int color = entry.online() ? 0x55FF55 : 0xAAAAAA;
                if (entry.owner()) {
                    color = 0xFFD966;
                }
                MutableComponent name = Component.literal(entry.playerName() == null || entry.playerName().isBlank() ? entry.playerUuid() : entry.playerName());
                graphics.drawString(font, name, rowNameX, y + 6, color, false);
                if (entry.owner()) {
                    graphics.drawString(font, Component.literal("主人").withStyle(ChatFormatting.GOLD), rightStartX + 92, y + 6, 0xFFD966, false);
                }
                graphics.fill(panelX + 16, y + 18, contentRight - 16, y + 19, 0xFF2F2F2F);
            }
            y += ROW_HEIGHT;
            rowIndex++;
        }

        if (items.isEmpty()) {
            graphics.drawCenteredString(font, Component.literal("没有匹配的玩家"), width / 2, listStartY + 20, 0x888888);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private Component permissionShortLabel(PetProgress.AccessMode mode) {
        return switch (mode) {
            case OWNER -> Component.literal("主人").withStyle(ChatFormatting.GOLD);
            case READ_ONLY -> Component.literal("只读").withStyle(ChatFormatting.WHITE);
            case EDITABLE -> Component.literal("可编辑").withStyle(ChatFormatting.AQUA);
            case SECRET -> Component.literal("保密").withStyle(ChatFormatting.GRAY);
        };
    }

    private String resolveOwnerDisplayName() {
        String direct = ownerPlayerName == null ? "" : ownerPlayerName.trim();
        if (!direct.isBlank() && !looksLikeUuid(direct) && !direct.equals(ownerPlayerUuid)) {
            return direct;
        }
        String fromUuid = resolveOwnerDisplayNameByUuid(ownerPlayerUuid);
        if (!fromUuid.isBlank()) {
            return fromUuid;
        }
        if (!direct.isBlank() && !looksLikeUuid(direct)) {
            return direct;
        }
        if (ownerPlayerUuid != null && !ownerPlayerUuid.isBlank() && !looksLikeUuid(ownerPlayerUuid)) {
            return ownerPlayerUuid;
        }
        return "未知";
    }

    private String resolveOwnerDisplayNameByUuid(String ownerUuid) {
        if (ownerUuid == null || ownerUuid.isBlank() || !looksLikeUuid(ownerUuid)) {
            return "";
        }
        try {
            java.util.UUID uuid = java.util.UUID.fromString(ownerUuid.trim());
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null) {
                if (minecraft.player != null && uuid.equals(minecraft.player.getUUID())) {
                    String local = minecraft.player.getGameProfile().getName();
                    if (local != null && !local.isBlank()) {
                        return local;
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
            java.util.UUID.fromString(value.trim());
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY < listStartY || mouseY > listEndY) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        List<PetPermissionEntry> items = buildVisibleItems();
        int visibleRows = Math.max(0, (listEndY - listStartY) / ROW_HEIGHT);
        int maxScroll = Math.max(0, items.size() - visibleRows);
        if (maxScroll <= 0) {
            return true;
        }
        if (delta < 0) {
            scrollOffset = Math.min(maxScroll, scrollOffset + 1);
        } else if (delta > 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
        }
        init();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            closeToParent();
            return true;
        }
        if (searchEdit != null && searchEdit.isFocused() && (keyCode == 257 || keyCode == 335)) {
            applySearch();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
