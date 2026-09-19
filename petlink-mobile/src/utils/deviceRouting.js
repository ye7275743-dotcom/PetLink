const VIEW_COOKIE = 'petlink_view'
const COOKIE_MAX_AGE = 60 * 60 * 24 * 30

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

function unwrap(value) {
  if (typeof value !== 'string') return value
  try { return JSON.parse(value) } catch { return null }
}

function normalizeUser(value) {
  const parsed = unwrap(value)
  if (!parsed) return null
  if (parsed?.roleCode) return parsed
  if (parsed?.data?.roleCode) return parsed.data
  return null
}

export function setViewPreference(view, documentRef = typeof document !== 'undefined' ? document : null) {
  if (!documentRef) return
  if (view !== 'pc' && view !== 'mobile') {
    documentRef.cookie = `${VIEW_COOKIE}=; Path=/; Max-Age=0; SameSite=Lax`
    return
  }
  documentRef.cookie = `${VIEW_COOKIE}=${view}; Path=/; Max-Age=${COOKIE_MAX_AGE}; SameSite=Lax`
}

export function bridgeMobileSessionToPc({ uniRef = typeof uni !== 'undefined' ? uni : null, storage = typeof localStorage !== 'undefined' ? localStorage : null } = {}) {
  if (!uniRef || !storage) return false
  let token = ''
  let rawUser = null
  try {
    token = uniRef.getStorageSync('petlink_mobile_token') || ''
    rawUser = uniRef.getStorageSync('petlink_mobile_user')
  } catch { return false }
  const user = normalizeUser(rawUser)
  if (!token || !user) return false
  try {
    storage.setItem('petlink_pc_token', token)
    storage.setItem('petlink_pc_user', JSON.stringify(user))
    return true
  } catch { return false }
}

function desktopBaseUrl(windowRef) {
  const dev = typeof import.meta !== 'undefined' && Boolean(import.meta.env?.DEV)
  return dev ? `${windowRef.location.protocol}//${windowRef.location.hostname}:5173/` : `${windowRef.location.origin}/`
}

/**
 * H5 is mounted at `/mobile/` in production and at port 5174 in development.
 * If somebody opens that entry in a desktop browser, leave before UniApp
 * mounts so the PC shell is not stretched across the viewport.
 */
export function redirectDesktopEntry({
  windowRef = typeof window !== 'undefined' ? window : null,
  navigatorRef = typeof navigator !== 'undefined' ? navigator : null
} = {}) {
  if (!windowRef?.location || !navigatorRef || isMobileDevice(navigatorRef, windowRef)) return false
  const requestedView = new URLSearchParams(windowRef.location.search || '').get('view')
  if (requestedView === 'mobile') return false
  const dev = typeof import.meta !== 'undefined' && Boolean(import.meta.env?.DEV)
  const target = dev
    ? `${windowRef.location.protocol}//${windowRef.location.hostname}:5173/`
    : `${windowRef.location.origin}/`
  windowRef.location.replace(target)
  return true
}

/** Switch to the desktop client while preserving the H5 session. */
export function openDesktopApp({
  windowRef = typeof window !== 'undefined' ? window : null,
  documentRef = typeof document !== 'undefined' ? document : null,
  uniRef = typeof uni !== 'undefined' ? uni : null
} = {}) {
  if (!windowRef?.location) return false
  setViewPreference('pc', documentRef)
  bridgeMobileSessionToPc({ uniRef, storage: windowRef.localStorage })
  windowRef.location.assign(desktopBaseUrl(windowRef))
  return true
}
