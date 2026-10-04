# Security Policy

## Supported versions

| Version | Supported |
| --- | --- |
| 1.x | yes |
| < 1.0 | no |

## Reporting a vulnerability

**Do not open a public issue for exploitable vulnerabilities.**

Report privately through [GitHub's private vulnerability reporting](https://github.com/PotenFYR-Studios/CustomDamageNumbers/security/advisories/new) on this repository.

Include what you can:

- Affected jar and version (`CustomDamageNumbers-1.0.0.jar` or `-Legacy-1.0.0.jar`)
- Server type and version (Paper/Spigot/Folia and the Minecraft version), plus the Java version
- A minimal reproduction: the config keys involved, the command run, and the console output
- Impact assessment (what an attacker could do)
- Any suggested fix (optional)

You will get an acknowledgment within 72 hours, followed by status updates as the fix progresses. We credit reporters in the release notes by default; tell us if you prefer to stay anonymous.

## Scope

**In scope:**

- The plugin jars as released: command handling and permission checks, config and message parsing, the player preference store, and the packet path that creates, moves and destroys displays
- Privilege boundaries: a subcommand that acts on another player or on the whole server without its permission, or a player-only guard that can be bypassed from the console or RCON
- The player-side toggle and style store: reading or writing another player's preference, or using them to interfere with staff tooling
- Config or message values that cause the plugin to execute code, resolve unexpected files, or reach the network

**Out of scope:**

- Minecraft, the server implementation (Paper, Spigot, Purpur, Folia) and their own vulnerabilities: report those upstream
- PacketEvents: report protocol-layer issues to the PacketEvents project
- Weaknesses that require the operator to disable the shipped defaults (`general.enabled: false`, `permissions.require-view-permission: false`, an open console)
- Other plugins, including LuckPerms, PlaceholderAPI, ViaVersion and Geyser
- The documentation site (static content, no backend)
- Servers whose operator has granted a permission they did not mean to grant

## Built-in defenses

These are documented behavior, not security claims to test around:

- Every subcommand checks its own permission before doing anything, and the sensitive ones (`reload`, `clear`, `test`, `stats`, `debug`, and the `others` variants) default to op
- `toggle` and `style` are player-only: a non-player sender is refused rather than silently acting on nobody
- The plugin never evaluates config or message strings as code, and never loads a file path from config
- No network access at runtime. bStats is off by default and needs both `advanced.metrics: true` and a non-zero `metrics-id` to report anything
- The console and RCON receive output with colour codes stripped and glyphs transliterated, so a terminal never has to interpret raw control codes
- Player preferences are stored per UUID, and a preference write is scoped to the player the command is allowed to act on

## Data handling

Config, messages and per-player preferences are local files under `plugins/CustomDamageNumbers/`. The plugin does not transmit them anywhere. If bStats is explicitly enabled it reports the standard anonymous bStats payload (server software, version, player count, plugin version) to bstats.org and nothing else; leave `advanced.metrics` at `false` to send nothing at all.
