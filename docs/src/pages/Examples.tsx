import Code from '../components/Code';

const RECIPES: { title: string; body: string; code: string; lang: string }[] = [
  {
    title: 'Install the right jar',
    body: 'Two jars, one plugin. Modern for 1.20.2 and newer, legacy for everything back to 1.17.',
    lang: 'bash',
    code: `plugins/
  PacketEvents-2.14.0.jar
  CustomDamageNumbers-1.0.0.jar          # 1.20.2 - 26.x
  # or
  CustomDamageNumbers-Legacy-1.0.0.jar   # 1.17.x - 1.20.1

# verify what actually loaded
/cdn version
/cdn backend`,
  },
  {
    title: 'Tune the animation',
    body: 'Every value is live. Edit, reload, and the next hit uses the new curve — no restart.',
    lang: 'yaml',
    code: `animation:
  duration-ticks: 30
  rise-ticks: 10
  vertical-speed: 0.08
  horizontal-randomness: 0.20
  scale-animation: true
  start-scale: 1.3
  end-scale: 0.8
  fade-out: true
  fade-start: 0.6
  bounce: true
  bounce-strength: 0.35
  rotation: false
  random-offset: true
  spawn-spread: 0.20

# /cdn reload`,
  },
  {
    title: 'Keep numbers on the mob',
    body: 'follow-entity anchors the number to the victim; follow-smoothing removes the jitter when the target is moving fast.',
    lang: 'yaml',
    code: `animation:
  follow-entity: true       # number travels with the damaged entity
  anchor-height: 1.8        # above the entity's feet
  follow-smoothing: 0.35    # 0 = snap each tick, higher = softer

# see it: /cdn test, then hit a mob and watch it move`,
  },
  {
    title: 'Stop a farm from flooding the screen',
    body: 'Merging folds rapid hits into one growing number; the caps protect the server if a farm gets out of hand.',
    lang: 'yaml',
    code: `merge-system:
  enabled: true
  merge-window-ms: 150
  max-merged-damage: 9999
  same-attacker-only: true

general:
  max-active-displays: 2000

performance:
  max-per-player: 50
  animation-interval: 1     # raise to 2 on very busy servers`,
  },
  {
    title: 'Give a rank its own style',
    body: 'Style profiles override individual styles. Hand one out with a permission instead of editing config per player.',
    lang: 'yaml',
    code: `style-profiles:
  fortnite:
    critical:
      format: "{damage}"
      color: "#FFFF00"
  mmo:
    normal:
      format: "{damage} DMG"
    critical:
      format: "{damage} CRIT"
      color: "#FF5555"

# then, in LuckPerms:
#   /lp group vip permission set cdn.style.mmo true
# and the player runs:
#   /cdn style mmo`,
  },
  {
    title: 'Custom colours and formats',
    body: 'A colour inside the format wins; the color setting is the default when the format does not set one. Hex, MiniMessage names and legacy codes all work.',
    lang: 'yaml',
    code: `styles:
  normal:
    enabled: true
    format: "{damage}"
    color: "#FFFFFF"
    bold: true
    shadow: true          # modern jar only
  critical:
    enabled: true
    format: "✧ {damage}"
    color: "#FF3333"
  fire:
    enabled: true
    format: "🔥 {damage}"
    color: "#FF9900"
  healing:
    enabled: true
    format: "+{damage}"
    color: "#00FF99"`,
  },
  {
    title: 'Let players turn it off',
    body: 'Numbers are cosmetic; a player who finds them noisy should be able to opt out without opening a ticket.',
    lang: 'bash',
    code: `/cdn toggle             # flip your own numbers
/cdn toggle off         # unambiguous
/cdn toggle off Steve   # staff, with cdn.toggle.others

# how many opted out? it is on the statistics panel
/cdn stats`,
  },
  {
    title: 'Test without hitting anything',
    body: 'A test display exercises the whole pipeline — backend, animation, viewers — with no damage event involved.',
    lang: 'bash',
    code: `/cdn test                 # one default display
/cdn test critical 25     # a critical style, value 25
/cdn test fire            # a specific style

/cdn debug on             # log every packet while you poke at it
/cdn clear                # wipe the live displays immediately`,
  },
];

export default function Examples() {
  return (
    <main className="mx-auto max-w-[1080px] px-6 pb-20 pt-9">
      <p className="mono-label mb-3">CustomDamageNumbers · recipes</p>
      <h1 className="text-[clamp(1.9em,3.6vw,2.6em)] font-extrabold leading-tight tracking-[-0.02em]">
        <span className="grad-text">Examples</span>
      </h1>
      <p className="mt-3 max-w-[760px] text-[1.04em] leading-[1.75] text-muted">
        Copy-paste recipes for the things people actually configure: the two jars, the animation
        curve, keeping numbers on the mob, surviving a mob farm, and handing a rank its own style.
      </p>

      <div className="mt-10 space-y-4">
        {RECIPES.map((r) => (
          <section key={r.title} className="glass-card !gap-2">
            <h2 className="text-[1.05em] font-bold text-white">{r.title}</h2>
            <p className="text-[0.86em] leading-[1.6] text-muted">{r.body}</p>
            <div className="mt-2">
              <Code content={r.code} lang={r.lang} />
            </div>
          </section>
        ))}
      </div>

      <section className="glass-card mt-10 !p-8">
        <h2 className="text-[1.15em] font-bold text-white">Where to go next</h2>
        <ul className="mt-2 space-y-1.5 text-[0.92em] text-ink2">
          <li>
            <a className="text-link hover:text-linkh" href="/docs#configuration">The full config reference</a> — every key and its default.
          </li>
          <li>
            <a className="text-link hover:text-linkh" href="/docs#troubleshooting">Troubleshooting</a> — nothing appears, nothing moves, and the jar guard.
          </li>
          <li>
            <a className="text-link hover:text-linkh" href="/docs#performance">Performance and memory</a> — what the audit measured.
          </li>
        </ul>
      </section>
    </main>
  );
}
