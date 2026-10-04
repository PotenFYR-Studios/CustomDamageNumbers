# CustomDamageNumbers 0.3.0 — Multi-Version Rendering, Animation & Player Control

Date: 2026-10-04
Status: approved (design accepted in session; "complete everything")
Repo: PotenFYR-Studios/CustomDamageNumbers

## 1. Problem

0.2.0 ships a working packet-based damage-number core, but:

1. The animation does not follow its configuration. `AnimationTask` hardcodes
   `y = t < 10 ? 0.08 : -0.06`; `animation.vertical-speed`, `scale-animation`,
   `start-scale`, `end-scale`, `fade-out`, `bounce`, `rotation` and
   `animation.random-offset` are never read from config.
2. Damage numbers are anchored to a snapshot `Location`, so as the victim moves
   the number stays behind instead of staying near the damaged entity.
3. There is no way for a player to switch damage indicators off from their side.
4. The advertised feature set is partly fiction: hit merging, particles, crit
   sound, MiniMessage styles, `styles.<type>.shadow`, and LuckPerms style
   routing (`LuckPermsHook` is never called) do not exist.
5. Coverage gap: only `EntityDamageByEntityEvent` is handled, so fall, fire-tick,
   poison, drowning and other environmental damage render nothing;
   `DamageType.FALL` and `styles.healing` are dead.
6. A real memory leak: `MetadataDebugListener` accumulates every TextDisplay
   entity id in an unbounded `HashSet`.
7. Hard version ceiling: `paper-api 26.3`, `release 25`, `api-version: '26.3'`.
   The jar cannot load below 26.3 (nor on Spigot).
8. No build/test tooling exists in the repo (no `mvnw`, no CI, no tests).

## 2. Goals

- Animation driven entirely by config, with a pure, unit-tested curve.
- Numbers anchored to (and following) the damaged entity.
- Player-side per-player on/off toggle, persisted.
- Every advertised feature actually implemented (or removed from the docs).
- Audit clean: no leaks, bounded state, correct lifecycle, O(1) hot paths.
- Multi-version: one codebase, two jars — modern 1.20.2–26.x (TextDisplay) and
  legacy 1.16.x–1.20.1 (packet-only armor-stand nametags).
- Works on any plugin-capable server: Spigot, Paper, Purpur, Pufferfish, Folia.
- Everything built and tested in Docker only.

## 3. Decisions taken (from the approval round)

| Question | Decision |
|---|---|
| 1.16–1.19.3 rendering | Ship a **separate legacy jar** (no armor-stand fallback inside the modern jar) |
| Java baseline | **Java 17** bytecode (`release 17`) |
| Player toggle | **Global per-player on/off**, persisted, via `/cdn toggle` |
| Advertised-but-missing features | **Implement all of them** |
| Verification | **Build + unit tests + real server smoke tests, via Docker only** |

Additional decisions made during design:

- **Compile against Spigot API 1.16.5**, not Paper API. A jar that cannot
  reference Paper-only classes cannot break on Spigot/Purpur/Pufferfish. Any
  Paper-only capability (`getDamageSource()`, `isCritical()`) is reached by
  reflection with a documented fallback.
- **Text payload crosses to PacketEvents as a String**, not an Adventure
  `Component`. PacketEvents 2.14 exposes `EntityDataTypes.COMPONENT` as
  `EntityDataType<String>` (JSON), so Adventure never leaves our classloader and
  there is no cross-plugin classloader hazard on Spigot. Adventure
  (`text-minimessage`, `text-serializer-json`, `text-serializer-legacy`) is
  shaded **and relocated** into our jar, used only internally to turn config
  text into a JSON string / legacy string.
- **Modern jar covers 1.20.2+**, not 1.19.4+. The TextDisplay metadata index
  block (Display 8–22, TextDisplay 23–27) is validated for 1.20.2 and later;
  1.19.4–1.20.1 uses a different field layout, and shipping an unverified index
  table is worse than routing those versions to the legacy jar. Documented as a
  compatibility boundary, not a silent failure: the legacy jar serves
  1.16.x–1.20.1, the modern jar serves 1.20.2–26.x.
- **Spigot parity is enforced at compile time** (spigot-api), **behaviour is
  tested on Paper** containers (Spigot 1.16.5 requires BuildTools; Paper is a
  superset of the Spigot API contract we use).

## 4. Architecture

Multi-module Maven (parent `packaging=pom`):

```
pom.xml                     parent: versions, repos, shade config
cdn-core/                   all version-independent logic (shaded into both jars)
cdn-modern/                 TextDisplay backend  -> CustomDamageNumbers-<v>.jar      (1.20.2–26.x)
cdn-legacy/                 ArmorStand backend  -> CustomDamageNumbers-Legacy-<v>.jar (1.16.x–1.20.1)
docker/                     compose harness + packet-asserting probe (E2E only)
```

