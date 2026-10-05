package in.potenfyr.cdn;

import com.github.retrooper.packetevents.PacketEvents;
import in.potenfyr.cdn.api.CustomDamageNumbersApi;
import in.potenfyr.cdn.api.ApiProvider;
import in.potenfyr.cdn.api.impl.ApiServiceImpl;
import in.potenfyr.cdn.command.CommandDispatcher;
import in.potenfyr.cdn.config.ConfigManager;
import in.potenfyr.cdn.damage.DamageListener;
import in.potenfyr.cdn.damage.DamageService;
import in.potenfyr.cdn.damage.LifecycleListener;
import in.potenfyr.cdn.damage.MergeRegistry;
import in.potenfyr.cdn.damage.StyleResolver;
import in.potenfyr.cdn.integration.LuckPermsHook;
import in.potenfyr.cdn.integration.PlaceholderApiHook;
import in.potenfyr.cdn.metrics.MetricsRegistrar;
import in.potenfyr.cdn.packet.AnimationTask;
import in.potenfyr.cdn.packet.EffectService;
import in.potenfyr.cdn.packet.MetadataDebugListener;
import in.potenfyr.cdn.packet.RenderBackend;
import in.potenfyr.cdn.platform.BukkitSchedulerAdapter;
import in.potenfyr.cdn.platform.Ecosystem;
import in.potenfyr.cdn.platform.FoliaSchedulerAdapter;
import in.potenfyr.cdn.platform.MinecraftVersion;
import in.potenfyr.cdn.platform.SchedulerAdapter;
import in.potenfyr.cdn.prefs.PlayerPreferences;
import in.potenfyr.cdn.util.DebugLogger;
import in.potenfyr.cdn.util.Messages;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * The shared plugin lifecycle.
 *
 * <p>Each platform jar supplies a concrete subclass that only chooses a
 * {@link RenderBackend} and declares which servers it serves; everything else —
 * services, listeners, commands, the animation task — is wired here so the two jars
 * cannot drift apart.</p>
 */
