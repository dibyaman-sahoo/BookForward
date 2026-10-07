// Shared view fragments.
import { html, esc, money, chip, label } from './ui.js';
import { fileUrl, api } from './api.js';
import { auth, loadSaved } from './auth.js';
import { toast } from './ui.js';
import { go } from './nav.js';

export const errorBox = (e) => html`<div class="alert error" role="alert">${e.message || 'Something went wrong'}</div>`;

export function bookCard(b) {
  const saved = auth.saved.has(b.id);
  return html`<article class="card book-card tilt" data-id="${b.id}">
    <a class="cover" href="#/listing/${b.id}" aria-label="${b.title}">${b.coverUrl ? html`<img src="${fileUrl(b.coverUrl)}" alt="Cover of ${b.title}" loading="lazy" decoding="async">` : ''}</a>
    <button class="heart ${saved ? 'on' : ''}" data-save="${b.id}" aria-pressed="${saved}" aria-label="${saved ? 'Remove from saved' : 'Save book'}">${saved ? '♥' : '♡'}</button>
    <div class="body"><h3><a href="#/listing/${b.id}" style="color:inherit">${b.title}</a></h3>
      <div class="muted small">${[b.author, b.city && '📍 ' + b.city].filter(Boolean).join(' · ')}</div>
      <div class="row between" style="margin-top:.5rem"><span class="price">${money(b.price)}</span>${b.status !== 'ACTIVE' ? chip(b.status) : html`<span class="chip">${label(b.bookCondition)}</span>`}</div></div></article>`;
}

/** Wires every .heart inside root to the saved API. */
export function wireHearts(root, { onChange } = {}) {
  root.querySelectorAll('[data-save]').forEach((btn) => btn.addEventListener('click', async (e) => {
    e.preventDefault();
    if (!auth.user) { toast('Sign in to save books', 'error'); return go('#/login'); }
    const id = btn.dataset.save; const on = !auth.saved.has(id);
    try {
      await api(`/api/saved/${id}`, { method: on ? 'POST' : 'DELETE' });
      await loadSaved();
      btn.classList.toggle('on', on); btn.textContent = on ? '♥' : '♡'; btn.setAttribute('aria-pressed', String(on));
      onChange?.(id, on);
    } catch (err) { toast(err.message, 'error'); }
  }));
}

export function pager(page, totalPages) {
  if (totalPages <= 1) return html``;
  return html`<nav class="pager" aria-label="Pagination"><button class="btn sm" data-page="${page - 1}" ${page <= 0 ? 'disabled' : ''}>← Prev</button><span class="muted small">Page ${page + 1} of ${totalPages}</span><button class="btn sm" data-page="${page + 1}" ${page + 1 >= totalPages ? 'disabled' : ''}>Next →</button></nav>`;
}

export const skeletonGrid = (n = 8) => html`<div class="grid">${Array.from({ length: n }, () => html`<div class="skeleton"></div>`)}</div>`;

export const LEVELS = ['SCHOOL', 'COLLEGE', 'COMPETITIVE_EXAM', 'OTHER'];
export const CONDITIONS = ['NEW', 'LIKE_NEW', 'GOOD', 'FAIR', 'POOR'];
export const options = (list, selected, any) => html`${any ? html`<option value="">${any}</option>` : ''}${list.map((v) => html`<option value="${v}" ${v === selected ? 'selected' : ''}>${label(v)}</option>`)}`;
