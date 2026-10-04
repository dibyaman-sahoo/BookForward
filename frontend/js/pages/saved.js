import { html, mount, $, $$, attachTilt } from '../ui.js';
import { api } from '../api.js';
import { bookCard, wireHearts, pager, errorBox } from '../components.js';

export default async function (root) {
  root.dataset.title = 'Saved books';
  let page = 0;
  mount(root, html`<h1>Saved books</h1><div id="out" class="skeleton"></div>`);
  async function load() {
    try {
      const res = await api('/api/saved', { query: { page, size: 12 } });
      mount($('#out', root), res.items.length ? html`<div class="grid">${res.items.map((b) => html`<div>${bookCard(b)}${b.status !== 'ACTIVE' && b.status !== 'RESERVED' ? html`<p class="muted small">No longer available</p>` : ''}</div>`)}</div>${pager(res.page, res.totalPages)}`
        : html`<div class="empty card">Nothing saved yet. Tap ♡ on any book to keep it here.<br><a class="btn primary" style="margin-top:1rem" href="#/browse">Browse books</a></div>`);
      $('#out', root).classList.remove('skeleton');
      wireHearts($('#out', root), { onChange: load }); attachTilt($('#out', root));
      $$('[data-page]', root).forEach((b) => b.addEventListener('click', () => { page = Number(b.dataset.page); load(); }));
    } catch (e) { mount($('#out', root), errorBox(e)); }
  }
  load();
}
