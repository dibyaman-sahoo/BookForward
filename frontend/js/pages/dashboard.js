import { html, mount, $, $$, toast, modal, money, chip, dateTime, label, showFieldErrors } from '../ui.js';
import { api, fileUrl } from '../api.js';
import { auth } from '../auth.js';
import { errorBox } from '../components.js';
import { go } from '../nav.js';

const TABS = [['listings', 'My listings'], ['received', 'Requests received'], ['sent', 'My requests'], ['orders', 'Orders']];

export default async function (root, { query }) {
  root.dataset.title = 'Dashboard';
  const tab = TABS.some(([k]) => k === query.tab) ? query.tab : 'listings';
  mount(root, html`<h1>Dashboard</h1><nav class="tabs" aria-label="Dashboard sections">${TABS.map(([k, t]) => html`<a href="#/dashboard?tab=${k}" class="${k === tab ? 'active' : ''}">${t}</a>`)}</nav><div id="pane" class="skeleton"></div>`);
  const pane = $('#pane', root);
  const done = (tpl) => { pane.classList.remove('skeleton'); mount(pane, tpl); };
  const reload = () => go(location.hash) || null;
  const run = (fn) => async (e) => { try { await fn(e.currentTarget.dataset); reload(); } catch (err) { toast(err.message, 'error'); } };
  try {
    if (tab === 'listings') {
      const res = await api('/api/me/listings', { query: { size: 50 } });
      done(res.items.length ? html`<div class="row between" style="margin-bottom:1rem"><span class="muted">${res.totalItems} listing(s)</span><a class="btn primary sm" href="#/sell">+ New listing</a></div>
        <table class="data"><thead><tr><th>Book</th><th>Price</th><th>Status</th><th></th></tr></thead><tbody>${res.items.map((b) => html`<tr>
          <td data-label="Book"><a href="#/listing/${b.id}"><strong>${b.title}</strong></a><div class="muted small">${b.author}</div></td><td data-label="Price">${money(b.price)}</td><td data-label="Status">${chip(b.status)}</td>
          <td data-label=""><div class="row"><a class="btn sm" href="#/sell/${b.id}">Edit</a>${['DRAFT', 'UNPUBLISHED'].includes(b.status) ? html`<button class="btn sm primary" data-act="publish" data-id="${b.id}">Publish</button>` : ''}${b.status === 'ACTIVE' ? html`<button class="btn sm" data-act="unpublish" data-id="${b.id}">Unpublish</button>` : ''}</div></td></tr>`)}</tbody></table>`
        : html`<div class="empty card">You have not listed any books yet.<br><a class="btn primary" style="margin-top:1rem" href="#/sell">List your first book</a></div>`);
      $$('[data-act]', root).forEach((b) => b.addEventListener('click', run((d) => api(`/api/listings/${d.id}/${d.act}`, { method: 'POST' }))));
    } else if (tab === 'received' || tab === 'sent') {
      const seller = tab === 'received';
      const res = await api('/api/requests/mine', { query: { role: seller ? 'seller' : 'buyer' } });
      done(res.length ? html`<div class="stack">${res.map((r) => html`<div class="card row between">
        <div class="row" style="flex:1;min-width:240px">${r.coverUrl ? html`<img src="${fileUrl(r.coverUrl)}" alt="" style="width:56px;height:72px;object-fit:cover;border-radius:8px">` : ''}
          <div><a href="#/listing/${r.listingId}"><strong>${r.listingTitle}</strong></a><div class="muted small">${seller ? 'From ' + r.buyer.name : 'To ' + r.seller.name} · ${dateTime(r.createdAt)}</div>${r.message ? html`<div class="small">“${r.message}”</div>` : ''}</div></div>
        <div class="row">${chip(r.status)}${r.status === 'PENDING' ? (seller ? html`<button class="btn sm primary" data-st="ACCEPTED" data-id="${r.id}">Accept</button><button class="btn sm danger" data-st="REJECTED" data-id="${r.id}">Reject</button>` : html`<button class="btn sm danger" data-st="CANCELLED" data-id="${r.id}">Cancel</button>`) : ''}${r.orderId ? html`<a class="btn sm" href="#/dashboard?tab=orders">View order</a>` : ''}</div></div>`)}</div>`
        : html`<div class="empty card">${seller ? 'No requests received yet.' : 'You have not requested any books yet.'}</div>`);
      $$('[data-st]', root).forEach((b) => b.addEventListener('click', run((d) => api(`/api/requests/${d.id}/status`, { method: 'PATCH', body: { status: d.st } }).then(() => toast('Request ' + d.st.toLowerCase())))));
    } else {
      const res = await api('/api/orders');
      done(res.length ? html`<div class="stack">${res.map((o) => {
        const buyer = o.buyer.id === auth.user.id;
        return html`<div class="card"><div class="row between"><div class="row" style="flex:1;min-width:240px">${o.coverUrl ? html`<img src="${fileUrl(o.coverUrl)}" alt="" style="width:56px;height:72px;object-fit:cover;border-radius:8px">` : ''}
          <div><a href="#/listing/${o.listingId}"><strong>${o.listingTitle}</strong></a><div class="muted small">${buyer ? 'Seller: ' + o.seller.name : 'Buyer: ' + o.buyer.name} · ${money(o.totalAmount)}</div></div></div>
          <div class="row">${chip(o.status)}
            ${o.status === 'CONFIRMED' && !buyer ? html`<button class="btn sm primary" data-os="HANDOVER" data-id="${o.id}">Mark handed over</button>` : ''}
            ${o.status === 'HANDOVER' && buyer ? html`<button class="btn sm primary" data-os="COMPLETED" data-id="${o.id}">Confirm received</button>` : ''}
            ${['CONFIRMED', 'HANDOVER'].includes(o.status) ? html`<button class="btn sm danger" data-cancel="${o.id}">Cancel</button>` : ''}
            ${o.status === 'COMPLETED' && buyer && !o.reviewed ? html`<button class="btn sm primary" data-review="${o.id}">Leave a review</button>` : ''}
            ${auth.features.paymentEnabled && buyer && ['CONFIRMED', 'HANDOVER'].includes(o.status) ? html`<button class="btn sm" data-pay="${o.id}">Start payment</button>` : ''}</div></div>
          <details style="margin-top:.6rem"><summary class="muted small">History</summary><ul class="small">${o.history.map((h) => html`<li>${dateTime(h.at)} — ${label(h.newStatus)} by ${h.actor}${h.reason ? ' (' + h.reason + ')' : ''}</li>`)}</ul></details></div>`; })}</div>`
        : html`<div class="empty card">No orders yet. Orders appear once a request is accepted.</div>`);
      $$('[data-os]', root).forEach((b) => b.addEventListener('click', run((d) => api(`/api/orders/${d.id}/status`, { method: 'PATCH', body: { status: d.os } }).then(() => toast('Order updated')))));
      $$('[data-pay]', root).forEach((b) => b.addEventListener('click', run((d) => api(`/api/orders/${b.dataset.pay}/payments`, { method: 'POST' }).then(() => toast('Payment started')))));
      $$('[data-cancel]', root).forEach((b) => b.addEventListener('click', () => {
        const m = modal(html`<h2>Cancel order</h2><form id="cf"><div class="field"><label for="reason">Reason *</label><textarea id="reason" name="reason" required maxlength="500"></textarea></div><div class="row"><button class="btn danger">Cancel order</button><button type="button" class="btn ghost" data-close>Keep order</button></div></form>`);
        $('#cf', m.el).addEventListener('submit', async (e) => { e.preventDefault(); try { await api(`/api/orders/${b.dataset.cancel}/status`, { method: 'PATCH', body: { status: 'CANCELLED', reason: new FormData(e.target).get('reason') } }); m.close(); toast('Order cancelled'); reload(); } catch (err) { toast(err.message, 'error'); } });
      }));
      $$('[data-review]', root).forEach((b) => b.addEventListener('click', () => {
        const m = modal(html`<h2>Rate the seller</h2><form id="rf"><div class="field"><label for="rating">Rating</label><select id="rating" name="rating"><option value="5">★★★★★ Excellent</option><option value="4">★★★★ Good</option><option value="3">★★★ OK</option><option value="2">★★ Poor</option><option value="1">★ Terrible</option></select></div><div class="field"><label for="comment">Comment</label><textarea id="comment" name="comment" maxlength="1000"></textarea></div><div class="row"><button class="btn primary">Submit review</button><button type="button" class="btn ghost" data-close>Later</button></div></form>`);
        $('#rf', m.el).addEventListener('submit', async (e) => { e.preventDefault(); const f = new FormData(e.target); try { await api('/api/reviews', { method: 'POST', body: { orderId: b.dataset.review, rating: Number(f.get('rating')), comment: f.get('comment') } }); m.close(); toast('Thanks for your review'); reload(); } catch (err) { toast(err.message, 'error'); } });
      }));
    }
  } catch (e) { done(errorBox(e)); }
}
