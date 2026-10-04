// Small UI toolkit: safe templating, toasts, modal, formatting, helpers.
const RAW = Symbol('raw');
export const raw = (s) => ({ [RAW]: true, s });
export const esc = (v) => String(v ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
function part(v) {
  if (v === null || v === undefined || v === false) return '';
  if (Array.isArray(v)) return v.map(part).join('');
  if (typeof v === 'object' && v[RAW]) return v.s;
  return esc(v);
}
/** Tagged template: every interpolated value is HTML-escaped unless it came from another html`` call. */
export const html = (strings, ...vals) => raw(strings.reduce((o, s, i) => o + s + (i < vals.length ? part(vals[i]) : ''), ''));
export const mount = (el, tpl) => { el.innerHTML = tpl.s; return el; };
export const $ = (sel, root = document) => root.querySelector(sel);
export const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];

export function toast(msg, type = 'ok', ms = 4200) {
  const t = document.createElement('div');
  t.className = 'toast' + (type === 'error' ? ' error' : '');
  t.textContent = msg;
  $('#toasts').appendChild(t);
  setTimeout(() => t.remove(), ms);
}

/** Opens a modal; returns { el, close }. Focus is trapped loosely and Esc closes. */
export function modal(contentTpl, { onClose } = {}) {
  const overlay = document.createElement('div');
  overlay.className = 'overlay';
  overlay.innerHTML = `<div class="card modal" role="dialog" aria-modal="true">${contentTpl.s}</div>`;
  const prev = document.activeElement;
  const close = () => { overlay.remove(); document.removeEventListener('keydown', onKey); prev?.focus?.(); onClose?.(); };
  const onKey = (e) => { if (e.key === 'Escape') close(); };
  overlay.addEventListener('mousedown', (e) => { if (e.target === overlay) close(); });
  document.addEventListener('keydown', onKey);
  document.body.appendChild(overlay);
  overlay.querySelector('input,select,textarea,button')?.focus();
  overlay.querySelectorAll('[data-close]').forEach((b) => b.addEventListener('click', close));
  return { el: overlay.firstElementChild, close };
}

export const money = (n) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(Number(n || 0));
export const dateTime = (iso) => iso ? new Date(iso).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' }) : '';
export const timeShort = (iso) => iso ? new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '';
export const label = (s) => String(s || '').toLowerCase().replace(/_/g, ' ').replace(/^\w/, (c) => c.toUpperCase());
export const stars = (avg) => avg == null ? '' : '★'.repeat(Math.round(avg)) + '☆'.repeat(5 - Math.round(avg));
export const debounce = (fn, ms = 300) => { let t; return (...a) => { clearTimeout(t); t = setTimeout(() => fn(...a), ms); }; };
export const statusClass = (s) => ({ ACTIVE: '', PENDING: 'warn', RESERVED: 'warn', CONFIRMED: 'warn', HANDOVER: 'warn', DRAFT: 'warn', REJECTED: 'bad', HIDDEN: 'bad', CANCELLED: 'bad', SOLD: '', COMPLETED: '', ACCEPTED: '' }[s] || '');
export const chip = (s) => html`<span class="chip ${statusClass(s)}">${label(s)}</span>`;

/** Mouse-driven 3D tilt for fine pointers only. */
export function attachTilt(root = document) {
  if (!matchMedia('(hover:hover) and (pointer:fine)').matches || matchMedia('(prefers-reduced-motion: reduce)').matches) return;
  root.querySelectorAll('.tilt').forEach((el) => {
    el.addEventListener('pointermove', (e) => {
      const r = el.getBoundingClientRect();
      el.style.setProperty('--ry', ((e.clientX - r.left) / r.width - 0.5) * 9 + 'deg');
      el.style.setProperty('--rx', (0.5 - (e.clientY - r.top) / r.height) * 9 + 'deg');
    });
    el.addEventListener('pointerleave', () => { el.style.setProperty('--rx', '0deg'); el.style.setProperty('--ry', '0deg'); });
  });
}
export function revealOnScroll(root = document) {
  const els = root.querySelectorAll('.reveal');
  if (!('IntersectionObserver' in window)) { els.forEach((e) => e.classList.add('in')); return; }
  const io = new IntersectionObserver((entries) => entries.forEach((en) => { if (en.isIntersecting) { en.target.classList.add('in'); io.unobserve(en.target); } }), { threshold: 0.12 });
  els.forEach((e) => io.observe(e));
}
export function showFieldErrors(form, err) {
  form.querySelectorAll('.field').forEach((f) => { f.classList.remove('invalid'); f.querySelector('.err')?.remove(); });
  (err.fieldErrors || []).forEach(({ field, message }) => {
    const input = form.querySelector(`[name="${field}"]`);
    const f = input?.closest('.field');
    if (f) { f.classList.add('invalid'); const d = document.createElement('div'); d.className = 'err'; d.textContent = message; f.appendChild(d); }
  });
}
