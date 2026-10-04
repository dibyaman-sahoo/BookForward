// WebSocket/STOMP: authenticated connect, auto-reconnect, presence + live events. UI reads state via events.
import { WS_URL, API_BASE } from './config.js';
import { api, tokenStore } from './api.js';

const listeners = {};
export const on = (evt, fn) => { (listeners[evt] ||= new Set()).add(fn); return () => listeners[evt].delete(fn); };
const emit = (evt, data) => listeners[evt]?.forEach((fn) => { try { fn(data); } catch (e) { console.error(e); } });

export const wsState = { status: 'offline', online: new Set() };
let client = null;

function setStatus(s) { wsState.status = s; emit('status', s); }

export async function refreshPresence() {
  try { wsState.online = new Set((await api('/api/presence')).online); emit('presence', wsState.online); } catch { /* retried on next reconnect */ }
}

export function connect() {
  if (client || !tokenStore.get() || !window.StompJs) return;
  setStatus('connecting');
  client = new window.StompJs.Client({
    brokerURL: WS_URL, reconnectDelay: 3000, heartbeatIncoming: 10000, heartbeatOutgoing: 10000,
  });
  // Re-read the token on every (re)connect attempt so a fresh login is honoured.
  client.beforeConnect = () => { client.connectHeaders = { Authorization: 'Bearer ' + tokenStore.get() }; };
  client.onConnect = () => {
    setStatus('online');
    client.subscribe('/user/queue/messages', (f) => emit('message', JSON.parse(f.body)));
    client.subscribe('/user/queue/notifications', (f) => emit('notification', JSON.parse(f.body)));
    client.subscribe('/user/queue/typing', (f) => emit('typing', JSON.parse(f.body)));
    client.subscribe('/user/queue/errors', (f) => emit('wserror', JSON.parse(f.body)));
    client.subscribe('/topic/presence', (f) => { wsState.online = new Set(JSON.parse(f.body).online); emit('presence', wsState.online); });
    refreshPresence();       // converge after (re)connect
    emit('reconnected');     // pages re-fetch durable state (messages, notifications)
  };
  client.onWebSocketClose = () => { if (client) setStatus(tokenStore.get() ? 'connecting' : 'offline'); };
  client.onStompError = (f) => { console.warn('STOMP error', f.headers?.message); };
  client.activate();
}
export function disconnect() { const c = client; client = null; setStatus('offline'); c?.deactivate(); wsState.online = new Set(); }
export const isConnected = () => !!client?.connected;
export function sendMessage(conversationId, content) { client.publish({ destination: '/app/chat.send', body: JSON.stringify({ conversationId, content }) }); }
export function sendTyping(conversationId) { if (isConnected()) client.publish({ destination: '/app/chat.typing', body: JSON.stringify({ conversationId }) }); }
export { API_BASE };
