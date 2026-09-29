package com.pycoder.customtamingframework.client.screen;

import com.pycoder.customtamingframework.CustomTamingFramework;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Data model, SNBT serialization, and rendering helpers for the draggable module system.
 * Each tab can contain a list of modules positioned at (x,y) relative to the tab body origin.
 * v0.1.4-alpha: first module type is PLAIN_TEXT.
 */
public final class ModuleSystem {

    private ModuleSystem() {}

    public enum ModuleType {
        PLAIN_TEXT("plain_text"),
        IMAGE("image"),
        DATA("data"),
        INVENTORY("inventory");

        private final String snbtKey;
        ModuleType(String snbtKey) { this.snbtKey = snbtKey; }
        public String snbtKey() { return snbtKey; }

        public static ModuleType fromSnbtKey(String key) {
            for (ModuleType t : values()) {
                if (t.snbtKey.equals(key)) return t;
            }
            return PLAIN_TEXT;
        }
    }

    /**
     * Module record stored as a CompoundTag in the tab's "modules" ListTag.
     * Fields: type (String), x (int), y (int), config (CompoundTag).
     * PLAIN_TEXT config: {text: String, color: int (default 0xE0E0E0)}.
     * INVENTORY config: {module_id: String, name: String, show_name: boolean, rows: int, cols: int,
     *                    death_drop: boolean, stack_mode: String, filter_mode: String,
     *                    filters: ListTag<String> }.
     */
    public record Module(ModuleType type, int x, int y, CompoundTag config) {

        public CompoundTag toSnbt() {
            CompoundTag tag = new CompoundTag();
            tag.putString("type", type.snbtKey());
            tag.putInt("x", x);
            tag.putInt("y", y);
            tag.put("config", config.copy());
            return tag;
        }

        public static Module fromSnbt(CompoundTag tag) {
            ModuleType type = ModuleType.fromSnbtKey(tag.getString("type"));
            int x = tag.getInt("x");
            int y = tag.getInt("y");
            CompoundTag config = tag.contains("config", Tag.TAG_COMPOUND)
                    ? tag.getCompound("config").copy()
                    : defaultConfig(type);
            ensureModuleId(config);
            return new Module(type, x, y, config);
        }

        public static CompoundTag defaultConfig(ModuleType type) {
            CompoundTag cfg = new CompoundTag();
            if (type == ModuleType.PLAIN_TEXT) {
                cfg.putString("text", "");
                cfg.putInt("color", 0xE0E0E0);
            } else if (type == ModuleType.IMAGE) {
                cfg.putString("image_file", "");
                cfg.putInt("image_width", 80);
                cfg.putInt("image_height", 80);
            } else if (type == ModuleType.DATA) {
                cfg.putString("data_key", "");
                cfg.putString("data_format", "value");
            } else if (type == ModuleType.INVENTORY) {
                cfg.putString("module_id", UUID.randomUUID().toString());
                cfg.putString("name", "物品栏模块");
                cfg.putBoolean("show_name", true);
                cfg.putInt("rows", 1);
                cfg.putInt("cols", 1);
                cfg.putBoolean("death_drop", true);
                cfg.putString("stack_mode", "vanilla");
                cfg.putString("filter_mode", "blacklist");
                cfg.put("filters", new ListTag());
            }
            return cfg;
        }
    }

    // ── Image texture cache (v0.1.4-alpha) ──
    private static final Map<String, DynamicTexture> imageCache = new HashMap<>();
    private static final Map<String, ResourceLocation> imageLocations = new HashMap<>();
    private static final Map<String, Integer> imageWidths = new HashMap<>();
    private static final Map<String, Integer> imageHeights = new HashMap<>();
    private static Path resourcesDir;

    /** Set the CTF_Settings/resources/ path (called once from screen). */
    public static void setResourcesDir(Path dir) {
        resourcesDir = dir;
    }

