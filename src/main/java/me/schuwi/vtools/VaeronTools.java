package me.schuwi.vtools;

import com.boydti.fawe.FaweAPI;
import me.schuwi.vtools.sweep.SweepCommand;
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
