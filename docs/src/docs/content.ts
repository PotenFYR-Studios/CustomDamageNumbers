export const CURRENT_VERSION = '1.0.0';

export interface DocSection {
  slug: string;
  title: string;
  blurb: string;
  category: 'Getting Started' | 'Core Concepts' | 'Guides' | 'API Reference';
  /** markdown-ish body blocks, rendered by the docs page */
  blocks: DocBlock[];
}

export type DocBlock =
  | { type: 'text'; content: string }
  | { type: 'code'; title?: string; lang: 'ts' | 'bash' | 'json' | 'yaml' | 'text'; content: string }
  | { type: 'table'; headers: string[]; rows: string[][] }
  | { type: 'note'; tone: 'info' | 'warn' | 'tip'; content: string }
  | { type: 'list'; items: string[] }
  | { type: 'h3'; content: string };

export interface DocVersion {
  version: string;
  label: string;
  deprecated?: boolean;
  sections: DocSection[];
}

const CURRENT: DocVersion = {
  version: CURRENT_VERSION,
  label: `v${CURRENT_VERSION} (latest)`,
  sections: [
    {
      slug: 'introduction',
      title: 'Introduction',
      blurb: 'What CustomDamageNumbers is and the promise it keeps.',
      category: 'Getting Started',
      blocks: [
        {
          type: 'text',
          content:
            'CustomDamageNumbers draws floating damage numbers with packets. Nothing is spawned into the world: every number is a client-side display entity sent straight over the protocol, positioned above the entity that took the hit. Remove the plugin and there is nothing left behind — no orphaned armour stands, no entity count drift.',
        },
        {
          type: 'list',
          items: [
            'Packet-only rendering. Real entities are never spawned, so nothing can leak into the world.',
            'Anchored to the victim: the number follows the damaged entity as it moves, with configurable smoothing.',
            'Config-driven animation: rise, fall, scale, fade, bounce, orbit and spawn spread are per-tick values in config.yml.',
            'Hit merging, so a flurry of hits grows one number instead of stacking twelve.',
            'Players choose: /cdn toggle switches numbers off for that player alone.',
            'Two jars, one codebase: 1.17.x through 26.x on Spigot, Paper, Purpur, Folia and their forks.',
          ],
        },
        {
          type: 'note',
          tone: 'info',
          content:
            'The only hard dependency is PacketEvents, the library that carries the protocol traffic for every supported version.',
        },
      ],
    },
    {
      slug: 'installation',
      title: 'Installation',
      blurb: 'Requirements and the two-minute install.',
      category: 'Getting Started',
      blocks: [
        {
          type: 'table',
          headers: ['Requirement', 'Minimum', 'Notes'],
          rows: [
            ['Server', '1.17.1', 'Spigot, Paper, Purpur, Pufferfish, Folia and forks'],
            ['Java', '17', 'the jars are Java 17 bytecode'],
            ['PacketEvents', '2.14', 'the one hard dependency'],
            ['LuckPerms', 'optional', 'style profiles and permission routing'],
            ['PlaceholderAPI', 'optional', 'damage placeholders in other plugins'],
          ],
        },
        {
          type: 'code',
          lang: 'bash',
          title: 'install',
          content: `plugins/
  PacketEvents-2.14.0.jar
  CustomDamageNumbers-1.0.0.jar          # or the Legacy jar, see the next section

# then restart, and check the console banner
/cdn version`,
        },
        {
          type: 'note',
          tone: 'warn',
          content:
            'The modern jar refuses to enable on a server older than 1.20.2 and tells you to use the Legacy jar. That is deliberate: a silent no-op is worse than a clear refusal.',
        },
      ],
    },
    {
      slug: 'which-jar',
      title: 'Which jar do I install?',
      blurb: 'The one choice that matters: modern or legacy renderer.',
      category: 'Getting Started',
      blocks: [
        {
          type: 'text',
          content:
            'Minecraft only gained the text display entity in 1.19.4, and only let plugins position it per-entity from 1.20.2. Older servers get an armour stand nametag instead. The two renderers are the same plugin with the same config; they differ in what the client can be asked to do.',
        },
        {
          type: 'table',
          headers: ['Jar', 'Server versions', 'Renderer', 'Fade / scale'],
          rows: [
            ['CustomDamageNumbers-1.0.0.jar', '1.20.2 - 26.x', 'TextDisplay (packet)', 'yes'],
            ['CustomDamageNumbers-Legacy-1.0.0.jar', '1.17.x - 1.20.1', 'armour stand nametag (packet)', 'position only'],
          ],
        },
        {
          type: 'list',
          items: [
            'Modern jar on a modern server: the full animation, including fade-out and per-entity scaling.',
            'Legacy jar on an older server: rise, fall, follow and drift animate; fade-out and scale are ignored, and the startup log says so.',
            'The legacy jar also runs on modern servers, but the modern jar is better there.',
          ],
        },
        {
          type: 'note',
          tone: 'tip',
          content: 'Run /cdn backend on a live server to see the active renderer and what it supports — no guessing from jar names.',
        },
      ],
    },
    {
      slug: 'commands',
      title: 'Commands',
      blurb: 'The full /cdn surface.',
      category: 'Core Concepts',
      blocks: [
        {
          type: 'table',
          headers: ['Command', 'What it does', 'Permission'],
          rows: [
            ['/cdn help', 'Framed command list, filtered to what you may use', '-'],
            ['/cdn version', 'Plugin, renderer and server version panel', '-'],
            ['/cdn backend', 'Active renderer and its capabilities', '-'],
            ['/cdn stats', 'Live counters, viewer slots, tick cost', 'cdn.stats'],
            ['/cdn toggle [on|off] [player]', 'Turn damage numbers on or off for yourself or another player', 'cdn.toggle / cdn.toggle.others'],
            ['/cdn style [player] <profile|default>', 'Pick a style profile', 'cdn.style / cdn.style.others'],
            ['/cdn test [type] [amount]', 'Spawn a test display, no damage needed', 'cdn.test'],
            ['/cdn clear [player|all]', 'Remove live displays immediately', 'cdn.clear'],
            ['/cdn reload [config|messages|all]', 'Reload configuration and/or messages', 'cdn.reload'],
            ['/cdn debug [on|off]', 'Verbose logging', 'cdn.debug'],
          ],
        },
        {
          type: 'text',
          content:
            'The aliases /damage and /damagedisplay point at the same command. Every panel adapts to where it is run: players get the box-drawing frame, the console and RCON get an ASCII frame with colour codes stripped.',
        },
      ],
    },
    {
      slug: 'permissions',
      title: 'Permissions',
      blurb: 'Who may do what.',
      category: 'Core Concepts',
      blocks: [
        {
          type: 'table',
          headers: ['Permission', 'Default', 'Grants'],
          rows: [
            ['cdn.view', 'true', 'see damage numbers (enforced only when require-view-permission is true)'],
            ['cdn.toggle', 'true', 'toggle your own numbers'],
            ['cdn.toggle.others', 'op', 'toggle another player'],
            ['cdn.style', 'true', 'choose your own style profile'],
            ['cdn.style.others', 'op', 'set another player style'],
            ['cdn.style.fortnite', 'true', 'the Fortnite style profile'],
            ['cdn.style.mmo', 'false', 'the MMO style profile'],
            ['cdn.test', 'op', 'spawn test displays'],
            ['cdn.clear', 'op', 'clear live displays'],
            ['cdn.stats', 'op', 'read the statistics panel'],
            ['cdn.reload', 'op', 'reload config and messages'],
            ['cdn.debug', 'op', 'toggle debug logging'],
          ],
        },
        {
          type: 'note',
          tone: 'tip',
          content:
            'With LuckPerms installed, the cdn.style.* permissions are read per player, so a rank can hand out a style profile without touching config.yml.',
        },
      ],
    },
    {
      slug: 'configuration',
      title: 'Configuration',
      blurb: 'config.yml, section by section.',
      category: 'Guides',
      blocks: [
        { type: 'h3', content: 'general' },
        {
          type: 'code',
          lang: 'yaml',
          title: 'config.yml (excerpt)',
          content: `general:
  enabled: true            # master switch
  debug: false             # /cdn debug toggles this live
  banner: true             # ASCII banner on startup
  view-distance: 32        # max distance a player receives numbers from
  disabled-worlds: [example_world]
  ignore-invisible-entities: true
  ignore-armor-stands: true
  ignore-npcs: true
  entities:
    mobs: true
    players: true
    animals: true
  self-damage: true
  nearby-viewers-only: true
  max-active-displays: 2000   # flood protection for mob farms
  cleanup-orphans: true       # drop displays whose viewers all left
  default-view-enabled: true  # what a player sees before /cdn toggle`,
        },
        { type: 'h3', content: 'animation' },
        {
          type: 'code',
          lang: 'yaml',
          title: 'config.yml (excerpt)',
          content: `animation:
  duration-ticks: 30        # total lifetime (20 ticks = 1s)
  rise-ticks: 10            # ticks rising before the sink starts
  vertical-speed: 0.08      # blocks per tick while rising
  horizontal-randomness: 0.20
  scale-animation: true
  start-scale: 1.3
  end-scale: 0.8
  fade-out: true            # modern jar only
  fade-start: 0.6           # progress point (0-1) where fading begins
  bounce: true
  bounce-strength: 0.35
  rotation: false
  rotation-speed: 0.10
  random-offset: true
  spawn-spread: 0.20        # so stacked hits stay readable
  follow-entity: true       # stay attached to the victim
  anchor-height: 1.8        # above the entity's feet
  follow-smoothing: 0.35    # 0 = snap every tick, higher = softer lag`,
        },
        { type: 'h3', content: 'merging, criticals, styles, particles' },
        {
          type: 'code',
          lang: 'yaml',
          title: 'config.yml (excerpt)',
          content: `merge-system:
  enabled: true
  merge-window-ms: 150      # a follow-up hit inside this window grows the number
  max-merged-damage: 9999
  same-attacker-only: true

critical-hits:
  enabled: true
  scale-multiplier: 1.5
  sound: true
  sound-type: ENTITY_PLAYER_ATTACK_CRIT
  volume: 1.0
  pitch: 1.2

styles:
  normal:   { enabled: true, format: "{damage}",    color: "#FFFFFF", bold: true, shadow: true }
  critical: { enabled: true, format: "✧ {damage}",  color: "#FF3333", bold: true, shadow: true }
  fire:     { enabled: true, format: "🔥 {damage}", color: "#FF9900", bold: true, shadow: true }
  healing:  { enabled: true, format: "+{damage}",   color: "#00FF99", bold: true, shadow: true }

style-profiles:
  fortnite:
    critical: { format: "{damage}", color: "#FFFF00" }
  mmo:
    normal:   { format: "{damage} DMG" }
    critical: { format: "{damage} CRIT", color: "#FF5555" }

particles:
  enabled: true
  critical: { enabled: true, particle: CRIT,      amount: 8 }
  fire:     { enabled: true, particle: FLAME,     amount: 6 }
  poison:   { enabled: true, particle: SPELL_MOB, amount: 5 }`,
        },
        { type: 'h3', content: 'performance, integrations, permissions, advanced' },
        {
          type: 'code',
          lang: 'yaml',
          title: 'config.yml (excerpt)',
          content: `performance:
  animation-interval: 1     # ticks between steps; 1 is smoothest
  max-per-player: 50        # simultaneous displays per player
  distance-culling: true    # detach viewers that walk out of range

integrations:
  luckperms: true
  placeholderapi: true

permissions:
  require-view-permission: false
  view-permission: "cdn.view"
  style-permissions: true

advanced:
  remove-on-death: true     # killing blows flash instead of full animation
  ignore-zero-damage: true
  packet-debug: false       # TextDisplay metadata dump, bounded
  debug-log-limit: 256
  folia-mode: auto          # auto | true | false
  metrics: false            # bStats is off unless you enable it
  metrics-id: 0`,
        },
        {
          type: 'note',
          tone: 'info',
          content: 'Every value is read on reload: /cdn reload applies changes live, without a restart.',
        },
      ],
    },
    {
      slug: 'animation',
      title: 'How the animation works',
      blurb: 'The number belongs to the entity, not to a fixed spot.',
      category: 'Core Concepts',
      blocks: [
        {
          type: 'text',
          content:
            'Each number is anchored above the victim and re-positioned every animation step, so a mob knocked backwards drags its number with it. The position is the anchor, plus the rise/fall curve for the current tick, plus that number\u2019s spawn offset. follow-smoothing blends the anchor toward the entity instead of snapping, which removes the jitter you get when a target moves fast.',
        },
        {
          type: 'list',
          items: [
            'Rise: vertical-speed blocks per tick for rise-ticks ticks, peaking at vertical-speed × rise-ticks.',
            'Fall: after the peak the number sinks for the remaining ticks.',
            'Bounce: an optional damped bounce layered on the rise (bounce-strength).',
            'Scale: start-scale to end-scale across the lifetime, folded with the critical multiplier.',
            'Fade: after fade-start of the lifetime, opacity ramps to zero (modern jar).',
            'Orbit: rotation makes the number circle the anchor instead of drifting in a line.',
          ],
        },
        {
          type: 'code',
          lang: 'text',
          title: 'the curve, in ticks (defaults)',
          content: `tick  0 |  start at anchor-height, scale 1.3
tick  5 |  rising, near full opacity
tick 10 |  peak: 0.8 blocks above the anchor
tick 15 |  sinking, scale shrinking
tick 20 |  past fade-start 0.6, fading out
tick 29 |  last frame, then the display is destroyed`,
        },
        {
          type: 'note',
          tone: 'tip',
          content:
            'A killing blow (remove-on-death: true) flashes the number briefly instead of playing the full curve — a 30-tick animation on a corpse reads as a bug to players.',
        },
      ],
    },
    {
      slug: 'merging',
      title: 'Hit merging',
      blurb: 'Ten hits, one number that grows.',
      category: 'Core Concepts',
      blocks: [
        {
          type: 'text',
          content:
            'When the same attacker hits the same victim twice inside merge-window-ms, the second hit is folded into the live number instead of spawning a new one. The merged total is what players read, capped by max-merged-damage.',
        },
        {
          type: 'table',
          headers: ['Setting', 'Default', 'Effect'],
          rows: [
            ['merge-system.enabled', 'true', 'turn merging off entirely'],
            ['merge-window-ms', '150', 'how long after a hit a follow-up still merges'],
            ['max-merged-damage', '9999', 'cap for the accumulated value'],
            ['same-attacker-only', 'true', 'a second attacker starts its own number'],
          ],
        },
      ],
    },
    {
      slug: 'player-control',
      title: 'Player control',
      blurb: 'Players decide what they see.',
      category: 'Core Concepts',
      blocks: [
        {
          type: 'text',
          content:
            'Damage numbers are cosmetic, so a player who finds them noisy should not have to ask an admin. /cdn toggle switches them off for that player alone and persists across restarts; /cdn style picks a profile when several are available.',
        },
        {
          type: 'code',
          lang: 'bash',
          title: 'from the player side',
          content: `/cdn toggle            # flip your own numbers on/off
/cdn toggle off        # unambiguous
/cdn style mmo         # switch profile (if you may use it)
/cdn style default     # back to styles.normal`,
        },
        {
          type: 'list',
          items: [
            'default-view-enabled decides what a player sees before they ever run the command.',
            'cdn.view is enforced only when permissions.require-view-permission is true.',
            'An opt-out survives restarts, and the statistics panel counts how many players have opted out.',
          ],
        },
      ],
    },
    {
      slug: 'performance',
      title: 'Performance and memory',
      blurb: 'What the audit found, and why nothing grows unbounded.',
      category: 'Guides',
      blocks: [
        {
          type: 'text',
          content:
            'The 1.0.0 audit looked for the two ways a display plugin leaks: registry maps that keys never leave, and viewers that are tracked but never detached. Both are bounded now.',
        },
        {
          type: 'list',
          items: [
            'Displays live in a bounded structure with an explicit destroy path on death, /cdn clear and shutdown.',
            'Viewer slots are reference-counted per player: a player who leaves is dropped from every display they were watching.',
            'distance-culling detaches viewers that walk out of view-distance mid-animation.',
            'max-active-displays caps the whole server; max-per-player caps one player.',
            'The packet debug listener tracks at most debug-log-limit entity ids and forgets destroyed ones (it was an unbounded set before).',
          ],
        },
        {
          type: 'table',
          headers: ['Counter', 'Where to read it', 'Healthy value'],
          rows: [
            ['Active displays', '/cdn stats', 'falls back to 0 when nothing is happening'],
            ['Viewer slots', '/cdn stats', 'grows with players, drops when they leave'],
            ['Merge entries', '/cdn stats', 'bounded by the active numbers'],
            ['Tick cost', '/cdn stats', 'a fraction of a millisecond; the panel shows avg and worst'],
          ],
        },
        {
          type: 'note',
          tone: 'tip',
          content:
            'If tick cost climbs, raise performance.animation-interval to 2 (numbers step every other tick) before lowering max-active-displays; the animation reads the same.',
        },
      ],
    },
    {
      slug: 'compatibility',
      title: 'Compatibility',
      blurb: 'Versions, server types and the plugins around them.',
      category: 'Guides',
      blocks: [
        {
          type: 'table',
          headers: ['Server', 'Jar', 'Status'],
          rows: [
            ['1.17.x - 1.19.x', 'Legacy', 'verified: spawn, animate, follow, destroy'],
            ['1.20.1', 'Legacy', 'verified'],
            ['1.20.2 - 1.21.x', 'Modern', 'verified, including fade and scale'],
            ['26.x (Paper)', 'Modern', 'verified; older clients connect through ViaVersion'],
            ['Folia', 'either', 'folia-mode: auto selects the region scheduler'],
          ],
        },
        {
          type: 'list',
          items: [
            'Compiled against the oldest supported API, so no Paper-only class can leak into the jar.',
            'PacketEvents carries the protocol work, so one code path serves every version.',
            'ViaVersion, ViaBackwards, ViaRewind, Geyser, ProtocolLib and ProtocolSupport are soft dependencies: detected, never required.',
            '1.16.x is out of reach: Paper 1.16.5 refuses to run on Java 17, and the jars are Java 17 bytecode.',
          ],
        },
      ],
    },
    {
      slug: 'api-reference',
      title: 'API Reference',
      blurb: 'Everything a script or another plugin can reach.',
      category: 'API Reference',
      blocks: [
        { type: 'h3', content: 'Commands' },
        {
          type: 'code',
          lang: 'bash',
          title: 'the complete surface',
          content: `/cdn help
/cdn version
/cdn backend
/cdn stats
/cdn toggle [on|off] [player]
/cdn style [player] <profile|default>
/cdn test [type] [amount]
/cdn clear [player|all]
/cdn reload [config|messages|all]
/cdn debug [on|off]`,
        },
        { type: 'h3', content: 'Config keys' },
        {
          type: 'table',
          headers: ['Key', 'Type', 'Meaning'],
          rows: [
            ['general.enabled', 'boolean', 'master switch'],
            ['general.view-distance', 'number', 'receive radius in blocks'],
            ['general.max-active-displays', 'number', 'server-wide cap'],
            ['general.default-view-enabled', 'boolean', 'state before /cdn toggle'],
            ['animation.*', 'mixed', 'lifetime, curve, scale, fade, follow'],
            ['merge-system.*', 'mixed', 'hit merging window and cap'],
            ['styles.<type>.*', 'mixed', 'format, colour, bold, italic, shadow'],
            ['style-profiles.<name>.*', 'mixed', 'per-profile style overrides'],
            ['performance.animation-interval', 'number', 'ticks between animation steps'],
            ['permissions.require-view-permission', 'boolean', 'gate numbers behind cdn.view'],
            ['advanced.folia-mode', 'auto | true | false', 'scheduler selection'],
          ],
        },
        { type: 'h3', content: 'Messages' },
        {
          type: 'text',
          content:
            'Every string the plugin prints lives in messages.yml, including the frame block (width, border, title, subtitle, label, value, accent, footer, glyph, bullet). Editing it re-skins the chat panels and the console panels at once; /cdn reload messages applies it live.',
        },
        {
          type: 'code',
          lang: 'yaml',
          title: 'messages.yml (frame)',
          content: `frame:
  width: 58
  border: "&8"
  title: "&d&l"
  subtitle: "&7"
  label: "&7"
  value: "&f"
  accent: "&b"
  footer: "&8"
  glyph: "◆"
  bullet: "▪"`,
        },
      ],
    },
    {
      slug: 'troubleshooting',
      title: 'Troubleshooting',
      blurb: 'The four things that actually go wrong.',
      category: 'Guides',
      blocks: [
        { type: 'h3', content: 'Nothing appears at all' },
        {
          type: 'list',
          items: [
            'Run /cdn backend: if it reports no active renderer on an old server, install the Legacy jar.',
            'Run /cdn stats: animation ticks that stay at 0 while damage lands means no display was ever created — check general.enabled and general.disabled-worlds.',
            'Check that the victim kind is enabled under general.entities.',
            'Run /cdn test: it renders a display with no damage involved, which separates a rendering problem from an event problem.',
          ],
        },
        { type: 'h3', content: 'Numbers appear but never move or fade' },
        {
          type: 'text',
          content:
            'Fade and per-entity scale need a text display. The legacy jar positions and drifts numbers but cannot fade them; on a modern server with the legacy jar installed, that is the whole explanation — switch to the modern jar.',
        },
        { type: 'h3', content: 'Numbers look noisy on a farm' },
        {
          type: 'list',
          items: [
            'Raise merge-system.merge-window-ms so rapid hits fold into one number.',
            'Lower general.max-active-displays and performance.max-per-player.',
            'Disable the particle effects you do not want, or particles.enabled entirely.',
          ],
        },
        { type: 'h3', content: 'The plugin disables itself with a message about the jar' },
        {
          type: 'text',
          content:
            'That is the version guard doing its job: the modern jar will not run on a server older than 1.20.2. Install the Legacy jar and restart.',
        },
        {
          type: 'note',
          tone: 'info',
          content: 'For anything else, /cdn debug on then /cdn test logs a line per packet sent, and that log is what a bug report needs.',
        },
      ],
    },
    {
      slug: 'migration',
      title: 'Migrating from 0.2.x',
      blurb: 'What changed, and what to do with your old config.',
      category: 'Getting Started',
      blocks: [
        {
          type: 'text',
          content:
            '1.0.0 replaces the fixed-location renderer with an entity-anchored one, makes the animation config real, and splits the single jar into modern and legacy builds. An old config still loads: every key it used exists, and new keys take their defaults.',
        },
        {
          type: 'list',
          items: [
            'Two jars now. Install the one matching your server version and delete the old single jar.',
            'Animation values now affect rendering. The old animation block was inert, so defaults may look different from what you were used to.',
            'The hard version ceiling is gone: the old build was pinned to api-version 26.3 only.',
            'Style profiles, /cdn style and /cdn toggle are new; nothing you had configured needs to change.',
          ],
        },
        {
          type: 'note',
          tone: 'warn',
          content:
            'If you edited messages.yml, keep your file: 1.0.0 adds the frame block but never overwrites an existing messages.yml, and the old label keys are still read.',
        },
      ],
    },
  ],
};

export const CURRENT_DOC: DocVersion = CURRENT;
export const DOC_VERSIONS: DocVersion[] = [CURRENT_DOC];
export const LATEST_VERSION = CURRENT_VERSION;
