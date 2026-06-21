package in.potenfyr.cdn;

import com.github.retrooper.packetevents.PacketEvents;
import in.potenfyr.cdn.command.CDNCommand;
import in.potenfyr.cdn.damage.DamageListener;
import in.potenfyr.cdn.packet.AnimationTask;
import in.potenfyr.cdn.util.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class CustomDamageNumbersPlugin extends JavaPlugin {

    private static CustomDamageNumbersPlugin instance;

    private ConfigManager configManager;

    @Override
    public void onEnable() {

        instance = this;

        saveDefaultConfig();

        configManager = new ConfigManager(this);

        PacketEvents.getAPI().init();


        Bukkit.getPluginManager().registerEvents(
                new DamageListener(),
                this
        );

        getCommand("cdn").setExecutor(
                new CDNCommand()
        );

        new AnimationTask().runTaskTimer(
                this,
                1L,
                configManager.getAnimationInterval()
        );

        getLogger().info("CustomDamageNumbers enabled.");
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
    }

    public static CustomDamageNumbersPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}