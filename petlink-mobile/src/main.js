import { createSSRApp } from 'vue'
import App from './App.vue'
import { redirectDesktopEntry } from './utils/deviceRouting.js'

// Keep the H5 fallback safe when it is opened directly from a desktop browser
// (for example during local development, where Nginx is not in front of Vite).
redirectDesktopEntry()

export function createApp(){ const app=createSSRApp(App); return {app} }
