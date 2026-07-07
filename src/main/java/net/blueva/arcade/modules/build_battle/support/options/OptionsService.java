package net.blueva.arcade.modules.build_battle.support.options;

import net.blueva.arcade.api.ModuleAPI;
import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.ui.ItemAPI;
import net.blueva.arcade.api.ui.MenuAPI;
import net.blueva.arcade.api.ui.MessageAPI;
import net.blueva.arcade.api.ui.menu.MenuDefinition;
import net.blueva.arcade.modules.build_battle.game.BuildBattleGame;
import net.blueva.arcade.modules.build_battle.state.ArenaState;
import net.blueva.arcade.modules.build_battle.state.BuildPhase;
import net.blueva.arcade.modules.build_battle.state.Plot;
import net.blueva.arcade.modules.build_battle.support.banner.BannerCreatorService;
import net.blueva.arcade.modules.build_battle.support.heads.HeadsService;
import net.blueva.arcade.modules.build_battle.support.particles.ParticleService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class OptionsService {

    public static final String OPTIONS_ITEM_ID = "build_battle_options";
    private static final int OPTIONS_SLOT = 8;

    private final ModuleConfigAPI moduleConfig;
    private final MenuAPI<Player, Material> menuAPI;
    private final ItemAPI<Player, ItemStack, Material> itemAPI;
    private final String moduleId;
    private final OptionsMenuRepository menuRepository;
    private final BannerCreatorService bannerCreatorService;
    private final ParticleService particleService;
    private final HeadsService headsService;
    private BuildBattleGame game;

    private static final List<BiomeEntry> BIOMES = List.of(
            new BiomeEntry(Biome.PLAINS, Material.GRASS_BLOCK, "<green>Plains"),
            new BiomeEntry(Biome.FOREST, Material.OAK_SAPLING, "<dark_green>Forest"),
            new BiomeEntry(Biome.BIRCH_FOREST, Material.BIRCH_SAPLING, "<aqua>Birch Forest"),
            new BiomeEntry(Biome.DARK_FOREST, Material.DARK_OAK_SAPLING, "<dark_blue>Dark Forest"),
            new BiomeEntry(Biome.JUNGLE, Material.JUNGLE_SAPLING, "<green>Jungle"),
            new BiomeEntry(Biome.SAVANNA, Material.ACACIA_SAPLING, "<yellow>Savanna"),
            new BiomeEntry(Biome.DESERT, Material.SAND, "<yellow>Desert"),
            new BiomeEntry(Biome.BEACH, Material.SAND, "<gold>Beach"),
            new BiomeEntry(Biome.SNOWY_PLAINS, Material.SNOW_BLOCK, "<white>Snowy Plains"),
            new BiomeEntry(Biome.SNOWY_TAIGA, Material.SPRUCE_SAPLING, "<white>Snowy Taiga"),
            new BiomeEntry(Biome.TAIGA, Material.SPRUCE_SAPLING, "<dark_green>Taiga"),
            new BiomeEntry(Biome.SWAMP, Material.LILY_PAD, "<dark_green>Swamp"),
            new BiomeEntry(Biome.BADLANDS, Material.TERRACOTTA, "<gold>Badlands"),
            new BiomeEntry(Biome.MUSHROOM_FIELDS, Material.RED_MUSHROOM, "<red>Mushroom Fields"),
            new BiomeEntry(Biome.OCEAN, Material.WATER_BUCKET, "<blue>Ocean"),
            new BiomeEntry(Biome.DEEP_OCEAN, Material.WATER_BUCKET, "<dark_blue>Deep Ocean"),
            new BiomeEntry(Biome.MEADOW, Material.GRASS_BLOCK, "<green>Meadow"),
            new BiomeEntry(Biome.FLOWER_FOREST, Material.POPPY, "<light_purple>Flower Forest")
    );

    public OptionsService(ModuleConfigAPI moduleConfig,
                          MenuAPI<Player, Material> menuAPI,
                          ItemAPI<Player, ItemStack, Material> itemAPI,
                          String moduleId) {
        this.moduleConfig = moduleConfig;
        this.menuAPI = menuAPI;
        this.itemAPI = itemAPI;
        this.moduleId = moduleId;
        this.menuRepository = new OptionsMenuRepository(moduleConfig);
        this.menuRepository.loadMenus();
        this.bannerCreatorService = new BannerCreatorService(moduleConfig, itemAPI);
        this.particleService = new ParticleService(moduleConfig, itemAPI);
        this.headsService = new HeadsService(moduleConfig, itemAPI);
    }

    public void setGame(BuildBattleGame game) {
        this.game = game;
    }

    public void giveOptionsItem(Player player) {
        if (itemAPI == null || player == null) {
            return;
        }
        String displayName = moduleConfig.getTranslation(player, "options.item.name");
        List<String> lore = moduleConfig.getTranslationList(player, "options.item.lore");
        ItemStack item = itemAPI.decorate(new ItemStack(Material.NETHER_STAR), displayName, lore);
        player.getInventory().setItem(OPTIONS_SLOT, item);
    }

    public void removeOptionsItem(Player player) {
        if (player == null) {
            return;
        }
        ItemStack item = player.getInventory().getItem(OPTIONS_SLOT);
        if (item != null && item.getType() == Material.NETHER_STAR) {
            player.getInventory().setItem(OPTIONS_SLOT, null);
        }
    }

    public boolean handleOptionsClick(Player player) {
        return openMenu(player, OptionsMenuRepository.MENU_MAIN);
    }

    public boolean openMenuById(Player player, String menuId) {
        if (menuId == null) {
            return false;
        }
        String normalized = menuId.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("build_battle_options_")) {
            normalized = normalized.substring("build_battle_options_".length());
        } else if (normalized.startsWith("options_")) {
            normalized = normalized.substring("options_".length());
        }
        String mapped = switch (normalized) {
            case "time" -> OptionsMenuRepository.MENU_TIME;
            case "weather" -> OptionsMenuRepository.MENU_WEATHER;
            default -> OptionsMenuRepository.MENU_MAIN;
        };
        return openMenu(player, mapped);
    }

    public boolean openMenu(Player player, String menuId) {
        if (menuAPI == null || player == null) {
            return false;
        }
        MenuDefinition<Material> menu = menuRepository.getMenu(menuId);
        if (menu == null) {
            return false;
        }
        return menuAPI.openMenu(player, menu, Map.of());
    }

    public boolean handleModuleAction(Player player, String payload) {
        if (player == null || payload == null || payload.isBlank()) {
            return false;
        }
        String[] parts = payload.trim().split("\\s+");
        if (parts.length == 0) {
            return false;
        }

        String action = parts[0].toLowerCase(Locale.ROOT);
        int idx = 0;
        if ("options".equals(action) && parts.length > 1) {
            action = parts[1].toLowerCase(Locale.ROOT);
            idx = 1;
        }

        switch (action) {
            case "menu" -> {
                if (parts.length > idx + 1) {
                    return openMenuById(player, parts[idx + 1]);
                }
                return openMenu(player, OptionsMenuRepository.MENU_MAIN);
            }
            case "time" -> {
                if (parts.length > idx + 1) {
                    applyTime(player, parts[idx + 1]);
                }
                return true;
            }
            case "weather" -> {
                if (parts.length > idx + 1) {
                    applyWeather(player, parts[idx + 1]);
                }
                return true;
            }
            case "floor" -> {
                enterFloorChangeMode(player);
                return true;
            }
            case "reset" -> {
                resetPlot(player);
                return true;
            }
            case "biome" -> {
                openBiomeMenu(player);
                return true;
            }
            case "banner" -> {
                bannerCreatorService.openBaseColorMenu(player);
                return true;
            }
            case "particle" -> {
                if (parts.length > idx + 1 && "menu".equals(parts[idx + 1].toLowerCase(Locale.ROOT))) {
                    particleService.openParticleMenu(player);
                    return true;
                }
                return false;
            }
            case "heads" -> {
                if (parts.length > idx + 1 && "menu".equals(parts[idx + 1].toLowerCase(Locale.ROOT))) {
                    headsService.openCategoriesMenu(player);
                    return true;
                }
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    private void applyTime(Player player, String timeKey) {
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = getContext(player);
        ArenaState state = getArenaState(context);
        if (state == null) {
            return;
        }
        Plot plot = state.getPlayerPlot(player.getUniqueId());
        if (plot == null) {
            return;
        }

        long ticks = switch (timeKey.toLowerCase(Locale.ROOT)) {
            case "day" -> 1000L;
            case "noon" -> 6000L;
            case "sunset" -> 12000L;
            case "night" -> 13000L;
            case "midnight" -> 18000L;
            case "sunrise" -> 23000L;
            default -> -1L;
        };

        if (ticks < 0) {
            return;
        }

        state.setPlotTimeOverride(player.getUniqueId(), ticks);
        for (Player p : getPlotMembers(state, plot)) {
            if (p.isOnline()) {
                p.setPlayerTime(ticks, false);
            }
        }
        sendMessage(player, "options.messages.time_changed");
    }

    private void applyWeather(Player player, String weatherKey) {
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = getContext(player);
        ArenaState state = getArenaState(context);
        if (state == null) {
            return;
        }
        Plot plot = state.getPlayerPlot(player.getUniqueId());
        if (plot == null) {
            return;
        }

        org.bukkit.WeatherType weather = switch (weatherKey.toLowerCase(Locale.ROOT)) {
            case "clear", "sunny" -> org.bukkit.WeatherType.CLEAR;
            case "rain", "rainy", "downfall" -> org.bukkit.WeatherType.DOWNFALL;
            default -> null;
        };

        if (weather == null) {
            return;
        }

        state.setPlotWeatherOverride(player.getUniqueId(), weather);
        for (Player p : getPlotMembers(state, plot)) {
            if (p.isOnline()) {
                p.setPlayerWeather(weather);
            }
        }
        sendMessage(player, "options.messages.weather_changed");
    }

    private void enterFloorChangeMode(Player player) {
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = getContext(player);
        ArenaState state = getArenaState(context);
        if (state == null) {
            return;
        }
        state.addFloorChangeMode(player.getUniqueId());
        player.closeInventory();
        sendMessage(player, "options.messages.floor_mode_entered");
    }

    public boolean handleFloorChange(Player player, Material material) {
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = getContext(player);
        ArenaState state = getArenaState(context);
        if (state == null) {
            return false;
        }
        if (!state.isInFloorChangeMode(player.getUniqueId())) {
            return false;
        }
        state.removeFloorChangeMode(player.getUniqueId());

        Plot plot = state.getPlayerPlot(player.getUniqueId());
        if (plot == null || plot.getMin() == null || plot.getMax() == null) {
            return false;
        }

        if (material == Material.WATER_BUCKET || material == Material.LAVA_BUCKET) {
            sendMessage(player, "options.messages.floor_invalid");
            return false;
        }
        if (!material.isBlock() || !material.isSolid()) {
            sendMessage(player, "options.messages.floor_invalid");
            return false;
        }

        if (context != null && context.getBlocksAPI() != null) {
            int minX = Math.min(plot.getMin().getBlockX(), plot.getMax().getBlockX());
            int minY = Math.min(plot.getMin().getBlockY(), plot.getMax().getBlockY());
            int minZ = Math.min(plot.getMin().getBlockZ(), plot.getMax().getBlockZ());
            int maxX = Math.max(plot.getMin().getBlockX(), plot.getMax().getBlockX());
            int maxZ = Math.max(plot.getMin().getBlockZ(), plot.getMax().getBlockZ());

            Location floorMin = new Location(plot.getMin().getWorld(), minX, minY, minZ);
            Location floorMax = new Location(plot.getMax().getWorld(), maxX, minY, maxZ);
            context.getBlocksAPI().setRegion(floorMin, floorMax, material);
        }

        sendMessage(player, "options.messages.floor_changed");
        return true;
    }

    private void resetPlot(Player player) {
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = getContext(player);
        ArenaState state = getArenaState(context);
        if (state == null) {
            return;
        }
        Plot plot = state.getPlayerPlot(player.getUniqueId());
        if (plot == null) {
            return;
        }

        if (context != null && context.getBlocksAPI() != null) {
            context.getBlocksAPI().setRegion(plot.getMin(), plot.getMax(), Material.AIR);
        }
        if (context != null) {
            game.regeneratePlotFloor(context, state, plot);
        }
        player.closeInventory();
        sendMessage(player, "options.messages.plot_reset");
    }

    private void openBiomeMenu(Player player) {
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = getContext(player);
        ArenaState state = getArenaState(context);
        if (state == null) {
            return;
        }
        Plot plot = state.getPlayerPlot(player.getUniqueId());
        if (plot == null) {
            return;
        }
        player.closeInventory();

        String title = moduleConfig.getTranslation(player, "options.biome.title");
        String legacyTitle = itemAPI != null ? itemAPI.formatInventoryTitle(title) : title;
        int size = ((BIOMES.size() - 1) / 9 + 1) * 9;
        size = Math.min(size, 54);

        Inventory inv = Bukkit.createInventory(null, size, legacyTitle);
        for (int i = 0; i < Math.min(BIOMES.size(), size); i++) {
            BiomeEntry entry = BIOMES.get(i);
            ItemStack item = new ItemStack(entry.icon());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(itemAPI != null ? itemAPI.formatDisplayName(entry.displayName()) : entry.displayName());
                item.setItemMeta(meta);
            }
            inv.setItem(i, item);
        }

        player.openInventory(inv);

        state.setPendingBiomeMenu(player.getUniqueId(), true);
    }

    public BannerCreatorService getBannerCreatorService() {
        return bannerCreatorService;
    }

    public ParticleService getParticleService() {
        return particleService;
    }

    public HeadsService getHeadsService() {
        return headsService;
    }

    public boolean handleBiomeMenuClick(Player player, InventoryClickEvent event) {
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = getContext(player);
        ArenaState state = getArenaState(context);
        if (state == null) {
            return false;
        }
        if (!state.isPendingBiomeMenu(player.getUniqueId())) {
            return false;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= BIOMES.size()) {
            return true;
        }

        BiomeEntry entry = BIOMES.get(slot);
        Plot plot = state.getPlayerPlot(player.getUniqueId());
        if (plot == null || plot.getMin() == null || plot.getMax() == null) {
            player.closeInventory();
            state.setPendingBiomeMenu(player.getUniqueId(), false);
            return true;
        }

        World world = plot.getMin().getWorld();
        if (world == null) {
            player.closeInventory();
            state.setPendingBiomeMenu(player.getUniqueId(), false);
            return true;
        }

        int minX = Math.min(plot.getMin().getBlockX(), plot.getMax().getBlockX());
        int minY = Math.min(plot.getMin().getBlockY(), plot.getMax().getBlockY());
        int minZ = Math.min(plot.getMin().getBlockZ(), plot.getMax().getBlockZ());
        int maxX = Math.max(plot.getMin().getBlockX(), plot.getMax().getBlockX());
        int maxY = Math.max(plot.getMin().getBlockY(), plot.getMax().getBlockY());
        int maxZ = Math.max(plot.getMin().getBlockZ(), plot.getMax().getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    block.setBiome(entry.biome());
                }
            }
        }

        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) {
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                world.refreshChunk(cx, cz);
            }
        }

        player.closeInventory();
        state.setPendingBiomeMenu(player.getUniqueId(), false);
        sendMessage(player, "options.messages.biome_changed");
        return true;
    }

    private List<Player> getPlotMembers(ArenaState state, Plot plot) {
        List<Player> members = new ArrayList<>();
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> ctx = state.getContext();
        if (ctx == null) {
            return members;
        }
        for (Player p : ctx.getPlayers()) {
            if (p != null && p.isOnline()) {
                Plot playerPlot = state.getPlayerPlot(p.getUniqueId());
                if (playerPlot != null && playerPlot.equals(plot)) {
                    members.add(p);
                }
            }
        }
        return members;
    }

    private GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> getContext(Player player) {
        if (game == null || player == null) {
            return null;
        }
        return game.getContext(player);
    }

    private ArenaState getArenaState(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        if (game == null || context == null) {
            return null;
        }
        ArenaState state = game.getArenaState(context);
        if (state == null || state.getPhase() != BuildPhase.BUILDING) {
            return null;
        }
        return state;
    }

    private void sendMessage(Player player, String path) {
        if (moduleConfig == null || player == null) {
            return;
        }
        String message = moduleConfig.getTranslation(player, path);
        if (message != null && !message.isBlank()) {
            @SuppressWarnings("unchecked")
            MessageAPI<Player> messagesAPI = (MessageAPI<Player>) ModuleAPI.getMessagesAPI();
            if (messagesAPI != null) {
                messagesAPI.sendRaw(player, message);
            } else {
                player.sendMessage(message);
            }
        }
    }

    private record BiomeEntry(Biome biome, Material icon, String displayName) {
    }
}
