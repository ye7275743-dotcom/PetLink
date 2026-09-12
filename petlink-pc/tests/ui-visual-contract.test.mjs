import test from 'node:test'
import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'

const tokens = readFileSync(new URL('../src/styles/tokens.css', import.meta.url), 'utf8')
const styles = readFileSync(new URL('../src/styles/app.css', import.meta.url), 'utf8')
const dashboard = readFileSync(new URL('../src/views/DashboardView.vue', import.meta.url), 'utf8')
const login = readFileSync(new URL('../src/views/LoginView.vue', import.meta.url), 'utf8')
const iconPaths = readFileSync(new URL('../src/components/iconPaths.js', import.meta.url), 'utf8')
const layout = readFileSync(new URL('../src/layouts/AdminLayout.vue', import.meta.url), 'utf8')

test('PC visual system keeps accessible focus and shared design tokens', () => {
  assert.match(tokens, /--radius-md:\s*14px/)
  assert.match(tokens, /--focus:\s*#2a847b/)
  assert.match(styles, /:focus-visible/)
  assert.match(styles, /\.hero-panel[\s\S]*\.hero-art/)
})

test('PC premium visual layer keeps depth and branded surfaces', () => {
  assert.match(tokens, /--accent-gradient:/)
  assert.match(tokens, /--shadow-float:/)
  assert.match(styles, /\.metric-card::before/)
  assert.match(styles, /backdrop-filter:\s*blur/)
  assert.match(styles, /\.hero-panel::after/)
})

test('PC public home and login consume generated rescue hero assets', () => {
  const publicHome=readFileSync(new URL('../src/views/PublicView.vue',import.meta.url),'utf8')
  assert.match(publicHome, /content\/home-hero\.jpg/)
  assert.match(publicHome, /fetchpriority="high"/)
  assert.match(dashboard,/work-metrics/)
  assert.match(dashboard,/待处理事项/)
  assert.match(login, /petlink-rescue-hero-v1\.jpg/)
  assert.ok(existsSync(new URL('../public/assets/content/home-hero.jpg', import.meta.url)))
  assert.ok(existsSync(new URL('../public/assets/petlink-rescue-hero-v1.jpg', import.meta.url)))
})

test('public home stays independent from the workbench component library', () => {
  const publicHome=readFileSync(new URL('../src/views/PublicView.vue',import.meta.url),'utf8')
  const main=readFileSync(new URL('../src/main.js',import.meta.url),'utf8')
  const router=readFileSync(new URL('../src/router/index.js',import.meta.url),'utf8')
  assert.doesNotMatch(publicHome, /<el-|v-loading/)
  assert.doesNotMatch(main, /ElementPlus from|element-plus\/dist\/index\.css/)
  assert.match(router, /const LoginView=\(\)=>import/)
  assert.match(router, /const AdminLayout=\(\)=>import/)
  assert.match(router, /scrollBehavior/)
  assert.match(router, /return \{top:0\}/)
})

test('PC navigation uses the shared semantic SVG icon dictionary', () => {
  for (const name of ['dashboard', 'clues', 'tasks', 'animals', 'adoptions', 'records', 'followups', 'announcements', 'users', 'spark', 'logout']) {
    assert.match(iconPaths, new RegExp(`${name}:`))
  }
  assert.match(layout, /<NavIcon class="brand-icon" name="spark"/)
  assert.doesNotMatch(layout, /[↗⌂⌖♡◎♥×]/)
})
