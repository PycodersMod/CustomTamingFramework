package com.example.customtamingframework.client.screen;

import com.example.customtamingframework.CustomTamingFramework;
import com.example.customtamingframework.config.CtfSnbtConfig;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

final class NativeFilePicker {
    private NativeFilePicker() {
    }

    static String pickAndCopyPngToResources() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(1);
            filters.put(stack.UTF8("*.png"));
            filters.flip();

            String selected = TinyFileDialogs.tinyfd_openFileDialog(
                    "选择图片 (PNG)",
                    "",
                    filters,
                    "PNG 图片 (*.png)",
                    false
            );
            if (selected == null || selected.isBlank()) {
                return null;
            }

            Path source = Path.of(selected);
            if (!Files.exists(source)) {
                return null;
            }

            String lowerName = source.getFileName().toString().toLowerCase(Locale.ROOT);
            if (!lowerName.endsWith(".png")) {
                return null;
            }

            String copiedName = "img_" + System.currentTimeMillis() + ".png";
            Path destDir = CtfSnbtConfig.resourcesDirectory();
            Files.copy(source, destDir.resolve(copiedName), StandardCopyOption.REPLACE_EXISTING);
            return copiedName;
        } catch (Throwable t) {
            CustomTamingFramework.LOGGER.error("Native file picker failed", t);
            return null;
        }
    }
}
