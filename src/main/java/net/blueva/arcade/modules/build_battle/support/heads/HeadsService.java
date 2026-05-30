package net.blueva.arcade.modules.build_battle.support.heads;

import net.blueva.arcade.api.ModuleAPI;
import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.ui.ItemAPI;
import net.blueva.arcade.api.ui.MessageAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HeadsService {

    private static final int BACK_SLOT = 45;

    private final ModuleConfigAPI moduleConfig;
    private final ItemAPI<Player, ItemStack, Material> itemAPI;



    private static final List<HeadCategory> CATEGORIES = List.of(
            new HeadCategory("mobs", "<red>Mobs", Material.ZOMBIE_HEAD, List.of(
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjQyNTQ4MzhjMzNlYTIyN2ZmY2EyMjNkZGRhYWJmZTBiMDIxNWY3MGRhNjQ5ZTk0NDQ3N2Y0NDM3MGNhNjk1MiJ9fX0=", "<green>Creeper"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTZmYzg1NGJiODRjZjRiNzY5NzI5Nzk3M2UwMmI3OWJjMTA2OTg0NjBiNTFhNjM5YzYwZTVlNDE3NzM0ZTExIn19fQ==", "<dark_green>Zombie"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzAxMjY4ZTljNDkyZGExZjBkODgyNzFjYjQ5MmE0YjMwMjM5NWY1MTVhN2JiZjc3ZjRhMjBiOTVmYzAyZWIyIn19fQ==", "<gray>Skeleton"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2E1OWJiMGE3YTMyOTY1YjNkOTBkOGVhZmE4OTlkMTgzNWY0MjQ1MDllYWRkNGU2YjcwOWFkYTUwYjljZiJ9fX0=", "<dark_purple>Enderman"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2RmNzRlMzIzZWQ0MTQzNjk2NWY1YzU3ZGRmMjgxNWQ1MzMyZmU5OTllNjhmYmI5ZDZjZjVjOGJkNDEzOWYifX19", "<dark_gray>Wither"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzk1M2I2YzY4NDQ4ZTdlNmI2YmY4ZmIyNzNkNzIwM2FjZDhlMWJlMTllODE0ODFlYWQ1MWY0NWRlNTlhOCJ9fX0=", "<dark_gray>Wither Skeleton"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjc4ZWYyZTRjZjJjNDFhMmQxNGJmZGU5Y2FmZjEwMjE5ZjViMWJmNWIzNWE0OWViNTFjNjQ2Nzg4MmNiNWYwIn19fQ==", "<gold>Blaze"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzRlOWM2ZTk4NTgyZmZkOGZmOGZlYjMzMjJjZDE4NDljNDNmYjE2YjE1OGFiYjExY2E3YjQyZWRhNzc0M2ViIn19fQ==", "<pink>Zombified Piglin"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGRlZGJlZTQyYmU0NzJlM2ViNzkxZTdkYmRmYWYxOGM4ZmU1OTNjNjM4YmExMzk2YzllZjY4ZjU1NWNiY2UifX19", "<dark_purple>Witch"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODkwOTFkNzllYTBmNTllZjdlZjk0ZDdiYmE2ZTVmMTdmMmY3ZDQ1NzJjNDRmOTBmNzZjNDgxOWE3MTQifX19", "<white>Iron Golem")
            )),
            new HeadCategory("animals", "<yellow>Animals", Material.PORKCHOP, List.of(
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjIxNjY4ZWY3Y2I3OWRkOWMyMmNlM2QxZjNmNGNiNmUyNTU5ODkzYjZkZjRhNDY5NTE0ZTY2N2MxNmFhNCJ9fX0=", "<light_purple>Pig"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWQ2YzZlZGE5NDJmN2Y1ZjcxYzMxNjFjNzMwNmY0YWVkMzA3ZDgyODk1ZjlkMmIwN2FiNDUyNTcxOGVkYzUifX19", "<white>Cow"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTYzODQ2OWE1OTljZWVmNzIwNzUzNzYwMzI0OGE5YWIxMWZmNTkxZmQzNzhiZWE0NzM1YjM0NmE3ZmFlODkzIn19fQ==", "<yellow>Chicken"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjlkMWQzMTEzZWM0M2FjMjk2MWRkNTlmMjgxNzVmYjQ3MTg4NzNjNmM0NDhkZmNhODcyMjMxN2Q2NyJ9fX0=", "<gray>Wolf"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTY1N2NkNWMyOTg5ZmY5NzU3MGZlYzRkZGNkYzY5MjZhNjhhMzM5MzI1MGMxYmUxZjBiMTE0YTFkYjEifX19", "<yellow>Ocelot"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjMxZjljY2M2YjNlMzJlY2YxM2I4YTExYWMyOWNkMzNkMThjOTVmYzczZGI4YTY2YzVkNjU3Y2NiOGJlNzAifX19", "<white>Sheep"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMDE0MzNiZTI0MjM2NmFmMTI2ZGE0MzRiODczNWRmMWViNWIzY2IyY2VkZTM5MTQ1OTc0ZTljNDgzNjA3YmFjIn19fQ==", "<blue>Squid"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODIyZDhlNzUxYzhmMmZkNGM4OTQyYzQ0YmRiMmY1Y2E0ZDhhZThlNTc1ZWQzZWIzNGMxOGE4NmU5M2IifX19", "<aqua>Villager")
            )),
            new HeadCategory("blocks", "<gold>Blocks", Material.GRASS_BLOCK, List.of(
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGU5YjhhYWU3ZjljYzc2ZDYyNWNjYjhhYmM2ODZmMzBkMzhmOWU2YzQyNTMzMDk4YjlhZDU3N2Y5MWMzMzNjIn19fQ==", "<gray>Stone"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWFiNDNiOGMzZDM0ZjEyNWU1YTNmOGI5MmNkNDNkZmQxNGM2MjQwMmMzMzI5ODQ2MWQ0ZDRkN2NlMmQzYWVhIn19fQ==", "<gold>Dirt"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTk1NTM0ZTAyYzU5YjMzZWNlNTYxOTI4MDMzMTk3OTc3N2UwMjVmYTVmYTgxYWU3NWU5OWZkOGVmZGViYjgifX19", "<gray>Cobblestone"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDNjNzQxNDlkYmM0MTM0ZDhiNWUzYmJlMjk5N2JhMzZhOGE4MDJlMWFmOTI5NThhNDkzYjczNmYxZjQ2OGM4In19fQ==", "<gold>Oak Log"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDM3ZGJjNzE5MWJmYzM5YTQ1MDRlMmZhODkwYjAyNTlmNTFjMDY0ODIyMWVlZTliNDdmNGRhMmIzZDA1ZjIxIn19fQ==", "<green>Leaves"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzhjOWE3MzAyNjljZTFkZTNlOWZhMDY0YWZiMzcwY2JjZDA3NjZkNzI5ZjNlMjllNGYzMjBhNDMzYjA5OGI1In19fQ==", "<green>Cactus"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzNmZWQ1MTRjM2UyMzhjYTdhYzFjOTRiODk3ZmY2NzExYjFkYmU1MDE3NGFmYzIzNWM4ZjgwZDAyOSJ9fX0=", "<green>Melon"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzAxNDYxOTczNjM0NTI1MTk2ZWNjNzU3NjkzYjE3MWFkYTRlZjI0YWE5MjgzNmY0MmVhMTFiZDc5YzNhNTAyZCJ9fX0=", "<aqua>Diamond Block"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjZkMWNlNjk3ZTlkYmFhNGNjZjY0MjUxNmFhYTU5ODEzMzJkYWMxZDMzMWFmZWUyZWUzZGNjODllZmRlZGIifX19", "<yellow>Gold Block"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmJhODQ1OTE0NWQ4M2ZmYzQ0YWQ1OGMzMjYwZTc0Y2E1YTBmNjM0YzdlZWI1OWExYWQzMjM0ODQ5YzkzM2MifX19", "<white>Iron Block"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzI4YTE4MTU2ODlkNzE5NGNmN2RiMDYxYjU5ZjYzMTA2MjY0YjUxMzg3OTc2YTdmYjc0YWI3OWI1NjQxIn19fQ==", "<red>TNT"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDkzNmE0YjZhYjM1OGVmM2YxYjljYmNkOTljYTYyZGM3ODc3ZDIxZWQ1NjQ3ZDhjNzBkOTY0OTA5ZjU3Y2EifX19", "<dark_purple>Obsidian")
            )),
            new HeadCategory("misc", "<light_purple>Misc", Material.PLAYER_HEAD, List.of(
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmFkYzA0OGE3Y2U3OGY3ZGFkNzJhMDdkYTI3ZDg1YzA5MTY4ODFlNTUyMmVlZWQxZTNkYWYyMTdhMzhjMWEifX19", "<green>Question Mark"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzA0MGZlODM2YTZjMmZiZDJjN2E5YzhlYzZiZTUxNzRmZGRmMWFjMjBmNTVlMzY2MTU2ZmE1ZjcxMmUxMCJ9fX0=", "<green>Arrow Up"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzQzNzM0NmQ4YmRhNzhkNTI1ZDE5ZjU0MGE5NWU0ZTc5ZGFlZGE3OTVjYmM1YTEzMjU2MjM2MzEyY2YifX19", "<red>Arrow Down"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmQ2OWUwNmU1ZGFkZmQ4NGU1ZjNkMWMyMTA2M2YyNTUzYjJmYTk0NWVlMWQ0ZDcxNTJmZGM1NDI1YmMxMmE5In19fQ==", "<yellow>Arrow Left"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTliZjMyOTJlMTI2YTEwNWI1NGViYTcxM2FhMWIxNTJkNTQxYTFkODkzODgyOWM1NjM2NGQxNzhlZDIyYmYifX19", "<yellow>Arrow Right"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGUzY2E1YjM5MGQxZTVmMjk3MjgzMjU3Y2U5MGFjNmY4NzgzZDc4NmVjYWVlMDk1YjQ5Y2M2Yjk0NGQ3MmQifX19", "<yellow>Hay Bale"),
                    new HeadDef("eyJ0ZXh0dXhlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWFmZjkzZWJlY2MxZjhmYmQxM2JhNzgzOWVjN2JkY2RlY2FiN2MwN2ZkOGJhNzhlZTc4YWQwYmQzYWNjYmUifX19", "<red>Redstone Lamp"),
                    new HeadDef("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2ViNGM0MWY0ODFlODE2Y2Y0YjUwN2IwYTE3NTk1ZjJiYTFmMjQ2NjRkYzQzMmJlMzQ3ZDRlN2E0ZWIzIn19fQ==", "<light_purple>Mycelium")
            ))
    );

    public HeadsService(ModuleConfigAPI moduleConfig,
                        ItemAPI<Player, ItemStack, Material> itemAPI) {
        this.moduleConfig = moduleConfig;
        this.itemAPI = itemAPI;
    }

    public void openCategoriesMenu(Player player) {
        String title = moduleConfig.getStringFrom("language.yml", "options.heads.title");
        String legacyTitle = itemAPI != null ? itemAPI.formatInventoryTitle(title) : title;
        int size = ((CATEGORIES.size() + 8) / 9 + 1) * 9;
        size = Math.min(size, 54);
        Inventory inv = Bukkit.createInventory(new HeadMenuHolder(HeadMenuHolder.Type.CATEGORIES), size, legacyTitle);

        for (int i = 0; i < CATEGORIES.size() && i < size; i++) {
            HeadCategory cat = CATEGORIES.get(i);
            ItemStack item = decorate(new ItemStack(cat.icon()), cat.displayName(), List.of());
            inv.setItem(i, item);
        }

        inv.setItem(size - 1, createBackItem());
        player.openInventory(inv);
    }

    public void openHeadsMenu(Player player, String categoryId) {
        HeadCategory category = findCategory(categoryId);
        if (category == null) {
            return;
        }

        String title = moduleConfig.getStringFrom("language.yml", "options.heads.category_title")
                .replace("{category}", stripColor(category.displayName()));
        String legacyTitle = itemAPI != null ? itemAPI.formatInventoryTitle(title) : title;
        int size = 54;
        Inventory inv = Bukkit.createInventory(new HeadMenuHolder(HeadMenuHolder.Type.HEADS, categoryId), size, legacyTitle);

        List<HeadDef> heads = category.heads();
        for (int i = 0; i < heads.size() && i < size; i++) {
            HeadDef def = heads.get(i);
            ItemStack skull = createHeadItem(def);
            inv.setItem(i, skull);
        }

        inv.setItem(BACK_SLOT, createBackItem());
        player.openInventory(inv);
    }

    public void openSearchMenu(Player player, String query) {
        String q = query.toLowerCase(Locale.ROOT);
        List<HeadDef> results = new ArrayList<>();
        for (HeadCategory cat : CATEGORIES) {
            for (HeadDef def : cat.heads()) {
                if (def.displayName().toLowerCase(Locale.ROOT).contains(q) ||
                    def.texture().toLowerCase(Locale.ROOT).contains(q)) {
                    results.add(def);
                }
            }
        }

        String title = moduleConfig.getStringFrom("language.yml", "options.heads.search_title");
        String legacyTitle = itemAPI != null ? itemAPI.formatInventoryTitle(title) : title;
        int size = Math.min(54, ((results.size() + 8) / 9 + 1) * 9);
        size = Math.max(size, 27);
        Inventory inv = Bukkit.createInventory(new HeadMenuHolder(HeadMenuHolder.Type.SEARCH), size, legacyTitle);

        for (int i = 0; i < results.size() && i < size; i++) {
            HeadDef def = results.get(i);
            ItemStack skull = createHeadItem(def);
            inv.setItem(i, skull);
        }

        inv.setItem(size - 1, createBackItem());
        player.openInventory(inv);
    }

    public boolean handleInventoryClick(Player player, HeadMenuHolder holder, int slot) {
        if (slot < 0) {
            return true;
        }

        if (holder.getType() == HeadMenuHolder.Type.CATEGORIES) {
            if (slot >= CATEGORIES.size()) {
                player.closeInventory();
                return true;
            }
            HeadCategory cat = CATEGORIES.get(slot);
            if (cat != null) {
                openHeadsMenu(player, cat.id());
            }
            return true;
        }

        if (holder.getType() == HeadMenuHolder.Type.HEADS) {
            if (slot == BACK_SLOT) {
                openCategoriesMenu(player);
                return true;
            }
            HeadCategory category = findCategory(holder.getCategoryId());
            if (category != null && slot >= 0 && slot < category.heads().size()) {
                HeadDef def = category.heads().get(slot);
                ItemStack skull = createHeadItem(def);
                player.getInventory().addItem(skull.clone());
            }
            return true;
        }

        if (holder.getType() == HeadMenuHolder.Type.SEARCH) {
            player.closeInventory();
            return true;
        }

        return false;
    }

    private HeadCategory findCategory(String id) {
        if (id == null) return null;
        for (HeadCategory cat : CATEGORIES) {
            if (cat.id().equalsIgnoreCase(id)) {
                return cat;
            }
        }
        return null;
    }

    private ItemStack createHeadItem(HeadDef def) {
        ItemStack skull = SkullUtil.createSkull(def.texture(), null);
        return decorate(skull, def.displayName(), List.of());
    }

    private ItemStack createBackItem() {
        String name = moduleConfig.getStringFrom("language.yml", "options.banner.back");
        return decorate(new ItemStack(Material.ARROW), name, List.of());
    }

    private ItemStack decorate(ItemStack item, String displayName, List<String> lore) {
        if (itemAPI != null) {
            return itemAPI.decorate(item, displayName, lore);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null && displayName != null && !displayName.isBlank()) {
            meta.setDisplayName(displayName);
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String stripColor(String input) {
        if (input == null) return "";
        return input.replaceAll("<[^>]+>", "").replaceAll("&[0-9a-fk-or]", "");
    }

    private record HeadCategory(String id, String displayName, Material icon, List<HeadDef> heads) {
    }

    private record HeadDef(String texture, String displayName) {
    }
}
