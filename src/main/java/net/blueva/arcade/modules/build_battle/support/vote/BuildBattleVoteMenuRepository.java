package net.blueva.arcade.modules.build_battle.support.vote;

import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.ui.menu.BedrockButtonDefinition;
import net.blueva.arcade.api.ui.menu.BedrockMenuDefinition;
import net.blueva.arcade.api.ui.menu.BedrockSimpleMenuDefinition;
import net.blueva.arcade.api.ui.menu.JavaItemDefinition;
import net.blueva.arcade.api.ui.menu.JavaMenuItem;
import net.blueva.arcade.api.ui.menu.MenuDefinition;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BuildBattleVoteMenuRepository {

    private static final int[] MENU_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };
    private static final int CLOSE_SLOT = 40;
    private static final int MENU_SIZE = 45;

    private final ModuleConfigAPI moduleConfig;
    private MenuDefinition<Material> cachedMenu;

    public BuildBattleVoteMenuRepository(ModuleConfigAPI moduleConfig) {
        this.moduleConfig = moduleConfig;
    }

    public void loadMenus() {
        cachedMenu = buildThemeMenu();
    }

    public MenuDefinition<Material> getMenu(String id) {
        if (id == null) {
            return null;
        }
        if (BuildBattleVoteService.MENU_THEMES.equals(id)) {
            return cachedMenu;
        }
        return null;
    }

    private MenuDefinition<Material> buildThemeMenu() {
        List<String> themes = moduleConfig.getStringList("themes");
        if (themes == null) {
            themes = List.of();
        }

        String title = "<green>Build Battle</green> <gray>-</gray> <yellow>Vote Theme</yellow>";
        List<JavaMenuItem<Material>> items = new ArrayList<>();

        int slotIndex = 0;
        for (String theme : themes) {
            if (theme == null || theme.isBlank()) {
                continue;
            }
            String themeId = toThemeId(theme);
            int slot = MENU_SLOTS[Math.min(slotIndex, MENU_SLOTS.length - 1)];
            slotIndex++;

            JavaItemDefinition<Material> def = JavaItemDefinition.of(
                    Material.PAPER,
                    1,
                    "<aqua>" + theme + "</aqua>",
                    List.of(
                            "<gray>Vote for " + theme + "</gray>",
                            "<gray>Votes:</gray> <white>{votes_theme_" + themeId + "}</white>",
                            "<green>Click to vote</green>"
                    ),
                    List.of("MODULE;build_battle;vote theme " + themeId)
            );
            items.add(JavaMenuItem.of(slot, def));
        }


        int randomSlot = MENU_SLOTS[Math.min(slotIndex, MENU_SLOTS.length - 1)];
        slotIndex++;
        JavaItemDefinition<Material> randomDef = JavaItemDefinition.of(
                Material.ENDER_PEARL,
                1,
                "<light_purple>Random</light_purple>",
                List.of(
                        "<gray>Let the game pick a random theme.</gray>",
                        "<gray>Votes:</gray> <white>{votes_theme_random}</white>",
                        "<green>Click to vote</green>"
                ),
                List.of("MODULE;build_battle;vote theme random")
        );
        items.add(JavaMenuItem.of(randomSlot, randomDef));


        JavaItemDefinition<Material> closeDef = JavaItemDefinition.of(
                Material.BARRIER,
                1,
                "<red>Close</red>",
                List.of("<gray>Close this menu.</gray>"),
                List.of("CLOSE")
        );
        items.add(JavaMenuItem.of(CLOSE_SLOT, closeDef));

        BedrockMenuDefinition bedrockMenu = buildBedrockMenu(themes);
        return new MenuDefinition<>(title, MENU_SIZE, items, bedrockMenu);
    }

    private BedrockMenuDefinition buildBedrockMenu(List<String> themes) {
        String title = "<green>Build Battle</green> <gray>-</gray> <yellow>Vote Theme</yellow>";
        List<String> content = List.of("<gray>Vote for the build theme.</gray>");
        List<BedrockButtonDefinition> buttons = new ArrayList<>();

        for (String theme : themes) {
            if (theme == null || theme.isBlank()) {
                continue;
            }
            String themeId = toThemeId(theme);
            buttons.add(BedrockButtonDefinition.of(
                    "<aqua>" + theme + "</aqua> <dark_gray>({votes_theme_" + themeId + "})</dark_gray>",
                    null,
                    List.of("MODULE;build_battle;vote theme " + themeId)
            ));
        }

        buttons.add(BedrockButtonDefinition.of(
                "<light_purple>Random</light_purple> <dark_gray>({votes_theme_random})</dark_gray>",
                null,
                List.of("MODULE;build_battle;vote theme random")
        ));

        buttons.add(BedrockButtonDefinition.of(
                "<red>Close</red>",
                null,
                List.of("CLOSE")
        ));

        return new BedrockSimpleMenuDefinition(title, content, buttons);
    }

    static String toThemeId(String theme) {
        return theme.toLowerCase(Locale.ROOT).replace(" ", "_");
    }
}
