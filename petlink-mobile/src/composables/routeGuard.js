import { onShow } from '@dcloudio/uni-app'
import { authApi } from '../api/index.js'
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
  if(!auth.state.token&&decision.redirect==='login'){
    const pages=typeof getCurrentPages==='function'?getCurrentPages():[]
    uni.setStorageSync('petlink_mobile_return_to',{name,q:pages[pages.length-1]?.options||{}})
  }
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
  onShow(async () => {
    const auth=useAuth()
    if(auth.state.token){try{auth.setUser(await authApi.me())}catch(e){if(e?.status===401||e?.code===40302)auth.logout()}}
    enforceMobileRoute(name)
  })
}
