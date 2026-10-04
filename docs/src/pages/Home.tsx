import { Link } from 'react-router-dom';
import Code from '../components/Code';

const FEATURES = [
  {
    icon: '💥',
    title: 'Packet-only rendering',
    body: 'Nothing is spawned into the world. Every number is a client-side display entity sent over the protocol, so removing the plugin leaves nothing behind.',
  },
  {
    icon: '🎯',
    title: 'Anchored to the entity',
    body: 'The number belongs to the victim. Knock a mob backwards and its number travels with it, with configurable follow smoothing instead of jitter.',
  },
  {
    icon: '🎛️',
    title: 'Config-driven animation',
    body: 'Rise, fall, bounce, orbit, scale, fade, spawn spread and anchor height are all values in config.yml. Change a number, /cdn reload, watch it move.',
  },
  {
    icon: '🔀',
    title: 'Hit merging',
    body: 'Ten hits in half a second grow one number instead of stacking ten. The window, the cap and the attacker rules are configurable.',
  },
  {
    icon: '🙈',
    title: 'Players choose',
    body: '/cdn toggle switches numbers off for that player alone and survives restarts. /cdn style picks a profile when their rank allows one.',
  },
  {
    icon: '🧭',
    title: '1.17.x to 26.x',
    body: 'Two jars from one codebase: a modern text display renderer and a legacy armour stand renderer, compiled against the oldest supported API.',
  },
];

/** the renderers and the guarantees, not a list of other plugins */
const MARQUEE = [
  'no entities spawned', 'packet-level', 'text display renderer', 'legacy armour stand renderer',
  'entity-anchored', 'config-driven animation', 'hit merging', 'critical styles', 'particles',
  'per-player toggle', 'style profiles', 'luckperms aware', 'placeholderapi ready',
  'folia supported', 'distance culling', 'bounded memory', 'ascii console frame', 'framed chat ui',
];

const QUICKSTART = `# plugins/
#   PacketEvents-2.14.0.jar
#   CustomDamageNumbers-1.0.0.jar        # 1.20.2 - 26.x
#   CustomDamageNumbers-Legacy-1.0.0.jar # 1.17.x - 1.20.1

# config.yml - the animation block is live, not decorative
animation:
  duration-ticks: 30
  rise-ticks: 10
  vertical-speed: 0.08
  scale-animation: true
  fade-out: true
  follow-entity: true      # the number stays on the damaged entity
  anchor-height: 1.8
  follow-smoothing: 0.35

merge-system:
  enabled: true
  merge-window-ms: 150     # rapid hits grow one number`;

