// REST client: bearer token, uniform errors, expiry handling.
import { API_BASE } from './config.js';

const TOKEN_KEY = 'bf.token';
export const tokenStore = {
  get: () => { try { return localStorage.getItem(TOKEN_KEY); } catch { return null; } },
  set: (t) => { try { localStorage.setItem(TOKEN_KEY, t); } catch { /* storage unavailable */ } },
  clear: () => { try { localStorage.removeItem(TOKEN_KEY); } catch { /* ignore */ } },
};

export class ApiError extends Error {
  constructor(status, body) {
    super(body?.message || 'Something went wrong');
    this.status = status; this.code = body?.code; this.fieldErrors = body?.fieldErrors || [];
  }
}

export const fileUrl = (path) => path ? (path.startsWith('http') ? path : API_BASE + path) : '';

export async function api(path, { method = 'GET', body, form, query, auth = true } = {}) {
  const url = new URL(API_BASE + path);
  Object.entries(query || {}).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') url.searchParams.set(k, v); });
  const headers = {};
  const token = tokenStore.get();
  if (auth && token) headers.Authorization = 'Bearer ' + token;
  let payload;
  if (form) payload = form;
  else if (body !== undefined) { headers['Content-Type'] = 'application/json'; payload = JSON.stringify(body); }
  let res;
  try {
    res = await fetch(url, { method, headers, body: payload });
  } catch {
    throw new ApiError(0, { code: 'NETWORK', message: 'Cannot reach the server. Check your connection and try again.' });
  }
  if (res.status === 204) return null;
  let data = null;
  try { data = await res.json(); } catch { /* non-JSON body */ }
  if (!res.ok) {
    if (res.status === 401 && token && data?.code !== 'INVALID_CREDENTIALS') window.dispatchEvent(new CustomEvent('auth:expired'));
    throw new ApiError(res.status, data || { message: 'Unexpected server response' });
  }
  return data;
}
