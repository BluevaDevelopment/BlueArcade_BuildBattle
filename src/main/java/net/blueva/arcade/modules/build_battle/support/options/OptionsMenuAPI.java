package net.blueva.arcade.modules.build_battle.support.options;

import net.blueva.arcade.api.ui.MenuAPI;
import net.blueva.arcade.api.ui.ModuleActionHandler;
import net.blueva.arcade.api.ui.menu.DynamicMenuDefinition;
import net.blueva.arcade.api.ui.menu.MenuDefinition;
import net.blueva.arcade.api.ui.menu.MenuEntry;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class OptionsMenuAPI implements MenuAPI<Player, Material> {

    private final MenuAPI<Player, Material> delegate;
    private final OptionsService optionsService;

    public OptionsMenuAPI(MenuAPI<Player, Material> delegate, OptionsService optionsService) {
        this.delegate = delegate;
        this.optionsService = optionsService;
    }

    @Override
    public boolean openMenu(Player player, MenuDefinition<Material> menu, Map<String, String> placeholders) {
        return delegate.openMenu(player, menu, placeholders);
    }

    @Override
    public boolean openDynamicMenu(Player player, DynamicMenuDefinition<Material> menu, List<MenuEntry<Material>> entries, int page, Map<String, String> placeholders) {
        return delegate.openDynamicMenu(player, menu, entries, page, placeholders);
    }

    @Override
    public boolean isBedrockPlayer(Player player) {
        return delegate.isBedrockPlayer(player);
    }

    @Override
    public void registerModuleActionHandler(String moduleId, ModuleActionHandler<Player> handler) {
        delegate.registerModuleActionHandler(moduleId, handler);
    }

    @Override
    public void unregisterModuleActionHandler(String moduleId) {
        delegate.unregisterModuleActionHandler(moduleId);
    }

    @Override
    public boolean openMenuById(Player player, String menuId) {
        String simpleName = menuId;
        if (menuId.startsWith("build_battle_options_")) {
            simpleName = menuId.substring("build_battle_options_".length());
        } else if (menuId.startsWith("options_")) {
            simpleName = menuId.substring("options_".length());
        }
        return optionsService.openMenuById(player, simpleName);
    }
}
