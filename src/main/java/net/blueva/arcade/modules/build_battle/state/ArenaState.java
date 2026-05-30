package net.blueva.arcade.modules.build_battle.state;

import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.modules.build_battle.support.particles.PlotParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaState {

    private final GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context;
    private final Map<UUID, Integer> playerPoints = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, Integer>> plotVotes = new ConcurrentHashMap<>();
    private final Set<UUID> votedCurrentPlot = ConcurrentHashMap.newKeySet();
    private final List<Location> plotSpawns = new ArrayList<>();
    private final Map<UUID, Location> playerPlotSpawn = new ConcurrentHashMap<>();
    private final List<Plot> plots = new ArrayList<>();
    private final Map<UUID, Plot> playerPlot = new ConcurrentHashMap<>();

    private VoteState voteState;
    private BuildPhase phase = BuildPhase.THEME_VOTING;
    private String selectedTheme = "random";
    private int currentPlotIndex = 0;
    private int buildTimeLeft = 0;
    private boolean ended = false;


    private final Map<UUID, Long> plotTimeOverrides = new ConcurrentHashMap<>();
    private final Map<UUID, WeatherType> plotWeatherOverrides = new ConcurrentHashMap<>();
    private final Set<UUID> floorChangeMode = ConcurrentHashMap.newKeySet();
    private final Set<UUID> pendingBiomeMenu = ConcurrentHashMap.newKeySet();
    private final Map<UUID, List<PlotParticle>> plotParticles = new ConcurrentHashMap<>();

    public ArenaState(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        this.context = context;
    }

    public GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> getContext() {
        return context;
    }

    public int getArenaId() {
        return context.getArenaId();
    }

    public VoteState getVoteState() {
        return voteState;
    }

    public void setVoteState(VoteState voteState) {
        this.voteState = voteState;
    }

    public BuildPhase getPhase() {
        return phase;
    }

    public void setPhase(BuildPhase phase) {
        this.phase = phase;
    }

    public String getSelectedTheme() {
        return selectedTheme;
    }

    public void setSelectedTheme(String selectedTheme) {
        this.selectedTheme = selectedTheme;
    }

    public void addPlotSpawn(Location location) {
        if (location != null) {
            plotSpawns.add(location);
        }
    }

    public List<Location> getPlotSpawns() {
        return List.copyOf(plotSpawns);
    }

    public void setPlayerPlotSpawn(UUID playerId, Location location) {
        if (playerId != null && location != null) {
            playerPlotSpawn.put(playerId, location);
        }
    }

    public Location getPlayerPlotSpawn(UUID playerId) {
        return playerPlotSpawn.get(playerId);
    }

    public Map<UUID, Location> getPlayerPlotSpawns() {
        return new ConcurrentHashMap<>(playerPlotSpawn);
    }

    public void addPlot(Plot plot) {
        if (plot != null) {
            plots.add(plot);
        }
    }

    public List<Plot> getPlots() {
        return List.copyOf(plots);
    }

    public Plot getPlot(int index) {
        if (index < 0 || index >= plots.size()) {
            return null;
        }
        return plots.get(index);
    }

    public void setPlayerPlot(UUID playerId, Plot plot) {
        if (playerId != null) {
            playerPlot.put(playerId, plot);
        }
    }

    public Plot getPlayerPlot(UUID playerId) {
        return playerPlot.get(playerId);
    }

    public int getCurrentPlotIndex() {
        return currentPlotIndex;
    }

    public void setCurrentPlotIndex(int currentPlotIndex) {
        this.currentPlotIndex = currentPlotIndex;
    }

    public void incrementPlotIndex() {
        this.currentPlotIndex++;
    }

    public int getBuildTimeLeft() {
        return buildTimeLeft;
    }

    public void setBuildTimeLeft(int buildTimeLeft) {
        this.buildTimeLeft = buildTimeLeft;
    }

    public void decrementBuildTime() {
        this.buildTimeLeft = Math.max(0, this.buildTimeLeft - 1);
    }

    public boolean isEnded() {
        return ended;
    }

    public void markEnded() {
        this.ended = true;
    }

    public void addPoints(UUID playerId, int points) {
        playerPoints.merge(playerId, points, Integer::sum);
    }

    public int getPoints(UUID playerId) {
        return playerPoints.getOrDefault(playerId, 0);
    }

    public Map<UUID, Integer> getPointsSnapshot() {
        return new ConcurrentHashMap<>(playerPoints);
    }

    public void castPlotVote(UUID voterId, UUID builderId, int points) {
        plotVotes.computeIfAbsent(builderId, k -> new ConcurrentHashMap<>()).put(voterId, points);
    }

    public boolean hasVotedCurrentPlot(UUID voterId) {
        return votedCurrentPlot.contains(voterId);
    }

    public void markVotedCurrentPlot(UUID voterId) {
        if (voterId != null) {
            votedCurrentPlot.add(voterId);
        }
    }

    public void clearVotedCurrentPlot() {
        votedCurrentPlot.clear();
    }

    public Map<UUID, Map<UUID, Integer>> getPlotVotes() {
        Map<UUID, Map<UUID, Integer>> copy = new ConcurrentHashMap<>();
        for (Map.Entry<UUID, Map<UUID, Integer>> entry : plotVotes.entrySet()) {
            copy.put(entry.getKey(), new ConcurrentHashMap<>(entry.getValue()));
        }
        return copy;
    }

    public void initializePlayer(UUID playerId) {
        playerPoints.putIfAbsent(playerId, 0);
    }



    public void setPlotTimeOverride(UUID plotOwnerId, long ticks) {
        if (plotOwnerId != null) {
            plotTimeOverrides.put(plotOwnerId, ticks);
        }
    }

    public Long getPlotTimeOverride(UUID plotOwnerId) {
        return plotTimeOverrides.get(plotOwnerId);
    }

    public void setPlotWeatherOverride(UUID plotOwnerId, WeatherType weather) {
        if (plotOwnerId != null) {
            plotWeatherOverrides.put(plotOwnerId, weather);
        }
    }

    public WeatherType getPlotWeatherOverride(UUID plotOwnerId) {
        return plotWeatherOverrides.get(plotOwnerId);
    }

    public void addFloorChangeMode(UUID playerId) {
        if (playerId != null) {
            floorChangeMode.add(playerId);
        }
    }

    public void removeFloorChangeMode(UUID playerId) {
        floorChangeMode.remove(playerId);
    }

    public boolean isInFloorChangeMode(UUID playerId) {
        return floorChangeMode.contains(playerId);
    }

    public void setPendingBiomeMenu(UUID playerId, boolean pending) {
        if (playerId == null) {
            return;
        }
        if (pending) {
            pendingBiomeMenu.add(playerId);
        } else {
            pendingBiomeMenu.remove(playerId);
        }
    }

    public boolean isPendingBiomeMenu(UUID playerId) {
        return pendingBiomeMenu.contains(playerId);
    }

    public List<PlotParticle> getPlotParticles(UUID plotOwnerId) {
        return plotParticles.computeIfAbsent(plotOwnerId, k -> new ArrayList<>());
    }

    public void clearPlotParticles(UUID plotOwnerId) {
        plotParticles.remove(plotOwnerId);
    }

    public void clearAllPlotParticles() {
        plotParticles.clear();
    }

    public Map<UUID, List<PlotParticle>> getAllPlotParticles() {
        return new ConcurrentHashMap<>(plotParticles);
    }
}
