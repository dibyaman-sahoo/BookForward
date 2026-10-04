// Hash-router helpers shared by main.js and pages (kept separate to avoid circular imports).
let renderFn = () => {};
export const setRender = (fn) => { renderFn = fn; };
export function parseHash() {
  const raw = location.hash.slice(1) || '/';
  const [path, qs] = raw.split('?');
  return { path, query: Object.fromEntries(new URLSearchParams(qs || '')) };
}
export const go = (hash) => { if (location.hash === hash) renderFn(); else location.hash = hash; };
