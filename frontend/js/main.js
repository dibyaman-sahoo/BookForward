import { $, html, mount, toast, esc } from './ui.js';
import { auth, restoreSession, onAuthChange, logout, clearSession, isStaff } from './auth.js';
import { api } from './api.js';
import * as ws from './ws.js';
import { go, parseHash } from './nav.js';
import { setRender } from './nav.js';

const routes = [
  [/^\/?$/, () => import('./pages/home.js')],
  [/^\/browse$/, () => import('./pages/browse.js')],
  [/^\/listing\/([\w-]+)$/, () => import('./pages/listing.js')],
  [/^\/sell(?:\/([\w-]+))?$/, () => import('./pages/sell.js'), { auth: true }],
  [/^\/login$/, () => import('./pages/login.js')],
  [/^\/register$/, () => import('./pages/login.js')],
  [/^\/saved$/, () => import('./pages/saved.js'), { auth: true }],
  [/^\/dashboard$/, () => import('./pages/dashboard.js'), { auth: true }],
  [/^\/messages$/, () => import('./pages/messages.js'), { auth: true }],
  [/^\/notifications$/, () => import('./pages/notifications.js'), { auth: true }],
  [/^\/profile$/, () => import('./pages/profile.js'), { auth: true }],
  [/^\/admin$/, () => import('./pages/admin.js'), { auth: true, staff: true }],
];

const app = $('#app');
let cleanup = null;
let unread = 0;

async function render() {
  cleanup?.(); cleanup = null;
  const { path, query } = parseHash();
  renderNav(path);
  for (const [re, loader, opts = {}] of routes) {
    const m = path.match(re);
    if (!m) continue;
    if (opts.auth && !auth.user) { sessionStorage.setItem('bf.next', location.hash); toast('Please sign in to continue', 'error'); return go('#/login'); }
    if (opts.staff && !isStaff()) { toast('You do not have access to that page', 'error'); return go('#/'); }
    app.innerHTML = '<div class="skeleton" style="min-height:60vh"></div>';
    try {
      const mod = await loader();
      app.innerHTML = '';
      app.classList.remove('page-enter'); void app.offsetWidth; app.classList.add('page-enter');
      cleanup = (await mod.default(app, { params: m.slice(1), query, path })) || null;
      document.title = (app.dataset.title ? app.dataset.title + ' · ' : '') + 'BookForward';
      window.scrollTo({ top: 0 });
      app.focus({ preventScroll: true });
    } catch (e) {
      console.error(e);
      mount(app, html`<div class="empty card"><h2>Something went wrong</h2><p>${e.message || 'Please try again.'}</p><a class="btn" href="#/">Back home</a></div>`);
    }
    return;
  }
  mount(app, html`<div class="empty card"><h1>404</h1><p>That page does not exist.</p><a class="btn primary" href="#/browse">Browse books</a></div>`);
}

function renderNav(path) {
  const link = (href, text, extra = '') => html`<a href="#${href}" class="${path === href ? 'active' : ''}" ${extra}>${text}</a>`;
  const u = auth.user;
  mount($('#navLinks'), html`
    ${link('/', 'Home')}
    ${link('/browse', 'Browse')}
    ${u ? html`
      ${link('/sell', 'Sell a book')}${link('/saved', 'Saved')}${link('/dashboard', 'Dashboard')}${link('/messages', 'Messages')}
      <a href="#/notifications" class="${path === '/notifications' ? 'active' : ''}" aria-label="Notifications">🔔${unread ? html`<span class="badge-dot">${unread > 9 ? '9+' : unread}</span>` : ''}</a>
      ${isStaff() ? link('/admin', 'Moderation') : ''}
      ${link('/profile', html`<span class="ws-dot ${ws.wsState.status}" title="Live connection: ${ws.wsState.status}"></span>${u.displayName.split(' ')[0]}`)}
      <button class="link" id="logoutBtn">Sign out</button>` : html`${link('/login', 'Sign in')}<a class="btn primary sm" href="#/register">Join free</a>`}`);
  $('#logoutBtn')?.addEventListener('click', async () => { await logout(); toast('Signed out'); go('#/'); });
  $('#navLinks').classList.remove('open'); $('#menuBtn').setAttribute('aria-expanded', 'false');
}

async function refreshUnread() {
  if (!auth.user) { unread = 0; return; }
  try { unread = (await api('/api/notifications/unread-count')).unread; } catch { /* ignore */ }
  renderNav(parseHash().path);
}

$('#menuBtn').addEventListener('click', () => {
  const nav = $('#navLinks'); const open = nav.classList.toggle('open');
  $('#menuBtn').setAttribute('aria-expanded', String(open));
});
setRender(render);
window.addEventListener('hashchange', render);
window.addEventListener('auth:expired', () => {
  if (!auth.user) return;
  clearSession(); ws.disconnect(); toast('Your session has expired. Please sign in again.', 'error'); go('#/login');
});
ws.on('status', () => renderNav(parseHash().path));
ws.on('notification', (n) => { unread += 1; renderNav(parseHash().path); toast(`🔔 ${n.title}`); });
ws.on('reconnected', refreshUnread);
onAuthChange((u) => { if (u) { ws.connect(); refreshUnread(); } else { ws.disconnect(); unread = 0; } });
window.addEventListener('notifications:changed', refreshUnread);

await restoreSession();
if (auth.user) { ws.connect(); refreshUnread(); }
render();
