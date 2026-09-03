import test from 'node:test'
import assert from 'node:assert/strict'
import { accountRule, passwordRule, phoneRule, textRule } from '../src/composables/validation.js'
import { mergePageRecords, hasMorePages } from '../src/composables/paginationPolicy.js'
import { animalActions, rescueActions } from '../src/composables/statePolicy.js'
import { validateImageMeta, MAX_IMAGE_BYTES } from '../src/composables/uploadPolicy.js'
import { idempotencyHeaders, normalizeFollowupPayload } from '../src/composables/idempotency.js'
import { mobileRouteDecision } from '../src/composables/routePolicy.js'
import fs from 'node:fs'

test('login/register validation follows Frozen account/password/phone rules', () => {
  assert.equal(accountRule('user_01'), '')
  assert.notEqual(accountRule('x'), '')
  assert.equal(passwordRule('Example123!'), '')
  assert.notEqual(passwordRule('short'), '')
  assert.equal(phoneRule('13800138000'), '')
  assert.notEqual(phoneRule('123'), '')
  assert.notEqual(textRule('   ', { label: '昵称', required: true, max: 50 }), '')
})

test('pagination appends later pages without duplicating ids and exposes hasMore', () => {
  assert.deepEqual(mergePageRecords([{id:'1'},{id:'2'}],[{id:'2'},{id:'3'}]), [{id:'1'},{id:'2'},{id:'3'}])
  assert.equal(hasMorePages({page:1,pages:3,total:50,count:20}), true)
  assert.equal(hasMorePages({page:3,pages:3,total:50,count:50}), false)
})

test('animal and rescue action buttons follow role/status policy', () => {
  assert.deepEqual(animalActions('TREATING'), ['TO_OBSERVING'])
  assert.deepEqual(animalActions('ADOPTED'), [])
  assert.deepEqual(rescueActions('IN_PROGRESS','RESCUER'), ['ADD_RECORD','SUCCESS','FAILED'])
  assert.deepEqual(rescueActions('FAILED','ADMIN'), ['REOPEN','CLOSE'])
  assert.deepEqual(rescueActions('IN_PROGRESS','USER'), [])
})

test('upload policy rejects oversized and unsupported images', () => {
  assert.equal(validateImageMeta({size:MAX_IMAGE_BYTES,type:'image/jpeg'}), '')
  assert.notEqual(validateImageMeta({size:MAX_IMAGE_BYTES+1,type:'image/jpeg'}), '')
  assert.notEqual(validateImageMeta({size:100,type:'image/gif'}), '')
})

test('idempotency helper preserves clue header and normalizes follow-up key', () => {
  assert.deepEqual(idempotencyHeaders('abc'), {'Idempotency-Key':'abc'})
  const body=normalizeFollowupPayload({content:' hello ',healthCondition:' ',imageTokens:['t1'],idempotencyKey:'ABC-DEF'})
  assert.equal(body.content,'hello')
  assert.equal(body.healthCondition,null)
  assert.equal(body.idempotencyKey,'abc-def')
  assert.deepEqual(body.imageTokens,['t1'])
})

test('mobile direct-route policy blocks USER from RESCUER pages', () => {
  assert.deepEqual(mobileRouteDecision({name:'waitingClue',hasToken:true,role:'USER'}), {allow:false,redirect:'home',clearSession:false})
  assert.equal(mobileRouteDecision({name:'waitingClue',hasToken:true,role:'RESCUER'}).allow, true)
  assert.deepEqual(mobileRouteDecision({name:'favorites',hasToken:false,role:null}), {allow:false,redirect:'login',clearSession:false})
  assert.deepEqual(mobileRouteDecision({name:'tasks',hasToken:true,role:'ADMIN'}), {allow:false,redirect:'login',clearSession:true})
})

test('mobile announcement admin helpers include Frozen version body', () => {
  const api = fs.readFileSync(new URL('../src/api/index.js', import.meta.url),'utf8')
  assert.match(api,/publish:\(id,version\).*data:\{version\}/s)
  assert.match(api,/withdrawAnnouncement:\(id,version\).*data:\{version\}/s)
})

test('adoption overview consumes the Frozen response field names', () => {
  const source = fs.readFileSync(new URL('../src/features/adoption/AdoptionScreens.vue', import.meta.url), 'utf8')
  assert.match(source, /pendingApplicationCount/)
  assert.doesNotMatch(source, /pendingCount/)
  assert.match(source, /animalStatus/)
})

test('all 29 UniApp pages register the page-level route guard on onShow', () => {
  const pagesConfig = JSON.parse(fs.readFileSync(new URL('../src/pages.json', import.meta.url), 'utf8'))
  assert.equal(pagesConfig.pages.length, 29)

  for (const page of pagesConfig.pages) {
    const match = /^pages\/([^/]+)\/index$/.exec(page.path)
    assert.ok(match, `unexpected page path: ${page.path}`)
    const pageName = match[1]
    const source = fs.readFileSync(new URL(`../src/${page.path}.vue`, import.meta.url), 'utf8')
    assert.match(source, /import\s*\{\s*useRouteGuard\s*\}\s*from\s*['"]\.\.\/\.\.\/composables\/routeGuard\.js['"]/)
    assert.match(source, new RegExp(`useRouteGuard\\(['"]${pageName}['"]\\)`), `${page.path} must guard itself`)
  }

  const guardSource = fs.readFileSync(new URL('../src/composables/routeGuard.js', import.meta.url), 'utf8')
  assert.match(guardSource, /export function useRouteGuard\(name\)/)
  assert.match(guardSource, /onShow\(\(\)\s*=>\s*enforceMobileRoute\(name\)\)/)
})
