package net.blueva.arcade.modules.build_battle.support.banner;

import net.blueva.arcade.api.ModuleAPI;
import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.ui.ItemAPI;
import net.blueva.arcade.api.ui.MessageAPI;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BannerCreatorService {

    private static final int BACK_SLOT = 45;
    private static final int CREATE_SLOT = 49;

    private final ModuleConfigAPI moduleConfig;
    private final ItemAPI<Player, ItemStack, Material> itemAPI;
    private final Map<Player, BannerBuilderState> playerStates = new HashMap<>();

    public BannerCreatorService(ModuleConfigAPI moduleConfig,
                                ItemAPI<Player, ItemStack, Material> itemAPI) {
        this.moduleConfig = moduleConfig;
        this.itemAPI = itemAPI;
    }

    public void openBaseColorMenu(Player player) {
        BannerBuilderState state = new BannerBuilderState();
        playerStates.put(player, state);

        String title = moduleConfig.getStringFrom("language.yml", "options.banner.color_title");
        String legacyTitle = itemAPI != null ? itemAPI.formatInventoryTitle(title) : title;
        Inventory inv = Bukkit.createInventory(new BannerMenuHolder(state, BannerMenuHolder.Stage.BASE), 54, legacyTitle);

        int slot = 0;
        for (DyeColor color : DyeColor.values()) {
            if (slot >= 54) break;
            Material mat = materialForColor(color);
            ItemStack item = decorate(new ItemStack(mat), "<light_purple>" + formatEnumName(color.name()), List.of());
            inv.setItem(slot++, item);
        }

        inv.setItem(BACK_SLOT, createBackItem());
        inv.setItem(CREATE_SLOT, createCreateItem(state));
        player.openInventory(inv);
    }

    public void openLayerMenu(Player player, BannerBuilderState state) {
        playerStates.put(player, state);

        String title = moduleConfig.getStringFrom("language.yml", "options.banner.layer_title");
        String legacyTitle = itemAPI != null ? itemAPI.formatInventoryTitle(title) : title;
        Inventory inv = Bukkit.createInventory(new BannerMenuHolder(state, BannerMenuHolder.Stage.LAYER), 54, legacyTitle);

        DyeColor contrast = contrastColor(state.getBaseColor());
        List<String> patternNames = BannerPatternUtil.getAvailableLayerPatternNames();
        int slot = 0;
        for (String patternName : patternNames) {
            if (slot >= 54 || slot == BACK_SLOT || slot == CREATE_SLOT) {
                if (slot == BACK_SLOT || slot == CREATE_SLOT) slot++;
                if (slot >= 54) break;
            }
            ItemStack preview = state.buildBanner();
            BannerMeta meta = (BannerMeta) preview.getItemMeta();
            if (meta != null) {
                Pattern p = BannerPatternUtil.createPattern(contrast, patternName);
                if (p != null) {
                    meta.addPattern(p);
                    preview.setItemMeta(meta);
                    preview = decorate(preview, "<white>" + formatEnumName(patternName),
                            List.of("<gray>Click to add this pattern.</gray>"));
                }
            }
            inv.setItem(slot++, preview);
        }

        inv.setItem(BACK_SLOT, createBackItem());
        inv.setItem(CREATE_SLOT, createCreateItem(state));
        player.openInventory(inv);
    }

    public void openLayerColorMenu(Player player, BannerBuilderState state) {
        playerStates.put(player, state);

        String title = moduleConfig.getStringFrom("language.yml", "options.banner.layer_color_title");
        String legacyTitle = itemAPI != null ? itemAPI.formatInventoryTitle(title) : title;
        Inventory inv = Bukkit.createInventory(new BannerMenuHolder(state, BannerMenuHolder.Stage.LAYER_COLOR), 54, legacyTitle);

        String lastTypeName = state.getLastPatternName();

        int slot = 0;
        for (DyeColor color : DyeColor.values()) {
            if (slot >= 54 || slot == BACK_SLOT || slot == CREATE_SLOT) {
                if (slot == BACK_SLOT || slot == CREATE_SLOT) slot++;
                if (slot >= 54) break;
            }
            ItemStack preview = state.buildBanner();
            BannerMeta meta = (BannerMeta) preview.getItemMeta();
            if (meta != null) {
                Pattern p = BannerPatternUtil.createPattern(color, lastTypeName);
                if (p != null) {
                    meta.addPattern(p);
                    preview.setItemMeta(meta);
                    preview = decorate(preview, "<light_purple>" + formatEnumName(color.name()),
                            List.of("<gray>Click to use this color.</gray>"));
                }
            }
            inv.setItem(slot++, preview);
        }

        inv.setItem(BACK_SLOT, createBackItem());
        inv.setItem(CREATE_SLOT, createCreateItem(state));
        player.openInventory(inv);
    }

    public boolean handleInventoryClick(Player player, BannerMenuHolder holder, int slot, org.bukkit.inventory.Inventory inventory) {
        BannerBuilderState state = holder.getState();
        if (state == null) {
            return false;
        }

        if (slot == BACK_SLOT) {
            player.closeInventory();
            openOptionsMain(player);
            return true;
        }

        if (slot == CREATE_SLOT) {
            player.closeInventory();
            ItemStack banner = createFinalBannerItem(state);
            player.getInventory().addItem(banner);
            sendMessage(player, "options.messages.banner_created");
            playerStates.remove(player);
            return true;
        }

        if (slot < 0 || slot >= inventory.getSize()) {
            return true;
        }

        ItemStack clicked = inventory.getItem(slot);
        if (clicked == null || clicked.getType() == Material.AIR) {
            return true;
        }

        switch (holder.getStage()) {
            case BASE -> {
                DyeColor color = dyeColorFromMaterial(clicked.getType());
                if (color != null) {
                    state.setBaseColor(color);
                    openLayerMenu(player, state);
                }
            }
            case LAYER -> {
                String patternName = patternNameFromSlot(slot);
                if (patternName != null) {
                    DyeColor contrast = contrastColor(state.getBaseColor());
                    Pattern p = BannerPatternUtil.createPattern(contrast, patternName);
                    if (p != null) {
                        state.addPattern(p, patternName);
                    }
                    openLayerColorMenu(player, state);
                }
            }
            case LAYER_COLOR -> {
                DyeColor color = dyeColorFromSlot(slot);
                if (color != null) {
                    String lastTypeName = state.getLastPatternName();
                    Pattern p = BannerPatternUtil.createPattern(color, lastTypeName);
                    if (p != null) {
                        state.replaceLastPattern(p, lastTypeName);
                    }
                    openLayerMenu(player, state);
                }
            }
        }
        return true;
    }

    private void openOptionsMain(Player player) {
        player.closeInventory();
    }

    private String patternNameFromSlot(int slot) {
        List<String> names = BannerPatternUtil.getAvailableLayerPatternNames();
        int effectiveSlot = effectiveContentSlot(slot);
        if (effectiveSlot >= 0 && effectiveSlot < names.size()) {
            return names.get(effectiveSlot);
        }
        return null;
    }

    private static DyeColor dyeColorFromSlot(int slot) {
        int effectiveSlot = effectiveContentSlot(slot);
        DyeColor[] colors = DyeColor.values();
        if (effectiveSlot >= 0 && effectiveSlot < colors.length) {
            return colors[effectiveSlot];
        }
        return null;
    }

    private static int effectiveContentSlot(int slot) {
        int effectiveSlot = slot;
        if (slot >= BACK_SLOT) effectiveSlot--;
        if (slot >= CREATE_SLOT) effectiveSlot--;
        return effectiveSlot;
    }

    private static DyeColor dyeColorFromMaterial(Material material) {
        if (material == null) return null;
        String name = material.name();
        if (!name.endsWith("_BANNER")) return null;
        try {
            return DyeColor.valueOf(name.substring(0, name.length() - 7));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Material materialForColor(DyeColor color) {
        try {
            return Material.valueOf(color.name() + "_BANNER");
        } catch (IllegalArgumentException e) {
            return Material.WHITE_BANNER;
        }
    }

    private static DyeColor contrastColor(DyeColor base) {
        if (base == null) return DyeColor.BLACK;
        return switch (base) {
            case BLACK, BLUE, BROWN, CYAN, GRAY, GREEN, LIGHT_BLUE, MAGENTA, ORANGE, PINK, PURPLE, RED, YELLOW ->
                    DyeColor.WHITE;
            default -> DyeColor.BLACK;
        };
    }

    private ItemStack createBackItem() {
        String name = moduleConfig.getStringFrom("language.yml", "options.banner.back");
        return decorate(new ItemStack(Material.ARROW), name, List.of());
    }

    private ItemStack createCreateItem(BannerBuilderState state) {
        String name = moduleConfig.getStringFrom("language.yml", "options.banner.create");
        return decorate(state.buildBanner(), name, List.of("<gray>Click to add this banner to your inventory.</gray>"));
    }

    private ItemStack createFinalBannerItem(BannerBuilderState state) {
        String name = moduleConfig.getStringFrom("language.yml", "options.banner.final_name");
        return decorate(state.buildBanner(), name, List.of());
    }

    private ItemStack decorate(ItemStack item, String displayName, List<String> lore) {
        if (itemAPI != null) {
            return itemAPI.decorate(item, displayName, lore);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null && displayName != null && !displayName.isBlank()) {
            meta.setDisplayName(displayName);
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String formatEnumName(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String part : value.toLowerCase(java.util.Locale.ROOT).split("_")) {
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
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
}
