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

test('PC dashboard and login consume the generated rescue hero asset', () => {
  assert.match(dashboard, /petlink-rescue-hero-v1\.jpg/)
  assert.match(login, /petlink-rescue-hero-v1\.jpg/)
  assert.ok(existsSync(new URL('../public/assets/petlink-rescue-hero-v1.jpg', import.meta.url)))
})

test('PC navigation uses the shared semantic SVG icon dictionary', () => {
  for (const name of ['dashboard', 'clues', 'tasks', 'animals', 'adoptions', 'records', 'followups', 'announcements', 'users', 'spark', 'logout']) {
    assert.match(iconPaths, new RegExp(`${name}:`))
  }
  assert.match(layout, /<NavIcon class="brand-icon" name="spark"/)
  assert.doesNotMatch(layout, /[↗⌂⌖♡◎♥×]/)
})
