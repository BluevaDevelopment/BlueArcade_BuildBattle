package net.blueva.arcade.modules.build_battle.setup;

import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.setup.GameSetupHandler;
import net.blueva.arcade.api.setup.SetupContext;
import net.blueva.arcade.api.setup.TabCompleteContext;
import net.blueva.arcade.api.setup.TabCompleteResult;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class BuildBattleSetup implements GameSetupHandler {

    private final ModuleConfigAPI moduleConfig;

    public BuildBattleSetup(ModuleConfigAPI moduleConfig) {
        this.moduleConfig = moduleConfig;
    }

    @Override
    public boolean handle(SetupContext context) {
        return handleInternal(castSetupContext(context));
    }

    private boolean handleInternal(SetupContext<Player, CommandSender, Location> context) {
        String subcommand = context.getArg(context.getStartIndex() - 1);
        if ("plot".equalsIgnoreCase(subcommand)) {
            return handlePlot(context);
        }
        return false;
    }

    @Override
    public TabCompleteResult tabComplete(TabCompleteContext context) {
        return tabCompleteInternal(castTabContext(context));
    }

    private TabCompleteResult tabCompleteInternal(TabCompleteContext<Player, CommandSender> context) {
        String subcommand = context.getArg(context.getStartIndex() - 1);

        if ("plot".equalsIgnoreCase(subcommand)) {
            if (context.getRelativeArgIndex() == 0) {
                return TabCompleteResult.of("add", "set", "remove", "spawn");
            }
            String handlerArg0 = context.getArg(context.getStartIndex());
            if ("spawn".equalsIgnoreCase(handlerArg0)) {
                if (context.getRelativeArgIndex() == 1) {
                    return TabCompleteResult.of("set");
                }
                if (context.getRelativeArgIndex() == 2) {
                    return TabCompleteResult.empty();
                }
            }
            if (("set".equalsIgnoreCase(handlerArg0) || "remove".equalsIgnoreCase(handlerArg0))
                    && context.getRelativeArgIndex() == 1) {
                return TabCompleteResult.empty();
            }
        }

        return TabCompleteResult.empty();
    }

    @Override
    public List<String> getSubcommands() {
        return List.of("plot");
    }

    private boolean handlePlot(SetupContext<Player, CommandSender, Location> context) {
        String action = context.getHandlerArg(0);
        if ("add".equalsIgnoreCase(action)) {
            return handlePlotAdd(context);
        }
        if ("set".equalsIgnoreCase(action)) {
            return handlePlotSet(context);
        }
        if ("remove".equalsIgnoreCase(action)) {
            return handlePlotRemove(context);
        }
        if ("spawn".equalsIgnoreCase(action)) {
            return handlePlotSpawn(context);
        }
        context.getMessagesAPI().sendRaw(context.getPlayer(), getSetupMessage(context.getPlayer(), "plot.usage"));
        return true;
    }

    private boolean handlePlotAdd(SetupContext<Player, CommandSender, Location> context) {
        Player player = context.getPlayer();
        if (player == null) {
            return true;
        }

        if (!context.getSelection().hasCompleteSelection(player)) {
            context.getMessagesAPI().sendRaw(player, getSetupMessage(context.getPlayer(), "plot.add.must_use_stick"));
            return true;
        }

        Location pos1 = context.getSelection().getPosition1(player);
        Location pos2 = context.getSelection().getPosition2(player);

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        int height = maxY - minY + 1;

        if (height < 3) {
            context.getMessagesAPI().sendRaw(player, getSetupMessage(context.getPlayer(), "plot.add.too_shallow"));
            return true;
        }

        World world = pos1.getWorld() != null ? pos1.getWorld() : pos2.getWorld();
        if (world == null) {
            return true;
        }

        int floorLayers = countFloorLayers(world, minX, maxX, minY, maxY, minZ, maxZ);
        if (floorLayers == 0) {
            context.getMessagesAPI().sendRaw(player, getSetupMessage(context.getPlayer(), "plot.add.no_floor"));
            return true;
        }
        if (floorLayers > 2) {
            context.getMessagesAPI().sendRaw(player, getSetupMessage(context.getPlayer(), "plot.add.too_many_floor_layers"));
            return true;
        }

        Material floorMaterial = detectFloorMaterial(world, minX, minY, minZ);

        int totalPlots = context.getData().getInt("game.plots.total", 0);
        int newPlotNumber = totalPlots + 1;
        String basePath = "game.plots.list.p" + newPlotNumber;

        context.getData().setRegionBounds(basePath + ".bounds", pos1, pos2);
        context.getData().setString(basePath + ".floor", floorMaterial.name());
        context.getData().setLocation(basePath + ".spawn", player.getLocation());
        context.getData().setInt("game.plots.total", newPlotNumber);
        context.getData().save();

        int x = (int) Math.abs(pos2.getX() - pos1.getX()) + 1;
        int y = height;
        int z = (int) Math.abs(pos2.getZ() - pos1.getZ()) + 1;
        int blocks = x * y * z;

        String msg = getSetupMessage(context.getPlayer(), "plot.add.set")
                .replace("{index}", String.valueOf(newPlotNumber))
                .replace("{blocks}", String.valueOf(blocks))
                .replace("{x}", String.valueOf(x))
                .replace("{y}", String.valueOf(y))
                .replace("{z}", String.valueOf(z))
                .replace("{floor}", floorMaterial.name());
        context.getMessagesAPI().sendRaw(player, msg);

        String spawnMsg = getSetupMessage(context.getPlayer(), "plot.add.spawn_set");
        if (spawnMsg != null && !spawnMsg.isEmpty()) {
            context.getMessagesAPI().sendRaw(player, spawnMsg.replace("{index}", String.valueOf(newPlotNumber)));
        }
        return true;
    }

    private boolean handlePlotSet(SetupContext<Player, CommandSender, Location> context) {
        String idArg = context.getHandlerArg(1);
        if (idArg == null) {
            context.getMessagesAPI().sendRaw(context.getPlayer(), getSetupMessage(context.getPlayer(), "plot.set.usage"));
            return true;
        }

        int plotId = parsePlotId(context, idArg);
        if (plotId == -1) {
            return true;
        }

        Player player = context.getPlayer();
        if (player == null) {
            return true;
        }

        if (!context.getSelection().hasCompleteSelection(player)) {
            context.getMessagesAPI().sendRaw(player, getSetupMessage(context.getPlayer(), "plot.add.must_use_stick"));
            return true;
        }

        Location pos1 = context.getSelection().getPosition1(player);
        Location pos2 = context.getSelection().getPosition2(player);

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        int height = maxY - minY + 1;

        if (height < 3) {
            context.getMessagesAPI().sendRaw(player, getSetupMessage(context.getPlayer(), "plot.add.too_shallow"));
            return true;
        }

        World world = pos1.getWorld() != null ? pos1.getWorld() : pos2.getWorld();
        if (world == null) {
            return true;
        }

        int floorLayers = countFloorLayers(world, minX, maxX, minY, maxY, minZ, maxZ);
        if (floorLayers == 0) {
            context.getMessagesAPI().sendRaw(player, getSetupMessage(context.getPlayer(), "plot.add.no_floor"));
            return true;
        }
        if (floorLayers > 2) {
            context.getMessagesAPI().sendRaw(player, getSetupMessage(context.getPlayer(), "plot.add.too_many_floor_layers"));
            return true;
        }

        Material floorMaterial = detectFloorMaterial(world, minX, minY, minZ);

        String basePath = "game.plots.list.p" + plotId;

        context.getData().setRegionBounds(basePath + ".bounds", pos1, pos2);
        context.getData().setString(basePath + ".floor", floorMaterial.name());
        context.getData().setLocation(basePath + ".spawn", player.getLocation());
        context.getData().save();

        int x = (int) Math.abs(pos2.getX() - pos1.getX()) + 1;
        int y = height;
        int z = (int) Math.abs(pos2.getZ() - pos1.getZ()) + 1;
        int blocks = x * y * z;

        String msg = getSetupMessage(context.getPlayer(), "plot.set.success")
                .replace("{index}", String.valueOf(plotId))
                .replace("{blocks}", String.valueOf(blocks))
                .replace("{x}", String.valueOf(x))
                .replace("{y}", String.valueOf(y))
                .replace("{z}", String.valueOf(z))
                .replace("{floor}", floorMaterial.name());
        context.getMessagesAPI().sendRaw(player, msg);
        return true;
    }

    private boolean handlePlotRemove(SetupContext<Player, CommandSender, Location> context) {
        String idArg = context.getHandlerArg(1);
        if (idArg == null) {
            context.getMessagesAPI().sendRaw(context.getPlayer(), getSetupMessage(context.getPlayer(), "plot.remove.usage"));
            return true;
        }

        int plotId = parsePlotId(context, idArg);
        if (plotId == -1) {
            return true;
        }

        int totalPlots = context.getData().getInt("game.plots.total", 0);

        // Remove the target plot
        context.getData().remove("game.plots.list.p" + plotId);

        // Shift remaining plots down to keep IDs contiguous
        for (int i = plotId + 1; i <= totalPlots; i++) {
            String oldPath = "game.plots.list.p" + i;
            String newPath = "game.plots.list.p" + (i - 1);

            Location oldMin = context.getData().getLocation(oldPath + ".bounds.min");
            Location oldMax = context.getData().getLocation(oldPath + ".bounds.max");
            String oldFloor = context.getData().getString(oldPath + ".floor");
            Location oldSpawn = context.getData().getLocation(oldPath + ".spawn");

            if (oldMin != null && oldMax != null) {
                context.getData().setRegionBounds(newPath + ".bounds", oldMin, oldMax);
            }
            if (oldFloor != null) {
                context.getData().setString(newPath + ".floor", oldFloor);
            }
            if (oldSpawn != null) {
                context.getData().setLocation(newPath + ".spawn", oldSpawn);
            }
            context.getData().remove(oldPath);
        }

        context.getData().setInt("game.plots.total", totalPlots - 1);
        context.getData().save();

        String msg = getSetupMessage(context.getPlayer(), "plot.remove.success")
                .replace("{index}", String.valueOf(plotId));
        context.getMessagesAPI().sendRaw(context.getPlayer(), msg);
        return true;
    }

    private boolean handlePlotSpawn(SetupContext<Player, CommandSender, Location> context) {
        String action = context.getHandlerArg(1);
        if (!"set".equalsIgnoreCase(action)) {
            context.getMessagesAPI().sendRaw(context.getPlayer(), getSetupMessage(context.getPlayer(), "plot.spawn.usage"));
            return true;
        }

        String idArg = context.getHandlerArg(2);
        if (idArg == null) {
            context.getMessagesAPI().sendRaw(context.getPlayer(), getSetupMessage(context.getPlayer(), "plot.spawn.usage"));
            return true;
        }

        int plotId = parsePlotId(context, idArg);
        if (plotId == -1) {
            return true;
        }

        Location loc = context.getPlayer().getLocation();
        String basePath = "game.plots.list.p" + plotId;
        context.getData().setLocation(basePath + ".spawn", loc);
        context.getData().save();

        String msg = getSetupMessage(context.getPlayer(), "plot.spawn.set")
                .replace("{index}", String.valueOf(plotId));
        context.getMessagesAPI().sendRaw(context.getPlayer(), msg);
        return true;
    }

    private int parsePlotId(SetupContext<Player, CommandSender, Location> context, String idArg) {
        try {
            int plotId = Integer.parseInt(idArg);
            int totalPlots = context.getData().getInt("game.plots.total", 0);
            if (plotId < 1 || plotId > totalPlots) {
                context.getMessagesAPI().sendRaw(context.getPlayer(), getSetupMessage(context.getPlayer(), "plot.not_found"));
                return -1;
            }
            return plotId;
        } catch (NumberFormatException e) {
            context.getMessagesAPI().sendRaw(context.getPlayer(), getSetupMessage(context.getPlayer(), "plot.invalid_id"));
            return -1;
        }
    }

    private int countFloorLayers(World world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        int layers = 0;
        for (int y = minY; y <= maxY; y++) {
            boolean hasBlock = false;
            for (int x = minX; x <= maxX && !hasBlock; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (world.getBlockAt(x, y, z).getType() != Material.AIR) {
                        hasBlock = true;
                        break;
                    }
                }
            }
            if (hasBlock) {
                layers++;
            } else {
                break;
            }
        }
        return layers;
    }

    private Material detectFloorMaterial(World world, int minX, int minY, int minZ) {
        Block block = world.getBlockAt(minX, minY, minZ);
        Material material = block.getType();
        if (material != Material.AIR) {
            return material;
        }
        return Material.GRASS_BLOCK;
    }

    private String getSetupMessage(Player player, String key) {
        String message = moduleConfig.getTranslation(player, "setup_messages." + key);
        if (message == null) {
            return "";
        }
        return message;
    }

    @SuppressWarnings("unchecked")
    private SetupContext<Player, CommandSender, Location> castSetupContext(SetupContext context) {
        return context;
    }

    @SuppressWarnings("unchecked")
    private TabCompleteContext<Player, CommandSender> castTabContext(TabCompleteContext context) {
        return context;
    }
}
