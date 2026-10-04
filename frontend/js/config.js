// API location. Override at deploy time by defining window.BOOKFORWARD_CONFIG before this module loads
// (see config.local.js / the nginx entrypoint in docker/).
const cfg = window.BOOKFORWARD_CONFIG || {};
export const API_BASE = (cfg.apiBase || 'http://localhost:8080').replace(/\/$/, '');
export const WS_URL = (cfg.wsUrl || API_BASE.replace(/^http/, 'ws') + '/ws');
