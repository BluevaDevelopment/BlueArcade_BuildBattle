package net.blueva.arcade.modules.build_battle.game;

import net.blueva.arcade.api.config.CoreConfigAPI;
import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.game.GamePhase;
import net.blueva.arcade.api.game.GameResult;
import net.blueva.arcade.api.module.ModuleInfo;
import net.blueva.arcade.api.stats.StatsAPI;
import net.blueva.arcade.modules.build_battle.state.ArenaState;
import net.blueva.arcade.modules.build_battle.state.BuildPhase;
import net.blueva.arcade.modules.build_battle.state.Plot;
import net.blueva.arcade.modules.build_battle.state.VoteState;
import net.blueva.arcade.modules.build_battle.support.outcome.OutcomeService;
import net.blueva.arcade.modules.build_battle.support.vote.BuildBattleVoteService;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BuildBattleGame {

    private final ModuleInfo moduleInfo;
    private final ModuleConfigAPI moduleConfig;
    private final CoreConfigAPI coreConfig;
    private final StatsAPI statsAPI;

    private final Map<Integer, ArenaState> arenas = new ConcurrentHashMap<>();
    private final Map<Player, Integer> playerArena = new ConcurrentHashMap<>();

    private final BuildBattleVoteService voteService;
    private final OutcomeService outcomeService;
    private final BuildBattlePlotService plotService;
    private final BuildBattlePlotVotingService plotVotingService;
    private net.blueva.arcade.modules.build_battle.support.options.OptionsService optionsService;
    private final Set<Integer> notificationSeconds;

    public BuildBattleGame(ModuleInfo moduleInfo,
                           ModuleConfigAPI moduleConfig,
                           CoreConfigAPI coreConfig,
                           StatsAPI statsAPI,
                           BuildBattleVoteService voteService) {
        this.moduleInfo = moduleInfo;
        this.moduleConfig = moduleConfig;
        this.coreConfig = coreConfig;
        this.statsAPI = statsAPI;
        this.voteService = voteService;
        this.outcomeService = new OutcomeService(moduleInfo, statsAPI, this);
        this.plotService = new BuildBattlePlotService();
        this.plotVotingService = new BuildBattlePlotVotingService(this, moduleConfig, outcomeService);
        this.notificationSeconds = loadNotificationSeconds(coreConfig);
    }

    private static Set<Integer> loadNotificationSeconds(CoreConfigAPI coreConfig) {
        Set<Integer> seconds = new HashSet<>();
        List<String> raw = coreConfig.getSettingsStringList("game.global.countdown_notifications");
        if (raw != null) {
            for (String s : raw) {
                try {
                    seconds.add(Integer.parseInt(s.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return seconds;
    }

    public void startGame(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();

        context.getSummarySettings().setGameSummaryEnabled(false);
        context.getSummarySettings().setFinalSummaryEnabled(false);

        context.getSchedulerAPI().cancelArenaTasks(arenaId);
        ArenaState state = new ArenaState(context);
        arenas.put(arenaId, state);

        if (voteService != null) {
            state.setVoteState(voteService.createVoteState());
            voteService.applyPendingVotes(state, context.getPlayers());
        }

        plotService.loadPlots(context, state);
        plotService.assignPlayersToPlots(context, state);
        plotService.regeneratePlotFloors(context, state);

        startParticleTask(context, state);

        for (Player player : context.getPlayers()) {
            playerArena.put(player, arenaId);
            state.initializePlayer(player.getUniqueId());
            if (player.isOnline()) {
                plotService.teleportToPlot(context, state, player);
            }
        }
    }

    public void handleCountdownTick(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                    int secondsLeft) {
        for (Player player : context.getPlayers()) {
            if (!player.isOnline()) {
                continue;
            }
            context.getSoundsAPI().play(player, coreConfig.getSound("sounds.starting_game.countdown"));
            String title = coreConfig.getLanguage("titles.starting_game.title")
                    .replace("{game_display_name}", moduleInfo.getName())
                    .replace("{time}", String.valueOf(secondsLeft));
            String subtitle = coreConfig.getLanguage("titles.starting_game.subtitle")
                    .replace("{game_display_name}", moduleInfo.getName())
                    .replace("{time}", String.valueOf(secondsLeft));
            context.getTitlesAPI().sendRaw(player, title, subtitle, 0, 20, 5);
        }
    }

    public void handleCountdownFinish(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        for (Player player : context.getPlayers()) {
            if (!player.isOnline()) {
                continue;
            }

            context.getSoundsAPI().play(player, coreConfig.getSound("sounds.starting_game.start"));
        }
    }

    public void beginPlaying(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        ArenaState state = getArenaState(context);
        if (state == null) {
            return;
        }

        state.setPhase(BuildPhase.BUILDING);

        if (voteService != null) {
            voteService.applyVotes(context, state);
            voteService.broadcastVoteResults(context, state);
        }

        String theme = state.getSelectedTheme();
        for (Player player : context.getPlayers()) {
            if (!player.isOnline()) {
                continue;
            }
            player.setGameMode(GameMode.CREATIVE);
            player.setAllowFlight(true);
            player.setFlying(true);
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
            player.setFireTicks(0);
            player.getInventory().clear();
            if (optionsService != null) {
                optionsService.giveOptionsItem(player);
            }


            context.getSchedulerAPI().runLater("arena_" + context.getArenaId() + "_flight_" + player.getUniqueId(), () -> {
                if (player.isOnline() && state.getPhase() == BuildPhase.BUILDING) {
                    player.setAllowFlight(true);
                    player.setFlying(true);
                }
            }, 2L);

            String title = moduleConfig.getStringFrom("language.yml", "titles.theme_reveal.title");
            String subtitle = moduleConfig.getStringFrom("language.yml", "titles.theme_reveal.subtitle");
            if (title != null && subtitle != null) {
                context.getTitlesAPI().sendRaw(player,
                        title.replace("{theme}", theme),
                        subtitle.replace("{theme}", theme),
                        0, 40, 20);
            }

            String broadcast = moduleConfig.getStringFrom("language.yml", "build.theme_broadcast");
            if (broadcast != null) {
                context.getMessagesAPI().sendRaw(player, broadcast.replace("{theme}", theme));
            }

            context.getScoreboardAPI().showScoreboard(player, "scoreboard.default");
        }

        updateScoreboard(context, state);
        startBuildTimer(context, state);
    }

    private void startBuildTimer(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                 ArenaState state) {
        int arenaId = context.getArenaId();

        int buildTimeSeconds = readBuildTimeSeconds(context);
        state.setBuildTimeLeft(buildTimeSeconds);

        String taskId = "arena_" + arenaId + "_build_battle_build_timer";

        context.getSchedulerAPI().runTimer(taskId, () -> {
            if (state.isEnded() || state.getPhase() != BuildPhase.BUILDING) {
                context.getSchedulerAPI().cancelTask(taskId);
                return;
            }

            state.decrementBuildTime();
            int timeLeft = state.getBuildTimeLeft();

            updateScoreboard(context, state);

            if (timeLeft <= 0) {
                context.getSchedulerAPI().cancelTask(taskId);
                endBuildPhase(context, state);
                return;
            }

            if (notificationSeconds.contains(timeLeft)) {
                for (Player player : context.getPlayers()) {
                    if (player.isOnline()) {
                        context.getSoundsAPI().play(player, coreConfig.getSound("sounds.in_game.countdown"));
                    }
                }
                broadcastTimeMessage(context, "build.time_remaining", timeLeft);
            }
        }, 20L, 20L);
    }

    private int readBuildTimeSeconds(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        try {
            Object raw = context.getDataAccess().getGameData("basic.time", Object.class);
            if (raw instanceof Number number) {
                return Math.max(10, number.intValue());
            }
        } catch (Exception ignored) {
        }
        return 300;
    }

    private void broadcastTimeMessage(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                      String path, int seconds) {
        String template = moduleConfig.getStringFrom("language.yml", path);
        if (template == null || template.isBlank()) {
            return;
        }
        int minutes = seconds / 60;
        int secs = seconds % 60;
        String timeStr = minutes > 0
                ? minutes + "m " + secs + "s"
                : secs + "s";
        String message = template.replace("{time}", timeStr);
        for (Player player : context.getPlayers()) {
            if (player.isOnline()) {
                context.getMessagesAPI().sendRaw(player, message);
            }
        }
    }

    private void updateScoreboard(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                  ArenaState state) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("theme", state.getSelectedTheme());
        placeholders.put("time", formatTime(state.getBuildTimeLeft()));

        for (Player player : context.getPlayers()) {
            if (player.isOnline()) {
                context.getScoreboardAPI().update(player, "scoreboard.default", placeholders);
            }
        }
    }

    private static String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void endBuildPhase(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                               ArenaState state) {
        state.setPhase(BuildPhase.PLOT_VOTING);

        for (Player player : context.getPlayers()) {
            if (!player.isOnline()) {
                continue;
            }
            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
            player.resetPlayerTime();
            player.resetPlayerWeather();
            if (optionsService != null) {
                optionsService.removeOptionsItem(player);
            }

            String title = moduleConfig.getStringFrom("language.yml", "titles.build_ended.title");
            String subtitle = moduleConfig.getStringFrom("language.yml", "titles.build_ended.subtitle");
            if (title != null && subtitle != null) {
                context.getTitlesAPI().sendRaw(player, title, subtitle, 0, 30, 10);
            }

            String message = moduleConfig.getStringFrom("language.yml", "build.build_ended");
            if (message != null) {
                context.getMessagesAPI().sendRaw(player, message);
            }
        }


        context.getSchedulerAPI().runLater("arena_" + context.getArenaId() + "_build_battle_vote_delay", () -> {
            plotVotingService.startPlotVoting(context, state);
        }, 40L);
    }

public void handleVoteItemClick(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                    Player voter,
                                    ItemStack item) {
        plotVotingService.handleVoteItemClick(context, voter, item);
    }

    public void regeneratePlotFloor(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                    ArenaState state,
                                    Plot plot) {
        plotService.regeneratePlotFloor(context, plot);
    }

private void startParticleTask(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                   ArenaState state) {
        String taskId = "arena_" + context.getArenaId() + "_build_battle_particles";
        context.getSchedulerAPI().runTimer(taskId, () -> {
            if (state.isEnded()) {
                context.getSchedulerAPI().cancelTask(taskId);
                return;
            }
            for (List<net.blueva.arcade.modules.build_battle.support.particles.PlotParticle> particles : state.getAllPlotParticles().values()) {
                for (net.blueva.arcade.modules.build_battle.support.particles.PlotParticle pp : particles) {
                    if (pp.location().getWorld() != null) {
                        try {
                            Particle particle = Particle.valueOf(pp.effect());
                            pp.location().getWorld().spawnParticle(particle, pp.location(), 5, 0.5, 0.5, 0.5, 0.02);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            }
        }, 20L, 20L);
    }

    public void finishGame(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();
        context.getSchedulerAPI().cancelArenaTasks(arenaId);

        ArenaState state = arenas.get(arenaId);
        if (state != null) {
            plotService.clearPlots(context, state);
            plotService.regeneratePlotFloors(context, state);
            state.clearAllPlotParticles();
        }
        arenas.remove(arenaId);
        resetWorldDefaults(context);
        resetPlayerStates(context.getPlayers());
        clearPlayerInventories(context.getPlayers());
        removePlayersFromArena(arenaId, context.getPlayers());


    }

    public void shutdown() {
        Set<ArenaState> states = Set.copyOf(arenas.values());
        for (ArenaState state : states) {
            state.getContext().getSchedulerAPI().cancelModuleTasks("build_battle");
            state.clearAllPlotParticles();
            resetWorldDefaults(state.getContext());
            resetPlayerStates(state.getContext().getPlayers());
            clearPlayerInventories(state.getContext().getPlayers());
        }

        arenas.clear();
        playerArena.clear();
    }

    public boolean handleVoteCommand(Player player, String[] args) {
        if (voteService == null || player == null) {
            return false;
        }

        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = getContext(player);
        ArenaState state = context != null ? getArenaState(context) : null;

        if (context == null || state == null) {
            return voteService.handleVoteCommandWithoutContext(player, args);
        }

        GamePhase phase = context.getPhase();
        if (phase == GamePhase.PLAYING || phase == GamePhase.ENDING || phase == GamePhase.FINISHED) {
            return false;
        }

        String[] safeArgs = args != null ? args : new String[0];
        return voteService.handleVoteCommand(player, context, state, safeArgs);
    }

    public GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> getContext(Player player) {
        Integer arenaId = playerArena.get(player);
        if (arenaId == null) {
            for (ArenaState state : arenas.values()) {
                if (state.getContext() != null && state.getContext().getPlayers().contains(player)) {
                    arenaId = state.getContext().getArenaId();
                    playerArena.put(player, arenaId);
                    break;
                }
            }
        }
        if (arenaId == null) {
            return null;
        }
        ArenaState state = arenas.get(arenaId);
        return state != null ? state.getContext() : null;
    }

    public ArenaState getArenaState(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        if (context == null) {
            return null;
        }
        return arenas.get(context.getArenaId());
    }

    public void endGame(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        ArenaState state = getArenaState(context);
        if (state == null || state.isEnded()) {
            return;
        }
        state.markEnded();
        outcomeService.endGame(context, state);
    }

    public Map<String, String> getCustomPlaceholders(Player player) {
        Map<String, String> placeholders = new HashMap<>();
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = getContext(player);
        if (context == null) {
            return placeholders;
        }
        ArenaState state = getArenaState(context);
        if (state == null) {
            return placeholders;
        }
        if (state.getPhase() == BuildPhase.BUILDING) {
            placeholders.put("theme", state.getSelectedTheme());
            placeholders.put("time", String.valueOf(state.getBuildTimeLeft()));
        } else if (state.getPhase() == BuildPhase.PLOT_VOTING) {
            List<Player> players = new ArrayList<>(context.getPlayers());
            int current = Math.min(state.getCurrentPlotIndex() + 1, players.size());
            placeholders.put("current", String.valueOf(current));
            placeholders.put("total", String.valueOf(players.size()));
            if (state.getCurrentPlotIndex() >= 0 && state.getCurrentPlotIndex() < players.size()) {
                Player builder = players.get(state.getCurrentPlotIndex());
                if (builder != null) {
                    placeholders.put("builder", builder.getName());
                }
            }
        }
        return placeholders;
    }

    public Map<Player, Integer> getPlayerArena() {
        return playerArena;
    }

    public void removePlayersFromArena(int arenaId, List<Player> players) {
        for (Player player : players) {
            playerArena.remove(player);
        }
    }

    public void setOptionsService(net.blueva.arcade.modules.build_battle.support.options.OptionsService optionsService) {
        this.optionsService = optionsService;
    }

    public net.blueva.arcade.modules.build_battle.support.options.OptionsService getOptionsService() {
        return optionsService;
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

    public StatsAPI getStatsAPI() {
        return statsAPI;
    }

    private void resetWorldDefaults(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        if (context == null || context.getArenaAPI() == null) {
            return;
        }
        World world = context.getArenaAPI().getWorld();
        if (world == null) {
            return;
        }
        world.setTime(1000L);
        world.setStorm(false);
        world.setThundering(false);
    }

    private void resetPlayerStates(List<Player> players) {
        if (players == null) {
            return;
        }
        Attribute maxHealthAttribute = maxHealthAttribute();
        for (Player player : players) {
            if (player == null) {
                continue;
            }
            player.setGameMode(GameMode.SURVIVAL);
            player.resetPlayerTime();
            player.resetPlayerWeather();
            if (maxHealthAttribute != null && player.getAttribute(maxHealthAttribute) != null) {
                player.getAttribute(maxHealthAttribute).setBaseValue(20.0);
            }
            player.setHealth(Math.min(player.getHealth(), 20.0));
        }
    }

    private void clearPlayerInventories(List<Player> players) {
        if (players == null) {
            return;
        }
        for (Player player : players) {
            if (player == null) {
                continue;
            }
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            player.getInventory().setExtraContents(null);
            player.updateInventory();
        }
    }

    private Attribute maxHealthAttribute() {
        Attribute attribute = attributeConstant("MAX_HEALTH");
        return attribute != null ? attribute : attributeConstant("GENERIC_MAX_HEALTH");
    }

    private Attribute attributeConstant(String fieldName) {
        try {
            Object value = Attribute.class.getField(fieldName).get(null);
            return value instanceof Attribute attr ? attr : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
