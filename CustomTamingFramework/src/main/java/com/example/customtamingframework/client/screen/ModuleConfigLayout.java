package com.example.customtamingframework.client.screen;

final class ModuleConfigLayout {
    private ModuleConfigLayout() {
    }

    static int panelWidth(int screenWidth) {
        return Math.min(400, screenWidth * 2 / 3);
    }

    static int panelHeight(ModuleSystem.Module module) {
        if (module != null && module.type() == ModuleSystem.ModuleType.DATA) {
            return 145;
        }
        if (module != null && (module.type() == ModuleSystem.ModuleType.IMAGE
                || module.type() == ModuleSystem.ModuleType.INVENTORY)) {
            return 230;
        }
        return 120;
    }

    static int imagePreviewWidth(int panelWidth) {
        return Math.min(150, Math.max(120, panelWidth - 210));
    }

    static int imagePreviewHeight(int panelHeight) {
        return panelHeight - 64;
    }

    static int clampSliderValue(int value) {
        return Math.max(0, Math.min(500, value));
    }

    static int clampSliderValue(int value, int max) {
        return Math.max(0, Math.min(max, value));
    }
}
