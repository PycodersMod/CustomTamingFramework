package com.pycoder.customtamingframework.registry;

import com.pycoder.customtamingframework.CustomTamingFramework;
import com.pycoder.customtamingframework.item.ManualItem;
import com.pycoder.customtamingframework.item.TamingCoreItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CtfItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CustomTamingFramework.MOD_ID);

    public static final RegistryObject<Item> TAMING_CORE = ITEMS.register("taming_core", () -> new TamingCoreItem(new Item.Properties()));
    public static final RegistryObject<Item> USER_MANUAL = ITEMS.register("user_manual", () -> new ManualItem("user_manual", new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ADMIN_MANUAL = ITEMS.register("admin_manual", () -> new ManualItem("admin_manual", new Item.Properties().stacksTo(1)));

    private CtfItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(CtfItems::addCreativeTabItems);
    }

    private static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS || event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES || event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(TAMING_CORE);
            event.accept(USER_MANUAL);
            event.accept(ADMIN_MANUAL);
        }
    }
}
