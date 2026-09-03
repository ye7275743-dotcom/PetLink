import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router/index.js'
import { useAuth } from './store/auth.js'
import { authApi } from './api/index.js'
import './styles/tokens.css'
import './styles/app.css'

const app=createApp(App).use(router).use(ElementPlus,{locale:zhCn})
app.mount('#app')

let refreshing=false
async function refreshOnResume(){
  const auth=useAuth();if(!auth.state.token||refreshing)return
  refreshing=true
  try{await auth.validateSession(authApi.me,{force:true})}catch{}finally{refreshing=false}
}
window.addEventListener('focus',refreshOnResume)
document.addEventListener('visibilitychange',()=>{if(document.visibilityState==='visible')refreshOnResume()})
