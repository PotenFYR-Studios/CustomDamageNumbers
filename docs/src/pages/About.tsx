export default function About() {
  return (
    <main className="mx-auto max-w-[820px] px-6 pb-20 pt-9">
      <p className="mono-label mb-3">CustomDamageNumbers · about</p>
      <h1 className="text-[clamp(1.9em,3.6vw,2.6em)] font-extrabold leading-tight tracking-[-0.02em]">
        <span className="grad-text">About</span>
      </h1>
      <p className="mt-3 text-[1.04em] leading-[1.75] text-muted">
        CustomDamageNumbers is a Minecraft plugin by PotenFYR Studios that draws floating damage
        numbers with packets instead of entities. It is the 1.0.0 release of a plugin that started
        as a small cosmetic add-on and grew into a multi-version rendering layer.
      </p>

      <h2 className="mb-3 mt-10 border-b pb-[0.35em] text-[1.32em] font-bold tracking-[-0.015em] text-white" style={{ borderColor: 'var(--line)' }}>
        The design decisions
      </h2>
      <ul className="space-y-2 text-[1.02em] leading-[1.7] text-ink2">
        <li>
          <strong className="text-white">Packets, never entities.</strong> A display entity in the
          world is a real entity: it can be pushed, saved, duplicated by a bad chunk save, or left
          behind when the plugin is removed. Sending it over the protocol means the world state
          never changes, and uninstalling is genuinely clean.
        </li>
        <li>
          <strong className="text-white">Two jars, one codebase.</strong> The text display entity
          arrived in 1.19.4 and became animatable per-entity in 1.20.2. Rather than pretend one
          renderer can do everything, the plugin ships a modern jar and a legacy jar behind a single
          interface; the rest of the plugin is identical.
        </li>
        <li>
          <strong className="text-white">Compile against the oldest API.</strong> The build targets
          the oldest supported server API, so a Paper-only class cannot accidentally leak into a jar
          that is supposed to run on Spigot.
        </li>
        <li>
          <strong className="text-white">The animation belongs to the entity.</strong> Numbers are
          anchored above the victim and re-positioned every step, because a number that stays where
          the hit happened looks broken the moment anything moves.
        </li>
        <li>
          <strong className="text-white">Bounded by construction.</strong> Displays, viewer slots,
          merge entries and the debug listener all have explicit caps and destroy paths. The audit
          that produced 1.0.0 was mostly about removing the ways those maps could grow forever.
        </li>
      </ul>

      <h2 className="mb-3 mt-10 border-b pb-[0.35em] text-[1.32em] font-bold tracking-[-0.015em] text-white" style={{ borderColor: 'var(--line)' }}>
        Verified, not asserted
      </h2>
      <p className="text-[1.02em] leading-[1.7] text-ink2">
        The build runs its unit suite on every push, and the rendering path is exercised end to end
        against real servers in Docker: Paper 1.17.1 and 1.20.1 with the legacy jar, 1.21.8 and 26.3
        with the modern jar. The end-to-end probe checks the display is created, positioned, moved
        across the animation, and destroyed — and that the plugin disables cleanly with no errors in
        the server log.
      </p>

      <h2 className="mb-3 mt-10 border-b pb-[0.35em] text-[1.32em] font-bold tracking-[-0.015em] text-white" style={{ borderColor: 'var(--line)' }}>
        PotenFYR Studios
      </h2>
      <p className="text-[1.02em] leading-[1.7] text-ink2">
        CustomDamageNumbers is built and maintained by{' '}
        <a className="text-link hover:text-linkh" href="https://potenfyr.in" target="_blank" rel="noopener noreferrer">PotenFYR Studios</a>. Issues,
        questions and feature requests belong in the{' '}
        <a className="text-link hover:text-linkh" href="https://github.com/PotenFYR-Studios/CustomDamageNumbers/issues" target="_blank" rel="noopener noreferrer">GitHub issues</a>{' '}
        — or the{' '}
        <a className="text-link hover:text-linkh" href="https://discord.com/invite/zUaN2FPBec" target="_blank" rel="noopener noreferrer">support Discord</a>.
      </p>
      <ul className="mt-4 space-y-2 text-[0.95em] text-ink2">
        <li>
          <a className="text-link hover:text-linkh" href="https://github.com/PotenFYR-Studios/CustomDamageNumbers" target="_blank" rel="noopener noreferrer">Source on GitHub</a>
        </li>
        <li>
          <a className="text-link hover:text-linkh" href="https://github.com/PotenFYR-Studios/CustomDamageNumbers/releases" target="_blank" rel="noopener noreferrer">Releases and changelog</a>
        </li>
        <li>
          <a className="text-link hover:text-linkh" href="/license">License</a> — Apache-2.0 with the Commons Clause
        </li>
      </ul>
    </main>
  );
}
