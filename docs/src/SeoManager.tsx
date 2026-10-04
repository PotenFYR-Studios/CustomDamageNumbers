import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';

const META: Record<string, { title: string; description: string }> = {
  '/': {
    title: 'CustomDamageNumbers: packet-level floating damage numbers for Minecraft',
    description: 'Floating damage numbers drawn entirely with packets, anchored to the damaged entity, animated from config. 1.17.x to 26.x from one codebase.',
  },
  '/docs': {
    title: 'Documentation | CustomDamageNumbers',
    description: 'Every command, config key and behaviour: the two jars, the animation curve, hit merging, style profiles, player control and the memory audit.',
  },
  '/examples': {
    title: 'Examples | CustomDamageNumbers',
    description: 'Copy-paste recipes: install the right jar, tune the animation, keep numbers on the mob, survive a mob farm and give a rank its own style.',
  },
  '/about': {
    title: 'About | CustomDamageNumbers',
    description: 'Why CustomDamageNumbers renders with packets instead of entities, why there are two jars, and how the rendering path is verified against real servers.',
  },
  '/license': {
    title: 'License | CustomDamageNumbers',
    description: 'CustomDamageNumbers is Apache-2.0 with the Commons Clause: free to run, fork and build on, even on a commercial server. Only reselling the plugin is off limits.',
  },
};

export default function SeoManager() {
  const location = useLocation();
  useEffect(() => {
    // custom domain: swap every absolute url to the live origin automatically.
    const GH = 'https://potenfyr-studios.github.io';
    if (!window.location.origin.startsWith(GH)) {
      const selectors = [
        'link[rel="canonical"]',
        'meta[property="og:url"]',
        'meta[property="og:image"]',
        'meta[name="twitter:image"]',
      ];
      for (const selector of selectors) {
        const tag = document.querySelector(selector);
        const attr = selector.startsWith('link') ? 'href' : 'content';
        if (tag) {
          const current = tag.getAttribute('href') ?? tag.getAttribute('content') ?? '';
          if (current.startsWith(GH)) {
            tag.setAttribute(attr, current.replace(GH, window.location.origin));
          }
        }
      }
    }
    // first path segment decides the page: /docs/introduction -> /docs
    const base = '/' + (location.pathname.split('/').filter(Boolean)[0] ?? '');
    const meta = META[base] ?? META['/'];
    document.title = meta.title;
    let tag = document.querySelector('meta[name="description"]');
    if (!tag) {
      tag = document.createElement('meta');
      tag.setAttribute('name', 'description');
      document.head.appendChild(tag);
    }
    tag.setAttribute('content', meta.description);
  }, [location.pathname]);
  return null;
}
