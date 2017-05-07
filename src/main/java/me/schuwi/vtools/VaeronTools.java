package me.schuwi.vtools;

import com.boydti.fawe.FaweAPI;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Schuwi
 * @version 1.0
 */
public class VaeronTools extends JavaPlugin {

    @Override
    public void onEnable() {
        FaweAPI.registerCommands(new SweepCommand());
    }
}