`cdn-core` package map:

```
in.potenfyr.cdn
├── CustomDamageNumbersPlugin        abstract JavaPlugin: lifecycle, services, wiring
├── platform/
│   ├── Ecosystem                    PAPER / SPIGOT / PURPUR / FOLIA / unknown detection
│   ├── MinecraftVersion             parse + compare "1.16.5", "26.3"
│   ├── SchedulerAdapter             interface (run, runAsync, runAtEntity, cancelAll)
│   ├── BukkitSchedulerAdapter
│   └── FoliaSchedulerAdapter        reflection (RegionScheduler / EntityScheduler)
├── config/
│   ├── ConfigManager                every key read, validated, clamped, defaults file
│   ├── AnimationSettings            immutable record built from config
│   └── StyleSettings                immutable record per damage type
├── text/
│   ├── TextRenderer                 MiniMessage/legacy -> JSON string + legacy string
│   └── LegacyToMiniMessage          &a..&f, &l/&o/&n/&m/&k -> MiniMessage tags
├── damage/
│   ├── DamageType                   NORMAL, CRITICAL, FIRE, MAGIC, POISON, EXPLOSION, FALL, HEALING
│   ├── DamageClassifier             DamageCause + reflection on getDamageSource()/isCritical()
│   ├── DamageListener               EntityDamageEvent + EntityDamageByEntityEvent
│   ├── FloatingDamage               model: entityId, uuid, victimId, anchor, damage, viewers...
│   ├── DamageService                spawn pipeline: filters, caps, viewers, prefs, merge, backend
│   ├── MergeRegistry                victim+attacker keyed merging
│   ├── AnimationCurve               PURE math: tick -> AnimationFrame
│   └── AnimationFrame               record(dx, dy, dz, scale, opacityByte)
├── packet/
│   ├── RenderBackend                interface: spawn / update / retext / destroy
│   ├── PacketUtil                   send helpers (spawn, teleport, metadata, destroy, particle, sound)
│   ├── AnimationTask                tick loop, anchoring, culling, expiry
│   └── MetadataDebugListener        opt-in, BOUNDED id tracking, unregistered on disable
├── prefs/PlayerPreferences          players.yml store, async save, per-player enable flag
├── integration/
│   ├── LuckPermsHook                style profile per group (actually called now)
│   └── PlaceholderApiHook           %cdn_shown% etc. (soft-depend, guarded)
├── metrics/MetricsRegistrar         bStats, guarded, config-gated
├── command/                         CommandDispatcher + SubCommand + impl/*
└── util/                            Messages, DebugLogger, ColorUtil
```

Backends:

- `TextDisplayBackend` (cdn-modern): `TEXT_DISPLAY` spawn; metadata
  `10=teleport-duration` (smooth interpolation), `12=scale(Vector3f)`,
  `15=billboard CENTER`, `23=text(String)`, `24=line width`,
  `25=background (transparent)`, `26=text opacity` (fade), `27=style flags`
  (shadow from config).
- `ArmorStandBackend` (cdn-legacy): packet-only `ARMOR_STAND` spawn with
  `0x20` invisible entity flag, `2=custom name` (String), `3=custom name visible`.
  Only universally stable indices are touched across 1.16–1.20.1. No opacity or
  scale exists pre-1.20.2, so fade/scale degrade to position-only there and are
  documented as such.

Both backends are chosen by the jar, and each refuses to start on a server it
does not serve, naming the jar to install instead.

## 5. Animation model

`AnimationSettings(durationTicks, riseTicks, verticalSpeed, horizontalRandomness,
scaleAnimation, startScale, endScale, fadeOut, fadeStart, bounce, bounceStrength,
rotation, rotationSpeed, randomOffset, followEntity, anchorHeight, followSmoothing,
criticalScaleMultiplier)`

`AnimationCurve.frame(tick, settings, angle)`:

- vertical: `peak = verticalSpeed * riseTicks`; rising phase
  `y = peak * easeOutCubic(tick/riseTicks)`; descending phase
  `y = peak * (1 - easeInCubic(d/(duration-riseTicks)))`; `bounce` adds a damped
  sine on top; result never exceeds `peak`.
- horizontal: deterministic per-display angle (golden angle from entity id) plus
  `rotation ? rotationSpeed*tick`; magnitude `horizontalRandomness * (1-p)`.
- scale: `scaleAnimation ? lerp(startScale, endScale, easeInOut(p)) : 1`, with the
  critical multiplier folded in at spawn.
