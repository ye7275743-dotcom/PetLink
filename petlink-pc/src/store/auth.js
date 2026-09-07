import { reactive } from 'vue'

function readUser(){try{const u=JSON.parse(localStorage.getItem('petlink_pc_user')||'null');return u?.roleCode?u:null}catch{return null}}
// Shared v1 keys are intentionally ignored; one fresh login is required after upgrade.
const state = reactive({
  token: localStorage.getItem('petlink_pc_token') || '',
  user: readUser(),
  lastValidatedAt: 0
})

let pendingValidation=null
export function useAuth(){
  const setSession = (token, user=null) => {
    state.token = token || ''
    state.user = user
    state.lastValidatedAt = user ? Date.now() : 0
    if(token) localStorage.setItem('petlink_pc_token', token); else localStorage.removeItem('petlink_pc_token')
    if(user) localStorage.setItem('petlink_pc_user', JSON.stringify(user)); else localStorage.removeItem('petlink_pc_user')
  }
  const setUser = user => { state.user=user; state.lastValidatedAt=Date.now(); localStorage.setItem('petlink_pc_user', JSON.stringify(user)) }
  const logout = () => setSession('', null)
  const validateSession = async(fetchMe,{force=false,maxAge=30000}={}) => {
    if(!state.token) return null
    if(!force && state.user && Date.now()-state.lastValidatedAt < maxAge) return state.user
    if(pendingValidation?.token===state.token)return pendingValidation.promise
    const token=state.token
    const promise=Promise.resolve().then(fetchMe).then(fresh=>{if(state.token===token)setUser(fresh);return fresh}).finally(()=>{if(pendingValidation?.promise===promise)pendingValidation=null})
    pendingValidation={token,promise};return promise
  }
  return { state, setSession, setUser, logout, validateSession }
}
