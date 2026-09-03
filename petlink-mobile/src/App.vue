<script>
import { authApi } from './api/index.js'
import { useAuth } from './store/auth.js'
import { enforceCurrentMobileRoute } from './composables/routeGuard.js'
async function refreshSession(){
  const auth=useAuth();if(!auth.state.token)return
  try{auth.setUser(await authApi.me())}catch(e){if(e?.status!==401)console.warn('PetLink session refresh failed',e)}
}
export default {
  onLaunch(){ refreshSession() },
  async onShow(){ await refreshSession(); enforceCurrentMobileRoute() }
}
</script>
<style>
@import './uni.scss';
.input:not(.area){height:72rpx}
</style>
