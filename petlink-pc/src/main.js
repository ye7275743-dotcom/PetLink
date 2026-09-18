import { createApp } from 'vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { provideGlobalConfig } from 'element-plus/es/components/config-provider/src/hooks/use-global-config.mjs'
import App from './App.vue'
import router from './router/index.js'
import { useAuth } from './store/auth.js'
import { authApi } from './api/index.js'
import { redirectMobileEntry } from './utils/deviceRouting.js'
import './styles/tokens.css'
import './styles/app.css'

// Run before Vue mounts so a phone never briefly renders the desktop shell.
if (!redirectMobileEntry()) {
  const app=createApp(App).use(router)
  provideGlobalConfig({locale:zhCn},app,true)
  app.mount('#app')
}

let refreshing=false
async function refreshOnResume(){
  const auth=useAuth();if(!auth.state.token||refreshing)return
  refreshing=true
  try{await auth.validateSession(authApi.me,{force:true});const allowed=router.currentRoute.value.meta.roles;if(allowed&&!allowed.includes(auth.state.user?.roleCode))router.replace('/login')}catch{}finally{refreshing=false}
}
window.addEventListener('focus',refreshOnResume)
document.addEventListener('visibilitychange',()=>{if(document.visibilityState==='visible')refreshOnResume()})
