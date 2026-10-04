import { html, mount, $, toast, showFieldErrors } from '../ui.js';
import { login, register } from '../auth.js';
import { go, parseHash } from '../nav.js';

export default async function (root) {
  const isReg = parseHash().path === '/register';
  root.dataset.title = isReg ? 'Join' : 'Sign in';
  mount(root, html`<div class="card" style="max-width:460px;margin:2rem auto">
    <h1 style="font-size:2rem">${isReg ? 'Create your account' : 'Welcome back'}</h1>
    <div id="msg"></div>
    <form id="f" novalidate>
      ${isReg ? html`<div class="field"><label for="displayName">Display name</label><input id="displayName" name="displayName" autocomplete="name" required minlength="2" maxlength="100"></div>` : ''}
      <div class="field"><label for="email">Email</label><input id="email" name="email" type="email" autocomplete="email" required></div>
      <div class="field"><label for="password">Password</label><input id="password" name="password" type="password" autocomplete="${isReg ? 'new-password' : 'current-password'}" required ${isReg ? 'minlength="8"' : ''}>${isReg ? html`<div class="muted small">At least 8 characters with a letter and a digit.</div>` : ''}</div>
      <button class="btn primary" style="width:100%" id="go">${isReg ? 'Create account' : 'Sign in'}</button>
    </form>
    <p class="center muted" style="margin-top:1rem">${isReg ? html`Already a member? <a href="#/login">Sign in</a>` : html`New here? <a href="#/register">Create an account</a>`}</p></div>`);
  const form = $('#f', root);
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const v = Object.fromEntries(new FormData(form));
    const btn = $('#go', root); btn.disabled = true; $('#msg', root).innerHTML = '';
    try {
      await (isReg ? register(v) : login(v.email, v.password));
      toast(isReg ? 'Welcome to BookForward!' : 'Signed in');
      const next = sessionStorage.getItem('bf.next'); sessionStorage.removeItem('bf.next');
      go(next && !next.startsWith('#/login') ? next : '#/');
    } catch (err) {
      showFieldErrors(form, err);
      mount($('#msg', root), html`<div class="alert error" role="alert">${err.message}</div>`);
    } finally { btn.disabled = false; }
  });
}
