import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const view = readFileSync(new URL('../src/views/TasksView.vue', import.meta.url), 'utf8')

test('PC rescuer task page exposes the waiting-acceptance workflow', () => {
  assert.match(view, /role==='RESCUER'/)
  assert.match(view, /等待接取/)
  assert.match(view, /clueApi\.waiting\(\{page,size\}\)/)
  assert.match(view, /rescueApi\.accept\(row\.id\)/)
  assert.match(view, /await refreshAll\(\)/)
})
