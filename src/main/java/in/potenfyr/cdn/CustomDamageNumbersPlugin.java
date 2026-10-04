package in.potenfyr.cdn;

import com.github.retrooper.packetevents.PacketEvents;
import in.potenfyr.cdn.command.CDNCommand;
import in.potenfyr.cdn.damage.DamageListener;
import in.potenfyr.cdn.packet.AnimationTask;
import in.potenfyr.cdn.util.ConfigManager;
import in.potenfyr.cdn.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class CustomDamageNumbersPlugin extends JavaPlugin {

    private static CustomDamageNumbersPlugin instance;

    private ConfigManager configManager;

    private Messages messages;

    private BukkitTask animationTask;

    @Override
    public void onEnable() {

        instance = this;

        saveDefaultConfig();

        configManager = new ConfigManager(this);

        messages = new Messages(this);

        // PacketEvents is a hard dependency: without it the plugin cannot function.
        if (Bukkit.getPluginManager().getPlugin("packetevents") == null) {
            getLogger().severe("PacketEvents is not installed. Disabling CustomDamageNumbers.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        PacketEvents.getAPI().init();

        // Optional packet-level metadata dump for diagnosing display issues.
        if (configManager.isPacketDebug()) {
            PacketEvents.getAPI().getEventManager().registerListener(
                    new in.potenfyr.cdn.damage.MetadataDebugListener());
        }


        Bukkit.getPluginManager().registerEvents(
                new DamageListener(),
                this
        );

        CDNCommand cdnCommand = new CDNCommand(messages);
        getCommand("cdn").setExecutor(cdnCommand);
        getCommand("cdn").setTabCompleter(cdnCommand);

        animationTask = new AnimationTask().runTaskTimer(
                this,
                1L,
                configManager.getAnimationInterval()
        );

        getLogger().info("CustomDamageNumbers enabled.");
    }

    @Override
    public void onDisable() {

        if (animationTask != null && !animationTask.isCancelled()) {
            animationTask.cancel();
        }

        // Despawn any live client-side displays so viewers never keep ghost entities.
        in.potenfyr.cdn.damage.DamageRenderer.clear();

        if (Bukkit.getPluginManager().getPlugin("packetevents") != null) {
            PacketEvents.getAPI().terminate();
        }
    }

    /**
     * Restarts the animation task; called after /cdn reload when the configured
     * animation interval has changed.
     */
    public void restartAnimationTask() {

        if (animationTask != null && !animationTask.isCancelled()) {
            animationTask.cancel();
        }

        animationTask = new AnimationTask().runTaskTimer(
                this,
                1L,
                configManager.getAnimationInterval()
        );
    }

    public static CustomDamageNumbersPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public Messages getMessages() {
        return messages;
    }
}