- opacity: `fadeOut && p > fadeStart ? 255*(1-(p-fadeStart)/(1-fadeStart)) : 255`,
  emitted as a byte.

The curve is pure (no Bukkit types) so it is fully unit-testable.

## 6. Anchoring

`FloatingDamage` holds `victimId` + `worldId`, plus a last-known anchor. Each
tick the task resolves the live entity; if present and valid, the anchor becomes
`entity.getLocation() + (0, anchorHeight, 0)`, optionally smoothed toward the
previous anchor (`followSmoothing`), and the frame offset is applied on top. If
the entity is gone, the last anchor freezes. Config: `animation.follow-entity`,
`animation.anchor-height`, `animation.follow-smoothing`.

## 7. Player control

`PlayerPreferences` (file-backed `players.yml`, async save, main-thread reads):

- `isEnabled(uuid)` / `setEnabled(uuid, boolean)` / `defaultEnabled()`.
- `/cdn toggle [on|off]` self-service (`cdn.toggle`, default true),
  `/cdn toggle <player>` with `cdn.toggle.others`.
- Toggling off destroys that player's live displays immediately and the spawn
  path skips opted-out viewers.
- `permissions.require-view-permission` + `cdn.view` remain as the
  permission-driven alternative.

## 8. Commands

`/cdn` (+ aliases `/damage`, `/damagedisplay`) with a dispatcher, per-subcommand
permissions and tab completion:

`help`, `reload [config|messages|all]`, `test [type] [amount] [player]`,
`toggle [on|off] [player]`, `on`, `off`, `debug [on|off]`, `stats`,
`clear [player|all]`, `style <player|self> <style>`, `backend`, `version`.

All player-facing text comes from `messages.yml` (legacy `&` codes and
MiniMessage both supported).

## 9. Feature completion

- **Hit merging**: `MergeRegistry` keyed by victim+attacker; a hit inside
  `merge-system.merge-window-ms` grows the existing number (capped by
  `max-merged-damage`), resets its lifetime and re-sends text via `retext()`.
  `same-attacker-only` honoured; merging is disabled by config switch.
- **Particles**: `WrapperPlayServerParticle` per damage type, config-driven
  (`particles.*`), sent only to viewers of the display.
- **Crit sound**: `WrapperPlayServerSoundEffect` (config `critical-hits.sound*`).
- **MiniMessage + shadow**: styles parsed through MiniMessage; `styles.<type>.shadow`
  finally applied to the TextDisplay style flags.
- **LuckPerms**: a player's group selects a style profile; `cdn.style.*`
  permissions act as the fallback.

## 10. Audit fixes

- `MetadataDebugListener`: bounded set (LRU, max 256) **and** removes ids on
  `DESTROY_ENTITIES`; listener unregistered on disable.
- PacketEvents lifecycle: stop calling `init()`/`terminate()` on an API we do
  not own; only register/unregister our own listeners.
- `DamageService` keeps an O(1) per-player display count index instead of the
  O(n) scan in the spawn hot path.
- Bounded global display count, viewer pruning on quit/death/world-unload,
  correct task cancellation, no static mutable registry.
- `DamageType.HEALING` added so `styles.healing` is reachable.
- Environmental damage now renders (fall, fire-tick, poison, drowning, ...).

## 11. Compatibility matrix

| Jar | Server versions | Java | Backend |
|---|---|---|---|
| CustomDamageNumbers-0.3.0.jar | 1.20.2 – 26.x | 17+ | TextDisplay (opacity, scale) |
| CustomDamageNumbers-Legacy-0.3.0.jar | 1.16.x – 1.20.1 | 17+ | packet-only armor stand nametags |

Server software: Spigot, Paper, Purpur, Pufferfish, Folia (Folia scheduler path).
1.16.5 servers must run Java 17+ (chosen Java baseline).

## 12. Dependencies

| Dependency | Scope | Why |
|---|---|---|
| packetevents-spigot 2.14.0 | provided (plugin) | all packet work |
| spigot-api 1.16.5 | provided | compile against oldest supported API |
| adventure-text-minimessage / -serializer-json / -serializer-legacy / -api | shaded + relocated | config text (MiniMessage) without classloader coupling |
| bstats-bukkit | shaded + relocated | opt-out metrics, config-gated |
| placeholderapi | provided, optional | `%cdn_*%` placeholders |
| junit-jupiter 5.x | test | unit tests |

No command framework: the dispatcher is hand-rolled because it is
multi-version-safe and unit-testable without a server.

## 13. Testing (Docker only)

Unit (JUnit 5, no server): `AnimationCurve`, `AnimationSettings` validation,
`ConfigManager` parsing/clamping, `MergeRegistry`, `PlayerPreferences`,
`DamageClassifier`, `MinecraftVersion`, `CommandDispatcher`, `TextRenderer`,
`LegacyToMiniMessage`.

