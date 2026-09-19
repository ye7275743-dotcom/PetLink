import test from 'node:test'
import assert from 'node:assert/strict'
import { bridgeMobileSessionToPc, isMobileDevice, openDesktopApp, redirectDesktopEntry, setViewPreference } from '../src/utils/deviceRouting.js'

function storage() {
  const values = new Map()
  return {
    values,
    getItem: key => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, String(value))
  }
}

test('mobile session bridge writes a PC-compatible user record', () => {
  const target = storage()
  const uniRef = {
    getStorageSync: key => key === 'petlink_mobile_token' ? 'mobile-token' : { type: 'object', data: { id: 4, roleCode: 'USER' } }
  }
  assert.equal(bridgeMobileSessionToPc({ uniRef, storage: target }), true)
  assert.equal(target.getItem('petlink_pc_token'), 'mobile-token')
  assert.match(target.getItem('petlink_pc_user'), /"roleCode":"USER"/)
})

test('manual mobile-to-PC switch preserves the session and records the preference', () => {
  const target = storage()
  const documentRef = { cookie: '' }
  const windowRef = {
    location: { origin: 'http://localhost:8081', assign: value => { windowRef.destination = value } },
    localStorage: target
  }
  const uniRef = {
    getStorageSync: key => key === 'petlink_mobile_token' ? 'rescuer-token' : { roleCode: 'RESCUER' }
  }
  assert.equal(openDesktopApp({ windowRef, documentRef, uniRef }), true)
  assert.match(documentRef.cookie, /petlink_view=pc/)
  assert.equal(windowRef.destination, 'http://localhost:8081/')
  assert.equal(target.getItem('petlink_pc_token'), 'rescuer-token')
})

test('invalid view values clear the preference instead of persisting arbitrary input', () => {
  const documentRef = { cookie: '' }
  setViewPreference('mobile', documentRef)
  assert.match(documentRef.cookie, /petlink_view=mobile/)
  setViewPreference('unexpected', documentRef)
  assert.match(documentRef.cookie, /Max-Age=0/)
})

test('mobile H5 entry redirects a desktop browser to the PC origin', () => {
  let replaced = ''
  const windowRef = {
    location: { origin: 'http://localhost:8081', protocol: 'http:', hostname: 'localhost', pathname: '/mobile/', replace: value => { replaced = value } },
    innerWidth: 1440
  }
  assert.equal(redirectDesktopEntry({
    windowRef,
    navigatorRef: { userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' }
  }), true)
  assert.equal(replaced, 'http://localhost:8081/')
  assert.equal(isMobileDevice({ userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)' }, { innerWidth: 390 }), true)
})

test('an explicit member handoff may open the H5 shell on a desktop preview', () => {
  let replaced = ''
  const windowRef = {
    location: { origin: 'http://localhost:8081', protocol: 'http:', hostname: 'localhost', pathname: '/mobile/', search: '?view=mobile', replace: value => { replaced = value } },
    innerWidth: 1440
  }
  assert.equal(redirectDesktopEntry({
    windowRef,
    navigatorRef: { userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' }
  }), false)
  assert.equal(replaced, '')
})
