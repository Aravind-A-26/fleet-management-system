import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/postcss';
import { fileURLToPath, URL } from 'node:url';
// Spring Boot is the server. Build the React client as a SPA it can serve.
export default defineConfig({
  plugins: [react()],
  resolve: { preserveSymlinks: true, alias: { '@': fileURLToPath(new URL('.', import.meta.url)) } },
  css: { postcss: { plugins: [tailwindcss()] } },
  server: { host: '127.0.0.1', port: 3000, strictPort: true,
    proxy: { '/api': { target: process.env.API_PROXY_TARGET || 'http://127.0.0.1:8080' } } },
  preview: { host: '127.0.0.1', port: 3000, strictPort: true,
    proxy: { '/api': { target: process.env.API_PROXY_TARGET || 'http://127.0.0.1:8080' } } },
});

