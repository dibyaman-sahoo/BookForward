// Auth state shared by the router, nav and pages.
import { api, tokenStore } from './api.js';

export const auth = { user: null, listeners: new Set(), saved: new Set(), features: { paymentEnabled: false } };
const emit = () => auth.listeners.forEach((fn) => fn(auth.user));
export const onAuthChange = (fn) => { auth.listeners.add(fn); return () => auth.listeners.delete(fn); };
export const isStaff = () => !!auth.user && (auth.user.roles.includes('MODERATOR') || auth.user.roles.includes('ADMIN'));
export const isAdmin = () => !!auth.user && auth.user.roles.includes('ADMIN');

export async function loadSaved() {
  try { auth.saved = new Set(auth.user ? await api('/api/saved/ids') : []); } catch { auth.saved = new Set(); }
}
export async function restoreSession() {
  api('/api/config', { auth: false }).then((f) => { auth.features = f; }).catch(() => {});
  if (!tokenStore.get()) return;
  try { auth.user = await api('/api/auth/me'); await loadSaved(); } catch { tokenStore.clear(); auth.user = null; }
  emit();
}
export async function login(email, password) { return start(await api('/api/auth/login', { method: 'POST', body: { email, password }, auth: false })); }
export async function register(payload) { return start(await api('/api/auth/register', { method: 'POST', body: payload, auth: false })); }
async function start(res) { tokenStore.set(res.token); auth.user = res.user; await loadSaved(); emit(); return res.user; }
export async function logout() {
  try { await api('/api/auth/logout', { method: 'POST' }); } catch { /* token may already be invalid */ }
  clearSession();
}
export function clearSession() { tokenStore.clear(); auth.user = null; auth.saved = new Set(); emit(); }
