import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const feature = readFileSync(new URL('../src/features/rescue/RescueScreens.vue', import.meta.url), 'utf8')
const page = readFileSync(new URL('../src/pages/tasks/index.vue', import.meta.url), 'utf8')

test('mobile rescuer task page refreshes both queues when shown', () => {
  assert.match(feature, /async function refreshAll\(\)/)
  assert.match(feature, /Promise\.all\(\[waiting\.refresh\(\),reloadTasks\(\)\]\)/)
  assert.match(feature, /defineExpose\(\{refresh:refreshAll\}\)/)
  assert.match(page, /onShow\(\(\) => \{[\s\S]*const result = screens\.value\?\.refresh\?\.\(\)[\s\S]*if \(result\) void result\.catch/)
})
