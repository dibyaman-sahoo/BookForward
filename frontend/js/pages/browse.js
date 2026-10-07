import { html, mount, $, $$, debounce, attachTilt, label } from '../ui.js';
import { api } from '../api.js';
import { bookCard, wireHearts, pager, skeletonGrid, options, LEVELS, CONDITIONS, errorBox } from '../components.js';
import { go } from '../nav.js';

const SORTS = [['relevance', 'Best match'], ['newest', 'Newest'], ['priceAsc', 'Price: low to high'], ['priceDesc', 'Price: high to low'], ['nearest', 'Nearest first']];

export default async function (root, { query }) {
  root.dataset.title = 'Browse';
  const q = { query: '', category: '', level: '', board: '', city: '', nearLat: '', nearLon: '', condition: '', minPrice: '', maxPrice: '', availability: 'available', sort: 'relevance', page: 0, ...query };
  q.page = Number(q.page) || 0;
  const LOC_KEY = 'bf_location';
  const loadLoc = () => { try { return JSON.parse(localStorage.getItem(LOC_KEY)); } catch { return null; } };
  const saveLoc = (v) => { try { v ? localStorage.setItem(LOC_KEY, JSON.stringify(v)) : localStorage.removeItem(LOC_KEY); } catch { /* ignore */ } };
  const saved = loadLoc();
  // First visit with no search/sort in the URL: show the books nearest to the remembered location.
  if (saved && !query?.sort && !query?.query && !q.nearLat) { q.nearLat = saved.lat; q.nearLon = saved.lon; q.sort = 'nearest'; }
  let cats = [];
  try { cats = await api('/api/categories', { auth: false }); } catch { /* filters still work without category list */ }
  let seq = 0;

  mount(root, html`
    <div class="row between" style="margin-bottom:1rem"><h1 style="margin:0;font-size:2rem">Browse books</h1>
      <button class="btn filter-toggle" id="ft" aria-expanded="false">Filters</button></div>
    <div class="browse">
      <aside class="card filters" id="filters" aria-label="Filters"><form id="ff" class="stack">
        <div class="field"><label for="f-query">Search</label><input id="f-query" name="query" type="search" placeholder="Title, author, publisher…" value="${q.query}"></div>
        <div class="field"><label for="f-city">City</label><input id="f-city" name="city" placeholder="Any city" value="${q.city}"><div class="row" style="gap:.5rem;margin-top:.4rem"><button type="button" class="btn sm" id="near">📍 Near me</button><button type="button" class="btn sm ghost" id="clearLoc" style="display:none">Clear</button></div><div class="muted small" id="nearMsg"></div><input type="hidden" name="nearLat" value="${q.nearLat}"><input type="hidden" name="nearLon" value="${q.nearLon}"></div>
        <div class="field"><label for="f-level">Academic level</label><select id="f-level" name="level">${options(LEVELS, q.level, 'Any level')}</select></div>
        <div class="field"><label for="f-category">Subject</label><select id="f-category" name="category"><option value="">All subjects</option>${cats.map((c) => html`<option value="${c.slug}" ${c.slug === q.category ? 'selected' : ''}>${c.name}</option>`)}</select></div>
        <div class="field"><label for="f-board">Board / context</label><input id="f-board" name="board" value="${q.board}"></div>
        <div class="field"><label for="f-condition">Condition</label><select id="f-condition" name="condition">${options(CONDITIONS, q.condition, 'Any condition')}</select></div>
        <div class="form-grid"><div class="field"><label for="f-min">Min ₹</label><input id="f-min" name="minPrice" type="number" min="0" inputmode="numeric" value="${q.minPrice}"></div><div class="field"><label for="f-max">Max ₹</label><input id="f-max" name="maxPrice" type="number" min="0" inputmode="numeric" value="${q.maxPrice}"></div></div>
        <div class="field"><label for="f-av">Availability</label><select id="f-av" name="availability"><option value="available" ${q.availability === 'available' ? 'selected' : ''}>Available now</option><option value="all" ${q.availability === 'all' ? 'selected' : ''}>Include reserved & sold</option></select></div>
        <div class="field"><label for="f-sort">Sort by</label><select id="f-sort" name="sort">${SORTS.map(([v, t]) => html`<option value="${v}" ${v === q.sort ? 'selected' : ''}>${t}</option>`)}</select></div>
        <button type="button" class="btn ghost" id="reset">Reset filters</button></form></aside>
      <section><div id="count" class="muted small" style="margin-bottom:.6rem" aria-live="polite"></div><div id="results">${skeletonGrid(6)}</div></section>
    </div>`);

  const form = $('#ff', root);
  const read = () => {
    const f = new FormData(form);
    return { query: f.get('query').trim(), category: f.get('category'), level: f.get('level'), board: f.get('board').trim(), city: f.get('city').trim(), nearLat: f.get('nearLat'), nearLon: f.get('nearLon'), condition: f.get('condition'), minPrice: f.get('minPrice'), maxPrice: f.get('maxPrice'), availability: f.get('availability'), sort: f.get('sort') };
  };
  const setNear = (lat, lon) => { form.nearLat.value = lat; form.nearLon.value = lon; };
  const syncNear = () => {
    const on = !!form.nearLat.value;
    $('#clearLoc', root).style.display = on ? '' : 'none';
    $('#nearMsg', root).textContent = on ? 'Showing nearest books first.' : '';
  };
  function locate() {
    const msg = $('#nearMsg', root);
    if (!navigator.geolocation) { msg.textContent = 'Your browser cannot detect location. Type a city instead.'; return; }
    msg.textContent = 'Detecting…';
    navigator.geolocation.getCurrentPosition((pos) => {
      const lat = pos.coords.latitude, lon = pos.coords.longitude;
      setNear(lat, lon); saveLoc({ lat, lon });
      form.sort.value = 'nearest'; syncNear(); reload();
    }, () => { msg.textContent = 'Location permission was denied. Type a city instead.'; if (form.sort.value === 'nearest') form.sort.value = 'relevance'; }, { timeout: 15000 });
  }
  async function load() {
    const mine = ++seq;
    const cur = { ...read(), page: q.page };
    const url = Object.entries(cur).filter(([k, v]) => v !== '' && v !== 0 && !(k === 'availability' && v === 'available') && !(k === 'sort' && v === 'relevance')).map(([k, v]) => `${k}=${encodeURIComponent(v)}`).join('&');
    history.replaceState(null, '', '#/browse' + (url ? '?' + url : ''));
    $('#results', root).setAttribute('aria-busy', 'true');
    try {
      const res = await api('/api/search/listings', { query: { ...cur, size: 12 }, auth: false });
      if (mine !== seq) return; // a newer request superseded this one
      $('#count', root).textContent = `${res.totalItems} book${res.totalItems === 1 ? '' : 's'} found`;
      mount($('#results', root), res.items.length ? html`<div class="grid">${res.items.map(bookCard)}</div>${pager(res.page, res.totalPages)}`
        : html`<div class="empty card"><h3>No books match those filters</h3><p>Try fewer filters or a different search term.</p></div>`);
      wireHearts($('#results', root)); attachTilt($('#results', root));
      $$('[data-page]', root).forEach((b) => b.addEventListener('click', () => { q.page = Number(b.dataset.page); load(); window.scrollTo({ top: 0, behavior: 'smooth' }); }));
    } catch (e) { if (mine === seq) mount($('#results', root), errorBox(e)); }
  }
  const reload = () => { q.page = 0; load(); };
  const debounced = debounce(reload, 350);
  form.addEventListener('submit', (e) => e.preventDefault());
  $('#near', root).addEventListener('click', locate);
  $('#clearLoc', root).addEventListener('click', () => { setNear('', ''); saveLoc(null); if (form.sort.value === 'nearest') form.sort.value = 'relevance'; syncNear(); reload(); });
  form.sort.addEventListener('change', () => { if (form.sort.value === 'nearest' && !form.nearLat.value) locate(); });
  syncNear();
  form.addEventListener('input', (e) => (e.target.type === 'search' || e.target.type === 'number' || e.target.name === 'board' || e.target.name === 'city') ? debounced() : reload());
  $('#reset', root).addEventListener('click', () => go('#/browse'));
  $('#ft', root).addEventListener('click', (e) => { const o = $('#filters', root).classList.toggle('open'); e.currentTarget.setAttribute('aria-expanded', String(o)); });
  load();
}