    /** Scan the resources directory for .png files and return sorted list. */
    public static List<String> scanResourceFiles() {
        List<String> files = new ArrayList<>();
        if (resourcesDir == null || !Files.isDirectory(resourcesDir)) return files;
        try {
            java.util.stream.Stream<Path> stream = Files.list(resourcesDir);
            stream.filter(p -> p.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".png"))
                  .map(p -> p.getFileName().toString())
                  .sorted()
                  .forEach(files::add);
            stream.close();
        } catch (IOException e) {
            CustomTamingFramework.LOGGER.error("Failed to scan resources directory", e);
        }
        return files;
    }

    /** Get the native dimensions of a loaded image. */
    public static int getImageWidth(String filename) {
        return imageWidths.getOrDefault(filename, 80);
    }
    public static int getImageHeight(String filename) {
        return imageHeights.getOrDefault(filename, 80);
    }

    private static ResourceLocation getOrLoadImage(String filename) {
        if (filename == null || filename.isEmpty()) return null;
        // Already cached
        ResourceLocation cached = imageLocations.get(filename);
        if (cached != null) return cached;

        if (resourcesDir == null) return null;
        Path filePath = resourcesDir.resolve(filename);
        if (!Files.exists(filePath)) return null;

        try {
            byte[] bytes = Files.readAllBytes(filePath);
            com.mojang.blaze3d.platform.NativeImage nativeImage =
                    com.mojang.blaze3d.platform.NativeImage.read(new java.io.ByteArrayInputStream(bytes));
            DynamicTexture tex = new DynamicTexture(nativeImage);
            ResourceLocation rl = new ResourceLocation("custom_taming_framework", "module_img_" + filename.replaceAll("[^a-zA-Z0-9_]", "_"));
            Minecraft.getInstance().getTextureManager().register(rl, tex);
            imageCache.put(filename, tex);
            imageLocations.put(filename, rl);
            imageWidths.put(filename, nativeImage.getWidth());
            imageHeights.put(filename, nativeImage.getHeight());
            return rl;
        } catch (IOException e) {
            CustomTamingFramework.LOGGER.error("Failed to load image module file: {}", filePath, e);
            return null;
        }
    }

    /** Public accessor for the texture loader (used by config popup preview). */
    public static ResourceLocation getOrLoadImageCached(String filename) {
        return getOrLoadImage(filename);
    }

    /** Release all cached image textures (call on screen close). */
    public static void clearImageCache() {
        for (Map.Entry<String, DynamicTexture> entry : imageCache.entrySet()) {
            Minecraft.getInstance().getTextureManager().release(imageLocations.get(entry.getKey()));
        }
        imageCache.clear();
        imageLocations.clear();
        imageWidths.clear();
        imageHeights.clear();
    }

    // ── SNBT I/O ──

    public static List<Module> readModules(CompoundTag tabData) {
        List<Module> result = new ArrayList<>();
        boolean changed = false;
        if (tabData.contains("modules", Tag.TAG_LIST)) {
            ListTag list = tabData.getList("modules", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                Module module = Module.fromSnbt(list.getCompound(i));
                ensureModuleId(module.config());
                result.add(module);
                if (!list.getCompound(i).contains("config", Tag.TAG_COMPOUND)
                        || !list.getCompound(i).getCompound("config").contains("module_id")) {
                    changed = true;
                }
            }
        }
        if (changed) {
            writeModules(tabData, result);
        }
        return result;
    }

    public static void writeModules(CompoundTag tabData, List<Module> modules) {
        ListTag list = new ListTag();
        for (Module m : modules) {
            list.add(m.toSnbt());
        }
        tabData.put("modules", list);
    }

    public static void initModules(CompoundTag tabData) {
        if (!tabData.contains("modules", Tag.TAG_LIST)) {
            tabData.put("modules", new ListTag());
        } else {
            readModules(tabData);
        }
    }

    public static String ensureModuleId(CompoundTag config) {
        if (config == null) {
            return "";
        }
        String id = config.getString("module_id");
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
            config.putString("module_id", id);
        }
        return id;
    }

    public static String moduleId(Module module) {
        if (module == null || module.config() == null) {
            return "";
        }
        return ensureModuleId(module.config());
    }

    public static boolean isInventoryModule(Module module) {
        return module != null && module.type() == ModuleType.INVENTORY;
    }

    public static String inventoryDisplayName(Module module) {
        if (!isInventoryModule(module)) {
            return "";
        }
        String name = module.config().getString("name");
        if (name == null || name.isBlank()) {
            return "物品栏模块";
        }
        return name;
    }

    public static boolean inventoryShowName(Module module) {
        return isInventoryModule(module) && module.config().getBoolean("show_name");
    }

    public static int inventoryRows(Module module) {
        if (!isInventoryModule(module)) {
            return 1;
        }
        return Math.max(1, module.config().getInt("rows"));
    }

    public static int inventoryCols(Module module) {
        if (!isInventoryModule(module)) {
            return 1;
        }
        return Math.max(1, module.config().getInt("cols"));
    }

    public static int inventorySlotCount(Module module) {
        return inventoryRows(module) * inventoryCols(module);
    }

    public static int inventoryGridWidth(Module module) {
        return isInventoryModule(module) ? inventoryCols(module) * 18 + 8 : 0;
    }

    public static int inventoryGridHeight(Module module) {
        return isInventoryModule(module) ? inventoryRows(module) * 18 + 8 : 0;
    }

    public static int inventoryGridX(int originX, Module module, Font font) {
        if (!isInventoryModule(module)) {
            return originX + module.x();
        }
        int ax = originX + module.x();
        int w = moduleWidth(module);
        int gridW = inventoryGridWidth(module);
        return ax + Math.max(0, (w - gridW) / 2);
    }

    public static int inventoryGridY(int originY, Module module, Font font) {
        if (!isInventoryModule(module)) {
            return originY + module.y();
        }
        int ay = originY + module.y();
        int labelH = inventoryShowName(module) && font != null ? font.lineHeight + MODULE_PAD : 0;
        return ay + labelH;
    }

    public static int inventorySlotIndexAt(Module module, Font font, int originX, int originY, double mouseX, double mouseY) {
        if (!isInventoryModule(module)) {
            return -1;
        }
        int gridX = inventoryGridX(originX, module, font);
        int gridY = inventoryGridY(originY, module, font);
        int relX = (int) mouseX - (gridX + 4);
        int relY = (int) mouseY - (gridY + 4);
        if (relX < 0 || relY < 0) {
            return -1;
        }
        int cols = inventoryCols(module);
        int rows = inventoryRows(module);
        if (relX >= cols * 18 || relY >= rows * 18) {
            return -1;
        }
        int col = relX / 18;
        int row = relY / 18;
        int index = row * cols + col;
        return index >= 0 && index < inventorySlotCount(module) ? index : -1;
    }

    public static boolean inventoryDeathDrop(Module module) {
        return isInventoryModule(module) && module.config().getBoolean("death_drop");
    }

    public static String inventoryStackMode(Module module) {
        if (!isInventoryModule(module)) {
            return "vanilla";
        }
        String mode = module.config().getString("stack_mode");
        if (mode == null || mode.isBlank()) {
            return "vanilla";
        }
        return mode;
    }

    public static boolean inventoryAllowAllItems(Module module) {
        return isInventoryModule(module) && module.config().getBoolean("all_items");
    }

    public static String inventoryFilterMode(Module module) {
        if (!isInventoryModule(module)) {
            return "blacklist";
        }
        String mode = module.config().getString("filter_mode");
        if (mode == null || mode.isBlank()) {
            return "blacklist";
        }
        return mode;
    }

    public static List<String> inventoryFilters(Module module) {
        List<String> filters = new ArrayList<>();
        if (!isInventoryModule(module)) {
            return filters;
        }
        if (module.config().contains("filters", Tag.TAG_LIST)) {
            ListTag list = module.config().getList("filters", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                String id = list.getString(i);
                if (id != null && !id.isBlank()) {
                    filters.add(id);
                }
            }
        }
        return filters;
    }

    public static boolean inventoryAllowsItem(Module module, ItemStack stack) {
        if (!isInventoryModule(module) || stack == null || stack.isEmpty()) {
            return false;
        }
        if (inventoryAllowAllItems(module)) {
            return true;
        }
        String itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()) != null
                ? net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString()
                : "";
        if (itemId.isBlank()) {
            return false;
        }
        List<String> filters = inventoryFilters(module);
        boolean whiteList = !"blacklist".equalsIgnoreCase(inventoryFilterMode(module));
        if (filters.isEmpty()) {
            return !whiteList;
        }
        boolean match = filters.contains(itemId);
        if (whiteList) {
            return match;
        }
        return !match;
    }

    public static int inventoryMaxStackSize(Module module, ItemStack stack) {
        if (!isInventoryModule(module) || stack == null || stack.isEmpty()) {
            return 1;
        }
        String mode = inventoryStackMode(module);
        if ("no_stack".equalsIgnoreCase(mode)) {
            return 1;
        }
        if ("infinite".equalsIgnoreCase(mode)) {
            return 9999;
        }
        return Math.max(1, stack.getMaxStackSize());
    }

    // ── Sizing ──

    public static final int MODULE_MIN_W = 80;
    public static final int MODULE_MIN_H = 22;
    public static final int MODULE_PAD = 4;
    public static final int LINE_SPACE = 9;

    public static int moduleWidth(Module m) {
        if (m.type() == ModuleType.IMAGE) {
            int cfgW = m.config().contains("image_width") ? m.config().getInt("image_width") : 80;
            return Math.max(1, cfgW);
        }
        if (m.type() == ModuleType.INVENTORY) {
            int gridW = inventoryCols(m) * 18 + 8;
            if (inventoryShowName(m)) {
                String label = inventoryDisplayName(m);
                int labelW = label == null ? 0 : label.length() * 6;
                return Math.max(gridW, labelW + MODULE_PAD * 2);
            }
            return Math.max(MODULE_MIN_W, gridW);
        }
        return Math.max(MODULE_MIN_W, 80);
    }

    public static int moduleWidth(Module m, Font font, Map<String, String> dataVars) {
        if (m.type() == ModuleType.IMAGE) {
            return moduleWidth(m);
        }
        if (m.type() == ModuleType.DATA) {
            String displayText = dataDisplayText(m, dataVars);
            int textWidth = font != null ? font.width(displayText) : displayText.length() * 6;
            return Math.max(MODULE_MIN_W, textWidth + MODULE_PAD * 2);
        }
        return moduleWidth(m);
    }

    public static int moduleHeight(Module m, Font font) {
        return moduleHeight(m, font, Map.of());
    }

    public static int moduleHeight(Module m, Font font, Map<String, String> dataVars) {
        if (m.type() == ModuleType.IMAGE) {
            int cfgH = m.config().contains("image_height") ? m.config().getInt("image_height") : 80;
            return Math.max(1, cfgH);
        }
        if (m.type() == ModuleType.INVENTORY) {
            int labelH = inventoryShowName(m) && font != null ? font.lineHeight + MODULE_PAD : 0;
            int gridH = inventoryRows(m) * 18 + 8;
            return Math.max(MODULE_MIN_H, labelH + gridH);
        }
        if (m.type() == ModuleType.PLAIN_TEXT) {
            String text = m.config().getString("text");
            if (text.isBlank()) text = "纯文本";
            int maxW = moduleWidth(m) - MODULE_PAD * 2;
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(text), maxW);
            return Math.max(MODULE_MIN_H, lines.size() * LINE_SPACE + MODULE_PAD * 2);
        }
        if (m.type() == ModuleType.DATA) {
            return Math.max(MODULE_MIN_H, font != null ? font.lineHeight + MODULE_PAD * 2 : MODULE_MIN_H);
        }
        return MODULE_MIN_H;
    }

    // ── Hit test (relative to tab body origin) ──

    public static int hitTest(List<Module> modules, int mx, int my, Font font) {
        return hitTest(modules, mx, my, font, Map.of());
    }

    public static int hitTest(List<Module> modules, int mx, int my, Font font, Map<String, String> dataVars) {
        int hit = hitTestLayer(modules, mx, my, font, dataVars, false);
        if (hit >= 0) {
            return hit;
        }
        return hitTestLayer(modules, mx, my, font, dataVars, true);
    }

    private static int hitTestLayer(List<Module> modules, int mx, int my, Font font,
                                    Map<String, String> dataVars, boolean imagesOnly) {
        for (int i = modules.size() - 1; i >= 0; i--) {
            Module m = modules.get(i);
            if ((m.type() == ModuleType.IMAGE) != imagesOnly) {
                continue;
            }
            int w = moduleWidth(m, font, dataVars);
            int h = moduleHeight(m, font, dataVars);
            if (mx >= m.x() && mx <= m.x() + w && my >= m.y() && my <= m.y() + h) {
                return i;
            }
        }
        return -1;
    }

    // ── Rendering (backward-compatible, no data map) ──

    /**
     * Render all modules at (originX, originY) with z-ordering isolation.
     */
    public static void renderModules(GuiGraphics graphics, Font font, int originX, int originY,
                                     List<Module> modules, int hoveredIdx, int draggedIdx) {
        renderModules(graphics, font, originX, originY, modules, hoveredIdx, draggedIdx, Map.of(), Map.of());
    }

    /**
     * Render all modules with an optional data map for DATA modules.
     * v0.1.4-alpha: supports IMAGE and DATA module types.
     * Uses two-pass rendering: (1) all fills/outlines, flush; (2) all content (text/blit), flush.
     * This prevents fill/blit interleaving from breaking shader state.
     */
    public static void renderModules(GuiGraphics graphics, Font font, int originX, int originY,
                                     List<Module> modules, int hoveredIdx, int draggedIdx,
                                     Map<String, String> dataVars) {
        renderModules(graphics, font, originX, originY, modules, hoveredIdx, draggedIdx, dataVars, Map.of());
    }

    public static void renderModules(GuiGraphics graphics, Font font, int originX, int originY,
                                     List<Module> modules, int hoveredIdx, int draggedIdx,
                                     Map<String, String> dataVars, Map<String, List<ItemStack>> inventoryStacks) {
        // PASS 1: images first so they stay behind text/data modules.
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            if (m.type() != ModuleType.IMAGE) {
                continue;
            }
            int ax = originX + m.x();
            int ay = originY + m.y();
            int w = moduleWidth(m, font, dataVars);
            int h = moduleHeight(m, font, dataVars);
            boolean isDragged = (i == draggedIdx);
            boolean isHovered = (i == hoveredIdx);

            int imgBg = isDragged ? 0x44555577 : isHovered ? 0x443A3A50 : 0x44333344;
            graphics.fill(ax, ay, ax + w, ay + h, imgBg);
            graphics.renderOutline(ax, ay, w, h, isDragged ? 0xFF88FF88 : isHovered ? 0xFFAAAAFF : 0xFF666688);
        }

        // PASS 2: text/data modules above images.
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            if (m.type() == ModuleType.IMAGE) {
                continue;
            }
            int ax = originX + m.x();
            int ay = originY + m.y();
            int w = moduleWidth(m, font, dataVars);
            int h = moduleHeight(m, font, dataVars);
            boolean isDragged = (i == draggedIdx);
            boolean isHovered = (i == hoveredIdx);
            int bgColor = isDragged ? 0xFF555577 : isHovered ? 0xFF3A3A50 : 0xFF333344;
            int borderColor = isDragged ? 0xFF88FF88 : isHovered ? 0xFFAAAAFF : 0xFF666688;
            graphics.fill(ax, ay, ax + w, ay + h, bgColor);
            graphics.renderOutline(ax, ay, w, h, borderColor);
        }

        // Flush all fills
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
        RenderSystem.disableDepthTest();

        // PASS 3: image content first so images stay behind everything else.
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            if (m.type() != ModuleType.IMAGE) {
                continue;
            }
            int ax = originX + m.x();
            int ay = originY + m.y();
            int w = moduleWidth(m, font, dataVars);
            int h = moduleHeight(m, font, dataVars);

            String imgFile = m.config().getString("image_file");
            if (imgFile.isEmpty()) {
                graphics.drawCenteredString(font, Component.literal("§8[未选择图片]"), ax + w / 2, ay + h / 2 - 4, 0x808080);
            } else {
                ResourceLocation rl = getOrLoadImage(imgFile);
                if (rl != null) {
                    int natW = getImageWidth(imgFile);
                    int natH = getImageHeight(imgFile);
                    RenderSystem.setShaderTexture(0, rl);
                    graphics.blit(rl, ax, ay, w, h, 0.0F, 0.0F, natW, natH, natW, natH);
                } else {
                    graphics.drawCenteredString(font, Component.literal("§8[图片未找到]"), ax + w / 2, ay + h / 2 - 4, 0x808080);
                }
            }
        }

        // PASS 4: non-image content above image modules.
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            int ax = originX + m.x();
            int ay = originY + m.y();
            int w = moduleWidth(m, font, dataVars);
            int h = moduleHeight(m, font, dataVars);

            if (m.type() == ModuleType.IMAGE) {
                continue;
            } else if (m.type() == ModuleType.PLAIN_TEXT) {
                String text = m.config().getString("text");
                int color = m.config().contains("color") ? m.config().getInt("color") : 0xE0E0E0;
                if (text.isBlank()) text = "纯文本" + (i+1);
                int maxW = w - MODULE_PAD * 2;
                List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(text), maxW);
                int lineY = ay + MODULE_PAD;
                for (net.minecraft.util.FormattedCharSequence line : lines) {
                    graphics.drawString(font, line, ax + MODULE_PAD, lineY, color, false);
                    lineY += LINE_SPACE;
                }
            } else if (m.type() == ModuleType.DATA) {
                String displayText = dataDisplayText(m, dataVars);
                graphics.drawString(font, Component.literal(displayText), ax + MODULE_PAD, ay + MODULE_PAD, 0xE0E0E0, false);
            } else if (m.type() == ModuleType.INVENTORY) {
                renderInventoryModule(graphics, font, ax, ay, w, h, m, inventoryStacks);
            }
        }

        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
        RenderSystem.enableDepthTest();
    }

    private static void renderInventoryModule(GuiGraphics graphics, Font font, int ax, int ay, int w, int h,
                                              Module module, Map<String, List<ItemStack>> inventoryStacks) {
        int labelH = inventoryShowName(module) && font != null ? font.lineHeight + MODULE_PAD : 0;
        int cols = inventoryCols(module);
        int rows = inventoryRows(module);
        int gridW = cols * 18 + 8;
        int gridH = rows * 18 + 8;
        int gridX = ax + Math.max(0, (w - gridW) / 2);
        int gridY = ay + labelH;

        String id = moduleId(module);
        int bgColor = 0xFF33404A;
        graphics.fill(ax, ay, ax + w, ay + h, bgColor);
        graphics.renderOutline(ax, ay, w, h, 0xFF7F8FA3);

        if (inventoryShowName(module)) {
            String label = inventoryDisplayName(module);
            graphics.drawString(font, Component.literal(label), ax + MODULE_PAD, ay + 3, 0xE0E0E0, false);
        }

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int slotIndex = row * cols + col;
                int slotX = gridX + 4 + col * 18;
                int slotY = gridY + 4 + row * 18;
                graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF2E2E36);
                graphics.renderOutline(slotX, slotY, 18, 18, 0xFF555566);
                List<ItemStack> stacks = inventoryStacks != null ? inventoryStacks.get(id) : null;
                if (stacks != null && slotIndex < stacks.size()) {
                    ItemStack stack = stacks.get(slotIndex);
                    if (stack != null && !stack.isEmpty()) {
                        graphics.renderItem(stack, slotX + 1, slotY + 1);
                        graphics.renderItemDecorations(font, stack, slotX + 1, slotY + 1);
                    }
                }
            }
        }
    }

    private static String dataDisplayText(Module m, Map<String, String> dataVars) {
        String key = m.config().getString("data_key");
        if (key == null || key.isBlank()) {
            key = "[数据键]";
        }
        return key + ": " + dataDisplayValue(m, dataVars);
    }

    private static String dataDisplayValue(Module m, Map<String, String> dataVars) {
        String dataKey = m.config().getString("data_key");
        if (dataKey == null || dataKey.isBlank()) {
            return "[无数据]";
        }
        String rawValue = dataVars != null ? dataVars.getOrDefault(dataKey, "") : "";
        if (rawValue.isBlank()) {
            return "[无数据]";
        }
        String dataFormat = m.config().getString("data_format");
        if ("value".equals(dataFormat)) {
            int slash = rawValue.indexOf('/');
            return slash >= 0 ? rawValue.substring(0, slash) : rawValue;
        }
        return rawValue;
    }
}
