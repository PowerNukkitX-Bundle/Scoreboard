package org.powernukkitx.scoreboard;

import cn.nukkit.plugin.PluginBase;
import org.powernukkitx.scoreboard.utils.ScoreboardManager;

public class Scoreboard extends PluginBase {

    private static Scoreboard INSTANCE;

    @Override
    public void onEnable() {
        INSTANCE = this;
        this.saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new ScoreboardManager(), this);
    }

    public static Scoreboard get() {
        return INSTANCE;
    }
}
