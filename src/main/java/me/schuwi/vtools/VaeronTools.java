package me.schuwi.vtools;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Schuwi
 * @version 1.0
 */
public class VaeronTools extends JavaPlugin {

    @Override
    public void onEnable() {
        getCommand("vaeSweep").setExecutor(new SweepCommand());
    }
}
