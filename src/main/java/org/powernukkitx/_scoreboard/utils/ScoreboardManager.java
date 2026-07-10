package org.powernukkitx._scoreboard.utils;

import org.powernukkitx.Player;
import org.powernukkitx.Server;
import org.powernukkitx.event.EventHandler;
import org.powernukkitx.event.Listener;
import org.powernukkitx.event.player.PlayerLoginEvent;
import org.powernukkitx.event.player.PlayerQuitEvent;
import org.powernukkitx.placeholderapi.PlaceholderAPI;
import org.powernukkitx.scoreboard.IScoreboardLine;
import org.powernukkitx._scoreboard.Scoreboard;
import org.powernukkitx.scoreboard.data.DisplaySlot;
import org.powernukkitx.utils.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringJoiner;

public class ScoreboardManager implements Listener {

    private final HashMap<Integer, List<String>> lineCache = new HashMap<>();
    private final HashMap<Player, QueuedScoreboard> scoreboards = new HashMap<>();

    private final String header;
    private final List<String> lines;

    public ScoreboardManager() {
        Config config = Scoreboard.get().getConfig();
        this.header = config.getString("header");
        this.lines = config.getStringList("lines");

        //This ensures that you don't have the same text multiple times. Minecraft does not like that!
        for (int i = 0; i < lines.size(); i++) {
            StringJoiner joiner = new StringJoiner("§");
            for (String part : String.valueOf(i).split("")) joiner.add(part);
            lines.set(i, "§" + joiner + "§r" + lines.get(i));
        }

        Server.getInstance().getScheduler().scheduleRepeatingTask(Scoreboard.get(), this::tick, 1);
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        QueuedScoreboard scoreboard = new QueuedScoreboard(getDisplayHeader(player));
        List<IScoreboardLine> lines = new ArrayList<>();
        List<String> texts = this.getDisplayLines(player);
        for (int i = 0; i < texts.size(); i++) {
            lines.add(scoreboard.buildLine(texts.get(i), i));
        }
        scoreboard.setLines(lines);
        scoreboard.addViewer(player, DisplaySlot.SIDEBAR);
        lineCache.put(player.getLoaderId(), new ArrayList<>(texts));
        scoreboards.put(player, scoreboard);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        int loaderId = player.getLoaderId();
        this.scoreboards.remove(player);
        this.lineCache.remove(loaderId);
    }

    protected String getDisplayHeader(Player player) {
        if (Server.getInstance().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            return PlaceholderAPI.get().processPlaceholders(player, header);
        } else return header;
    }

    protected List<String> getDisplayLines(Player player) {
        if (Server.getInstance().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            return lines.stream().map(s -> PlaceholderAPI.get().processPlaceholders(player, s)).toList();
        } else return lines;
    }

    private void tick() {
        for (Player player : scoreboards.keySet()) {
            QueuedScoreboard scoreboard = scoreboards.get(player);
            String header = getDisplayHeader(player);
            if (!scoreboard.getDisplayName().equals(header)) {
                scoreboard.setDisplayName(header);
            } else {
                List<String> sentLines = this.lineCache.get(player.getLoaderId());
                List<String> curLines = getDisplayLines(player);
                for (int i = sentLines.size() - 1; i >= 0; i--) {
                    String curLine = curLines.get(i);
                    if (!sentLines.get(i).equals(curLine)) {
                        sentLines.set(i, curLine);
                        scoreboard.update(i, curLine);
                    }
                }
            }
        }
    }
}
