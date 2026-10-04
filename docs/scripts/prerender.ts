// Static generation for every route: each page is rendered to real HTML so direct
// refreshes, crawlers and no-JS visitors get content on first response.
// Runs after `vite build`; no extra dependencies (react-dom + react-router only).
import { createServer } from 'vite';
import { renderToString } from 'react-dom/server';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import React from 'react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import App from '../src/App';
import Home from '../src/pages/Home';
import Docs from '../src/pages/Docs';
import Examples from '../src/pages/Examples';
import About from '../src/pages/About';
import License from '../src/pages/License';

const e = React.createElement;

const CANON = 'https://cdn.docs.potenfyr.in';

interface Page {
  path: string;
  file: string;
  title: string;
  description: string;
}

const pages: Page[] = [
  {
    path: '/',
    file: 'index.html',
    title: 'CustomDamageNumbers: packet-level floating damage numbers for Minecraft',
    description:
      'Floating damage numbers drawn entirely with packets, anchored to the damaged entity, animated from config. 1.17.x to 26.x from one codebase.',
  },
  {
    path: '/docs',
    file: 'docs/index.html',
    title: 'Documentation | CustomDamageNumbers',
    description:
      'Every command, config key and behaviour: the two jars, the animation curve, hit merging, style profiles, player control and the memory audit.',
  },
  {
    path: '/examples',
    file: 'examples/index.html',
    title: 'Examples | CustomDamageNumbers',
    description:
      'Copy-paste recipes: install the right jar, tune the animation, keep numbers on the mob, survive a mob farm and give a rank its own style.',
  },
  {
    path: '/about',
    file: 'about/index.html',
    title: 'About | CustomDamageNumbers',
    description:
      'Why CustomDamageNumbers renders with packets instead of entities, why there are two jars, and how the rendering path is verified against real servers.',
  },
  {
    path: '/license',
    file: 'license/index.html',
    title: 'License | CustomDamageNumbers',
    description:
      'CustomDamageNumbers is Apache-2.0 with the Commons Clause: free to run, fork and build on, even on a commercial server. Only reselling the plugin is off limits.',
  },
];

/** silence the useLayoutEffect SSR warning: no client layout to replay on the server */
const originalWarn = console.error;
console.error = (...args: unknown[]) => {
  if (typeof args[0] === 'string' && args[0].includes('useLayoutEffect does nothing on the server')) {
    return;
  }
  originalWarn(...args);
};

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');

const template = await readFile(resolve(root, 'dist/index.html'), 'utf8');

/** renders one route to markup with the same nesting main.tsx uses */
function markup(path: string): string {
  return renderToString(
    e(
      MemoryRouter,
      { initialEntries: [path] },
      e(
        Routes,
        null,
        e(
          Route,
          { path: '/', element: e(App) },
          e(Route, { index: true, element: e(Home) }),
          e(Route, { path: 'docs', element: e(Docs) }),
          e(Route, { path: 'examples', element: e(Examples) }),
          e(Route, { path: 'about', element: e(About) }),
          e(Route, { path: 'license', element: e(License) }),
        ),
      ),
    ),
  );
}

/** swaps the per-route head tags into the built shell */
function withHead(html: string, page: Page): string {
  const url = page.path === '/' ? `${CANON}/` : `${CANON}${page.path}`;

  return html
    .replace(/<title>.*?<\/title>/s, `<title>${page.title}</title>`)
    .replace(/(<meta name="title" content=").*?(")/s, `$1${page.title}$2`)
    .replace(/(<meta name="description" content=").*?(")/s, `$1${page.description}$2`)
    .replace(/(<meta property="og:title" content=").*?(")/s, `$1${page.title}$2`)
    .replace(/(<meta property="og:description" content=").*?(")/s, `$1${page.description}$2`)
    .replace(/(<meta name="twitter:title" content=").*?(")/s, `$1${page.title}$2`)
    .replace(/(<meta name="twitter:description" content=").*?(")/s, `$1${page.description}$2`)
    .replace(/(<link rel="canonical" href=").*?(")/s, `$1${url}$2`)
    .replace(/(<meta property="og:url" content=").*?(")/s, `$1${url}$2`);
}

let emitted = 0;

for (const page of pages) {
  const html = withHead(template, page).replace(
    '<div id="root"></div>',
    `<div id="root">${markup(page.path)}</div>`,
  );

  const target = resolve(root, 'dist', page.file);
  await mkdir(dirname(target), { recursive: true });
  await writeFile(target, html, 'utf8');
  emitted += 1;
  console.log(`[prerender] ${page.path} -> dist/${page.file}`);
}

const sitemap = [
  '<?xml version="1.0" encoding="UTF-8"?>',
  '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">',
  ...pages.map((page) => {
    const url = page.path === '/' ? `${CANON}/` : `${CANON}${page.path}`;
    return `  <url><loc>${url}</loc><changefreq>weekly</changefreq><priority>${page.path === '/' ? '1.0' : '0.7'}</priority></url>`;
  }),
  '</urlset>',
  '',
].join('\n');

await writeFile(resolve(root, 'dist/sitemap.xml'), sitemap, 'utf8');

console.log(`[prerender] ${emitted} routes + sitemap.xml`);
