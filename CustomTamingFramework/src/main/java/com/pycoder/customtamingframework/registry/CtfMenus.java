package com.pycoder.customtamingframework.registry;

import com.pycoder.customtamingframework.CustomTamingFramework;
import com.pycoder.customtamingframework.menu.ModuleInventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CtfMenus {
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, CustomTamingFramework.MOD_ID);

    public static final RegistryObject<MenuType<ModuleInventoryMenu>> MODULE_INVENTORY =
            MENUS.register("module_inventory", () -> IForgeMenuType.create(ModuleInventoryMenu::new));

    private CtfMenus() {
    }

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
