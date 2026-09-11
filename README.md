# CustomDamageNumbers

Packet-based floating damage numbers for modern Paper/Spigot Minecraft servers. When an entity takes damage, clients receive a lightweight text-display animation showing the amount dealt — no entity spawning, no client mod required.

Built on [PacketEvents](https://github.com/retrooper/packetevents) for protocol-level performance, optimized for Paper 1.21+ on Java 21.

## Highlights

- Packet-driven damage number rendering (no laggy armor stands or entities)
- Configurable formatting, colors, and visibility distance
- Per-viewer protocol compatibility via ViaVersion / ViaBackwards / ProtocolSupport
- Optional [LuckPerms](https://luckperms.net) integration for per-group behavior
- `/cdn` admin command with config and message reloads

## Requirements

| Component | Version |
|-----------|---------|
| Server | Paper or Spigot 1.21+ |
| Java | 21+ |
| Dependency | PacketEvents 2.8+ (required) |
| Optional | LuckPerms, ProtocolLib, ViaVersion, ViaBackwards, ProtocolSupport |

## Building

```bash
mvn package
```

The plugin jar is output to `target/`.

## Configuration

`config.yml` controls enabling, debug logging, render distance and performance tuning; `messages.yml` controls all player-facing text. Both support in-game reloads via `/cdn`.

---

Built by **[PotenFYR Studios](https://github.com/PotenFYR-Studios)** · [potenfyr.in](https://potenfyr.in)
