package com.pockyl.hextale.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.menu.AlembicMenu;
import com.pockyl.hextale.menu.ResearchTableMenu;
import com.pockyl.hextale.menu.RuneEngraverMenu;
import com.pockyl.hextale.menu.SeparatorMenu;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Hextale.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ResearchTableMenu>> RESEARCH_TABLE =
            MENUS.register("research_table", () -> IMenuTypeExtension.create(ResearchTableMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<AlembicMenu>> ALEMBIC =
            MENUS.register("alembic", () -> IMenuTypeExtension.create(AlembicMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<SeparatorMenu>> SEPARATOR =
            MENUS.register("separator", () -> IMenuTypeExtension.create(SeparatorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<RuneEngraverMenu>> RUNE_ENGRAVER =
            MENUS.register("rune_engraver", () -> IMenuTypeExtension.create(RuneEngraverMenu::new));

    private ModMenus() {
    }

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
