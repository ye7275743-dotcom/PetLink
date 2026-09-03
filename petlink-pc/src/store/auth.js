import { reactive } from 'vue'

const state = reactive({
  token: localStorage.getItem('petlink_token') || '',
  user: JSON.parse(localStorage.getItem('petlink_user') || 'null'),
  lastValidatedAt: 0
})

export function useAuth(){
  const setSession = (token, user=null) => {
    state.token = token || ''
    state.user = user
    state.lastValidatedAt = user ? Date.now() : 0
    if(token) localStorage.setItem('petlink_token', token); else localStorage.removeItem('petlink_token')
    if(user) localStorage.setItem('petlink_user', JSON.stringify(user)); else localStorage.removeItem('petlink_user')
  }
  const setUser = user => { state.user=user; state.lastValidatedAt=Date.now(); localStorage.setItem('petlink_user', JSON.stringify(user)) }
  const logout = () => setSession('', null)
  const validateSession = async(fetchMe,{force=false,maxAge=30000}={}) => {
    if(!state.token) return null
    if(!force && state.user && Date.now()-state.lastValidatedAt < maxAge) return state.user
    const fresh=await fetchMe(); setUser(fresh); return fresh
  }
  return { state, setSession, setUser, logout, validateSession }
}
