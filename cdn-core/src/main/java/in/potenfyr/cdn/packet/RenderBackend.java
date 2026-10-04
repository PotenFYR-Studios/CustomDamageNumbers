package in.potenfyr.cdn.packet;

import in.potenfyr.cdn.damage.AnimationFrame;
import in.potenfyr.cdn.damage.FloatingDamage;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Renders damage numbers for a server generation.
 *
 * <p>Two implementations exist and each ships in its own jar: a TextDisplay backend
 * for 1.20.2+ and a packet-only armor-stand backend for 1.16.x-1.20.1. Everything
 * upstream of this interface is identical on both, which is what keeps the version
 * split to a single seam.</p>
 */
public interface RenderBackend {

    /** Short identifier used by {@code /cdn backend} and the debug log. */
    String id();

    /** Whether this backend can animate per-entity scale. */
    boolean supportsScale();

    /** Whether this backend can animate per-entity opacity. */
    boolean supportsOpacity();

    /** Sends the spawn packet(s) for a display to each viewer. */
    void spawn(FloatingDamage display, Collection<Player> viewers);

    /** Moves an existing display and applies any scale/opacity change for this frame. */
    void move(FloatingDamage display, AnimationFrame frame, Collection<Player> viewers);

    /** Re-sends only the text, used when hit merging grows a number. */
    void retext(FloatingDamage display, Collection<Player> viewers);

    /** Removes a display from each viewer's client. */
    void destroy(FloatingDamage display, Collection<Player> viewers);
}
