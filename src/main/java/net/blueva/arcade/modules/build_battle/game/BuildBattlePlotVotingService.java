package net.blueva.arcade.modules.build_battle.game;

import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.modules.build_battle.state.ArenaState;
import net.blueva.arcade.modules.build_battle.state.BuildPhase;
import net.blueva.arcade.modules.build_battle.support.outcome.OutcomeService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class BuildBattlePlotVotingService {

    private final BuildBattleGame game;
    private final ModuleConfigAPI moduleConfig;
    private final OutcomeService outcomeService;

    BuildBattlePlotVotingService(BuildBattleGame game,
                                  ModuleConfigAPI moduleConfig,
                                  OutcomeService outcomeService) {
        this.game = game;
        this.moduleConfig = moduleConfig;
        this.outcomeService = outcomeService;
    }

    void startPlotVoting(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                         ArenaState state) {
        List<Player> players = new ArrayList<>(context.getPlayers());
        if (players.isEmpty()) {
            game.endGame(context);
            return;
        }

        state.setCurrentPlotIndex(0);
        state.clearVotedCurrentPlot();

        for (Player player : context.getPlayers()) {
            if (!player.isOnline()) {
                continue;
            }
            String title = moduleConfig.getStringFrom("language.yml", "titles.voting_started.title");
            String subtitle = moduleConfig.getStringFrom("language.yml", "titles.voting_started.subtitle");
            if (title != null && subtitle != null) {
                context.getTitlesAPI().sendRaw(player, title, subtitle, 0, 30, 10);
            }
        }

        advanceToNextPlot(context, state);
    }

    void handleVoteItemClick(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                             Player voter,
                             ItemStack item) {
        ArenaState state = game.getArenaState(context);
        if (state == null || state.getPhase() != BuildPhase.PLOT_VOTING) {
            return;
        }

        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        List<Player> players = new ArrayList<>(context.getPlayers());
        if (state.getCurrentPlotIndex() < 0 || state.getCurrentPlotIndex() >= players.size()) {
            return;
        }

        Player builder = players.get(state.getCurrentPlotIndex());
        if (builder == null || voter.getUniqueId().equals(builder.getUniqueId())) {
            String msg = moduleConfig.getStringFrom("language.yml", "plot_voting.self_voting_disabled");
            if (msg != null) {
                context.getMessagesAPI().sendRaw(voter, msg);
            }
            return;
        }

        if (state.hasVotedCurrentPlot(voter.getUniqueId())) {
            return;
        }

        int points = resolveVotePoints(item.getType());
        if (points <= 0) {
            return;
        }

        String ratingName = resolveVoteLabel(item.getType());

        state.castPlotVote(voter.getUniqueId(), builder.getUniqueId(), points);
        state.markVotedCurrentPlot(voter.getUniqueId());
        voter.getInventory().clear();

        String msg = moduleConfig.getStringFrom("language.yml", "plot_voting.vote_cast");
        if (msg != null) {
            context.getMessagesAPI().sendRaw(voter, msg.replace("{rating_name}", ratingName != null ? ratingName : String.valueOf(points)));
        }
    }

    private void advanceToNextPlot(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                   ArenaState state) {
        List<Player> players = new ArrayList<>(context.getPlayers());
        List<Location> plotSpawns = state.getPlotSpawns();

        if (state.getCurrentPlotIndex() >= players.size()) {
            endVotingPhase(context, state);
            return;
        }

        Player builder = players.get(state.getCurrentPlotIndex());
        Location plot = state.getPlayerPlotSpawn(builder.getUniqueId());
        if (plot == null && !plotSpawns.isEmpty()) {
            plot = plotSpawns.get(state.getCurrentPlotIndex() % plotSpawns.size());
        }

        state.clearVotedCurrentPlot();
        final Location targetPlot = plot;

        Long plotTime = state.getPlotTimeOverride(builder.getUniqueId());
        WeatherType plotWeather = state.getPlotWeatherOverride(builder.getUniqueId());

        for (Player player : players) {
            if (!player.isOnline()) {
                continue;
            }
            player.getInventory().clear();
            player.setAllowFlight(true);
            player.setFlying(true);

            if (plotTime != null) {
                player.setPlayerTime(plotTime, false);
            } else {
                player.resetPlayerTime();
            }
            if (plotWeather != null) {
                player.setPlayerWeather(plotWeather);
            } else {
                player.resetPlayerWeather();
            }

            if (targetPlot != null) {
                context.getSchedulerAPI().runAtEntity(player, () -> player.teleport(BuildBattlePlotService.centerLocation(targetPlot)));
            }

            String header = moduleConfig.getStringFrom("language.yml", "plot_voting.header");
            if (header != null) {
                context.getMessagesAPI().sendRaw(player, header.replace("{player}", builder.getName()));
            }

            if (!player.getUniqueId().equals(builder.getUniqueId())) {
                giveVoteItems(context, player);
            }
        }

        updateVotingScoreboard(context, state, builder);

        int plotDisplaySeconds = moduleConfig.getInt("voting.plot_display_seconds");
        String taskId = "arena_" + context.getArenaId() + "_build_battle_plot_timer";
        final int[] ticksLeft = {plotDisplaySeconds};

        context.getSchedulerAPI().runTimer(taskId, () -> {
            ticksLeft[0]--;
            if (ticksLeft[0] <= 0) {
                context.getSchedulerAPI().cancelTask(taskId);
                applyDefaultVotesForCurrentPlot(context, state, builder);
                state.incrementPlotIndex();
                advanceToNextPlot(context, state);
            }
        }, 20L, 20L);
    }

    private void giveVoteItems(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                               Player player) {
        Map<String, Map<String, Object>> voteItems = loadVoteItemsConfig();
        for (Map.Entry<String, Map<String, Object>> entry : voteItems.entrySet()) {
            Map<String, Object> config = entry.getValue();
            boolean enabled = (boolean) config.getOrDefault("enabled", false);
            if (!enabled) {
                continue;
            }

            String materialName = (String) config.get("material");
            int slot = ((Number) config.getOrDefault("slot", 0)).intValue();
            String displayName = (String) config.get("display_name");
            @SuppressWarnings("unchecked")
            List<String> lore = (List<String>) config.get("lore");

            if (materialName == null || slot < 0 || slot > 8) {
                continue;
            }

            Material material;
            try {
                material = Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException ex) {
                continue;
            }

            ItemStack item = new ItemStack(material);
            if (context.getItemAPI() != null) {
                item = context.getItemAPI().decorate(item, displayName, lore);
            } else {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    if (displayName != null) meta.setDisplayName(displayName);
                    if (lore != null) meta.setLore(lore);
                    item.setItemMeta(meta);
                }
            }

            player.getInventory().setItem(slot, item);
        }
    }

    private Map<String, Map<String, Object>> loadVoteItemsConfig() {
        Map<String, Map<String, Object>> items = new LinkedHashMap<>();
        String[] keys = {"super_poop", "poop", "ok", "good", "awesome", "god"};
        for (String key : keys) {
            String prefix = "vote_items." + key;
            Map<String, Object> config = new HashMap<>();
            config.put("enabled", moduleConfig.getBoolean(prefix + ".enabled"));
            config.put("material", moduleConfig.getString(prefix + ".material"));
            config.put("slot", moduleConfig.getInt(prefix + ".slot"));
            config.put("display_name", moduleConfig.getString(prefix + ".display_name"));
            config.put("lore", moduleConfig.getStringList(prefix + ".lore"));
            config.put("points", moduleConfig.getInt(prefix + ".points"));
            items.put(key, config);
        }
        return items;
    }

    private int resolveVotePoints(Material material) {
        Map<String, Map<String, Object>> voteItems = loadVoteItemsConfig();
        for (Map.Entry<String, Map<String, Object>> entry : voteItems.entrySet()) {
            Map<String, Object> config = entry.getValue();
            String matName = (String) config.get("material");
            if (matName != null && material == Material.valueOf(matName.toUpperCase())) {
                return ((Number) config.getOrDefault("points", 0)).intValue();
            }
        }
        return 0;
    }

    private String resolveVoteLabel(Material material) {
        Map<String, Map<String, Object>> voteItems = loadVoteItemsConfig();
        for (Map.Entry<String, Map<String, Object>> entry : voteItems.entrySet()) {
            Map<String, Object> config = entry.getValue();
            String matName = (String) config.get("material");
            if (matName != null && material == Material.valueOf(matName.toUpperCase())) {
                String displayName = (String) config.get("display_name");
                return displayName != null ? displayName : entry.getKey();
            }
        }
        return null;
    }

    private void applyDefaultVotesForCurrentPlot(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                                 ArenaState state,
                                                 Player builder) {
        int defaultPoints = moduleConfig.getInt("voting.default_vote_points");
        for (Player player : context.getPlayers()) {
            if (!player.isOnline()) {
                continue;
            }
            if (player.getUniqueId().equals(builder.getUniqueId())) {
                continue;
            }
            if (!state.hasVotedCurrentPlot(player.getUniqueId())) {
                state.castPlotVote(player.getUniqueId(), builder.getUniqueId(), defaultPoints);
            }
        }
    }

    private void updateVotingScoreboard(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                        ArenaState state,
                                        Player builder) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("current", String.valueOf(state.getCurrentPlotIndex() + 1));
        placeholders.put("total", String.valueOf(context.getPlayers().size()));
        placeholders.put("builder", builder.getName());

        for (Player player : context.getPlayers()) {
            if (player.isOnline()) {
                context.getScoreboardAPI().update(player, "scoreboard.voting", placeholders);
            }
        }
    }

    private void endVotingPhase(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                ArenaState state) {
        state.setPhase(BuildPhase.ENDED);

        String message = moduleConfig.getStringFrom("language.yml", "plot_voting.voting_ended");
        if (message != null) {
            for (Player player : context.getPlayers()) {
                if (player.isOnline()) {
                    context.getMessagesAPI().sendRaw(player, message);
                }
            }
        }

        outcomeService.endGame(context, state);
    }
}
