import test from 'node:test'
import assert from 'node:assert/strict'
import {readFileSync} from 'node:fs'

const api=readFileSync(new URL('../src/api/index.js',import.meta.url),'utf8')
const dashboard=readFileSync(new URL('../src/views/DashboardView.vue',import.meta.url),'utf8')
const users=readFileSync(new URL('../src/views/UsersView.vue',import.meta.url),'utf8')

test('M08 PC API exposes all ten Frozen routes',()=>{
  for(const route of ['/admin/users','/enable','/disable','/promote-rescuer','/admin/rescue-tasks','/admin/animals','/admin/adoption-records','/admin/stats/overview','/admin/stats/trends']) assert.ok(api.includes(route),route)
})

test('M08 dashboard and user detail consume Frozen response field names',()=>{
  for(const group of ['rescueClues','rescueTasks','animals','adoptionApplications']) assert.ok(dashboard.includes(`count('${group}'`),group)
  assert.ok(dashboard.includes("timeZone:'Asia/Shanghai'"))
  assert.ok(users.includes('detail.statistics'))
  assert.ok(!users.includes('detail.counts'))
})
