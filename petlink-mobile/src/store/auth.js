import { reactive } from 'vue'
function readUser(){
  try{
    const value=uni.getStorageSync('petlink_mobile_user')
    if(value?.roleCode)return value
    if(value?.data?.roleCode)return value.data
    if(typeof value==='string'){
      const parsed=JSON.parse(value)
      return parsed?.roleCode?parsed:(parsed?.data?.roleCode?parsed.data:null)
    }
  }catch{}
  return null
}
const state=reactive({token:uni.getStorageSync('petlink_mobile_token')||'',user:readUser(),lastValidatedAt:0})
export function useAuth(){
  const setSession=(token,user)=>{state.token=token||'';state.user=user||null;state.lastValidatedAt=user?Date.now():0;if(token)uni.setStorageSync('petlink_mobile_token',token);else uni.removeStorageSync('petlink_mobile_token');if(user)uni.setStorageSync('petlink_mobile_user',user);else uni.removeStorageSync('petlink_mobile_user')}
  const setUser=user=>{state.user=user;state.lastValidatedAt=Date.now();uni.setStorageSync('petlink_mobile_user',user)}
  const logout=()=>setSession('',null)
  return{state,setSession,setUser,logout}
}
