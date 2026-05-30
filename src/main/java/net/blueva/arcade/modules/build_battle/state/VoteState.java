package net.blueva.arcade.modules.build_battle.state;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;




public class VoteState {

    private final Map<String, Integer> themeVotes = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerVotes = new ConcurrentHashMap<>();
    private final String defaultTheme;

    public VoteState(String defaultTheme) {
        this.defaultTheme = defaultTheme != null ? defaultTheme : "random";
    }

    public void castVote(UUID playerId, String theme) {
        if (playerId == null || theme == null) {
            return;
        }

        String previous = playerVotes.put(playerId, theme);

        if (previous != null && previous.equals(theme)) {
            return;
        }

        if (previous != null) {
            themeVotes.computeIfPresent(previous, (key, value) -> Math.max(0, value - 1));
        }
        themeVotes.merge(theme, 1, Integer::sum);
    }

    public int getVotes(String theme) {
        if (theme == null) {
            return 0;
        }
        return themeVotes.getOrDefault(theme, 0);
    }

    public String getPlayerVote(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return playerVotes.get(playerId);
    }

    public void clearPlayerVotes(UUID playerId) {
        if (playerId == null) {
            return;
        }
        String theme = playerVotes.remove(playerId);
        if (theme == null) {
            return;
        }
        themeVotes.computeIfPresent(theme, (key, value) -> {
            int nextValue = value - 1;
            return nextValue > 0 ? nextValue : null;
        });
    }

    public void clearAll() {
        playerVotes.clear();
        themeVotes.clear();
    }

    public String resolveWinner() {
        if (themeVotes.isEmpty()) {
            return defaultTheme;
        }

        int maxVotes = -1;
        String winningTheme = null;
        boolean tie = false;
        for (Map.Entry<String, Integer> entry : themeVotes.entrySet()) {
            int count = entry.getValue();
            if (count > maxVotes) {
                maxVotes = count;
                winningTheme = entry.getKey();
                tie = false;
            } else if (count == maxVotes) {
                tie = true;
            }
        }

        if (winningTheme == null) {
            return defaultTheme;
        }

        if (tie) {
            return defaultTheme;
        }

        return winningTheme;
    }

    public boolean hasVotes() {
        return !themeVotes.isEmpty() && themeVotes.values().stream().anyMatch(count -> count > 0);
    }
}
