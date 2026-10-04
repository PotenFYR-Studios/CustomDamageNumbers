# Contributing to CustomDamageNumbers

Thanks for helping improve CustomDamageNumbers. This guide covers everything you need to get a PR merged.

## Project rules (non-negotiable)

1. **Packets, never entities.** No code path may spawn a real entity into the world, not even temporarily. The promise that uninstalling leaves nothing behind is the reason this plugin exists; a PR that breaks it will not be merged.
2. **Compile against the oldest supported API.** The build targets the oldest server API in the support matrix so a Paper-only class cannot leak into a jar that must run on Spigot.
3. **Both jars must build from one source tree.** `cdn-core` holds the version-independent plugin, `cdn-modern` and `cdn-legacy` hold the renderers behind the `RenderBackend` interface. Version-specific code belongs in a renderer, not in core.
4. **No new runtime dependencies without a reason in the PR.** The current hard dependency is PacketEvents. Anything else needs to be justified in the description and relocated in the shadow jar if it is bundled.
5. **Behaviour changes need a test.** Unit tests live beside the modules (`cdn-core/src/test`, `cdn-modern/src/test`, `cdn-legacy/src/test`); rendering changes also get exercised by the Docker end-to-end harness.

## Setup

You need JDK 17 or newer and Docker (the build and the end-to-end tests run in containers; there is no host Maven or Gradle requirement).

```bash
git clone https://github.com/PotenFYR-Studios/CustomDamageNumbers.git
cd CustomDamageNumbers
```

## Commands

| Command | What it does |
| --- | --- |
| `docker run --rm -v "$PWD:/app" -w /app gradle:jdk17 gradle build --no-daemon` | Compile both jars and run the unit suite |
| `bash docker/e2e/run.sh` | End-to-end matrix: real servers in Docker (Paper 1.17.1, 1.20.1, 1.21.8, 26.3) |
| `bash docker/e2e/run.sh 1.21.8` | One version only, for a fast loop |
| `cd docs && bun install && bun run build` | Build the documentation site |

The end-to-end harness boots a real server, installs the built jar plus PacketEvents, drives the plugin through its commands (over RCON on versions mineflayer cannot speak), and asserts the display is created, positioned, moved across the animation and destroyed — and that the plugin disables cleanly with no errors in the log. Evidence lands in `docker/e2e/evidence/` (gitignored).

## Repository layout

```
cdn-core/          the plugin: config, damage pipeline, packet layer, commands, messages
  src/main/java/in/potenfyr/cdn/
  src/main/resources/       config.yml, messages.yml
  src/test/java/            unit tests
cdn-modern/        TextDisplay renderer (1.20.2 - 26.x)
cdn-legacy/        armour stand renderer (1.17.x - 1.20.1)
docker/e2e/        prepare.js, probe.js, run.sh - the end-to-end matrix
docs/              documentation site (Vite + React), deployed to
                   https://cdn.docs.potenfyr.in via GitHub Pages
docs/superpowers/  specs and implementation plans for larger changes
```

## Adding a config option

1. Add the key to `cdn-core/src/main/resources/config.yml` with a comment explaining the effect and the units (ticks, blocks, milliseconds).
2. Read it in `ConfigManager` and expose a typed accessor; do not read `config.getX` from feature code.
3. Use it, add a unit test that proves the default and a changed value both behave, and document it in `docs/src/docs/content.ts`.
4. Confirm `/cdn reload` picks it up live: a config option that needs a restart is a bug.

## Adding a command

Subcommands live in `cdn-core/src/main/java/in/potenfyr/cdn/command/impl/` and implement `SubCommand` (`name`, `permission`, `usage`, `description`, `execute`, `complete`). Register it in the dispatcher, declare the permission in both `plugin.yml` files, add it to the framed help output, and cover the permission path in a test.

## Working on the docs site

```bash
cd docs
bun install --frozen-lockfile
bun run dev      # vite dev server with HMR
bun run build    # production build (static prerender of every route)
```

Documentation content lives in `docs/src/docs/content.ts` as typed data, not JSX, so the prerenderer can enumerate the sections. New sections are picked up by the build and by the docs sidebar automatically.

## Commits and PRs

- **Conventional Commits**: `feat:`, `fix:`, `docs:`, `chore:`, `refactor:`, `test:`. The release workflow builds its changelog from these.
- Keep PRs focused: one fix or feature per PR.
- Run the build and the relevant end-to-end version before pushing, and say in the PR which versions you exercised.

## Reporting bugs and security issues

- Bugs: [open an issue](https://github.com/PotenFYR-Studios/CustomDamageNumbers/issues) with the plugin version, server type and version, the config keys involved, and `/cdn stats` output.
- Vulnerabilities: **never** in a public issue — see [SECURITY.md](SECURITY.md) for private reporting.

## License

By contributing you agree that your contributions are licensed under the [Apache License 2.0 with the Commons Clause](LICENSE), same as the rest of the project.
