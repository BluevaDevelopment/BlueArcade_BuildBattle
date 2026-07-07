package net.blueva.arcade.modules.build_battle.support.outcome;

import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.module.ModuleInfo;
import net.blueva.arcade.api.stats.StatsAPI;
import net.blueva.arcade.modules.build_battle.game.BuildBattleGame;
import net.blueva.arcade.modules.build_battle.state.ArenaState;
import net.blueva.arcade.modules.build_battle.state.Plot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OutcomeService {

    private final ModuleInfo moduleInfo;
    private final StatsAPI statsAPI;
    private final BuildBattleGame game;

    public OutcomeService(ModuleInfo moduleInfo, StatsAPI statsAPI, BuildBattleGame game) {
        this.moduleInfo = moduleInfo;
        this.statsAPI = statsAPI;
        this.game = game;
    }

    public void endGame(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                        ArenaState state) {
        if (state == null || state.isEnded()) {
            return;
        }
        state.markEnded();

        context.getSchedulerAPI().cancelArenaTasks(context.getArenaId());

        List<Map.Entry<UUID, Integer>> sorted = calculateSortedResults(state);
        Player winner = resolveWinner(context, sorted);

        teleportToWinnerPlot(context, state, winner);
        sendResultMessages(context, sorted);
        showFinalScoreboard(context, sorted);
        sendWinnerTitles(context, winner);
        recordStats(context, sorted);

        context.getSchedulerAPI().runLater(
                "arena_" + context.getArenaId() + "_build_battle_end_delay",
                context::endGame,
                100L
        );
    }

    private List<Map.Entry<UUID, Integer>> calculateSortedResults(ArenaState state) {
        Map<UUID, Map<UUID, Integer>> plotVotes = state.getPlotVotes();
        Map<UUID, Integer> totals = new HashMap<>();

        for (Map.Entry<UUID, Map<UUID, Integer>> entry : plotVotes.entrySet()) {
            UUID builderId = entry.getKey();
            int total = entry.getValue().values().stream().mapToInt(Integer::intValue).sum();
            totals.put(builderId, total);
            state.addPoints(builderId, total);
        }

        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(totals.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        return sorted;
    }

    private Player resolveWinner(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                 List<Map.Entry<UUID, Integer>> sorted) {
        if (sorted.isEmpty()) {
            return null;
        }
        UUID winnerId = sorted.get(0).getKey();
        Player winner = org.bukkit.Bukkit.getPlayer(winnerId);
        if (winner != null) {
            context.setWinner(winner);
        }
        return winner;
    }

    private void teleportToWinnerPlot(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                      ArenaState state,
                                      Player winner) {
        if (winner == null) {
            return;
        }
        Plot winnerPlot = state.getPlayerPlot(winner.getUniqueId());
        Location target = winnerPlot != null ? winnerPlot.findSafeTeleport() : state.getPlayerPlotSpawn(winner.getUniqueId());
        if (target == null) {
            return;
        }
        for (Player player : context.getPlayers()) {
            if (player.isOnline()) {
                player.teleport(target);
            }
        }
    }

    private void sendResultMessages(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                    List<Map.Entry<UUID, Integer>> sorted) {
        Map<String, String> basePlaceholders = new HashMap<>();
        for (int i = 0; i < 5; i++) {
            int rank = i + 1;
            if (i < sorted.size()) {
                Player p = org.bukkit.Bukkit.getPlayer(sorted.get(i).getKey());
                basePlaceholders.put("place_" + rank, p != null ? p.getName() : "???");
                basePlaceholders.put("score_" + rank, String.valueOf(sorted.get(i).getValue()));
            } else {
                basePlaceholders.put("place_" + rank, "-");
                basePlaceholders.put("score_" + rank, "0");
            }
        }

        for (Player player : context.getPlayers()) {
            if (!player.isOnline()) {
                continue;
            }
            List<String> lines = game.getModuleConfig().getTranslationList(player, "messages.result_lines");
            if (lines == null || lines.isEmpty()) {
                continue;
            }
            Map<String, String> playerPlaceholders = new HashMap<>(basePlaceholders);

            int playerPos = 0;
            int playerScore = 0;
            for (int i = 0; i < sorted.size(); i++) {
                if (sorted.get(i).getKey().equals(player.getUniqueId())) {
                    playerPos = i + 1;
                    playerScore = sorted.get(i).getValue();
                    break;
                }
            }
            playerPlaceholders.put("player_position", playerPos > 0 ? String.valueOf(playerPos) : "-");
            playerPlaceholders.put("player_score", String.valueOf(playerScore));

            for (String line : lines) {
                String processed = line;
                for (Map.Entry<String, String> entry : playerPlaceholders.entrySet()) {
                    processed = processed.replace("{" + entry.getKey() + "}", entry.getValue());
                }
                context.getMessagesAPI().sendRaw(player, processed);
            }
        }
    }

    private void showFinalScoreboard(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                     List<Map.Entry<UUID, Integer>> sorted) {
        Map<String, String> basePlaceholders = new HashMap<>();

        for (int i = 0; i < 5; i++) {
            int rank = i + 1;
            if (i < sorted.size()) {
                Player p = org.bukkit.Bukkit.getPlayer(sorted.get(i).getKey());
                basePlaceholders.put("place_" + rank, p != null ? p.getName() : "???");
                basePlaceholders.put("score_" + rank, String.valueOf(sorted.get(i).getValue()));
            } else {
                basePlaceholders.put("place_" + rank, "-");
                basePlaceholders.put("score_" + rank, "0");
            }
        }
        basePlaceholders.put("players", String.valueOf(context.getPlayers().size()));

        for (Player player : context.getPlayers()) {
            if (!player.isOnline()) {
                continue;
            }
            List<String> lines = game.getModuleConfig().getTranslationList(player, "messages.result_lines");
            if (lines == null || lines.isEmpty()) {
                continue;
            }
            Map<String, String> playerPlaceholders = new HashMap<>(basePlaceholders);

            int playerPos = 0;
            int playerScore = 0;
            for (int i = 0; i < sorted.size(); i++) {
                if (sorted.get(i).getKey().equals(player.getUniqueId())) {
                    playerPos = i + 1;
                    playerScore = sorted.get(i).getValue();
                    break;
                }
            }
            playerPlaceholders.put("player_position", playerPos > 0 ? String.valueOf(playerPos) : "-");
            playerPlaceholders.put("player_score", String.valueOf(playerScore));

            context.getScoreboardAPI().showModuleFinalScoreboard(
                    player, "scoreboard.final.winner", playerPlaceholders);
        }
    }

    private void sendWinnerTitles(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                  Player winner) {
        if (winner == null) {
            return;
        }
        for (Player player : context.getPlayers()) {
            if (player.isOnline()) {
                String title = game.getModuleConfig().getTranslation(player, "titles.winner.title");
                String subtitle = game.getModuleConfig().getTranslation(player, "titles.winner.subtitle");
                if (title == null || subtitle == null) {
                    continue;
                }
                context.getTitlesAPI().sendRaw(player, title,
                        subtitle.replace("{player}", winner.getName()), 0, 40, 20);
            }
        }
    }

    private void recordStats(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                             List<Map.Entry<UUID, Integer>> sorted) {
        if (statsAPI == null) {
            return;
        }
        for (Map.Entry<UUID, Integer> entry : sorted) {
            Player player = org.bukkit.Bukkit.getPlayer(entry.getKey());
            if (player == null) {
                continue;
            }
            int score = entry.getValue();
            if (score > 0) {
                statsAPI.addModuleStat(player, moduleInfo.getId(), "points_total", score);
            }
            int highest = statsAPI.getModuleStat(player, moduleInfo.getId(), "points_highest");
            if (score > highest) {
                statsAPI.addModuleStat(player, moduleInfo.getId(), "points_highest", score - highest);
            }
        }
        if (!sorted.isEmpty()) {
            Player winner = org.bukkit.Bukkit.getPlayer(sorted.get(0).getKey());
            if (winner != null) {
                statsAPI.addModuleStat(winner, moduleInfo.getId(), "wins", 1);
            }
        }
        for (Player player : context.getPlayers()) {
            statsAPI.addModuleStat(player, moduleInfo.getId(), "games_played", 1);
        }
    }
}
