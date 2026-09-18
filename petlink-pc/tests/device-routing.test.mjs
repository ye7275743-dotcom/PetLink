import test from 'node:test'
import assert from 'node:assert/strict'
import {
  bridgePcSessionToMobile,
  isMobileDevice,
  mobileRouteFor,
  readViewPreference,
  redirectMobileEntry,
  setViewPreference
} from '../src/utils/deviceRouting.js'

function storage(seed = {}) {
  const values = new Map(Object.entries(seed))
  return {
    getItem: key => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, String(value)),
    removeItem: key => values.delete(key),
    values
  }
}

test('mobile device detection covers phones and avoids desktop resize false positives', () => {
  assert.equal(isMobileDevice({ userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)' }, { innerWidth: 390 }), true)
  assert.equal(isMobileDevice({ userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' }, { innerWidth: 1440 }), false)
  assert.equal(isMobileDevice({ userAgent: 'Mozilla/5.0', maxTouchPoints: 5 }, { innerWidth: 1200, matchMedia: () => ({ matches: true }) }), false)
})

test('public deep links preserve their target when mapped to mobile routes', () => {
  assert.equal(mobileRouteFor('/'), '/mobile/#/pages/home/index')
  assert.equal(mobileRouteFor('/dashboard'), '/mobile/#/pages/home/index')
  assert.equal(mobileRouteFor('/adopt/42'), '/mobile/#/pages/animal/index?id=42')
  assert.equal(mobileRouteFor('/news/7'), '/mobile/#/pages/announcementDetail/index?id=7')
  assert.equal(mobileRouteFor('/tasks'), '/mobile/#/pages/tasks/index')
  assert.equal(mobileRouteFor('/users'), '')
})

test('manual view preference overrides automatic detection and does not loop on explicit mobile paths', () => {
  const cookies = { cookie: '' }
  setViewPreference('pc', cookies)
  assert.equal(readViewPreference(cookies), 'pc')
  let replaced = ''
  const desktopWindow = {
    location: { pathname: '/', replace: value => { replaced = value } },
    localStorage: storage(),
    innerWidth: 390
  }
  assert.equal(redirectMobileEntry({
    windowRef: desktopWindow,
    documentRef: cookies,
    navigatorRef: { userAgent: 'iPhone' }
  }), false)
  assert.equal(replaced, '')
  assert.equal(redirectMobileEntry({
    windowRef: { ...desktopWindow, location: { pathname: '/mobile/', replace: value => { replaced = value } } },
    documentRef: { cookie: '' },
    navigatorRef: { userAgent: 'iPhone' }
  }), false)
})

test('mobile entry redirects before the PC shell mounts and keeps the same origin', () => {
  let replaced = ''
  const windowRef = {
    location: { protocol: 'http:', hostname: 'localhost', pathname: '/adopt/42', replace: value => { replaced = value } },
    localStorage: storage(),
    innerWidth: 390
  }
  assert.equal(redirectMobileEntry({
    windowRef,
    documentRef: { cookie: '' },
    navigatorRef: { userAgent: 'Mozilla/5.0 (iPhone)' }
  }), true)
  assert.match(replaced, /\/mobile\/#\/pages\/animal\/index\?id=42$/)
})

test('administrator dashboard stays on the PC workbench when opened in a phone emulator', () => {
  let replaced = ''
  const windowRef = {
    location: { protocol: 'http:', hostname: 'localhost', pathname: '/dashboard', replace: value => { replaced = value } },
    localStorage: storage({
      petlink_pc_user: JSON.stringify({ id: 1, roleCode: 'ADMIN' })
    }),
    innerWidth: 390
  }
  assert.equal(redirectMobileEntry({
    windowRef,
    documentRef: { cookie: '' },
    navigatorRef: { userAgent: 'Mozilla/5.0 (iPhone)' }
  }), false)
  assert.equal(replaced, '')
})

test('rescuer dashboard deep links use the mobile landing screen in a phone emulator', () => {
  let replaced = ''
  const windowRef = {
    location: { protocol: 'http:', hostname: 'localhost', pathname: '/dashboard', replace: value => { replaced = value } },
    localStorage: storage({
      petlink_pc_user: JSON.stringify({ id: 2, roleCode: 'RESCUER' })
    }),
    innerWidth: 390
  }
  assert.equal(redirectMobileEntry({
    windowRef,
    documentRef: { cookie: '' },
    navigatorRef: { userAgent: 'Mozilla/5.0 (iPhone)' }
  }), true)
  assert.match(replaced, /\/mobile\/#\/pages\/home\/index$/)
})

test('automatic PC to mobile handoff copies only member-capable sessions', () => {
  const member = storage({
    petlink_pc_token: 'token-user',
    petlink_pc_user: JSON.stringify({ id: 3, roleCode: 'RESCUER' })
  })
  assert.equal(bridgePcSessionToMobile(member), true)
  assert.equal(member.getItem('petlink_mobile_token'), 'token-user')
  assert.match(member.getItem('petlink_mobile_user'), /RESCUER/)

  const admin = storage({
    petlink_pc_token: 'token-admin',
    petlink_pc_user: JSON.stringify({ id: 1, roleCode: 'ADMIN' })
  })
  assert.equal(bridgePcSessionToMobile(admin), false)
  assert.equal(admin.getItem('petlink_mobile_token'), null)
})
