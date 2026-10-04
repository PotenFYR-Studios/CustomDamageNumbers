package in.potenfyr.cdn.damage;

import in.potenfyr.cdn.config.MergeSettings;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Tracks the most recent number per victim/attacker pair so rapid hits grow one
 * display instead of stacking several.
 *
 * <p>Bounded by construction: an entry exists only while its display is alive, and
 * {@link #prune(long)} drops anything older than the merge window. There is no
 * unbounded per-entity map anywhere in this class.</p>
 */
public final class MergeRegistry {

    /** Used in the key when merging is configured to ignore the attacker. */
    private static final UUID ANY_ATTACKER = new UUID(0L, 0L);

    private final Map<Key, Entry> entries = new HashMap<>();
    private final LongSupplier clock;

    public MergeRegistry() {
        this(System::currentTimeMillis);
    }

    /** Test constructor with an injectable clock. */
    public MergeRegistry(LongSupplier clock) {
        this.clock = clock;
    }

    /**
     * Finds a display the incoming hit may merge into.
     *
     * @return the mergeable display, or {@code null}
     */
    public FloatingDamage find(UUID victimId, UUID attackerId, MergeSettings settings) {

        if (!settings.enabled() || victimId == null) {
            return null;
        }

        Entry entry = entries.get(key(victimId, attackerId, settings));

        if (entry == null) {
            return null;
        }

        if (clock.getAsLong() - entry.timestamp > settings.windowMs()) {
            return null;
        }

        return entry.display;
    }

    /** Records (or refreshes) the mergeable display for a victim/attacker pair. */
    public void record(FloatingDamage display, UUID victimId, UUID attackerId, MergeSettings settings) {

        if (!settings.enabled() || victimId == null || display == null) {
            return;
        }

        entries.put(key(victimId, attackerId, settings), new Entry(display, clock.getAsLong()));
    }

    /** Drops the entry pointing at a display that has expired or been cleared. */
    public void forget(FloatingDamage display) {

        Iterator<Entry> iterator = entries.values().iterator();

        while (iterator.hasNext()) {

            if (iterator.next().display == display) {
                iterator.remove();
            }
        }
    }

    /** Drops entries older than the merge window; called from the animation tick. */
    public int prune(long windowMs) {

        long now = clock.getAsLong();
        int removed = 0;

        Iterator<Map.Entry<Key, Entry>> iterator = entries.entrySet().iterator();

        while (iterator.hasNext()) {

            if (now - iterator.next().getValue().timestamp > windowMs) {
                iterator.remove();
                removed++;
            }
        }

        return removed;
    }

    public int size() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
    }

    private Key key(UUID victimId, UUID attackerId, MergeSettings settings) {

        UUID attacker = settings.sameAttackerOnly() && attackerId != null ? attackerId : ANY_ATTACKER;

        return new Key(victimId, attacker);
    }

    private record Key(UUID victim, UUID attacker) {
    }

    private static final class Entry {

        private final FloatingDamage display;
        private final long timestamp;

        private Entry(FloatingDamage display, long timestamp) {
            this.display = display;
            this.timestamp = timestamp;
        }
    }
}
