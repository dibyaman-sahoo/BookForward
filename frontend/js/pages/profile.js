import { html, mount, $, toast, showFieldErrors } from '../ui.js';
import { api } from '../api.js';
import { auth } from '../auth.js';
import { options, LEVELS } from '../components.js';

export default async function (root) {
  root.dataset.title = 'Profile';
  const p = await api('/api/profile');
  mount(root, html`<div class="card" style="max-width:640px;margin:auto"><h1 style="font-size:2rem">Your profile</h1><p class="muted">${p.email}</p><div id="msg"></div>
    <form id="f"><div class="field"><label for="displayName">Display name</label><input id="displayName" name="displayName" value="${p.displayName}" required></div>
    <div class="field"><label for="bio">About you</label><textarea id="bio" name="bio" maxlength="500">${p.bio || ''}</textarea></div>
    <div class="form-grid"><div class="field"><label for="institution">School / college</label><input id="institution" name="institution" value="${p.institution || ''}"></div>
    <div class="field"><label for="city">City</label><input id="city" name="city" value="${p.city || ''}"></div>
    <div class="field"><label for="academicLevel">Academic level</label><select id="academicLevel" name="academicLevel">${options(LEVELS, p.academicLevel, 'Not set')}</select></div>
    <div class="field"><label for="board">Board / course</label><input id="board" name="board" value="${p.board || ''}"></div></div>
    <div class="field"><label for="targetExam">Target exam</label><input id="targetExam" name="targetExam" value="${p.targetExam || ''}"></div>
    <button class="btn primary">Save profile</button></form></div>`);
  const form = $('#f', root);
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const v = Object.fromEntries(new FormData(form)); Object.keys(v).forEach((k) => { if (v[k] === '') v[k] = null; });
    try { const r = await api('/api/profile', { method: 'PUT', body: v }); auth.user.displayName = r.displayName; toast('Profile saved'); $('#msg', root).innerHTML = ''; }
    catch (err) { showFieldErrors(form, err); mount($('#msg', root), html`<div class="alert error">${err.message}</div>`); }
  });
}
