import { reactive } from 'vue'
const state=reactive({token:uni.getStorageSync('petlink_mobile_token')||'',user:uni.getStorageSync('petlink_mobile_user')||null,lastValidatedAt:0})
export function useAuth(){
  const setSession=(token,user)=>{state.token=token||'';state.user=user||null;state.lastValidatedAt=user?Date.now():0;if(token)uni.setStorageSync('petlink_mobile_token',token);else uni.removeStorageSync('petlink_mobile_token');if(user)uni.setStorageSync('petlink_mobile_user',user);else uni.removeStorageSync('petlink_mobile_user')}
  const setUser=user=>{state.user=user;state.lastValidatedAt=Date.now();uni.setStorageSync('petlink_mobile_user',user)}
  const logout=()=>setSession('',null)
  return{state,setSession,setUser,logout}
}
