/**
 * Device-aware entry routing for the two same-origin PetLink clients.
 *
 * Production Nginx performs the first redirect for `/`. This client-side
 * layer is a fallback for local Vite usage, deep links and unusual browsers.
 */

const VIEW_COOKIE = 'petlink_view'
const COOKIE_MAX_AGE = 60 * 60 * 24 * 30
const MOBILE_ROLES = new Set(['USER', 'RESCUER'])

function safeJson(value) {
  if (typeof value !== 'string') return value
  try { return JSON.parse(value) } catch { return null }
}

function normalizeUser(value) {
  const parsed = safeJson(value)
  if (!parsed) return null
  if (parsed?.roleCode) return parsed
  if (parsed?.data?.roleCode) return parsed.data
  return null
}

function readCookie(name, documentRef = typeof document !== 'undefined' ? document : null) {
  if (!documentRef?.cookie) return ''
  const prefix = `${name}=`
  const item = documentRef.cookie.split(';').map(part => part.trim()).find(part => part.startsWith(prefix))
  return item ? decodeURIComponent(item.slice(prefix.length)) : ''
}

export function readViewPreference(documentRef = typeof document !== 'undefined' ? document : null) {
  const value = readCookie(VIEW_COOKIE, documentRef)
  return value === 'pc' || value === 'mobile' ? value : ''
}

export function setViewPreference(view, documentRef = typeof document !== 'undefined' ? document : null) {
  if (!documentRef) return
  if (view !== 'pc' && view !== 'mobile') {
    documentRef.cookie = `${VIEW_COOKIE}=; Path=/; Max-Age=0; SameSite=Lax`
    return
  }
  documentRef.cookie = `${VIEW_COOKIE}=${encodeURIComponent(view)}; Path=/; Max-Age=${COOKIE_MAX_AGE}; SameSite=Lax`
}

export function isMobileDevice(navigatorRef = typeof navigator !== 'undefined' ? navigator : null, windowRef = typeof window !== 'undefined' ? window : null) {
  if (!navigatorRef) return false
  if (navigatorRef.userAgentData?.mobile === true) return true
  const userAgent = String(navigatorRef.userAgent || '').toLowerCase()
  if (/android|iphone|ipod|ipad|mobile|blackberry|iemobile|opera mini|tablet/.test(userAgent)) return true
  const coarsePointer = windowRef?.matchMedia?.('(pointer: coarse)')?.matches
  const touchDevice = Number(navigatorRef.maxTouchPoints || 0) > 1
  const narrowScreen = Number(windowRef?.innerWidth || 0) > 0 && Number(windowRef.innerWidth) < 768
  return Boolean(coarsePointer && touchDevice && narrowScreen)
}

function encodeQuery(params) {
  const query = new URLSearchParams()
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') query.set(key, String(value))
  })
  const text = query.toString()
  return text ? `?${text}` : ''
}

/** Map routes which have a first-class mobile screen. */
export function mobileRouteFor(pathname = '/') {
  const path = pathname || '/'
  if (path === '/' || path === '/adopt') return '/mobile/#/pages/home/index'
  // A rescuer can arrive here after signing in through the PC entry. The
  // mobile home is the shared landing screen and exposes the rescue shortcut
  // for RESCUER accounts; ADMIN is kept on the PC workbench by the guard in
  // redirectMobileEntry because mobile has no administrative console.
  if (path === '/dashboard') return '/mobile/#/pages/home/index'
  if (path === '/stories' || path === '/guide') return '/mobile/#/pages/home/index'
  const animal = path.match(/^\/adopt\/([^/]+)$/)
  if (animal) return `/mobile/#/pages/animal/index${encodeQuery({ id: decodeURIComponent(animal[1]) })}`
  if (path === '/news') return '/mobile/#/pages/announcements/index'
  const notice = path.match(/^\/news\/([^/]+)$/)
  if (notice) return `/mobile/#/pages/announcementDetail/index${encodeQuery({ id: decodeURIComponent(notice[1]) })}`
  if (path === '/login') return '/mobile/#/pages/login/index'
  if (path === '/register') return '/mobile/#/pages/register/index'
  if (path === '/tasks') return '/mobile/#/pages/tasks/index'
  if (path === '/animals') return '/mobile/#/pages/animalManage/index'
  if (path === '/followups') return '/mobile/#/pages/rescuerFollowups/index'
  if (path === '/profile') return '/mobile/#/pages/profile/index'
  return ''
}

