package net.blueva.arcade.modules.build_battle.game;

import net.blueva.arcade.api.game.GameContext;
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
import java.util.List;

class BuildBattlePlotService {

    void loadPlots(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                   ArenaState state) {
        if (context.getDataAccess() == null) {
            return;
        }
        Integer totalPlotsObj = context.getDataAccess().getGameData("game.plots.total", Integer.class);
        int totalPlots = totalPlotsObj != null ? totalPlotsObj : 0;

        World arenaWorld = context.getArenaAPI() != null ? context.getArenaAPI().getWorld() : null;

        for (int i = 1; i <= totalPlots; i++) {
            String basePath = "game.plots.list.p" + i;
            Location min = context.getDataAccess().getGameLocation(basePath + ".bounds.min");
            Location max = context.getDataAccess().getGameLocation(basePath + ".bounds.max");
            String floorName = context.getDataAccess().getGameData(basePath + ".floor", String.class);

            if (min == null || max == null) {
                continue;
            }
            if (min.getWorld() == null && arenaWorld != null) {
                min = new Location(arenaWorld, min.getX(), min.getY(), min.getZ());
            }
            if (max.getWorld() == null && arenaWorld != null) {
                max = new Location(arenaWorld, max.getX(), max.getY(), max.getZ());
            }

            Material floorMaterial = Material.GRASS_BLOCK;
            if (floorName != null) {
                try {
                    floorMaterial = Material.valueOf(floorName.toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }

            Location spawn = context.getDataAccess().getGameLocation(basePath + ".spawn");
            if (spawn == null) {
                spawn = calculatePlotCenter(min, max);
            }
            Plot plot = new Plot(min, max, floorMaterial, spawn);
            state.addPlot(plot);
            state.addPlotSpawn(spawn);
        }
    }

    void assignPlayersToPlots(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                              ArenaState state) {
        List<Player> players = new ArrayList<>(context.getPlayers());
        List<Plot> plots = state.getPlots();

        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (i < plots.size()) {
                Plot plot = plots.get(i);
                state.setPlayerPlotSpawn(player.getUniqueId(), plot.getSpawn());
                state.setPlayerPlot(player.getUniqueId(), plot);
            } else if (!plots.isEmpty()) {
                Plot plot = plots.get(i % plots.size());
                state.setPlayerPlotSpawn(player.getUniqueId(), plot.getSpawn());
                state.setPlayerPlot(player.getUniqueId(), plot);
            }
        }
    }

    void regeneratePlotFloors(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                              ArenaState state) {
        if (context.getBlocksAPI() == null) {
            return;
        }
        for (Plot plot : state.getPlots()) {
            regeneratePlotFloor(context, plot);
        }
    }

    void clearPlots(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                    ArenaState state) {
        if (context.getBlocksAPI() == null) {
            return;
        }
        for (Plot plot : state.getPlots()) {
            if (plot.getMin() == null || plot.getMax() == null) {
                continue;
            }
            context.getBlocksAPI().setRegion(plot.getMin(), plot.getMax(), Material.AIR);
        }
    }

    void teleportToPlot(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                        ArenaState state,
                        Player player) {
        Location plot = state.getPlayerPlotSpawn(player.getUniqueId());
        if (plot == null || plot.getWorld() == null) {
            return;
        }
        context.getSchedulerAPI().runAtEntity(player, () -> player.teleport(centerLocation(plot)));
    }

    void regeneratePlotFloor(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                             Plot plot) {
        if (context.getBlocksAPI() == null || plot.getFloorMaterial() == null || plot.getMin() == null || plot.getMax() == null) {
            return;
        }
        int minX = Math.min(plot.getMin().getBlockX(), plot.getMax().getBlockX());
        int minY = Math.min(plot.getMin().getBlockY(), plot.getMax().getBlockY());
        int minZ = Math.min(plot.getMin().getBlockZ(), plot.getMax().getBlockZ());
        int maxX = Math.max(plot.getMin().getBlockX(), plot.getMax().getBlockX());
        int maxZ = Math.max(plot.getMin().getBlockZ(), plot.getMax().getBlockZ());

        Location floorMin = new Location(plot.getMin().getWorld(), minX, minY, minZ);
        Location floorMax = new Location(plot.getMax().getWorld(), maxX, minY, maxZ);
        context.getBlocksAPI().setRegion(floorMin, floorMax, plot.getFloorMaterial());
    }

    static Location centerLocation(Location loc) {
        double centeredX = Math.floor(loc.getX()) + 0.5;
        double centeredZ = Math.floor(loc.getZ()) + 0.5;
        return new Location(loc.getWorld(), centeredX, loc.getY(), centeredZ, loc.getYaw(), loc.getPitch());
    }

    private Location calculatePlotCenter(Location min, Location max) {
        double centerX = (min.getX() + max.getX()) / 2.0;
        double centerZ = (min.getZ() + max.getZ()) / 2.0;
        double centerY = Math.min(min.getY(), max.getY()) + 1.0;
        World world = min.getWorld() != null ? min.getWorld() : max.getWorld();
        return new Location(world, centerX, centerY, centerZ);
    }
}
