package net.blueva.arcade.modules.build_battle;

import net.blueva.arcade.api.ModuleAPI;
import net.blueva.arcade.api.achievements.AchievementsAPI;
import net.blueva.arcade.api.config.CoreConfigAPI;
import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.events.CustomEventRegistry;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.game.GameModule;
import net.blueva.arcade.api.game.GameResult;
import net.blueva.arcade.api.module.ModuleInfo;
import net.blueva.arcade.api.stats.StatDefinition;
import net.blueva.arcade.api.stats.StatScope;
import net.blueva.arcade.api.stats.StatsAPI;
import net.blueva.arcade.api.setup.SetupRequirement;
import net.blueva.arcade.api.ui.ItemAPI;
import net.blueva.arcade.api.ui.MenuAPI;
import net.blueva.arcade.api.ui.ModuleActionHandler;
import net.blueva.arcade.api.ui.VoteMenuAPI;
import net.blueva.arcade.modules.build_battle.game.BuildBattleGame;
import net.blueva.arcade.modules.build_battle.listener.BuildBattleListener;
import net.blueva.arcade.modules.build_battle.listener.BuildBattleVoteListener;
import net.blueva.arcade.modules.build_battle.setup.BuildBattleSetup;
import net.blueva.arcade.modules.build_battle.support.options.OptionsService;
import net.blueva.arcade.modules.build_battle.support.vote.BuildBattleVoteService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BuildBattleModule implements GameModule<Player, Location, World, Material, ItemStack, Sound, Block, Entity, Listener, EventPriority> {

    private ModuleConfigAPI moduleConfig;
    private CoreConfigAPI coreConfig;
    private ModuleInfo moduleInfo;
    private StatsAPI statsAPI;
    private MenuAPI<Player, Material> menuAPI;
    private ItemAPI<Player, ItemStack, Material> itemAPI;

    private BuildBattleGame game;

    @Override
    public void onLoad() {
        moduleInfo = ModuleAPI.getModuleInfo("build_battle");
        if (moduleInfo == null) {
            throw new IllegalStateException("ModuleInfo not available for BuildBattle module");
        }

        moduleConfig = ModuleAPI.getModuleConfig(moduleInfo.getId());
        coreConfig = ModuleAPI.getCoreConfig();
        statsAPI = ModuleAPI.getStatsAPI();

        registerConfigs();
        registerStats();
        registerAchievements();

        MenuAPI<Player, Material> menuAPI = ModuleAPI.getMenuAPI();
        this.menuAPI = menuAPI;
        @SuppressWarnings("unchecked")
        ItemAPI<Player, ItemStack, Material> itemAPI = (ItemAPI<Player, ItemStack, Material>) ModuleAPI.getItemAPI();
        this.itemAPI = itemAPI;
        BuildBattleVoteService voteService = new BuildBattleVoteService(moduleConfig, menuAPI, itemAPI, moduleInfo.getId());

        game = new BuildBattleGame(moduleInfo, moduleConfig, coreConfig, statsAPI, voteService);
        voteService.setGame(game);

        OptionsService optionsService = null;
        try {
            optionsService = new OptionsService(moduleConfig, menuAPI, itemAPI, moduleInfo.getId());
            optionsService.setGame(game);
            optionsService.getParticleService().setGame(game);
            game.setOptionsService(optionsService);
        } catch (Exception e) {
            org.bukkit.Bukkit.getLogger().warning("[BuildBattle] Failed to initialize OptionsService: " + e.getMessage());
            e.printStackTrace();
        }

        final OptionsService finalOptionsService = optionsService;
        if (menuAPI != null) {
            menuAPI.registerModuleActionHandler(moduleInfo.getId(), (player, payload) -> {
                if (player == null || payload == null || payload.isBlank()) {
                    return false;
                }
                String[] args = payload.trim().split("\\s+");
                if (finalOptionsService != null && finalOptionsService.handleModuleAction(player, payload)) {
                    return true;
                }
                return game.handleVoteCommand(player, args);
            });
        }

        if (menuAPI != null) {
            menuAPI.registerModuleMenuAPI(moduleInfo.getId(), new BuildBattleModuleMenuAPI(menuAPI, voteService, finalOptionsService));
        }

        voteService.registerWaitingItem();
        voteService.registerClickHandler(game);
        ModuleAPI.getSetupAPI().registerHandler(moduleInfo.getId(), new BuildBattleSetup(moduleConfig));

        VoteMenuAPI voteMenu = ModuleAPI.getVoteMenuAPI();
        if (moduleConfig != null && voteMenu != null) {
            String voteItemMaterial = moduleConfig.getString("menus.vote.item");
            Material material;
            try {
                material = Material.valueOf(voteItemMaterial.toUpperCase());
            } catch (IllegalArgumentException | NullPointerException e) {
                material = Material.CRAFTING_TABLE;
            }
            voteMenu.registerGame(
                    moduleInfo.getId(),
                    material,
                    moduleConfig.getStringFrom("language.yml", "vote_menu.name"),
                    moduleConfig.getStringListFrom("language.yml", "vote_menu.lore")
            );
        }
    }

    @Override
    public void onStart(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        game.startGame(context);
    }

    @Override
    public void onCountdownTick(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                int secondsLeft) {
        game.handleCountdownTick(context, secondsLeft);
    }

    @Override
    public void onCountdownFinish(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        game.handleCountdownFinish(context);
    }

    @Override
    public boolean freezePlayersOnCountdown() {
        return false;
    }

    @Override
    public Set<SetupRequirement> getDisabledRequirements() {
        return Set.of(SetupRequirement.SPAWNS);
    }

    @Override
    public void onGameStart(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        game.beginPlaying(context);
    }

    @Override
    public void onEnd(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                      GameResult<Player> result) {
        game.finishGame(context);
    }

    @Override
    public void onDisable() {
        if (game != null) {
            game.shutdown();
        }
        if (menuAPI != null && moduleInfo != null) {
            menuAPI.unregisterModuleMenuAPI(moduleInfo.getId());
        }
        if (itemAPI != null) {
            itemAPI.unregisterWaitingItem("build_battle_vote_theme");
            itemAPI.unregisterClickHandler("build_battle_vote_theme");
        }
    }

    @Override
    public void registerEvents(CustomEventRegistry<Listener, EventPriority> registry) {
        registry.register(new BuildBattleListener(game));
        registry.register(new BuildBattleVoteListener(game));
    }

    @Override
    public Map<String, String> getCustomPlaceholders(Player player) {
        if (game == null || player == null) {
            return new HashMap<>();
        }
        return game.getCustomPlaceholders(player);
    }

    public ModuleConfigAPI getModuleConfig() {
        return moduleConfig;
    }

    public CoreConfigAPI getCoreConfig() {
        return coreConfig;
    }

    public ModuleInfo getModuleInfo() {
        return moduleInfo;
    }

    private void registerConfigs() {
        moduleConfig.register("language.yml", 1);
        moduleConfig.register("settings.yml", 1);
        moduleConfig.register("achievements.yml", 1);
        moduleConfig.register("store.yml", 1);
        registerMenuConfigSafe("menus/java/build_battle_options_main.yml", 1);
        registerMenuConfigSafe("menus/java/build_battle_options_time.yml", 1);
        registerMenuConfigSafe("menus/java/build_battle_options_weather.yml", 1);
        registerMenuConfigSafe("menus/bedrock/build_battle_options_main.yml", 1);
        registerMenuConfigSafe("menus/bedrock/build_battle_options_time.yml", 1);
        registerMenuConfigSafe("menus/bedrock/build_battle_options_weather.yml", 1);
    }

    private void registerMenuConfigSafe(String path, int version) {
        try {
            moduleConfig.register(path, version);
        } catch (Exception e) {
            org.bukkit.Bukkit.getLogger().warning("[BuildBattle] Failed to register config " + path + ": " + e.getMessage());
        }
    }

    private void registerStats() {
        if (statsAPI == null) {
            return;
        }

        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("wins", moduleConfig.getStringFrom("language.yml", "stats.labels.wins"),
                        moduleConfig.getStringFrom("language.yml", "stats.descriptions.wins"), StatScope.MODULE));
        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("games_played", moduleConfig.getStringFrom("language.yml", "stats.labels.games_played"),
                        moduleConfig.getStringFrom("language.yml", "stats.descriptions.games_played"), StatScope.MODULE));
        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("points_total", moduleConfig.getStringFrom("language.yml", "stats.labels.points_total"),
                        moduleConfig.getStringFrom("language.yml", "stats.descriptions.points_total"), StatScope.MODULE));
        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("points_highest", moduleConfig.getStringFrom("language.yml", "stats.labels.points_highest"),
                        moduleConfig.getStringFrom("language.yml", "stats.descriptions.points_highest"), StatScope.MODULE));
    }

    private void registerAchievements() {
        AchievementsAPI achievementsAPI = ModuleAPI.getAchievementsAPI();
        if (achievementsAPI != null) {
            achievementsAPI.registerModuleAchievements(moduleInfo.getId(), "achievements.yml");
        }
    }
}
