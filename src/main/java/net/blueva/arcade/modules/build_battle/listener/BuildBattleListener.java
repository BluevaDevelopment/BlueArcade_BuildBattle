package net.blueva.arcade.modules.build_battle.listener;

import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.game.GamePhase;
import net.blueva.arcade.modules.build_battle.game.BuildBattleGame;
import net.blueva.arcade.modules.build_battle.state.ArenaState;
import net.blueva.arcade.modules.build_battle.state.BuildPhase;
import net.blueva.arcade.modules.build_battle.state.Plot;
import net.blueva.arcade.modules.build_battle.support.banner.BannerMenuHolder;
import net.blueva.arcade.modules.build_battle.support.heads.HeadMenuHolder;
import net.blueva.arcade.modules.build_battle.support.particles.ParticleMenuHolder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

public class BuildBattleListener implements Listener {

    private final BuildBattleGame game;

    public BuildBattleListener(BuildBattleGame game) {
        this.game = game;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = game.getContext(player);
        if (context == null || !context.isPlayerPlaying(player)) {
            return;
        }
        if (event.getTo() == null) {
            return;
        }
        if (context.getPhase() != GamePhase.PLAYING) {
            return;
        }

        ArenaState state = game.getArenaState(context);
        if (state != null && state.getPhase() == BuildPhase.BUILDING) {
            Plot plot = state.getPlayerPlot(player.getUniqueId());
            if (plot != null) {
                if (!plot.isInside(event.getTo())) {
                    player.teleport(plot.findSafeTeleport());
                }
                return;
            }
        }

        if (!context.isInsideBounds(event.getTo())) {
            Location spawn = state != null ? state.getPlayerPlotSpawn(player.getUniqueId()) : null;
            if (spawn != null) {
                player.teleport(spawn);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = game.getContext(player);
        if (context == null || !context.isPlayerPlaying(player)) {
            return;
        }

        ArenaState state = game.getArenaState(context);
        if (state == null || state.getPhase() != BuildPhase.BUILDING) {
            event.setCancelled(true);
            return;
        }

        Plot plot = state.getPlayerPlot(player.getUniqueId());
        if (plot != null) {
            if (!plot.isInside(event.getBlock().getLocation())) {
                event.setCancelled(true);
                return;
            }
        } else if (!context.isInsideBounds(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = game.getContext(player);
        if (context == null || !context.isPlayerPlaying(player)) {
            return;
        }

        ArenaState state = game.getArenaState(context);
        if (state == null) {
            event.setCancelled(true);
            return;
        }

        if (state.getPhase() == BuildPhase.PLOT_VOTING) {
            ItemStack item = event.getItemInHand();
            if (item != null && item.getType() != Material.AIR) {
                game.handleVoteItemClick(context, player, item);
            }
            event.setCancelled(true);
            return;
        }

        if (state.getPhase() != BuildPhase.BUILDING) {
            event.setCancelled(true);
            return;
        }


        if (state.isInFloorChangeMode(player.getUniqueId())) {
            event.setCancelled(true);
            if (game.getOptionsService() != null) {
                game.getOptionsService().handleFloorChange(player, event.getBlock().getType());
            }
            return;
        }

        Plot plot = state.getPlayerPlot(player.getUniqueId());
        if (plot != null) {
            if (!plot.isInside(event.getBlock().getLocation())) {
                event.setCancelled(true);
                return;
            }
        } else if (!context.isInsideBounds(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player target)) {
            return;
        }

        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = game.getContext(target);
        if (context == null || !context.isPlayerPlaying(target)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = game.getContext(player);
        if (context == null || !context.isPlayerPlaying(player)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = game.getContext(player);
        if (context == null || !context.isPlayerPlaying(player)) {
            return;
        }

        ArenaState state = game.getArenaState(context);
        if (state != null && state.getPhase() == BuildPhase.PLOT_VOTING) {
            if (event.getClickedInventory() != player.getInventory()) {
                event.setCancelled(true);
            }
            return;
        }


        if (event.getInventory().getHolder() instanceof BannerMenuHolder holder) {
            event.setCancelled(true);
            if (game.getOptionsService() != null) {
                game.getOptionsService().getBannerCreatorService().handleInventoryClick(player, holder, event.getRawSlot(), event.getInventory());
            }
            return;
        }


        if (event.getInventory().getHolder() instanceof ParticleMenuHolder holder) {
            event.setCancelled(true);
            if (game.getOptionsService() != null) {
                game.getOptionsService().getParticleService().handleInventoryClick(player, holder, event.getRawSlot());
            }
            return;
        }


        if (event.getInventory().getHolder() instanceof HeadMenuHolder holder) {
            event.setCancelled(true);
            if (game.getOptionsService() != null) {
                game.getOptionsService().getHeadsService().handleInventoryClick(player, holder, event.getRawSlot());
            }
            return;
        }

        if (state != null && state.getPhase() == BuildPhase.BUILDING) {

            if (game.getOptionsService() != null && game.getOptionsService().handleBiomeMenuClick(player, event)) {
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = game.getContext(player);
        if (context == null || !context.isPlayerPlaying(player)) {
            return;
        }

        ArenaState state = game.getArenaState(context);
        if (state == null) {
            return;
        }

        if (state.getPhase() == BuildPhase.BUILDING) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType() == Material.NETHER_STAR) {
                if (game.getOptionsService() != null) {
                    game.getOptionsService().handleOptionsClick(player);
                }
                event.setCancelled(true);
            }
            return;
        }

        if (state.getPhase() == BuildPhase.PLOT_VOTING) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType() == Material.AIR) {
                return;
            }
            game.handleVoteItemClick(context, player, item);
            event.setCancelled(true);
        }
    }

}
