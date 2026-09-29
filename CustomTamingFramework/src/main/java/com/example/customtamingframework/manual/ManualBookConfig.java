package com.example.customtamingframework.manual;

import com.example.customtamingframework.CustomTamingFramework;
import com.example.customtamingframework.config.CtfUiSettings;
import com.example.customtamingframework.config.CtfSnbtConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ManualBookConfig {
    public static final String USER_MANUAL_ID = "user_manual";
    public static final String ADMIN_MANUAL_ID = "admin_manual";
    private static final String FILE_NAME = "manual.snbt";

    private static ManualBookData userManual = defaultUserManual();
    private static ManualBookData adminManual = defaultAdminManual();

    private ManualBookConfig() {
    }

    public static void init() {
        Path settingsDir = CtfSnbtConfig.settingsDirectory();
        if (settingsDir == null) {
            return;
        }

        Path file = settingsDir.resolve(FILE_NAME);
        if (Files.notExists(file)) {
            writeDefaults(file);
        }
        load(file);
        syncUserManualCover(CtfUiSettings.visibleForNonOp(
                CtfSnbtConfig.uiSettings(),
                CtfUiSettings.SHOW_NON_OP_GENERAL_CONFIG_SCREEN));
    }

    public static ManualBookData manual(String id) {
        if (ADMIN_MANUAL_ID.equals(id)) {
            return adminManual;
        }
        return userManual;
    }

    public static String titleKey(String id) {
        return switch (id) {
            case ADMIN_MANUAL_ID -> "item.custom_taming_framework.admin_manual";
            default -> "item.custom_taming_framework.user_manual";
        };
    }

    private static void load(Path file) {
        try {
            CompoundTag root = TagParser.parseTag(Files.readString(file, StandardCharsets.UTF_8));
            userManual = readManual(root.getCompound(USER_MANUAL_ID), defaultUserManual());
            adminManual = readManual(root.getCompound(ADMIN_MANUAL_ID), defaultAdminManual());
            CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] manual.snbt.loaded=PASS");
        } catch (Exception e) {
            CustomTamingFramework.LOGGER.error("Failed to load manual.snbt; using defaults", e);
            userManual = defaultUserManual();
            adminManual = defaultAdminManual();
        }
    }

    private static ManualBookData readManual(CompoundTag tag, ManualBookData fallback) {
        if (tag.isEmpty()) {
            return fallback;
        }
        String cover = tag.contains("cover", Tag.TAG_STRING) ? tag.getString("cover") : fallback.cover();
        List<ManualChapter> chapters = new ArrayList<>();
        ListTag list = tag.getList("chapters", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            String raw = list.getString(i);
            int split = raw.indexOf(':');
            String chapterTitle = split >= 0 ? raw.substring(0, split).trim() : raw.trim();
            String chapterBody = split >= 0 ? raw.substring(split + 1).trim() : "";
            if (chapterTitle.isBlank()) {
                chapterTitle = "章节" + (i + 1);
            }
            chapters.add(new ManualChapter(chapterTitle, chapterBody));
        }
        if (chapters.isEmpty()) {
            return fallback;
        }
        return new ManualBookData(cover, List.copyOf(chapters));
    }

    private static void writeDefaults(Path file) {
        try {
            Files.createDirectories(file.getParent());
            write(file, defaultUserManual(), defaultAdminManual());
            CustomTamingFramework.LOGGER.info("[CTF_SELFTEST] manual.snbt.defaults_generated=PASS");
        } catch (IOException e) {
            CustomTamingFramework.LOGGER.error("Failed to generate manual.snbt defaults", e);
        }
    }

    public static void syncUserManualCover(boolean generalConfigVisible) {
        userManual = new ManualBookData(buildUserManualCover(generalConfigVisible), userManual.chapters());
        save();
    }

    private static void save() {
        Path settingsDir = CtfSnbtConfig.settingsDirectory();
        if (settingsDir == null) {
            return;
        }
        Path file = settingsDir.resolve(FILE_NAME);
        try {
            write(file, userManual, adminManual);
        } catch (IOException e) {
            CustomTamingFramework.LOGGER.error("Failed to save manual.snbt", e);
        }
    }

    private static void write(Path file, ManualBookData user, ManualBookData admin) throws IOException {
        Files.createDirectories(file.getParent());
        CompoundTag root = new CompoundTag();
        root.put(USER_MANUAL_ID, writeManual(user));
        root.put(ADMIN_MANUAL_ID, writeManual(admin));
        Files.writeString(file, CtfSnbtConfig.prettyPrintTag(root), StandardCharsets.UTF_8);
    }

    private static CompoundTag writeManual(ManualBookData data) {
        CompoundTag tag = new CompoundTag();
        tag.putString("cover", data.cover());
        ListTag chapters = new ListTag();
        for (ManualChapter chapter : data.chapters()) {
            chapters.add(net.minecraft.nbt.StringTag.valueOf(chapter.title() + ":" + chapter.content()));
        }
        tag.put("chapters", chapters);
        return tag;
    }

    private static ManualBookData defaultUserManual() {
        return new ManualBookData(
                buildUserManualCover(false),
                List.of(
                        new ManualChapter("快速开始", "先获取驯兽核心，再对准可驯化生物使用。\\n手册后续会补完整流程。"),
                        new ManualChapter("标签页", "通用配置和宠物配置共用同一套模块体系。\\n模块的添加、编辑和排序都会在这里说明。"),
                        new ManualChapter("数据变量", "这里会写进化点、阶段值和手册交互相关说明。\\n后续还会补充更完整的配置范例。")
                )
        );
    }

    private static String buildUserManualCover(boolean generalConfigVisible) {
        String firstLine = generalConfigVisible
                ? "你想起了一些遗失的记忆，好像是，shift+T？也对着生物试试呢？"
                : "你想起了一些遗失的记忆，好像是用准心对准生物然后，shift+T？";
        return firstLine + "\\n这里会逐步补充驯兽、数据和配置相关说明。";
    }

    private static ManualBookData defaultAdminManual() {
        return new ManualBookData(
                "这是给整合包作者和管理员看的 CTF 管理手册。\\n内容偏向配置、调试和扩展。",
                List.of(
                        new ManualChapter("配置总览", "basic.snbt、GeneralGUI.snbt 与 manual.snbt 都属于这个系统。\\n它们分别控制基础规则、标签页和手册正文。"),
                        new ManualChapter("设置入口", "settings 栏目会逐步扩展为游戏内可调的开关和规则。\\n当前先保留可见性控制等基础项。"),
                        new ManualChapter("扩展方向", "后续版本会继续补充更细的物品、规则与交互入口。\\n同时也会继续完善手册与设置编辑器。")
                )
        );
    }

    public record ManualBookData(String cover, List<ManualChapter> chapters) {
        public ManualBookData {
            cover = cover == null ? "" : cover;
            chapters = chapters == null ? List.of() : List.copyOf(chapters);
        }
    }

    public record ManualChapter(String title, String content) {
        public ManualChapter {
            title = title == null ? "" : title;
            content = content == null ? "" : content;
        }
    }
}
