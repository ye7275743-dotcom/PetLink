import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const router = readFileSync(new URL('../src/router/index.js', import.meta.url), 'utf8')
const layout = readFileSync(new URL('../src/layouts/AdminLayout.vue', import.meta.url), 'utf8')
const api = readFileSync(new URL('../src/api/index.js', import.meta.url), 'utf8')
const view = readFileSync(new URL('../src/views/ProfileView.vue', import.meta.url), 'utf8')

test('PC workbench exposes the profile page for ADMIN and RESCUER', () => {
  assert.match(router, /path:'\/profile'.*roles:\['ADMIN','RESCUER'\]/s)
  assert.match(layout, /\['\/profile','profile','个人资料',\['ADMIN','RESCUER'\]\]/)
  assert.match(api, /patchMe:\s*data\s*=>\s*request\.patch\('\/users\/me'/)
  assert.match(view, /authApi\.me\(\)/)
  assert.match(view, /authApi\.patchMe\(/)
  assert.match(view, /更新昵称和联系方式/)
})
