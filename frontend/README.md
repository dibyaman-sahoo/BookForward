# Frontend

Framework-free SPA. `index.html` shell; `css/` (base, components, scene = 3D/animation); `js/` (api, auth, ws, ui, router in main.js); `js/pages/` one module per view. Configure the API in `env.js`. Run: `../scripts/dev-frontend.sh`. Deploy: any static host or the nginx Dockerfile (`API_BASE_URL` env). All dynamic HTML must use the escaping `html` tag from `ui.js`.
