import { computed } from 'vue'
import { useAuth } from '../store/auth.js'

export const routes = {
  home:'home', animal:'animal', clue:'clue', clueEdit:'clueEdit', tasks:'tasks', waitingClue:'waitingClue',
  taskWaitingStart:'taskWaitingStart', taskProgress:'taskProgress', addRescueRecord:'addRescueRecord',
  submitRescueResult:'submitRescueResult', responsibleAnimals:'responsibleAnimals', animalManage:'animalManage',
  applicationDetail:'applicationDetail', applicationRejected:'applicationRejected', applicationInvalidated:'applicationInvalidated',
  adoptionOverview:'adoptionOverview', followupHistory:'followupHistory', followupDetail:'followupDetail',
  rescuerFollowups:'rescuerFollowups', apply:'apply', records:'records', followup:'followup', favorites:'favorites',
  announcements:'announcements', announcementDetail:'announcementDetail', profile:'profile', profileEdit:'profileEdit',
  login:'login', register:'register'
}

export function useNavigation(){
  const auth = useAuth()
  const logged = computed(() => !!auth.state.token)
  function goto(name,q={}){
    const query = Object.entries(q).filter(([,v])=>v!==undefined&&v!==null&&v!=='').map(([k,v])=>`${encodeURIComponent(k)}=${encodeURIComponent(v)}`).join('&')
    uni.navigateTo({url:`/pages/${routes[name]}/index${query?'?'+query:''}`})
  }
  function switchMain(name){ uni.redirectTo({url:`/pages/${routes[name]}/index`}) }
  function requireLogin(name,q={}){ if(!logged.value){ uni.setStorageSync('petlink_mobile_return_to',{name,q});goto('login'); return false } goto(name,q); return true }
  return { goto, switchMain, requireLogin, logged }
}
