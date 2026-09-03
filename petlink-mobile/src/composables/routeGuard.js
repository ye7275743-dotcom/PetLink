import { onShow } from '@dcloudio/uni-app'
import { useAuth } from '../store/auth.js'
import { mobileRouteDecision } from './routePolicy.js'

function routeUrl(name){ return `/pages/${name}/index` }

export function enforceMobileRoute(name){
  const auth = useAuth()
  const decision = mobileRouteDecision({
    name,
    hasToken: !!auth.state.token,
    role: auth.state.user?.roleCode || null
  })
  if (decision.allow) return true
  if (decision.clearSession) auth.logout()
  uni.reLaunch({ url: routeUrl(decision.redirect) })
  return false
}

export function enforceCurrentMobileRoute(){
  const pages = typeof getCurrentPages === 'function' ? getCurrentPages() : []
  const route = pages[pages.length-1]?.route || ''
  const match = /^pages\/([^/]+)\/index$/.exec(route)
  return match ? enforceMobileRoute(match[1]) : true
}

export function useRouteGuard(name){
  onShow(() => enforceMobileRoute(name))
}
