import { html, mount, $, $$, toast, modal, money, label, chip, stars, dateTime, showFieldErrors } from '../ui.js';
import { api, fileUrl } from '../api.js';
import { auth } from '../auth.js';
import { errorBox, wireHearts } from '../components.js';
import { go } from '../nav.js';

const IMG_LABEL = { FRONT_COVER: 'Front cover', DETAILS_PAGE: 'Details page', INDEX_PAGE: 'Index page', EXTRA: 'Extra' };

export default async function (root, { params }) {
  const id = params[0];
  let d;
  try { d = await api(`/api/listings/${id}`, { auth: !!auth.user }); }
  catch (e) { mount(root, html`<div class="empty card"><h2>${e.status === 404 ? 'This listing is not available' : 'Could not load listing'}</h2><p>${e.message}</p><a class="btn" href="#/browse">Browse books</a></div>`); return; }
  const s = d.summary; root.dataset.title = s.title;
  const mine = d.ownedByViewer;
  const imgs = d.images;
  const reviews = await api(`/api/listings/${id}/reviews`, { auth: false }).catch(() => null);

  mount(root, html`<p><a href="#/browse">← Back to browse</a></p>
    <div class="detail">
      <div class="gallery">${imgs.length ? html`<img class="main" id="main" src="${fileUrl(imgs[0].url)}" alt="${IMG_LABEL[imgs[0].type]} of ${s.title}">
        <div class="thumbs">${imgs.map((i, n) => html`<button class="${n === 0 ? 'on' : ''}" data-src="${fileUrl(i.url)}" aria-label="Show ${IMG_LABEL[i.type]}"><img src="${fileUrl(i.url)}" alt="" loading="lazy"></button>`)}</div>`
        : html`<div class="skeleton" style="aspect-ratio:3/4"></div>`}</div>
      <div class="stack">
        <div>${chip(s.status)}<span class="chip">${label(s.bookCondition)}</span><span class="chip">${label(s.academicLevel)}</span>${s.ncertApplicable ? html`<span class="chip">NCERT</span>` : ''}</div>
        <h1 style="margin:0">${s.title}</h1><div class="muted">by ${s.author}</div>
        <div class="price" style="font-size:2rem">${money(s.price)}</div>
        ${d.moderationReason ? html`<div class="alert error"><strong>Moderation note:</strong> ${d.moderationReason}</div>` : ''}
        <div class="row">${mine ? html`
            <a class="btn" href="#/sell/${id}">Edit</a>
            ${['DRAFT', 'UNPUBLISHED'].includes(s.status) ? html`<button class="btn primary" id="publish">Publish</button>` : ''}
            ${s.status === 'ACTIVE' ? html`<button class="btn" id="unpublish">Unpublish</button>` : ''}
            <button class="btn danger" id="remove">Delete</button>` : html`
            <button class="btn primary" id="request" ${s.status !== 'ACTIVE' ? 'disabled' : ''}>${s.status === 'ACTIVE' ? 'Request this book' : 'Not available'}</button>
            <button class="btn" id="chat">Message seller</button>
            <button class="btn ghost heart-inline" data-save="${id}" id="saveBtn">${auth.saved.has(id) ? '♥ Saved' : '♡ Save'}</button>`}</div>
        <div class="card"><dl class="dl"><dt>Subject</dt><dd>${s.category}</dd><dt>Publisher</dt><dd>${d.publisher || '—'}</dd><dt>ISBN</dt><dd>${d.isbn || '—'}</dd><dt>Board</dt><dd>${s.board || '—'}</dd><dt>Listed</dt><dd>${dateTime(s.createdAt)}</dd></dl></div>
        ${d.description ? html`<div><h3>About this copy</h3><p style="white-space:pre-wrap">${d.description}</p></div>` : ''}
        <div class="card row between"><div><div class="muted small">Seller</div><strong>${s.sellerName}</strong>
          <div>${d.sellerReviewCount ? html`<span class="stars">${stars(d.sellerRating)}</span> <span class="muted small">${d.sellerRating.toFixed(1)} (${d.sellerReviewCount})</span>` : html`<span class="muted small">No reviews yet</span>`}</div></div>
          ${!mine ? html`<button class="btn sm ghost" id="report">Report</button>` : ''}</div>
      </div></div>
    <section style="margin-top:2rem"><h2>Reviews</h2>${reviews?.reviews.items.length ? html`<div class="stack">${reviews.reviews.items.map((r) => html`<div class="card"><span class="stars">${stars(r.rating)}</span> <strong>${r.reviewerName}</strong> <span class="muted small">${dateTime(r.createdAt)}</span>${r.comment ? html`<p style="margin:.4rem 0 0">${r.comment}</p>` : ''}</div>`)}</div>` : html`<p class="muted">No reviews for this book yet.</p>`}</section>`);

  $$('.thumbs button', root).forEach((b) => b.addEventListener('click', () => { $('#main', root).src = b.dataset.src; $$('.thumbs button', root).forEach((x) => x.classList.toggle('on', x === b)); }));
  const needAuth = () => { if (auth.user) return true; toast('Sign in to continue', 'error'); sessionStorage.setItem('bf.next', location.hash); go('#/login'); return false; };
  const act = (sel, fn) => $(sel, root)?.addEventListener('click', async () => { try { await fn(); } catch (e) { toast(e.message, 'error'); } });

  if (!mine) {
    wireHearts(root, { onChange: (_, on) => { $('#saveBtn', root).textContent = on ? '♥ Saved' : '♡ Save'; } });
    act('#chat', async () => { if (!needAuth()) return; const c = await api('/api/conversations', { method: 'POST', body: { listingId: id } }); go('#/messages?c=' + c.id); });
    $('#request', root)?.addEventListener('click', () => {
      if (!needAuth()) return;
      const m = modal(html`<h2>Request this book</h2><p class="muted">The seller will be notified and a chat opens automatically.</p><form id="rf"><div class="field"><label for="msg">Message (optional)</label><textarea id="msg" name="message" maxlength="500" placeholder="Hi! I'd like to buy this. Can we meet near campus?"></textarea></div><div class="row"><button class="btn primary">Send request</button><button type="button" class="btn ghost" data-close>Cancel</button></div></form>`);
      $('#rf', m.el).addEventListener('submit', async (e) => { e.preventDefault(); try { await api('/api/requests', { method: 'POST', body: { listingId: id, message: new FormData(e.target).get('message') } }); m.close(); toast('Request sent'); go('#/dashboard?tab=sent'); } catch (err) { toast(err.message, 'error'); } });
    });
    $('#report', root)?.addEventListener('click', () => {
      if (!needAuth()) return;
      const m = modal(html`<h2>Report listing</h2><form id="rp"><div class="field"><label for="reason">Reason</label><select id="reason" name="reason"><option>Misleading information</option><option>Photos do not match</option><option>Inappropriate content</option><option>Suspected scam</option><option>Other</option></select></div><div class="field"><label for="details">Details</label><textarea id="details" name="details" maxlength="1000"></textarea></div><div class="row"><button class="btn danger">Submit report</button><button type="button" class="btn ghost" data-close>Cancel</button></div></form>`);
      $('#rp', m.el).addEventListener('submit', async (e) => { e.preventDefault(); const f = new FormData(e.target); try { await api('/api/reports', { method: 'POST', body: { targetType: 'LISTING', targetId: id, reason: f.get('reason'), details: f.get('details') } }); m.close(); toast('Thanks — our moderators will review it.'); } catch (err) { toast(err.message, 'error'); } });
    });
  } else {
    act('#publish', async () => { await api(`/api/listings/${id}/publish`, { method: 'POST' }); toast('Listing published'); go(location.hash); });
    act('#unpublish', async () => { await api(`/api/listings/${id}/unpublish`, { method: 'POST' }); toast('Listing unpublished'); go(location.hash); });
    act('#remove', async () => { if (!confirm('Delete this listing? This cannot be undone.')) return; await api(`/api/listings/${id}`, { method: 'DELETE' }); toast('Listing deleted'); go('#/dashboard'); });
  }
}
