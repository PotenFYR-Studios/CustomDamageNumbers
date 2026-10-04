# Implementation Plan — CustomDamageNumbers 0.3.0

Spec: `docs/superpowers/specs/2026-10-04-multiversion-animation-player-control-design.md`
Rule for every task: build and test **in Docker only** (`maven:3.9-eclipse-temurin-17`
for compile/unit, per-version Paper containers for E2E). No host tooling.

Task order is dependency order. Each task ends with evidence (build output,
test output, container log), never a claim.

## T0 — Toolchain
Deliverable: `docker/mvn` wrapper script + `.mvn/` free build path; a scratch
Maven local repo volume so re-builds are fast.
Verify: `docker run --rm -v repo:/app -v cdn-m2:/root/.m2 -w /app
maven:3.9-eclipse-temurin-17 mvn -q -B validate` succeeds on the current POM.

## T1 — Multi-module skeleton
Deliverable: parent `pom.xml` (packaging pom, `release 17`, version 0.3.0) +
`cdn-core` / `cdn-modern` / `cdn-legacy` modules with shade config, resource
filtering, and per-module `plugin.yml` (`api-version: '1.16'` legacy,
`'1.20'` modern — a value the target servers accept).
Verify: `mvn -q -B package` produces two jars; `unzip -l` shows relocated
`in/potenfyr/cdn/libs/kyori/**` inside them.

## T2 — Platform + scheduler layer
Deliverable: `MinecraftVersion`, `Ecosystem`, `SchedulerAdapter` +
Bukkit/Folia adapters.
Verify: unit tests for version parsing/compare and ecosystem detection;
Folia adapter exercised through a reflective-failure test (no Folia container).

## T3 — Config, styles, text pipeline
Deliverable: `ConfigManager` (every key read, clamped, defaults written),
`AnimationSettings`, `StyleSettings`, `TextRenderer` (MiniMessage + legacy ->
JSON string and legacy string, internally relocated Adventure),
`LegacyToMiniMessage`.
Verify: unit tests — clamping, defaults, style resolution, `&a` conversion,
MiniMessage parse, invalid-format fallback.

## T4 — Models + animation curve
Deliverable: `FloatingDamage` (victim/world anchoring fields), `AnimationFrame`,
`AnimationCurve`.
Verify: unit tests — rise then fall, peak bound, fade ramp, scale endpoints,
bounce damping, rotation orbit, duration-0 safety.

## T5 — Damage pipeline
Deliverable: `DamageType` (+HEALING), `DamageClassifier` (reflection-guarded
`getDamageSource()` / `isCritical()`), `DamageListener` (base + by-entity events),
`DamageService` (filters, caps, O(1) per-player index, viewer selection,
preferences, merging), `MergeRegistry`.
Verify: unit tests for classification, merging window/cap, caps and indexes.
Behaviour is proven in T10.

## T6 — Player preferences
Deliverable: `PlayerPreferences` (players.yml, async save, defaults).
Verify: unit tests — round-trip, default when absent, concurrent toggle.

## T7 — Packet layer + backends
Deliverable: `PacketUtil`, `RenderBackend`, `AnimationTask`,
`TextDisplayBackend` (modern), `ArmorStandBackend` (legacy),
`MetadataDebugListener` (bounded).
Verify: unit tests for the metadata payload builders (pure functions returning
`List<EntityData<?>>` and JSON strings); E2E in T10 for the wire behaviour.

## T8 — Commands, messages, integrations, metrics
Deliverable: `CommandDispatcher` + subcommands, `messages.yml` rewrite,
`LuckPermsHook` wired, `PlaceholderApiHook`, `MetricsRegistrar`.
Verify: unit tests for routing/permissions/tab completion; `mvn package` clean.

## T9 — Plugin lifecycle
Deliverable: `CustomDamageNumbersPlugin` (core) + `ModernPlugin`/`LegacyPlugin`,
backend self-gating with an explicit "install the other jar" message,
correct PacketEvents listener lifecycle, clean disable.
Verify: unit-testable lifecycle pieces; E2E for the real thing.

## T10 — Docker E2E harness
Deliverable: `docker/compose.yml`, per-version server setup script, mineflayer
probe (`docker/probe/`), runner script asserting the six behaviours in spec §13.
Verify: the probe passes on Paper 1.16.5, 1.20.1, 1.21.x and 26.3 (26.3 with the
documented bot limitation), with captured packet traces saved as evidence.

## T11 — Docs, versioning, release
Deliverable: README rewritten to match reality (matrix, commands, config,
install), CHANGELOG entry, `plugin.yml` version bump to 0.3.0.
Verify: README claims cross-checked against code one by one; `mvn package`
from a clean Maven repo.

## T12 — Review + ship
Deliverable: self-review pass against the spec checklist, any fixes, then commit
and push to `origin/master`.
Verify: `git status` clean, `git log` shows the work, pushed refs match.
