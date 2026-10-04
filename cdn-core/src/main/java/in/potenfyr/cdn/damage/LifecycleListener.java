package in.potenfyr.cdn.damage;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldUnloadEvent;

/**
 * Keeps live displays consistent with the world state: a disconnecting player stops
 * being a viewer, and displays in an unloaded world are destroyed rather than left
 * dangling (spec section 10).
 */
public final class LifecycleListener implements Listener {

    private final DamageService service;

    public LifecycleListener(DamageService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        service.onQuit(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent event) {

        if (event.getWorld() == null) {
            return;
        }

        service.clearWorld(event.getWorld().getUID());
    }
}
