import { html, mount, $, $$, dateTime } from '../ui.js';
import { api } from '../api.js';
import * as ws from '../ws.js';
import { errorBox } from '../components.js';

export default async function (root) {
  root.dataset.title = 'Notifications';
  async function load() {
    try {
      const res = await api('/api/notifications', { query: { size: 50 } });
      mount(root, html`<div class="row between"><h1>Notifications</h1><button class="btn sm" id="all">Mark all read</button></div>
        ${res.items.length ? html`<div class="stack">${res.items.map((n) => html`<a class="card row between" style="color:inherit;${n.seen ? 'opacity:.7' : 'border-color:var(--accent)'}" href="${n.link || '#/notifications'}" data-id="${n.id}">
          <div><strong>${n.title}</strong><div class="muted small">${n.body || ''}</div></div><span class="muted small">${dateTime(n.createdAt)}</span></a>`)}</div>`
        : html`<div class="empty card">You're all caught up.</div>`}`);
      $('#all', root)?.addEventListener('click', async () => { await api('/api/notifications/read-all', { method: 'POST' }); window.dispatchEvent(new Event('notifications:changed')); load(); });
      $$('[data-id]', root).forEach((a) => a.addEventListener('click', () => { api(`/api/notifications/${a.dataset.id}/read`, { method: 'PATCH' }).then(() => window.dispatchEvent(new Event('notifications:changed'))).catch(() => {}); }));
    } catch (e) { mount(root, errorBox(e)); }
  }
  await load();
  const off = [ws.on('notification', load), ws.on('reconnected', load)];
  return () => off.forEach((f) => f());
}