export default function Home() {
  return (
    <main>
      {/* HERO: glow orbs + vivid 90deg display gradient are landing-only (SPEC 5.14, appendix) */}
      <section className="relative overflow-hidden pb-16 pt-[72px] text-center">
        <div className="pointer-events-none absolute -top-48 left-1/2 h-[30rem] w-[52rem] -translate-x-1/2 rounded-full blur-3xl" style={{ background: 'rgba(139, 92, 246, 0.18)' }} />
        <div className="pointer-events-none absolute -left-32 top-40 h-[31rem] w-[31rem] rounded-full blur-3xl" style={{ background: 'rgba(236, 72, 153, 0.15)' }} />
        <div className="pointer-events-none absolute -right-32 top-64 h-[26rem] w-[26rem] rounded-full blur-3xl" style={{ background: 'rgba(6, 182, 212, 0.12)' }} />

        <div className="relative mx-auto max-w-4xl px-6">
          <div
            className="mb-6 inline-flex items-center gap-2 rounded-full px-3.5 py-1.5 font-mono text-[0.75em] text-muted backdrop-blur-md"
            style={{ border: '1px solid rgba(255,255,255,0.08)', background: 'rgba(255,255,255,0.03)' }}
          >
            <span className="h-2 w-2 rounded-full bg-emerald-400" style={{ boxShadow: '0 0 8px rgba(16,185,129,.5)' }} />
            v1.0.0 · 1.17.x → 26.x · two jars, one codebase
          </div>

          <h1 className="text-[clamp(2.6em,6vw,4em)] font-extrabold leading-[1.08] tracking-tight">
            Damage numbers.
            <br />
            <span className="grad-text-vivid">Nothing spawned.</span>
          </h1>

          <p className="mx-auto mt-6 max-w-[720px] text-[1.04em] leading-[1.75] text-muted">
            Floating damage numbers drawn entirely with packets. Anchored to the entity that took
            the hit, animated from config, merged when hits come fast, and switchable by the players
            who find them noisy.
          </p>

          <div className="mt-10 flex flex-wrap items-center justify-center gap-4">
            <Link to="/docs" className="btn-primary">
              Get started
            </Link>
            <Link to="/examples" className="btn-ghost">
              See examples
            </Link>
          </div>

          <div className="mx-auto mt-10 flex max-w-2xl flex-col items-stretch gap-2 sm:flex-row sm:items-center sm:justify-between">
            <div
              className="flex items-center gap-2 overflow-hidden rounded-xl px-5 py-3.5 font-mono text-sm backdrop-blur"
              style={{ border: '1px solid var(--line)', background: '#151828' }}
            >
              <span className="shrink-0 text-accent">$</span>
              <code className="truncate text-ink2">/cdn version</code>
            </div>
            <span
              className="shrink-0 rounded-xl px-4 py-3.5 text-center font-mono text-xs text-muted backdrop-blur"
              style={{ border: '1px solid rgba(255,255,255,0.08)', background: 'rgba(255,255,255,0.03)' }}
            >
              run it after install
            </span>
          </div>
        </div>
      </section>

      {/* MARQUEE: mono micro-labels, pause on hover (SPEC 7) */}
      <section className="overflow-hidden border-y border-white/[0.06] bg-white/[0.02] py-5">
        <div className="flex w-max animate-marquee gap-10 px-5">
          {[...MARQUEE, ...MARQUEE].map((label, i) => (
            <span key={i} className="flex items-center gap-2 whitespace-nowrap font-mono text-xs uppercase tracking-widest text-faint">
              <span className="text-accent">◆</span>
              {label}
            </span>
          ))}
        </div>
      </section>

      {/* LIVE NUMBERS: stat tiles (SPEC 5.6) */}
      <section className="mx-auto max-w-6xl px-6 py-20">
        <div className="mx-auto grid max-w-3xl grid-cols-2 gap-3.5 sm:grid-cols-4">
          {[
            { value: '0', label: 'entities spawned', icon: '🫥', color: 'grad-text font-mono' },
            { value: '10', label: 'commands on /cdn', icon: '⌨️', color: 'text-[#8b5cf6]' },
            { value: '18', label: 'animation settings', icon: '🎛️', color: 'text-[#ec4899]' },
            { value: '26.x', label: 'newest server', icon: '🚀', color: 'text-[#f97316]' },
          ].map((s) => (
            <div key={s.label} className="stat-tile">
              <div className={`text-3xl font-extrabold font-mono ${s.color}`}>{s.value}</div>
              <div className="mt-1 text-[11.5px] uppercase tracking-[1.4px] text-muted">{s.label}</div>
            </div>
          ))}
        </div>
      </section>

      {/* CODE + FLOW */}
      <section className="mx-auto grid max-w-6xl items-start gap-8 px-6 pb-20 lg:grid-cols-[3fr_2fr]">
        <div>
          <p className="mono-label mb-2">Install</p>
          <h2 className="mb-4 text-3xl font-bold text-white">Two minutes, two files</h2>
          <p className="mb-6 text-muted">
            Drop the right jar next to PacketEvents and edit the animation block. Everything below
            is live the moment you run /cdn reload.
          </p>
          <Code content={QUICKSTART} lang="bash" />
        </div>

        <div className="glass-card flex flex-col items-center justify-center gap-4 !p-6">
          <h3 className="mb-2 text-lg font-bold text-white">Many hits become one number</h3>
          <div className="flex flex-wrap justify-center gap-2">
            {['hit 12', 'hit 9', 'hit 14', 'crit 31'].map((l) => (
              <span
                key={l}
                className="rounded-lg px-3 py-1.5 font-mono text-xs text-ink2"
                style={{ border: '1px solid rgba(255,255,255,0.08)', background: 'rgba(255,255,255,0.03)' }}
              >
                {l}
              </span>
            ))}
          </div>
          <div className="text-2xl text-accent">⇅</div>
          <div
            className="w-full max-w-xs rounded-xl px-8 py-4 text-center"
            style={{ border: '1px solid var(--line)', background: '#151828' }}
          >
            <span className="font-mono font-bold text-white">merge-system</span>
            <p className="text-xs text-faint">150ms window · same attacker</p>
          </div>
          <div className="text-2xl text-accent">↓</div>
          <div
            className="rounded-lg px-6 py-2 font-mono text-sm"
            style={{ border: '1px solid rgba(16,185,129,.3)', background: 'rgba(16,185,129,.12)', color: '#34d399' }}
          >
            66 · one number, anchored
          </div>
        </div>
      </section>

      {/* FEATURES: glass cards + beam (landing only, SPEC 11.7) */}
      <section className="mx-auto max-w-6xl px-6 pb-24">
        <p className="mono-label mb-2 text-center">Features</p>
        <h2 className="mb-10 text-center text-3xl font-bold text-white">
          Everything you need, <span className="grad-text">nothing you don't</span>
        </h2>
        <div className="grid gap-3.5 md:grid-cols-2 lg:grid-cols-3">
          {FEATURES.map((f) => (
            <div key={f.title} className="glass-card">
              <div className="icon-tile">{f.icon}</div>
              <h3 className="mt-1.5 text-[0.98em] font-bold text-white">{f.title}</h3>
              <p className="text-[0.83em] leading-[1.55] text-muted">{f.body}</p>
            </div>
          ))}
        </div>
      </section>

      {/* CTA */}
      <section className="mx-auto max-w-4xl px-6 pb-24">
        <div className="glass-card items-center !p-10 text-center">
          <h2 className="text-3xl font-extrabold text-white">Pick your jar and go</h2>
          <p className="mx-auto mt-3 max-w-md text-muted">
            Modern for 1.20.2 and newer, legacy for everything back to 1.17. Same config, same
            commands, same animation.
          </p>
          <Link to="/docs" className="btn-primary mt-8">
            Read the docs
          </Link>
        </div>
      </section>
    </main>
  );
}