public abstract class CustomDamageNumbersPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private Messages messages;
    private DebugLogger debug;

    private SchedulerAdapter scheduler;
    private PlayerPreferences preferences;
    private MergeRegistry mergeRegistry;
    private EffectService effects;
    private DamageService damageService;
    private AnimationTask animationTask;
    private SchedulerAdapter.SchedulerHandle animationHandle;
    private MetadataDebugListener debugListener;
    private MetricsRegistrar metrics;
    private CommandDispatcher dispatcher;

    private Ecosystem ecosystem = Ecosystem.UNKNOWN;
    private MinecraftVersion serverVersion = MinecraftVersion.UNKNOWN;
    private StyleResolver styleResolver = StyleResolver.NONE;
    private CustomDamageNumbersApi api;

    /** Creates the renderer for this jar. */
    protected abstract RenderBackend createBackend();

    /** Whether this jar can serve the running server version. */
    protected abstract boolean isServerSupported(MinecraftVersion version);

    /** What to tell the operator when this jar cannot serve the server. */
    protected abstract String unsupportedServerHint(MinecraftVersion version);

    @Override
    public final void onEnable() {

        saveDefaultConfig();

        messages = new Messages(this);
        debug = new DebugLogger(getLogger(), "CDN", () -> configManager != null && configManager.isDebug());

        serverVersion = MinecraftVersion.parse(getServer().getBukkitVersion());
        ecosystem = Ecosystem.detect(getServer().getName(), getServer().getVersion(),
                CustomDamageNumbersPlugin::classPresent);

        getLogger().info("Detected " + ecosystem.displayName() + " on Minecraft " + serverVersion.raw() + ".");

        if (!isServerSupported(serverVersion)) {

            getLogger().severe("This jar does not support Minecraft " + serverVersion.raw() + ".");
            getLogger().severe(unsupportedServerHint(serverVersion));

            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (getServer().getPluginManager().getPlugin("packetevents") == null) {

            getLogger().severe("PacketEvents is not installed; CustomDamageNumbers cannot render anything.");
            getLogger().severe("Install PacketEvents " + "2.x" + " and restart the server.");

            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            // PacketEvents owns its own lifecycle: we only touch the API it exposes.
            PacketEvents.getAPI().getPlayerManager();
        } catch (Throwable notReady) {

            getLogger().severe("PacketEvents is installed but not ready: " + notReady);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        configManager = new ConfigManager(this);

        Boolean foliaOverride = configManager.getFoliaModeOverride();
        boolean folia = foliaOverride != null ? foliaOverride : ecosystem.isRegionThreaded();

        SchedulerAdapter bukkitScheduler = new BukkitSchedulerAdapter(this);
        scheduler = folia ? FoliaSchedulerAdapter.create(this, bukkitScheduler) : bukkitScheduler;

        preferences = PlayerPreferences.inDataFolder(
                getDataFolder(), getLogger(), scheduler, configManager.isDefaultViewEnabled());
        preferences.load();

        mergeRegistry = new MergeRegistry();
        effects = new EffectService(configManager);

        // Player style override first, then the LuckPerms/permission-derived profile.
        LuckPermsHook luckPerms = new LuckPermsHook(configManager.isLuckPermsEnabled());
        styleResolver = playerId -> {

            String override = preferences.styleFor(playerId);

            return override != null ? override : luckPerms.profileFor(playerId);
        };

        RenderBackend backend = createBackend();

        damageService = new DamageService(
                configManager, debug, backend, effects, preferences, mergeRegistry, styleResolver);

        getServer().getPluginManager().registerEvents(new DamageListener(damageService, configManager), this);
        getServer().getPluginManager().registerEvents(new LifecycleListener(damageService), this);

        animationTask = new AnimationTask(damageService, debug);

        dispatcher = new CommandDispatcher(this);

        PluginCommand command = getCommand("cdn");

        if (command != null) {
            command.setExecutor(dispatcher);
            command.setTabCompleter(dispatcher);
        } else {
            getLogger().warning("Command 'cdn' is missing from plugin.yml; commands will not work.");
        }

        startAnimationTask();

        if (configManager.isPacketDebug()) {
            debugListener = new MetadataDebugListener(debug, configManager.getDebugLogLimit());
            PacketEvents.getAPI().getEventManager().registerListener(debugListener);
        }

        metrics = new MetricsRegistrar(this, debug);
        metrics.start(configManager.isMetricsEnabled(), configManager.getMetricsId());

        api = new ApiServiceImpl(this, configManager, damageService, preferences);
        ApiProvider.register(this, api);

        registerPlaceholderApi();

        getLogger().info("CustomDamageNumbers " + getDescription().getVersion()
                + " enabled using the " + backend.id() + " backend on the "
                + scheduler.describe() + ".");

        printBanner(backend);

        onEnabled();
    }

    /** Prints the console banner: what is running, where, and with which renderer. */
    private void printBanner(RenderBackend backend) {

        if (!configManager.isBannerEnabled()) {
            return;
        }

        List<in.potenfyr.cdn.util.ChatFrame.Row> rows = List.of(
                in.potenfyr.cdn.util.ChatFrame.Row.of("Renderer", backend.id()
                        + (backend.supportsScale() && backend.supportsOpacity()
                                ? " (scale + fade)" : " (position only)")),
                in.potenfyr.cdn.util.ChatFrame.Row.of("Server",
                        ecosystem.displayName() + " " + serverVersion.raw()),
                in.potenfyr.cdn.util.ChatFrame.Row.of("Scheduler", scheduler.describe()),
                in.potenfyr.cdn.util.ChatFrame.Row.of("Storage", "plugins/CustomDamageNumbers"),
                in.potenfyr.cdn.util.ChatFrame.Row.of("Support", "github.com/PotenFYR-Studios/CustomDamageNumbers"));

        getLogger().info(System.lineSeparator() + in.potenfyr.cdn.util.BannerRenderer.render(
                getDescription().getVersion(),
                "packet-level floating damage numbers",
                rows,
                "no entities spawned - all client-side"));
    }

    /**
     * Hook for platform jars to run code once the plugin is fully wired.
     *
     * <p>{@link #onEnable()} is final on purpose: both jars must go through the same
     * startup path so they cannot drift apart.</p>
     */
    protected void onEnabled() {
        // No-op by default.
    }

    @Override
    public final void onDisable() {

        // Logged first so a failure in any single teardown step is still attributable.
        getLogger().info("Disabling CustomDamageNumbers...");

        ApiProvider.unregister(this);
        api = null;

        if (animationHandle != null) {
            animationHandle.cancel();
            animationHandle = null;
        }

        // Despawn every live display so no viewer keeps a ghost entity.
        if (damageService != null) {
            try {
                damageService.clear();
            } catch (Throwable failure) {
                getLogger().log(java.util.logging.Level.WARNING, "Could not clear live displays.", failure);
            }
        }

        if (preferences != null) {
            try {
                preferences.write();
            } catch (Throwable failure) {
                getLogger().log(java.util.logging.Level.WARNING, "Could not save player preferences.", failure);
            }
        }

        if (debugListener != null) {

            try {
                PacketEvents.getAPI().getEventManager().unregisterListener(debugListener);
            } catch (Throwable ignored) {
                // PacketEvents may already be shutting down; nothing to do.
            }

            debugListener = null;
        }

        if (metrics != null) {
            metrics.shutdown();
            metrics = null;
        }

        // PacketEvents is shared with other plugins: we never terminate it.
        getLogger().info("CustomDamageNumbers disabled.");
    }

    /** Re-reads config and messages, and restarts the animation task with the new interval. */
    public void reloadEverything() {

        configManager.reload();
        messages.reload();

        if (preferences != null) {
            preferences.setDefaultEnabled(configManager.isDefaultViewEnabled());
        }

        restartAnimationTask();
    }

    private void startAnimationTask() {

        animationHandle = scheduler.runTimer(
                animationTask, 1L, configManager.getAnimationInterval());
    }

    /** Restarts the tick task so a changed {@code performance.animation-interval} applies. */
    public void restartAnimationTask() {

        if (animationHandle != null) {
            animationHandle.cancel();
        }

        startAnimationTask();
    }

    private void registerPlaceholderApi() {

        if (!configManager.isPlaceholderApiEnabled()) {
            return;
        }

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }

        try {

            PlaceholderApiHook hook = new PlaceholderApiHook(this);

            if (hook.register()) {
                debug.debug("Registered PlaceholderAPI expansion cdn.");
            }

        } catch (Throwable failure) {
            getLogger().warning("Could not register the PlaceholderAPI expansion: " + failure);
        }
    }

    private static boolean classPresent(String className) {

        try {
            Class.forName(className);
            return true;
        } catch (Throwable absent) {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Accessors for services and commands
    // ------------------------------------------------------------------

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public Messages getMessages() {
        return messages;
    }

    public DebugLogger getDebug() {
        return debug;
    }

    public SchedulerAdapter getScheduler() {
        return scheduler;
    }

    public PlayerPreferences getPreferences() {
        return preferences;
    }

    public MergeRegistry getMergeRegistry() {
        return mergeRegistry;
    }

    public EffectService getEffects() {
        return effects;
    }

    public DamageService getDamageService() {
        return damageService;
    }

    public AnimationTask getAnimationTask() {
        return animationTask;
    }

    public CommandDispatcher getDispatcher() {
        return dispatcher;
    }

    public Ecosystem getEcosystem() {
        return ecosystem;
    }

    public MinecraftVersion getServerVersion() {
        return serverVersion;
    }

    public StyleResolver getStyleResolver() {
        return styleResolver;
    }

    /** The active renderer. */
    public RenderBackend getBackend() {
        return damageService == null ? null : damageService.backend();
    }
}
