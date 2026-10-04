<!-- markdownlint-disable -->
<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:8b5cf6,50:ec4899,100:f97316&height=220&section=header&text=CustomDamageNumbers&fontSize=52&fontColor=ffffff&fontAlignY=34&animation=twinkling" width="100%" alt="CustomDamageNumbers Banner"/>

[![Typing SVG](https://readme-typing-svg.demolab.com?font=Fira+Code&weight=600&size=20&pause=1200&color=8B5CF6&center=true&vCenter=true&width=800&lines=Packet-level+floating+damage+numbers+%F0%9F%92%A5;No+armor+stands+%C2%B7+No+entities+%C2%B7+No+client+mods;Six+damage+types+%C2%B7+Crits+%C2%B7+Hit+merging+%C2%B7+Particles;By+PotenFYR+Studios)](https://github.com/PotenFYR-Studios/CustomDamageNumbers)

<p align="center">
  <a href="https://papermc.io"><img src="https://img.shields.io/badge/Platform-Paper%2026.3-8b5cf6?style=for-the-badge&logo=minecraft&logoColor=white&labelColor=1c1e26" alt="Paper 26.3" /></a>
  <a href="https://openjdk.org"><img src="https://img.shields.io/badge/Java-25%2B-f97316?style=for-the-badge&logo=openjdk&logoColor=white&labelColor=1c1e26" alt="Java 25+" /></a>
  <a href="https://github.com/retrooper/packetevents"><img src="https://img.shields.io/badge/Powered%20by-PacketEvents%202.14%2B-ec4899?style=for-the-badge&labelColor=1c1e26" alt="PacketEvents 2.14+" /></a>
  <img src="https://img.shields.io/badge/Status-Alpha-eac54f?style=for-the-badge&labelColor=1c1e26" alt="Status: Alpha" />
  <a href="https://github.com/PotenFYR-Studios/CustomDamageNumbers"><img src="https://komarev.com/ghpvc/?username=PotenFYR-Studios-CustomDamageNumbers&color=ec4899&style=for-the-badge&label=VIEW&labelColor=1c1e26" alt="View" /></a>
</p>

</div>

---

## 🎯 What Is CustomDamageNumbers?

**CustomDamageNumbers** is a packet-based floating damage number plugin for modern Paper/Spigot Minecraft servers, built by **PotenFYR Studios**. Vanilla shows no damage feedback, and the classic workaround (spawning invisible armor stands with custom names) churns entities and lags mob farms. CustomDamageNumbers instead renders each damage amount as a lightweight client-side text display, delivered straight over the protocol via [PacketEvents](https://github.com/retrooper/packetevents): no entity spawning, no client mod required. When an entity takes damage, nearby players see an animated number rise, fade and disappear, purely as packets.

## ✨ Highlights

- **Packet-driven rendering**: text-display spawn/despawn and metadata packets through PacketEvents 2.x; nothing is spawned into the world.
- **Six damage types**: dedicated styles (format, color, bold/italic, shadow) for normal, critical, fire, magic, poison and explosion damage.
- **Critical hit treatment**: custom symbol (✧), 1.5× scale, bold formatting and an optional crit sound (`ENTITY_PLAYER_ATTACK_CRIT`).
- **Hit merging**: rapid strikes inside a configurable window (default 150 ms) collapse into a single number, with same-attacker-only and max-merged caps.
- **Configurable animation**: duration, vertical rise, horizontal randomness, scale animation, fade-out, bounce and optional rotation.
- **Damage particles**: optional crit/flame/poison particle bursts alongside the numbers.
- **Performance guardrails**: view distance, nearby-viewers-only targeting, global/per-player display caps, packet batching, distance culling, orphan cleanup, async dispatch and a Folia compatibility mode.
- **LuckPerms integration**: optional per-group style behavior (e.g. `cdn.style.fortnite`, `cdn.style.mmo`).
- **Protocol compatibility**: per-viewer protocol handling alongside ViaVersion, ViaBackwards, ViaRewind, ProtocolSupport and Geyser-Spigot.
- **`/cdn` admin command**: in-game reload of config and messages, plus a test display spawner.

## 📦 Requirements

| Component | Version |
|-----------|---------|
| Minecraft | 26.3 |
| Server | Paper 26.3 |
| Java | 25+ |
| Dependency | PacketEvents 2.14+ (required, hard dependency) |
| Optional | LuckPerms, ProtocolLib, ViaVersion, ViaBackwards, ViaRewind, ProtocolSupport, Geyser-Spigot |

## 🔨 Building

```bash
mvn package
```

The plugin jar is output to `target/`.

## ⚙️ Configuration

`config.yml` controls everything:

| Section | What it tunes |
|---------|---------------|
| `general` | Enable/disable, debug logging, view distance, disabled worlds, entity filters (armor stands, NPCs, invisibles), display caps |
| `animation` | Duration, rise speed, randomness, scale, fade-out, bounce, rotation |
| `merge-system` | Hit-merge window, per-attacker rules, merged-damage cap |
| `critical-hits` | Symbol, scale multiplier, bold, crit sound + volume/pitch |
| `styles` | Per-damage-type format/color with MiniMessage support |
| `particles` | Crit, fire and poison particle effects |
| `performance` | Async packets, animation interval, batching, culling |
| `integrations` | LuckPerms toggle (plus reserved flags for future hooks) |
| `advanced` | Entity-ID range, orphan cleanup, packet debug, Folia mode |

`messages.yml` controls all player-facing text. Both reload live via `/cdn reload`, no restart needed.

## ⌨️ Commands & Permissions

| Command | Permission | Description |
|---------|------------|-------------|
| `/cdn` | none | Help overview (aliases: `/damage`, `/damagedisplay`) |
| `/cdn reload` | `cdn.reload` (OP) | Reload `config.yml` and `messages.yml` |
| `/cdn test` | `cdn.test` (OP) | Spawn a test critical damage display |

## 🧪 Test Server

Run `testserver\run-testserver.ps1` from PowerShell to build the plugin, install it in the local Paper server, run the bundled runtime probe, and stop the server after validation:

```powershell
.\testserver\run-testserver.ps1 -JavaPath "C:\Path\To\Java25\bin\java.exe"
```

Use `-SkipBuild` to test the existing jar in `target`, or provide `-JavaPath` when Java 25 is not the default `java` on `PATH`. The script requires Maven on `PATH` when building.

## 🔄 Migration & Changelog

### 0.2.0 — Paper 26.3 upgrade

- **Target**: Minecraft/Paper **26.3** (`api-version: '26.3'`), Java **25**.
- **Paper API**: `26.3-R0.1-SNAPSHOT` (was `1.21.6-R0.1-SNAPSHOT`).
- **PacketEvents**: `2.14.0` (first release with Minecraft 26.3 support; was `2.12.1`).
- **LuckPerms API**: `5.5` (was `5.4`).
- **Damage detection**: critical hits now use the modern `EntityDamageByEntityEvent#isCritical()` instead of the fall-distance heuristic; damage-type classification consults the damage-source type key first and falls back to `DamageCause`.
- **Rendering**: text-display metadata indices were re-validated against the current display-entity protocol layout (Display base 8–22, Text Display 23–27, unchanged since 1.20.2) and are now documented in one place; the deprecated `EntityDataTypes.COMPONENT` (JSON string) was replaced with `EntityDataTypes.ADV_COMPONENT` (native Adventure); each display carries a stable fake-entity UUID instead of a random UUID per viewer.
- **Viewer tracking**: teleport/destroy packets are only sent to players that actually received the spawn packet; distance culling detaches out-of-range viewers; orphan cleanup and plugin-disable now despawn live displays.
- **Behavior fixes**: killing blows flash briefly instead of playing the full animation (`advanced.remove-on-death`); the previously ignored `animation.horizontal-randomness` setting now controls spawn spread; `styles.<type>` format/color/bold/italic settings are now honored for every damage type; zero-damage fake hits are filtered (`advanced.ignore-zero-damage`).
- **Commands**: `/cdn` messages come from `messages.yml` (Adventure components, legacy `&` codes still work); added tab completion and a `cdn.test` permission check.
- **Config compatibility**: all existing `config.yml` and `messages.yml` keys keep working; missing keys fall back to built-in defaults and new keys are optional. No user settings are overwritten on startup.

### Upgrading from 0.1.x

1. Update the server to Paper 26.3 and Java 25+.
2. Update PacketEvents to 2.14.0 or newer.
3. Replace the plugin jar. Existing `config.yml` / `messages.yml` files can be kept as-is.

## 🤝 Contributing

CustomDamageNumbers is maintained internally by PotenFYR Studios and is not currently accepting outside contributions.

## 📜 License

CustomDamageNumbers is developed internally by PotenFYR Studios. No open-source license has been published for this repository. All rights reserved by PotenFYR Studios. If a `LICENSE` file is added later, that file is the authoritative statement of the licensing terms.

---

<!-- markdownlint-disable -->
<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:f97316,50:ec4899,100:8b5cf6&height=120&section=footer&text=Made%20with%20%E2%9D%A4%EF%B8%8F%20by%20PotenFYR%20Studios&fontSize=22&fontColor=ffffff&animation=twinkling" width="100%" alt="footer"/>

</div>
<!-- markdownlint-enable -->
