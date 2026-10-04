import { html, mount, $, attachTilt, revealOnScroll } from '../ui.js';
import { api } from '../api.js';
import { auth } from '../auth.js';
import { bookCard, wireHearts, skeletonGrid } from '../components.js';

export default async function (root) {
  root.dataset.title = 'Home';
  mount(root, html`
    <section class="hero">
      <div>
        <p class="chip">Educational book reuse marketplace</p>
        <h1>Give every book <em>another chapter.</em></h1>
        <p class="muted" style="font-size:1.12rem;max-width:34rem">Find affordable school, college and exam-prep books from students who have finished with them — and pass on your own. Verified with real photos, arranged through safe in-app chat.</p>
        <div class="row"><a class="btn primary" href="#/browse">Browse books</a><a class="btn" href="#/${auth.user ? 'sell' : 'register'}">List a book</a></div>
      </div>
      <div class="stage" aria-hidden="true"><div class="shelf">
        ${[['one', 'Calculus', 'Vol. I'], ['two', 'Physics', 'NCERT'], ['three', 'Organic Chem', 'Reference']].map(([c, t, s]) => html`
        <div class="book3d ${c}"><div class="f">${t}<small>${s}</small></div><div class="b"></div><div class="s"></div><div class="p"></div><div class="t"></div><div class="m"></div></div>`)}
      </div></div>
    </section>
    <section class="reveal" style="margin-block:2rem"><h2>Browse by subject</h2><div id="cats" class="row"></div></section>
    <section class="reveal" style="margin-block:2rem"><div class="row between"><h2>Just listed</h2><a href="#/browse?sort=newest">See all →</a></div><div id="latest">${skeletonGrid(4)}</div></section>
    <section class="reveal" style="margin-block:2rem"><h2>How it works</h2><div class="steps">
      ${[['List with proof', 'Add details and three photos: front cover, details page and index page.'], ['Discover & save', 'Search by title, author, level, condition and price. Save favourites.'], ['Request & chat', 'Send a request, talk in real time, agree on handover.'], ['Complete & review', 'Confirm the handover, then rate the seller to build trust.']]
        .map(([t, d], i) => html`<div class="card step tilt"><div class="n">${i + 1}</div><h3>${t}</h3><p class="muted small">${d}</p></div>`)}</div></section>`);
  revealOnScroll(root); attachTilt(root);
  try {
    const [cats, latest] = await Promise.all([api('/api/categories', { auth: false }), api('/api/search/listings', { query: { sort: 'newest', size: 4 }, auth: false })]);
    mount($('#cats', root), html`${cats.map((c) => html`<a class="chip cat-chip" href="#/browse?category=${c.slug}">${c.name}</a>`)}`);
    mount($('#latest', root), latest.items.length ? html`<div class="grid">${latest.items.map(bookCard)}</div>`
      : html`<div class="empty card">No listings yet — be the first to <a href="#/sell">list a book</a>.</div>`);
    wireHearts($('#latest', root)); attachTilt($('#latest', root));
  } catch (e) { mount($('#latest', root), html`<div class="alert error">${e.message}</div>`); }
}
