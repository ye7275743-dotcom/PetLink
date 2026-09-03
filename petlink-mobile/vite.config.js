import { defineConfig } from 'vite'
import uniModule from '@dcloudio/vite-plugin-uni'

// Compatible with Node environments where the UniApp plugin is exposed either
// directly as the default export or under a nested `default` property.
const uni = typeof uniModule === 'function' ? uniModule : uniModule?.default
if (typeof uni !== 'function') {
  throw new TypeError('Unable to load @dcloudio/vite-plugin-uni')
}

export default defineConfig({
  plugins: [uni()],
  // Keep local development at `/`, while allowing production H5 to be
  // mounted below the same-origin `/mobile/` Nginx location.
  base: process.env.PETLINK_H5_BASE || '/',
  server: {
    port: 5174,
    proxy: {
      '/api': {
        target: process.env.PETLINK_API_TARGET || 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: { sourcemap: false }
})
