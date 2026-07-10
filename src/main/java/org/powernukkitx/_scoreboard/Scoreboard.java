package org.powernukkitx._scoreboard;

import org.powernukkitx.plugin.PluginBase;
import org.powernukkitx._scoreboard.utils.ScoreboardManager;

public class Scoreboard extends PluginBase {

    private static Scoreboard INSTANCE;

    public static Scoreboard get() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        this.saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new ScoreboardManager(), this);
    }
}
