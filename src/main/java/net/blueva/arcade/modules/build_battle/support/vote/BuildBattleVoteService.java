package net.blueva.arcade.modules.build_battle.support.vote;

import net.blueva.arcade.api.ModuleAPI;
import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.game.GamePhase;
import net.blueva.arcade.api.ui.ItemAPI;
import net.blueva.arcade.api.ui.LobbyItemDefinition;
import net.blueva.arcade.api.ui.MenuAPI;
import net.blueva.arcade.api.ui.MessageAPI;
import net.blueva.arcade.api.ui.menu.MenuDefinition;
import net.blueva.arcade.api.utils.PlayerUtil;
import net.blueva.arcade.modules.build_battle.game.BuildBattleGame;
import net.blueva.arcade.modules.build_battle.state.ArenaState;
import net.blueva.arcade.modules.build_battle.state.VoteState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BuildBattleVoteService {

    private static final String VOTE_PERMISSION_BASE = "bluearcade.build_battle.votes";
    private static final String WAITING_ITEM_ID = "build_battle_vote_theme";
    public static final String COMMAND = "buildbattlevote";
    public static final String MENU_THEMES = "vote_theme";

    private final ModuleConfigAPI moduleConfig;
    private final MenuAPI<Player, Material> menuAPI;
    private final ItemAPI<Player, ItemStack, Material> itemAPI;
    private final String moduleId;
    private final BuildBattleVoteMenuRepository menuRepository;
    private final VoteState waitingVoteState;
    private BuildBattleGame game;

    public BuildBattleVoteService(ModuleConfigAPI moduleConfig,
                                   MenuAPI<Player, Material> menuAPI,
                                   ItemAPI<Player, ItemStack, Material> itemAPI,
                                   String moduleId) {
        this.moduleConfig = moduleConfig;
        this.menuAPI = menuAPI;
        this.itemAPI = itemAPI;
        this.moduleId = moduleId;
        this.menuRepository = new BuildBattleVoteMenuRepository(moduleConfig);
        this.menuRepository.loadMenus();
        this.waitingVoteState = createVoteState();
    }

    public VoteState createVoteState() {
        String defaultTheme = normalizeOption(
                moduleConfig.getString("votes.defaults.theme"),
                getValidThemeIds(),
                "random"
        );
        return new VoteState(defaultTheme);
    }

    public VoteState getWaitingVoteState() {
        return waitingVoteState;
    }

    public void setGame(BuildBattleGame game) {
        this.game = game;
    }

    public void applyPendingVotes(ArenaState state, List<Player> players) {
        if (state == null || players == null || players.isEmpty()) {
            return;
        }
        VoteState voteState = state.getVoteState();
        if (voteState == null) {
            return;
        }

        for (Player player : players) {
            if (player == null) {
                continue;
            }
            String theme = waitingVoteState.getPlayerVote(player.getUniqueId());
            if (theme != null) {
                voteState.castVote(player.getUniqueId(), theme);
            }
            waitingVoteState.clearPlayerVotes(player.getUniqueId());
        }
        waitingVoteState.clearAll();
    }

    public void registerWaitingItem() {
        if (itemAPI == null || moduleConfig == null) {
            return;
        }

        if (!isWaitingItemEnabled()) {
            unregisterWaitingItem();
            return;
        }

        String materialName = moduleConfig.getString("waiting_items.vote_theme.material");
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            material = Material.PAPER;
        }

        int slot = moduleConfig.getInt("waiting_items.vote_theme.slot");
        String displayName = moduleConfig.getString("waiting_items.vote_theme.display_name");
        List<String> lore = moduleConfig.getStringList("waiting_items.vote_theme.lore");

        LobbyItemDefinition<Material> definition = new LobbyItemDefinition<>(
                WAITING_ITEM_ID,
                material,
                slot,
                displayName,
                lore,
                List.of(),
                true
        );

        itemAPI.registerWaitingItem(moduleId, definition);
    }

    public void registerClickHandler(BuildBattleGame game) {
        if (itemAPI == null) {
            return;
        }
        if (!isWaitingItemEnabled()) {
            itemAPI.unregisterClickHandler(WAITING_ITEM_ID);
            return;
        }
        itemAPI.registerClickHandler(WAITING_ITEM_ID,
                player -> game.handleVoteCommand(player, new String[]{"menu", "theme"}));
    }

    public void unregisterWaitingItem() {
        if (itemAPI == null) {
            return;
        }
        itemAPI.unregisterWaitingItem(WAITING_ITEM_ID);
        itemAPI.unregisterClickHandler(WAITING_ITEM_ID);
    }

    private boolean isWaitingItemEnabled() {
        return moduleConfig != null && moduleConfig.getBoolean("waiting_items.vote_theme.enabled");
    }

    public boolean handleVoteCommand(Player player,
                                     GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                     ArenaState state,
                                     String[] args) {
        if (player == null || context == null || state == null) {
            return false;
        }

        GamePhase phase = context.getPhase();
        if (phase == GamePhase.PLAYING || phase == GamePhase.ENDING || phase == GamePhase.FINISHED) {
            sendMessage(context, player, "votes.messages.not_available");
            return true;
        }

        if (args.length == 0) {
            return openMenu(player, state, MENU_THEMES);
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("menu")) {
            return openMenu(player, state, MENU_THEMES);
        }

        if (action.equals("vote")) {
            if (args.length < 3) {
                sendMessage(context, player, "votes.messages.invalid");
                return true;
            }

            String theme = args[2].toLowerCase(Locale.ROOT);

            if (!isThemeValid(theme)) {
                sendMessage(context, player, "votes.messages.invalid");
                return true;
            }
            if (!hasThemePermission(player, theme)) {
                String message = moduleConfig.getStringFrom("language.yml", "votes.messages.no_permission");
                if (message != null) {
                    message = message.replace("{theme}", getThemeLabel(theme));
                    context.getMessagesAPI().sendRaw(player, message);
                }
                return true;
            }

            VoteState voteState = state.getVoteState();
            if (voteState == null) {
                return true;
            }

            voteState.castVote(player.getUniqueId(), theme);

            String themeLabel = getThemeLabel(theme);
            String message = moduleConfig.getStringFrom("language.yml", "votes.messages.broadcast");
            if (message != null && !message.isBlank()) {
                int voteCount = voteState.getVotes(theme);
                message = message.replace("{player}", player.getName())
                        .replace("{theme}", themeLabel)
                        .replace("{votes}", String.valueOf(voteCount));
                broadcastMessage(context, message);
            }
            return true;
        }

        return openMenu(player, state, MENU_THEMES);
    }

    public boolean handleVoteCommandWithoutContext(Player player, String[] args) {
        if (player == null) {
            return false;
        }

        String[] safeArgs = args != null ? args : new String[0];
        if (safeArgs.length == 0) {
            return openMenuWaiting(player);
        }

        String action = safeArgs[0].toLowerCase(Locale.ROOT);
        if (action.equals("menu")) {
            return openMenuWaiting(player);
        }

        if (action.equals("vote")) {
            if (safeArgs.length < 3) {
                return true;
            }

            String theme = safeArgs[2].toLowerCase(Locale.ROOT);
            if (!isThemeValid(theme)) {
                return true;
            }
            if (!hasThemePermission(player, theme)) {
                return true;
            }

            waitingVoteState.castVote(player.getUniqueId(), theme);
            broadcastWaitingVote(player, theme, waitingVoteState);
            return openMenuWaiting(player);
        }

        return false;
    }

    public void applyVotes(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                           ArenaState state) {
        if (context == null || state == null) {
            return;
        }

        VoteState voteState = state.getVoteState();
        if (voteState == null) {
            return;
        }

        String theme = voteState.resolveWinner();
        if ("random".equalsIgnoreCase(theme)) {
            List<String> themes = moduleConfig.getStringList("themes");
            if (themes != null && !themes.isEmpty()) {
                theme = themes.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(themes.size()));
            } else {
                theme = "House";
            }
        }
        state.setSelectedTheme(theme);
    }

    public void broadcastVoteResults(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                     ArenaState state) {
        if (context == null || state == null) {
            return;
        }
        VoteState voteState = state.getVoteState();
        if (voteState == null) {
            return;
        }

        String theme = state.getSelectedTheme();
        String themeLabel = getThemeLabel(theme);

        String source = voteState.hasVotes() ?
                moduleConfig.getStringFrom("language.yml", "votes.messages.selected.sources.popular") :
                moduleConfig.getStringFrom("language.yml", "votes.messages.selected.sources.default");

        String message = moduleConfig.getStringFrom("language.yml", "votes.messages.selected.theme");
        if (message != null && !message.isBlank()) {
            message = message.replace("{theme}", themeLabel)
                    .replace("{source}", source);
            broadcastMessage(context, message);
        }
    }

    private void broadcastWaitingVote(Player player, String theme, VoteState voteState) {
        if (player == null || theme == null) {
            return;
        }

        String message = moduleConfig.getStringFrom("language.yml", "votes.messages.broadcast");
        if (message == null || message.isBlank()) {
            return;
        }

        int voteCount = voteState != null ? voteState.getVotes(theme) : 0;
        message = message.replace("{player}", player.getName())
                .replace("{theme}", getThemeLabel(theme))
                .replace("{votes}", String.valueOf(voteCount));

        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = getGameContext(player);
        if (context == null) {
            broadcastToWaitingArena(player, message);
            return;
        }

        broadcastMessage(context, message);
    }

    private void broadcastToWaitingArena(Player sender, String message) {
        if (sender == null || message == null || message.isBlank()) {
            return;
        }
        @SuppressWarnings("unchecked")
        PlayerUtil<Player> playerUtil = (PlayerUtil<Player>) ModuleAPI.getPlayerUtil();
        if (playerUtil == null) {
            return;
        }
        Integer senderArenaId = playerUtil.getPlayerArena(sender);
        if (senderArenaId == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        MessageAPI<Player> messagesAPI = (MessageAPI<Player>) ModuleAPI.getMessagesAPI();
        for (Player online : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (online == null || !online.isOnline()) {
                continue;
            }
            Integer onlineArenaId = playerUtil.getPlayerArena(online);
            if (!senderArenaId.equals(onlineArenaId)) {
                continue;
            }
            if (messagesAPI != null) {
                messagesAPI.sendRaw(online, message);
            } else {
                online.sendMessage(message);
            }
        }
    }

    private GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> getGameContext(Player player) {
        if (game == null || player == null) {
            return null;
        }
        return game.getContext(player);
    }

    private boolean openMenu(Player player, ArenaState state, String menuId) {
        VoteState voteState = state != null ? state.getVoteState() : null;
        return openMenu(player, voteState, menuId);
    }

    private boolean openMenuWaiting(Player player) {
        return openMenu(player, waitingVoteState, MENU_THEMES);
    }

    private boolean openMenu(Player player, VoteState voteState, String menuId) {
        if (menuAPI == null || player == null) {
            return false;
        }

        MenuDefinition<Material> menu = menuRepository.getMenu(menuId);
        if (menu == null) {
            return false;
        }

        return menuAPI.openMenu(player, menu, buildPlaceholders(player, voteState));
    }

    private Map<String, String> buildPlaceholders(Player player, VoteState voteState) {
        Map<String, String> placeholders = new HashMap<>();
        for (String themeId : getValidThemeIds()) {
            placeholders.put("{votes_theme_" + themeId + "}", String.valueOf(voteState != null
                    ? voteState.getVotes(themeId)
                    : 0));
        }
        placeholders.put("{selected_theme}", resolveWinningLabel(voteState));
        placeholders.put("{player_vote_theme}", resolvePlayerVoteLabel(player, voteState));
        return placeholders;
    }

    private String resolveWinningLabel(VoteState voteState) {
        String option = voteState != null ? voteState.resolveWinner() : null;
        return getThemeLabel(option != null ? option : waitingVoteState.resolveWinner());
    }

    private String resolvePlayerVoteLabel(Player player, VoteState voteState) {
        if (player == null || voteState == null) {
            return getThemeLabel(waitingVoteState.resolveWinner());
        }
        String option = voteState.getPlayerVote(player.getUniqueId());
        if (option == null) {
            option = waitingVoteState.resolveWinner();
        }
        return getThemeLabel(option);
    }

    private Set<String> getValidThemeIds() {
        Set<String> ids = moduleConfig.getStringList("themes").stream()
                .filter(t -> t != null && !t.isBlank())
                .map(BuildBattleVoteMenuRepository::toThemeId)
                .collect(Collectors.toSet());
        ids.add("random");
        return ids;
    }

    private boolean isThemeValid(String theme) {
        return theme != null && getValidThemeIds().contains(theme.toLowerCase(Locale.ROOT));
    }

    private boolean hasThemePermission(Player player, String theme) {
        if (player == null || theme == null) {
            return false;
        }
        String permission = VOTE_PERMISSION_BASE + "." + theme.toLowerCase(Locale.ROOT);
        return player.hasPermission(permission) || player.hasPermission(VOTE_PERMISSION_BASE + ".*");
    }

    private String getThemeLabel(String themeId) {
        if (themeId == null) {
            return "";
        }
        if ("random".equalsIgnoreCase(themeId)) {
            return "Random";
        }
        List<String> themes = moduleConfig.getStringList("themes");
        if (themes != null) {
            for (String theme : themes) {
                if (theme != null && BuildBattleVoteMenuRepository.toThemeId(theme).equals(themeId.toLowerCase(Locale.ROOT))) {
                    return theme;
                }
            }
        }
        return themeId;
    }

    private String normalizeOption(String raw, Set<String> validOptions, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return validOptions.contains(normalized) ? normalized : fallback;
    }

    private void sendMessage(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                            Player player, String messagePath) {
        if (context == null || player == null || messagePath == null) {
            return;
        }
        String message = moduleConfig.getStringFrom("language.yml", messagePath);
        if (message != null && !message.isBlank()) {
            context.getMessagesAPI().sendRaw(player, message);
        }
    }

    private void broadcastMessage(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                 String message) {
        if (context == null || message == null || message.isBlank()) {
            return;
        }
        for (Player player : context.getPlayers()) {
            if (player != null && player.isOnline()) {
                context.getMessagesAPI().sendRaw(player, message);
            }
        }
    }
}
