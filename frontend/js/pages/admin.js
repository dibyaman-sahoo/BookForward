import { html, mount, $, $$, toast, modal, chip, dateTime, label, money } from '../ui.js';
import { api } from '../api.js';
import { auth, isAdmin } from '../auth.js';
import { errorBox, pager } from '../components.js';
import { go } from '../nav.js';

export default async function (root, { query }) {
  root.dataset.title = 'Moderation';
  const tabs = [['reports', 'Reports'], ['listings', 'Listings']].concat(isAdmin() ? [['users', 'Users'], ['audit', 'Audit log']] : []);
  const tab = tabs.some(([k]) => k === query.tab) ? query.tab : 'reports';
  const page = Number(query.page) || 0;
  mount(root, html`<h1>${isAdmin() ? 'Admin' : 'Moderation'}</h1><nav class="tabs">${tabs.map(([k, t]) => html`<a href="#/admin?tab=${k}" class="${k === tab ? 'active' : ''}">${t}</a>`)}</nav><div id="pane" class="skeleton"></div>`);
  const pane = $('#pane', root);
  const done = (t) => { pane.classList.remove('skeleton'); mount(pane, t); };
  const refresh = () => go(location.hash);
  const pageLinks = (res) => { $$('[data-page]', root).forEach((b) => b.addEventListener('click', () => go(`#/admin?tab=${tab}&page=${b.dataset.page}`))); };
  const reasonModal = (title, cta, fn) => {
    const m = modal(html`<h2>${title}</h2><form id="rm"><div class="field"><label for="r">Reason *</label><textarea id="r" name="reason" required maxlength="500"></textarea></div><div class="row"><button class="btn primary">${cta}</button><button type="button" class="btn ghost" data-close>Cancel</button></div></form>`);
    $('#rm', m.el).addEventListener('submit', async (e) => { e.preventDefault(); try { await fn(new FormData(e.target).get('reason')); m.close(); refresh(); } catch (err) { toast(err.message, 'error'); } });
  };
  try {
    if (tab === 'reports') {
      const res = await api('/api/admin/reports', { query: { status: 'OPEN', page } });
      done(res.items.length ? html`<div class="stack">${res.items.map((r) => html`<div class="card"><div class="row between"><div><strong>${label(r.targetType)}: ${r.targetType === 'LISTING' ? html`<a href="#/listing/${r.targetId}">${r.targetLabel}</a>` : r.targetLabel}</strong><div class="muted small">${r.reason} · reported by ${r.reporter} · ${dateTime(r.createdAt)}</div>${r.details ? html`<p class="small">${r.details}</p>` : ''}</div>
        <div class="row">${r.targetType === 'LISTING' ? html`<button class="btn sm danger" data-a="HIDE" data-id="${r.id}">Hide listing</button>` : ''}${r.targetType === 'USER' ? html`<button class="btn sm danger" data-a="SUSPEND_USER" data-id="${r.id}">Suspend user</button>` : ''}${r.targetType === 'REVIEW' ? html`<button class="btn sm danger" data-a="HIDE_REVIEW" data-id="${r.id}">Hide review</button>` : ''}<button class="btn sm" data-a="DISMISS_REPORT" data-id="${r.id}">Dismiss</button></div></div></div>`)}</div>${pager(res.page, res.totalPages)}` : html`<div class="empty card">No open reports 🎉</div>`);
      $$('[data-a]', root).forEach((b) => b.addEventListener('click', () => reasonModal('Resolve report', 'Confirm', (reason) => api(`/api/admin/reports/${b.dataset.id}/resolve`, { method: 'POST', body: { action: b.dataset.a, reason } }))));
      pageLinks();
    } else if (tab === 'listings') {
      const status = query.status || 'ACTIVE';
      const res = await api('/api/admin/listings', { query: { status, page } });
      done(html`<div class="row" style="margin-bottom:1rem">${['ACTIVE', 'HIDDEN', 'REJECTED', 'DRAFT'].map((s) => html`<a class="chip cat-chip" href="#/admin?tab=listings&status=${s}" style="${s === status ? 'outline:2px solid var(--accent)' : ''}">${label(s)}</a>`)}</div>
        ${res.items.length ? html`<table class="data"><thead><tr><th>Book</th><th>Seller</th><th>Price</th><th>Status</th><th></th></tr></thead><tbody>${res.items.map((b) => html`<tr><td data-label="Book"><a href="#/listing/${b.id}">${b.title}</a></td><td data-label="Seller">${b.sellerName}</td><td data-label="Price">${money(b.price)}</td><td data-label="Status">${chip(b.status)}</td>
          <td data-label=""><div class="row">${['HIDDEN', 'REJECTED'].includes(b.status) ? html`<button class="btn sm primary" data-m="APPROVE" data-id="${b.id}">Approve</button>` : html`<button class="btn sm" data-m="HIDE" data-id="${b.id}">Hide</button><button class="btn sm danger" data-m="REJECT" data-id="${b.id}">Reject</button>`}</div></td></tr>`)}</tbody></table>${pager(res.page, res.totalPages)}` : html`<div class="empty card">Nothing here.</div>`}`);
      $$('[data-m]', root).forEach((b) => b.addEventListener('click', async () => {
        if (b.dataset.m === 'APPROVE') { try { await api(`/api/admin/listings/${b.dataset.id}/moderate`, { method: 'POST', body: { action: 'APPROVE' } }); toast('Approved'); refresh(); } catch (e) { toast(e.message, 'error'); } }
        else reasonModal(label(b.dataset.m) + ' listing', 'Confirm', (reason) => api(`/api/admin/listings/${b.dataset.id}/moderate`, { method: 'POST', body: { action: b.dataset.m, reason } }));
      }));
      pageLinks();
    } else if (tab === 'users') {
      const res = await api('/api/admin/users', { query: { q: query.q, page } });
      done(html`<form id="sf" class="row" style="margin-bottom:1rem"><input name="q" placeholder="Search name or email" value="${query.q || ''}" style="max-width:320px"><button class="btn">Search</button></form>
        <table class="data"><thead><tr><th>User</th><th>Roles</th><th>Status</th><th></th></tr></thead><tbody>${res.items.map((u) => html`<tr><td data-label="User"><strong>${u.displayName}</strong><div class="muted small">${u.email}</div></td><td data-label="Roles">${u.roles.map((r) => html`<span class="chip">${r}</span>`)}</td><td data-label="Status">${chip(u.status)}</td>
          <td data-label=""><div class="row"><button class="btn sm" data-roles="${u.id}" data-mod="${u.roles.includes('MODERATOR')}" ${u.id === auth.user.id ? 'disabled' : ''}>${u.roles.includes('MODERATOR') ? 'Remove moderator' : 'Make moderator'}</button><button class="btn sm ${u.status === 'ACTIVE' ? 'danger' : ''}" data-status="${u.id}" data-to="${u.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE'}" ${u.id === auth.user.id ? 'disabled' : ''}>${u.status === 'ACTIVE' ? 'Suspend' : 'Reinstate'}</button></div></td></tr>`)}</tbody></table>${pager(res.page, res.totalPages)}`);
      $('#sf', root).addEventListener('submit', (e) => { e.preventDefault(); go(`#/admin?tab=users&q=${encodeURIComponent(new FormData(e.target).get('q'))}`); });
      const patch = async (fn) => { try { await fn(); toast('Updated'); refresh(); } catch (e) { toast(e.message, 'error'); } };
      $$('[data-roles]', root).forEach((b) => b.addEventListener('click', () => { const u = res.items.find((x) => x.id === b.dataset.roles); const roles = new Set(u.roles); b.dataset.mod === 'true' ? roles.delete('MODERATOR') : roles.add('MODERATOR'); patch(() => api(`/api/admin/users/${u.id}/roles`, { method: 'PUT', body: { roles: [...roles] } })); }));
      $$('[data-status]', root).forEach((b) => b.addEventListener('click', () => patch(() => api(`/api/admin/users/${b.dataset.status}/status`, { method: 'PATCH', body: { status: b.dataset.to } }))));
      pageLinks();
    } else {
      const res = await api('/api/admin/audit', { query: { page } });
      done(html`<table class="data"><thead><tr><th>When</th><th>Actor</th><th>Action</th><th>Resource</th><th>Details</th></tr></thead><tbody>${res.items.map((a) => html`<tr><td data-label="When">${dateTime(a.createdAt)}</td><td data-label="Actor">${a.actor}</td><td data-label="Action">${label(a.action)}</td><td data-label="Resource">${a.resourceType}</td><td data-label="Details">${a.metadata || ''}</td></tr>`)}</tbody></table>${pager(res.page, res.totalPages)}`);
      pageLinks();
    }
  } catch (e) { done(errorBox(e)); }
}
