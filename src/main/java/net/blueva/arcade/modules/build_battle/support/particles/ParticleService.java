package net.blueva.arcade.modules.build_battle.support.particles;

import net.blueva.arcade.api.ModuleAPI;
import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.ui.ItemAPI;
import net.blueva.arcade.api.ui.MessageAPI;
import net.blueva.arcade.modules.build_battle.game.BuildBattleGame;
import net.blueva.arcade.modules.build_battle.state.ArenaState;
import net.blueva.arcade.modules.build_battle.state.Plot;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ParticleService {

    private static final int BACK_SLOT = 45;
    private static final int REMOVE_SLOT = 49;



    private static final List<ParticleEntry> PARTICLES = List.of(
            new ParticleEntry("FLAME", Material.BLAZE_POWDER, "<gold>Flame"),
            new ParticleEntry("HEART", Material.RED_DYE, "<red>Heart"),
            new ParticleEntry("NOTE", Material.NOTE_BLOCK, "<yellow>Note"),
            new ParticleEntry("CRIT", Material.IRON_SWORD, "<gray>Crit"),
            new ParticleEntry("WITCH", Material.POTION, "<dark_purple>Witch Magic"),
            new ParticleEntry("DRIP_WATER", Material.WATER_BUCKET, "<aqua>Water Drip"),
            new ParticleEntry("DRIP_LAVA", Material.LAVA_BUCKET, "<gold>Lava Drip"),
            new ParticleEntry("ANGRY_VILLAGER", Material.EMERALD, "<red>Angry Villager"),
            new ParticleEntry("HAPPY_VILLAGER", Material.WHEAT, "<green>Happy Villager"),
            new ParticleEntry("PORTAL", Material.OBSIDIAN, "<dark_purple>Portal"),
            new ParticleEntry("ENCHANT", Material.ENCHANTING_TABLE, "<light_purple>Enchant"),
            new ParticleEntry("FIREWORKS_SPARK", Material.FIREWORK_ROCKET, "<aqua>Firework"),
            new ParticleEntry("LAVA", Material.MAGMA_BLOCK, "<gold>Lava"),
            new ParticleEntry("SLIME", Material.SLIME_BALL, "<green>Slime"),
            new ParticleEntry("SNOWBALL", Material.SNOWBALL, "<white>Snowball"),
            new ParticleEntry("CLOUD", Material.WHITE_WOOL, "<white>Cloud"),
            new ParticleEntry("SMOKE_NORMAL", Material.COAL, "<gray>Smoke"),
            new ParticleEntry("SMOKE_LARGE", Material.CHARCOAL, "<dark_gray>Large Smoke"),
            new ParticleEntry("SPELL", Material.GLOWSTONE_DUST, "<yellow>Spell"),
            new ParticleEntry("SPELL_INSTANT", Material.SUGAR, "<white>Instant Spell"),
            new ParticleEntry("SPELL_MOB", Material.NETHER_WART, "<dark_purple>Mob Spell"),
            new ParticleEntry("TOWN_AURA", Material.GRASS_BLOCK, "<green>Town Aura"),
            new ParticleEntry("BUBBLE", Material.BUBBLE_CORAL, "<aqua>Bubble"),
            new ParticleEntry("SPLASH", Material.PRISMARINE_SHARD, "<blue>Splash"),
            new ParticleEntry("SNOW_SHOVEL", Material.WOODEN_SHOVEL, "<white>Snow Shovel"),
            new ParticleEntry("EXPLOSION", Material.TNT, "<red>Explosion"),
            new ParticleEntry("EXPLOSION_LARGE", Material.TNT, "<dark_red>Large Explosion"),
            new ParticleEntry("SOUL_FIRE_FLAME", Material.SOUL_TORCH, "<aqua>Soul Flame"),
            new ParticleEntry("ASH", Material.GRAY_DYE, "<gray>Ash"),
            new ParticleEntry("SOUL", Material.SOUL_LANTERN, "<aqua>Soul"),
            new ParticleEntry("FALLING_SPORE_BLOSSOM", Material.SPORE_BLOSSOM, "<green>Spore Blossom"),
            new ParticleEntry("SNOWFLAKE", Material.SNOW_BLOCK, "<white>Snowflake"),
            new ParticleEntry("GLOW", Material.GLOWSTONE, "<yellow>Glow"),
            new ParticleEntry("GLOW_SQUID_INK", Material.GLOW_INK_SAC, "<aqua>Glow Ink"),
            new ParticleEntry("TOTEM_OF_UNDYING", Material.TOTEM_OF_UNDYING, "<gold>Totem"),
            new ParticleEntry("WAX_ON", Material.HONEYCOMB, "<gold>Wax On"),
            new ParticleEntry("WAX_OFF", Material.HONEYCOMB, "<yellow>Wax Off")
    );

    private final ModuleConfigAPI moduleConfig;
    private final ItemAPI<Player, ItemStack, Material> itemAPI;
    private BuildBattleGame game;

    public ParticleService(ModuleConfigAPI moduleConfig,
                           ItemAPI<Player, ItemStack, Material> itemAPI) {
        this.moduleConfig = moduleConfig;
        this.itemAPI = itemAPI;
    }

    public void setGame(BuildBattleGame game) {
        this.game = game;
    }

    public void openParticleMenu(Player player) {
        String title = moduleConfig.getStringFrom("language.yml", "options.particle.title");
        String legacyTitle = itemAPI != null ? itemAPI.formatInventoryTitle(title) : title;
        Inventory inv = Bukkit.createInventory(new ParticleMenuHolder(ParticleMenuHolder.Type.SELECT), 54, legacyTitle);

        int slot = 0;
        for (ParticleEntry entry : PARTICLES) {
            if (slot >= 54 || slot == BACK_SLOT || slot == REMOVE_SLOT) {
                if (slot == BACK_SLOT || slot == REMOVE_SLOT) slot++;
                if (slot >= 54) break;
            }
            ItemStack item = new ItemStack(entry.icon());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(itemAPI != null ? itemAPI.formatDisplayName(entry.displayName()) : entry.displayName());
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
        }

        inv.setItem(BACK_SLOT, createBackItem());
        inv.setItem(REMOVE_SLOT, createRemoveItem());
        player.openInventory(inv);
    }

    public void openRemoveMenu(Player player) {
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = getContext(player);
        ArenaState state = getArenaState(context);
        if (state == null) {
            return;
        }
        Plot plot = state.getPlayerPlot(player.getUniqueId());
        if (plot == null) {
            return;
        }

        List<PlotParticle> particles = state.getPlotParticles(player.getUniqueId());
        String title = moduleConfig.getStringFrom("language.yml", "options.particle.title") + " - Remove";
        String legacyTitle = itemAPI != null ? itemAPI.formatInventoryTitle(title) : title;
        int size = Math.min(54, ((particles.size() + 8) / 9) * 9 + 9);
        size = Math.max(size, 27);
        Inventory inv = Bukkit.createInventory(new ParticleMenuHolder(ParticleMenuHolder.Type.REMOVE), size, legacyTitle);

        for (int i = 0; i < particles.size() && i < size; i++) {
            PlotParticle pp = particles.get(i);
            ItemStack item = new ItemStack(Material.BLAZE_POWDER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String locStr = String.format("%d, %d, %d", pp.location().getBlockX(), pp.location().getBlockY(), pp.location().getBlockZ());
                meta.setDisplayName(itemAPI != null ? itemAPI.formatDisplayName("<yellow>" + pp.effect()) : pp.effect());
                meta.setLore(List.of(
                        itemAPI != null ? itemAPI.formatDisplayName("<gray>Location: " + locStr) : "Location: " + locStr,
                        itemAPI != null ? itemAPI.formatDisplayName("<red>Click to remove") : "Click to remove"
                ));
                item.setItemMeta(meta);
            }
            inv.setItem(i, item);
        }

        inv.setItem(size - 1, createBackItem());
        player.openInventory(inv);
    }

    public boolean handleInventoryClick(Player player, ParticleMenuHolder holder, int slot) {
        if (holder.getType() == ParticleMenuHolder.Type.SELECT) {
            if (slot == BACK_SLOT) {
                player.closeInventory();
                return true;
            }
            if (slot == REMOVE_SLOT) {
                player.closeInventory();
                openRemoveMenu(player);
                return true;
            }
            ParticleEntry entry = particleEntryFromSlot(slot);
            if (entry == null) {
                return true;
            }
            placeParticle(player, entry);
            return true;
        } else if (holder.getType() == ParticleMenuHolder.Type.REMOVE) {
            GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = getContext(player);
            ArenaState state = getArenaState(context);
            if (state == null) {
                return true;
            }
            List<PlotParticle> particles = state.getPlotParticles(player.getUniqueId());
            if (slot >= 0 && slot < particles.size()) {
                particles.remove(slot);
                player.closeInventory();
                sendMessage(player, "options.messages.particle_removed");
            }
            return true;
        }
        return false;
    }

    private void placeParticle(Player player, ParticleEntry entry) {
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = getContext(player);
        ArenaState state = getArenaState(context);
        if (state == null) {
            return;
        }
        Plot plot = state.getPlayerPlot(player.getUniqueId());
        if (plot == null) {
            return;
        }

        int limit = moduleConfig.getInt("particles.limit");
        List<PlotParticle> particles = state.getPlotParticles(player.getUniqueId());
        if (particles.size() >= limit) {
            sendMessage(player, "options.messages.particle_limit_reached");
            return;
        }

        Location loc = player.getLocation().clone();
        particles.add(new PlotParticle(loc, entry.effect().toUpperCase(Locale.ROOT)));
        player.closeInventory();
        sendMessage(player, "options.messages.particle_added");
    }

    private ParticleEntry particleEntryFromSlot(int slot) {
        int effectiveSlot = slot;
        if (slot >= BACK_SLOT) effectiveSlot--;
        if (slot >= REMOVE_SLOT) effectiveSlot--;
        if (effectiveSlot >= 0 && effectiveSlot < PARTICLES.size()) {
            return PARTICLES.get(effectiveSlot);
        }
        return null;
    }

    private ItemStack createBackItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = moduleConfig.getStringFrom("language.yml", "options.banner.back");
            meta.setDisplayName(itemAPI != null ? itemAPI.formatDisplayName(name) : name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createRemoveItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = "<red>Remove Particles";
            meta.setDisplayName(itemAPI != null ? itemAPI.formatDisplayName(name) : name);
            item.setItemMeta(meta);
        }
        return item;
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
        return game.getArenaState(context);
    }

    private void sendMessage(Player player, String path) {
        if (moduleConfig == null || player == null) return;
        String message = moduleConfig.getStringFrom("language.yml", path);
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

    private record ParticleEntry(String effect, Material icon, String displayName) {
    }
}
