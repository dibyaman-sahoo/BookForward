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

  const knownCategoryIds = new Set(cats.map((c) => String(c.id)));
  const isOtherSubject = s.categoryId && !knownCategoryIds.has(String(s.categoryId));

  mount(root, html`<div class="card" style="max-width:860px;margin:auto"><h1 style="font-size:2rem">${editId ? 'Edit listing' : 'List a book'}</h1>
    <div id="msg"></div>
    <form id="f" novalidate>
      <div class="form-grid">
        <div class="field"><label for="title">Title *</label><input id="title" name="title" required maxlength="200" value="${s.title || ''}"></div>
        <div class="field"><label for="academicLevel">Academic level *</label><select id="academicLevel" name="academicLevel" required>${options(LEVELS, s.academicLevel, 'Choose…')}</select></div>
        <div class="field"><label for="categoryId">Subject *</label>
          <select id="categoryId" name="categoryId" required>
            <option value="">Choose…</option>
            ${cats.map((c) => html`<option value="${c.id}" ${!isOtherSubject && c.id === s.categoryId ? 'selected' : ''}>${c.name}</option>`)}
            <option value="OTHER" ${isOtherSubject ? 'selected' : ''}>Other (type your own)</option>
          </select>
          <input id="categoryOther" name="categoryOther" maxlength="80" placeholder="e.g. Microbiology, Cell Biology, Biochemistry…" style="margin-top:.5rem;${isOtherSubject ? '' : 'display:none'}" value="${isOtherSubject ? (s.categoryName || '') : ''}">
        </div>
        <div class="field"><label for="author">Author</label><input id="author" name="author" maxlength="150" value="${s.author || ''}"></div>
        <div class="field"><label for="publisher">Publisher</label><input id="publisher" name="publisher" maxlength="150" value="${d?.publisher || ''}"></div>
        <div class="field"><label for="isbn">ISBN</label><input id="isbn" name="isbn" maxlength="20" value="${d?.isbn || ''}"></div>
        <div class="field"><label for="board">Board / context</label><input id="board" name="board" maxlength="50" value="${s.board || ''}"></div>
        <div class="field"><label for="bookCondition">Condition *</label><select id="bookCondition" name="bookCondition" required>${options(CONDITIONS, s.bookCondition, 'Choose…')}</select></div>
        <div class="field"><label for="price">Price (₹) *</label><input id="price" name="price" type="number" min="0" max="99999" step="1" inputmode="numeric" required value="${s.price ?? ''}"></div>
      </div>
      <div class="field"><label for="description">Description</label><textarea id="description" name="description" maxlength="4000" placeholder="Edition, highlighting, missing pages, notes…">${d?.description || ''}</textarea></div>
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
    const body = {
      title: fd.get('title'),
      author: fd.get('author') || null,
      publisher: fd.get('publisher') || null,
      isbn: fd.get('isbn') || null,
      description: fd.get('description'),
      categoryId: rawCategory === 'OTHER' ? null : rawCategory,
      categoryOther: rawCategory === 'OTHER' ? (fd.get('categoryOther') || '').trim() : null,
      academicLevel: fd.get('academicLevel'),
      board: fd.get('board') || null,
      bookCondition: fd.get('bookCondition'),
      price: fd.get('price'),
    };
    if (!body.academicLevel || !body.bookCondition || body.price === '' || (!body.categoryId && !body.categoryOther)) { mount($('#msg', root), html`<div class="alert error">Please complete all required fields.</div>`); return; }
    if (mode === 'publish') {
      const missing = REQUIRED.filter(([t]) => !have[t] && !files[t]).map(([, l]) => l);
      if (missing.length) { mount($('#msg', root), html`<div class="alert error">Add the required photo: ${missing.join(', ')}.</div>`); return; }
    }
    const btns = $$('button', form); btns.forEach((b) => (b.disabled = true)); $('#msg', root).innerHTML = '';
    try {
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
