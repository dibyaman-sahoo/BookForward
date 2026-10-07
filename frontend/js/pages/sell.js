import { html, mount, $, $$, toast, showFieldErrors, label } from '../ui.js';
import { api, fileUrl } from '../api.js';
import { options, LEVELS, CONDITIONS } from '../components.js';
import { go } from '../nav.js';

const REQUIRED = [['FRONT_COVER', 'Front cover']];
const OPTIONAL = [['BACK_COVER', 'Back cover'], ['DETAILS_PAGE', 'Publication / details page'], ['INDEX_PAGE', 'Index / chapters page'], ['DAMAGE_PAGE', 'Damage (torn / missing page, if any)']];
const ALL_SLOTS = [...REQUIRED, ...OPTIONAL];
const OK_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

export default async function (root, { params }) {
  const editId = params[0];
  root.dataset.title = editId ? 'Edit listing' : 'Sell a book';
  const cats = await api('/api/categories', { auth: false });
  let d = null;
  if (editId) { try { d = await api(`/api/listings/${editId}`); if (!d.ownedByViewer) throw new Error('not owner'); } catch { toast('Listing not found', 'error'); return go('#/dashboard'); } }
  const s = d?.summary || {};
  const have = Object.fromEntries((d?.images || []).map((i) => [i.type, i.url]));
  const files = {};
  let currentId = editId; // set once a draft exists so retries after a failed upload do not create duplicates

  mount(root, html`<div class="card" style="max-width:860px;margin:auto"><h1 style="font-size:2rem">${editId ? 'Edit listing' : 'List a book'}</h1>
    <div id="msg"></div>
    <form id="f" novalidate>
      <div class="form-grid">
        <div class="field"><label for="title">Title *</label><input id="title" name="title" required maxlength="200" value="${s.title || ''}"></div>
        <div class="field"><label for="academicLevel">Academic level *</label><select id="academicLevel" name="academicLevel" required>${options(LEVELS, s.academicLevel, 'Choose…')}</select></div>
        <div class="field"><label for="categoryId">Subject *</label>
          <select id="categoryId" name="categoryId" required>
            <option value="">Choose…</option>
            ${cats.map((c) => html`<option value="${c.id}" ${c.id === d?.categoryId ? 'selected' : ''}>${c.name}</option>`)}
            <option value="OTHER">Other (type your own)</option>
          </select>
          <input id="categoryOther" name="categoryOther" maxlength="80" placeholder="e.g. Microbiology, Cell Biology, Biochemistry…" style="margin-top:.5rem;display:none">
        </div>
        <div class="field"><label for="author">Author</label><input id="author" name="author" maxlength="150" value="${s.author || ''}"></div>
        <div class="field"><label for="publisher">Publisher</label><input id="publisher" name="publisher" maxlength="150" value="${d?.publisher || ''}"></div>
        <div class="field"><label for="isbn">ISBN</label><input id="isbn" name="isbn" maxlength="20" value="${d?.isbn || ''}"></div>
        <div class="field"><label for="board">Board / context</label><input id="board" name="board" maxlength="50" value="${s.board || ''}"></div>
        <div class="field"><label for="bookCondition">Condition *</label><select id="bookCondition" name="bookCondition" required>${options(CONDITIONS, s.bookCondition, 'Choose…')}</select></div>
        <div class="field"><label for="price">Price (₹) *</label><input id="price" name="price" type="number" min="0" max="99999" step="1" inputmode="numeric" required value="${s.price ?? ''}"></div>
      </div>
      <div class="field"><label for="description">Description</label><textarea id="description" name="description" maxlength="4000" placeholder="Edition, highlighting, missing pages, notes…">${d?.description || ''}</textarea></div>

      <h3>Book location</h3>
      <div class="row" style="gap:.75rem;align-items:center;margin-bottom:.75rem"><button type="button" class="btn" id="detect">📍 Detect my location</button><span class="muted small" id="detectMsg">Fills area, city, state and pincode automatically.</span></div>
      <div class="form-grid">
        <div class="field"><label for="addressLine">Flat / house / building no.</label><input id="addressLine" name="addressLine" maxlength="200" value="${d?.addressLine || ''}"></div>
        <div class="field"><label for="area">Area / locality</label><input id="area" name="area" maxlength="150" value="${d?.area || ''}"></div>
        <div class="field"><label for="city">City *</label><input id="city" name="city" maxlength="100" value="${s.city || ''}"></div>
        <div class="field"><label for="state">State</label><input id="state" name="state" maxlength="100" value="${s.state || ''}"></div>
        <div class="field"><label for="postalCode">Pincode</label><input id="postalCode" name="postalCode" maxlength="20" inputmode="numeric" value="${d?.postalCode || ''}"></div>
      </div>
      <input type="hidden" id="latitude" name="latitude" value="${d?.latitude ?? ''}"><input type="hidden" id="longitude" name="longitude" value="${d?.longitude ?? ''}">

      <h3>Photos <span class="muted small">(front cover required · up to 5 photos · JPEG/PNG/WebP · max 5 MB each)</span></h3>
      <div class="upload-grid">${ALL_SLOTS.map(([t, l]) => html`<div><div class="drop" data-type="${t}">${have[t] ? html`<img src="${fileUrl(have[t])}" alt="">` : ''}<span>${have[t] ? l + ' ✓ (tap to replace)' : l}</span><input type="file" accept="image/jpeg,image/png,image/webp" aria-label="${l}"></div></div>`)}</div>
      <div class="row" style="margin-top:1.5rem"><button class="btn primary" data-mode="publish">${editId ? 'Save & publish' : 'Publish listing'}</button><button class="btn" data-mode="draft">Save as draft</button><a class="btn ghost" href="#/dashboard">Cancel</a></div>
    </form></div>`);

  const categorySelect = $('#categoryId', root);
  const categoryOther = $('#categoryOther', root);
  categorySelect.addEventListener('change', () => {
    categoryOther.style.display = categorySelect.value === 'OTHER' ? '' : 'none';
    if (categorySelect.value === 'OTHER') categoryOther.focus();
  });

  // ----- location: detect (browser GPS + OpenStreetMap lookup) or type by hand -----
  const latEl = $('#latitude', root), lonEl = $('#longitude', root), msgEl = $('#detectMsg', root);
  ['city', 'state', 'postalCode'].forEach((id) => $('#' + id, root).addEventListener('input', () => { latEl.value = ''; lonEl.value = ''; }));
  $('#detect', root).addEventListener('click', () => {
    if (!navigator.geolocation) { msgEl.textContent = 'Your browser cannot detect location. Please type it below.'; return; }
    msgEl.textContent = 'Detecting…';
    navigator.geolocation.getCurrentPosition(async (pos) => {
      const { latitude, longitude } = pos.coords;
      try {
        const r = await fetch(`https://nominatim.openstreetmap.org/reverse?format=jsonv2&addressdetails=1&accept-language=en&lat=${latitude}&lon=${longitude}`);
        const a = (await r.json()).address || {};
        $('#area', root).value = a.suburb || a.neighbourhood || a.city_district || a.quarter || '';
        $('#city', root).value = a.city || a.town || a.village || a.municipality || a.county || a.state_district || '';
        $('#state', root).value = a.state || '';
        $('#postalCode', root).value = a.postcode || '';
        latEl.value = latitude; lonEl.value = longitude;
        msgEl.textContent = 'Location detected. Please add your flat / house number and check the rest.';
      } catch { msgEl.textContent = 'Could not look up the address. Please type it below.'; }
    }, () => { msgEl.textContent = 'Location permission was denied. Please type it below.'; }, { enableHighAccuracy: false, timeout: 15000 });
  });
  async function geocodeIfNeeded(body) {
    if (body.latitude || !body.city) return;
    try {
      const q = [body.area, body.city, body.state, body.postalCode].filter(Boolean).join(', ');
      const r = await fetch(`https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&accept-language=en&q=${encodeURIComponent(q)}`);
      const j = await r.json();
      if (j[0]) { body.latitude = Number(j[0].lat); body.longitude = Number(j[0].lon); }
    } catch { /* coordinates are optional */ }
  }

  $$('.drop', root).forEach((drop) => $('input', drop).addEventListener('change', (e) => {
    const f = e.target.files[0]; if (!f) return;
    if (!OK_TYPES.includes(f.type)) { toast('Please choose a JPEG, PNG or WebP image', 'error'); e.target.value = ''; return; }
    if (f.size > 5 * 1024 * 1024) { toast('Image must be 5 MB or smaller', 'error'); e.target.value = ''; return; }
    files[drop.dataset.type] = f;
    drop.querySelector('img')?.remove();
    const img = document.createElement('img'); img.alt = ''; img.src = URL.createObjectURL(f); drop.prepend(img);
  }));

  const form = $('#f', root);
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const mode = e.submitter?.dataset.mode || 'draft';
    const fd = new FormData(form);
    const rawCategory = fd.get('categoryId');
    const text = (k) => (fd.get(k) || '').toString().trim() || null;
    const body = {
      title: fd.get('title'),
      author: text('author'),
      publisher: text('publisher'),
      isbn: text('isbn'),
      description: fd.get('description'),
      categoryId: rawCategory === 'OTHER' ? null : rawCategory,
      categoryOther: rawCategory === 'OTHER' ? (fd.get('categoryOther') || '').trim() : null,
      academicLevel: fd.get('academicLevel'),
      board: text('board'),
      bookCondition: fd.get('bookCondition'),
      price: fd.get('price'),
      addressLine: text('addressLine'),
      area: text('area'),
      city: text('city'),
      state: text('state'),
      postalCode: text('postalCode'),
      latitude: fd.get('latitude') ? Number(fd.get('latitude')) : null,
      longitude: fd.get('longitude') ? Number(fd.get('longitude')) : null,
    };
    if (!body.academicLevel || !body.bookCondition || body.price === '' || (!body.categoryId && !body.categoryOther)) { mount($('#msg', root), html`<div class="alert error">Please complete all required fields.</div>`); return; }
    if (!body.city) { mount($('#msg', root), html`<div class="alert error">Please add the book's city (use “Detect my location” or type it).</div>`); return; }
    if (mode === 'publish') {
      const missing = REQUIRED.filter(([t]) => !have[t] && !files[t]).map(([, l]) => l);
      if (missing.length) { mount($('#msg', root), html`<div class="alert error">Add the required photo: ${missing.join(', ')}.</div>`); return; }
    }
    const btns = $$('button', form); btns.forEach((b) => (b.disabled = true)); $('#msg', root).innerHTML = '';
    try {
      await geocodeIfNeeded(body);
      const saved = currentId ? await api(`/api/listings/${currentId}`, { method: 'PUT', body }) : await api('/api/listings', { method: 'POST', body });
      const id = currentId = saved.summary.id;
      for (const [type, file] of Object.entries(files)) {
        const f = new FormData(); f.append('file', file);
        await api(`/api/listings/${id}/images`, { method: 'POST', form: f, query: { type } });
      }
      if (mode === 'publish' && ['DRAFT', 'UNPUBLISHED'].includes(saved.summary.status)) await api(`/api/listings/${id}/publish`, { method: 'POST' });
      toast(mode === 'publish' ? 'Listing published' : 'Draft saved');
      go('#/listing/' + id);
    } catch (err) {
      showFieldErrors(form, err);
      mount($('#msg', root), html`<div class="alert error" role="alert">${err.message}</div>`);
      btns.forEach((b) => (b.disabled = false));
    }
  });
}
