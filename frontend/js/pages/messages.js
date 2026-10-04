import { html, mount, $, $$, timeShort, dateTime, toast } from '../ui.js';
import { api } from '../api.js';
import { auth } from '../auth.js';
import * as ws from '../ws.js';
import { errorBox } from '../components.js';

export default async function (root, { query }) {
  root.dataset.title = 'Messages';
  let convs = []; let active = query.c || null; let msgs = []; let typingTimer = null; let lastTyping = 0;

  async function loadList() { convs = await api('/api/conversations'); }
  async function loadMsgs() {
    if (!active) return;
    const res = await api(`/api/conversations/${active}/messages`, { query: { size: 100 } });
    msgs = res.items.slice().reverse(); // oldest first; message ids/timestamps reconcile ordering
    await api(`/api/conversations/${active}/read`, { method: 'POST' }).catch(() => {});
    const c = convs.find((x) => x.id === active); if (c) c.unread = 0;
  }
  const other = () => convs.find((c) => c.id === active);

  function draw() {
    const o = other();
    mount(root, html`<div class="row between"><h1 style="font-size:1.8rem">Messages</h1><span class="muted small"><span class="ws-dot ${ws.wsState.status}"></span>${ws.wsState.status === 'online' ? 'Live' : ws.wsState.status === 'connecting' ? 'Reconnecting…' : 'Offline — messages send via fallback'}</span></div>
      <div class="chat ${active ? 'has-active' : ''}">
        <aside class="card list" aria-label="Conversations">${convs.length ? convs.map((c) => html`<a class="conv ${c.id === active ? 'active' : ''}" data-c="${c.id}"><div class="row between"><strong><span class="ws-dot ${ws.wsState.online.has(c.other.id) ? 'online' : ''}"></span>${c.other.name}</strong>${c.unread ? html`<span class="unread">${c.unread}</span>` : ''}</div><div class="muted small">${c.listingTitle || ''}</div><div class="small" style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${c.lastMessage ? c.lastMessage.content : 'No messages yet'}</div></a>`)
          : html`<div class="empty">No conversations yet.<br>Use “Message seller” on a listing.</div>`}</aside>
        <section class="card pane">${o ? html`
          <div class="row between" style="padding:.8rem 1rem;border-bottom:1px solid var(--line)"><div><button class="btn sm ghost" id="back" aria-label="Back to conversations">←</button> <strong>${o.other.name}</strong> <span class="muted small">${ws.wsState.online.has(o.other.id) ? 'online' : 'offline'}</span><div class="muted small" id="typing" style="min-height:1.1em"></div></div>${o.listingId ? html`<a class="small" href="#/listing/${o.listingId}">${o.listingTitle}</a>` : ''}</div>
          <div class="msgs" id="msgs" role="log" aria-live="polite">${msgs.map(bubble)}</div>
          <form class="composer" id="cf"><input id="ci" autocomplete="off" maxlength="2000" placeholder="Write a message…" aria-label="Message"><button class="btn primary">Send</button></form>`
          : html`<div class="empty" style="margin:auto">Select a conversation</div>`}</section></div>`);
    wire();
    const box = $('#msgs', root); if (box) box.scrollTop = box.scrollHeight;
  }
  const bubble = (m) => html`<div class="msg ${m.type === 'SYSTEM' ? 'sys' : m.senderId === auth.user.id ? 'me' : ''}" data-mid="${m.id}">${m.content}<time>${timeShort(m.createdAt)}</time></div>`;

  function wire() {
    $$('[data-c]', root).forEach((a) => a.addEventListener('click', async () => { active = a.dataset.c; history.replaceState(null, '', '#/messages?c=' + active); await loadMsgs().catch((e) => toast(e.message, 'error')); draw(); }));
    $('#back', root)?.addEventListener('click', () => { active = null; history.replaceState(null, '', '#/messages'); draw(); });
    const ci = $('#ci', root);
    ci?.addEventListener('input', () => { if (Date.now() - lastTyping > 2000) { lastTyping = Date.now(); ws.sendTyping(active); } });
    $('#cf', root)?.addEventListener('submit', async (e) => {
      e.preventDefault();
      const text = ci.value.trim(); if (!text) return; ci.value = '';
      try {
        if (ws.isConnected()) ws.sendMessage(active, text);
        else append(await api(`/api/conversations/${active}/messages`, { method: 'POST', body: { content: text } })); // REST fallback while offline
      } catch (err) { toast(err.message, 'error'); ci.value = text; }
    });
  }
  function append(m) {
    if (msgs.some((x) => x.id === m.id)) return; // de-duplicate REST/WS delivery
    msgs.push(m);
    const box = $('#msgs', root); if (box) { box.insertAdjacentHTML('beforeend', bubble(m).s); box.scrollTop = box.scrollHeight; }
  }

  try { await loadList(); await loadMsgs(); } catch (e) { mount(root, errorBox(e)); return; }
  draw();

  const off = [
    ws.on('message', async (m) => {
      const c = convs.find((x) => x.id === m.conversationId);
      if (!c) { await loadList().catch(() => {}); } else { c.lastMessage = m; if (m.senderId !== auth.user.id && m.conversationId !== active) c.unread += 1; }
      if (m.conversationId === active) { append(m); if (m.senderId !== auth.user.id) api(`/api/conversations/${active}/read`, { method: 'POST' }).catch(() => {}); }
      if (m.conversationId !== active || !c) draw();
    }),
    ws.on('typing', (t) => { if (t.conversationId === active) { const el = $('#typing', root); if (el) { el.textContent = 'typing…'; clearTimeout(typingTimer); typingTimer = setTimeout(() => { el.textContent = ''; }, 2500); } } }),
    ws.on('presence', () => { const keep = $('#ci', root)?.value; draw(); if (keep) $('#ci', root).value = keep; }),
    ws.on('status', () => { const keep = $('#ci', root)?.value; draw(); if (keep) $('#ci', root).value = keep; }),
    ws.on('wserror', (e) => toast(e.message, 'error')),
    ws.on('reconnected', async () => { try { await loadList(); await loadMsgs(); draw(); } catch { /* retried on next event */ } }),
  ];
  return () => { off.forEach((f) => f()); clearTimeout(typingTimer); };
}
