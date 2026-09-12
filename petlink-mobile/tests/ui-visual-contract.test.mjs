import test from 'node:test'
import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'

const styles = readFileSync(new URL('../src/uni.scss', import.meta.url), 'utf8')
const shell = readFileSync(new URL('../src/components/MobileShell.vue', import.meta.url), 'utf8')
const home = readFileSync(new URL('../src/features/animal/AnimalScreens.vue', import.meta.url), 'utf8')
const iconPaths = readFileSync(new URL('../src/components/iconPaths.js', import.meta.url), 'utf8')
const icon = readFileSync(new URL('../src/components/Icon.vue', import.meta.url), 'utf8')

test('Mobile visual system keeps touch targets, focus states, and layered hero', () => {
  assert.match(styles, /min-height:\s*76rpx/)
  assert.match(styles, /focus-visible/)
  assert.match(styles, /hero-visual/)
  assert.match(styles, /\.title::before/)
  assert.match(styles, /\.card::before/)
  assert.match(shell, /min-height:\s*72rpx/)
  assert.match(shell, /\.brand-mark/)
})

test('Mobile premium visual layer keeps depth and touch surfaces', () => {
  assert.match(styles, /\$petlink-accent-gradient:/)
  assert.match(styles, /\.card[\s\S]*inset 0 1px/)
  assert.match(styles, /\.hero::after/)
  assert.match(styles, /\$petlink-shadow-float/)
  assert.match(shell, /backdrop-filter:\s*blur/)
  assert.match(shell, /\.bottom-nav::before/)
  assert.match(shell, /transform: translateY\(-7rpx\)/)
})

test('Mobile home consumes the generated rescue hero asset', () => {
  assert.match(home, /content\/home-hero\.jpg/)
  assert.ok(existsSync(new URL('../src/static/assets/content/home-hero.jpg', import.meta.url)))
})

test('Mobile navigation and actions use semantic SVG icons instead of Unicode glyphs', () => {
  for (const name of ['home', 'pin', 'tasks', 'heart', 'users', 'back', 'send', 'eye', 'trash']) {
    assert.match(iconPaths, new RegExp(`${name}:`))
  }
  assert.match(icon, /aria-label/)
  assert.match(shell, /<Icon name="home"/)
  assert.doesNotMatch(shell, /[↗⌂⌖♡◎♥×]/)
})