function mobileBaseUrl(windowRef = typeof window !== 'undefined' ? window : null) {
  if (!windowRef?.location) return '/mobile/'
  const dev = typeof import.meta !== 'undefined' && Boolean(import.meta.env?.DEV)
  return dev ? `${windowRef.location.protocol}//${windowRef.location.hostname}:5174/` : '/mobile/'
}

export function mobileUrlFor(pathname = '/', windowRef = typeof window !== 'undefined' ? window : null) {
  const route = mobileRouteFor(pathname) || '/mobile/#/pages/home/index'
  if (!route.startsWith('/mobile/')) return route
  const base = mobileBaseUrl(windowRef)
  return base.endsWith('/') ? `${base}${route.slice('/mobile/'.length)}` : `${base}/${route.slice('/mobile/'.length)}`
}

function readStorage(storage, key) {
  if (!storage) return ''
  try { return storage.getItem(key) || '' } catch { return '' }
}

function writeStorage(storage, key, value) {
  if (!storage) return
  try { storage.setItem(key, value) } catch { /* storage can be disabled by privacy mode */ }
}

/** Copy a PC session only when the mobile destination has no session. */
export function bridgePcSessionToMobile(storage = typeof localStorage !== 'undefined' ? localStorage : null) {
  const pcToken = readStorage(storage, 'petlink_pc_token')
  const mobileToken = readStorage(storage, 'petlink_mobile_token')
  const user = normalizeUser(readStorage(storage, 'petlink_pc_user'))
  if (!pcToken || mobileToken || !user || !MOBILE_ROLES.has(user.roleCode)) return false
  writeStorage(storage, 'petlink_mobile_token', pcToken)
  writeStorage(storage, 'petlink_mobile_user', JSON.stringify({ type: 'object', data: user }))
  return true
}

export function redirectMobileEntry({
  windowRef = typeof window !== 'undefined' ? window : null,
  documentRef = typeof document !== 'undefined' ? document : null,
  navigatorRef = typeof navigator !== 'undefined' ? navigator : null
} = {}) {
  if (!windowRef?.location || !navigatorRef) return false
  const pathname = windowRef.location.pathname || '/'
  if (pathname.startsWith('/mobile/') || pathname.startsWith('/view/')) return false
  const target = mobileRouteFor(pathname)
  if (!target) return false
  // The mobile client intentionally has no ADMIN console. Keep an existing
  // administrator session on the responsive PC workbench instead of sending
  // it to a public mobile page with no permitted actions.
  const currentUser = normalizeUser(readStorage(windowRef.localStorage, 'petlink_pc_user'))
  if (pathname === '/dashboard' && currentUser?.roleCode === 'ADMIN') return false
  const mobileDevice = isMobileDevice(navigatorRef, windowRef)
  // A stale `petlink_view=mobile` cookie must not make a desktop browser
  // render the H5 shell at full width. The PC preference remains supported
  // as the explicit mobile-profile "切换到电脑版" action.
  const preference = readViewPreference(documentRef)
  if (preference === 'pc' || !mobileDevice) return false
  bridgePcSessionToMobile(windowRef.localStorage)
  windowRef.location.replace(mobileUrlFor(pathname, windowRef))
  return true
}

export function openMobileApp({
  windowRef = typeof window !== 'undefined' ? window : null,
  documentRef = typeof document !== 'undefined' ? document : null,
  pathname = '/'
} = {}) {
  if (!windowRef?.location) return false
  setViewPreference('mobile', documentRef)
  bridgePcSessionToMobile(windowRef.localStorage)
  windowRef.location.assign(mobileUrlFor(pathname, windowRef))
  return true
}
