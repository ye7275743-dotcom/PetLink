import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'

const api = fs.readFileSync(new URL('../src/api/index.js', import.meta.url),'utf8')
const view = fs.readFileSync(new URL('../src/views/AnnouncementsView.vue', import.meta.url),'utf8')

test('announcement publish and withdraw send Frozen version body',()=>{
  assert.match(api,/adminPublishAnnouncement:\s*\(id,version\).*\{version\}/s)
  assert.match(api,/adminWithdrawAnnouncement:\s*\(id,version\).*\{version\}/s)
  assert.match(view,/adminPublishAnnouncement\(row\.id,row\.version\)/)
  assert.match(view,/adminWithdrawAnnouncement\(row\.id,row\.version\)/)
})