End-to-end (Docker): one container per server version, plugin installed, a
**mineflayer** bot joins and is opped, then:

1. asserts the plugin enabled and no error lines appear in the server log;
2. drives `/cdn test` and asserts the real packet stream: spawn of
   `text_display` (modern) or `armor_stand` (legacy) → metadata carrying our
   text → `entity_teleport` sequence whose Y rises then falls → `destroy_entities`;
3. asserts anchoring: the teleport stream tracks an entity that moves;
4. asserts `/cdn toggle off` suppresses spawn packets and `/cdn toggle on`
   restores them;
5. drives `help`, `stats`, `version`, `backend`, `clear` and asserts replies;
6. stops the server and asserts a clean disable with no lingering tasks.

Matrix: Paper 1.16.5 (legacy), Paper 1.20.1 (legacy), Paper 1.21.x (modern),
Paper 26.3 (modern) — with the limitation recorded that a 26.x-capable bot may
not exist, in which case 26.3 is validated for enable/commands/clean-disable
plus the modern backend path proven on 1.21.x.

## 14. Out of scope

- No client mod, no resource pack.
- No rewrite of the packet layer away from PacketEvents.
- No support below 1.16.x.
- `advanced.folia-mode` remains a config switch, but Folia is exercised only by
  the scheduler adapter's unit tests, not by a Folia container.

## 15. Risks

| Risk | Mitigation |
|---|---|
| Relocated MiniMessage breaks at runtime | E2E asserts a MiniMessage style actually renders; fallback is unrelocated shading on Paper |
| Legacy armor-stand metadata index drift | only indices 0/2/3 are used, stable across 1.16–1.20.1 |
| Modern index block wrong for 26.x | constant block reused from the working 0.2.0 build, asserted on 1.21.x E2E |
| 26.3 bot unavailable | 26.3 validated for lifecycle + commands; backend proven on 1.21.x |
| Compiling against 1.16.5 API blocks a needed modern call | the four modern-only calls are reflection-guarded with fallbacks |

## 16. Implementation notes (added during execution)

Findings that changed or sharpened the design, recorded because the spec is the
authority for a later reader:

1. **The build is Gradle, not Maven** (instruction from the maintainer mid-flight).
   Kotlin DSL, wrapper committed, `com.gradleup.shadow` 9.6.1 for the platform jars,
   `gradle.properties` as the single source of version truth.
2. **The practical legacy floor is 1.17.1, not 1.16.5.** Both jars are Java 17
   bytecode; Paper 1.16.5 refuses to boot on Java 17 at all — verified in a container:
   `Unsupported Java detected (61.0). Only up to Java 16 is supported.` A 1.16.x server
   therefore cannot load this build. Supporting it needs `javaRelease=16` or lower and
   a Java 16 server, and is recorded here rather than silently promised.
3. **Adventure's JSON serializer needs the gson artifact.** `adventure-text-serializer-json`
   is only an API plus a dummy implementation that throws
   `No JsonComponentSerializer implementation found`; the working serializer comes from
   `adventure-text-serializer-gson`. Gson is therefore shaded and relocated too.
4. **Metadata payloads are described without PacketEvents types.** Reading
   `EntityDataTypes` runs a static initialiser that loads versioned registries and
   needs a live `PacketEventsAPI`, so a payload built from those constants cannot be
   unit-tested. The renderers now emit a JDK-only `MetadataValue` record and
   `PacketUtil` converts it to `EntityData` at send time. This is what makes the exact
   index/kind/value contract testable.
5. **Format colours beat the style colour.** If `styles.<type>.color` were applied
   unconditionally it repainted `"&c{damage}"` white. A colour inside the format now
   wins; the setting is the default. Documented in config.yml.
6. **Server detection needs both name and banner.** Newer Paper versions report a
   version string without "paper" in it, which made the plugin report "Unknown" on
   1.21.8 and 26.3; detection now consults `Bukkit.getName()` as well.
7. **Damage-cause mapping is name-based.** A switch over `DamageCause` constants
   cannot compile against the 1.16.5 API for causes added later (STALAGMITE,
   SONIC_BOOM), and a name mapping also keeps working on future versions without a
   recompile.
8. **Verified end to end in containers** (packet-level, not log-level): the modern
   renderer on Paper 1.21.8 — spawn, damage text at metadata index 23, 29 teleports
   rising then falling, destroy, toggle suppression and restoration, and a real mob hit
   — plus the legacy renderer on Paper 1.17.1 and 1.20.1, and the modern jar on Paper
   26.3 with an older-protocol bot translated through ViaVersion.

