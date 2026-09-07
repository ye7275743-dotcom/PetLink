const PUBLIC_ROUTES = new Set([
  'home','animal','announcements','announcementDetail','login','register','profile'
])

const RESCUER_ONLY_ROUTES = new Set([
  'tasks','waitingClue','taskWaitingStart','taskProgress','addRescueRecord',
  'submitRescueResult','responsibleAnimals','animalManage','adoptionOverview',
  'rescuerFollowups'
])

const MEMBER_ROUTES = new Set([
  'clue','clueEdit','applicationDetail','applicationRejected','applicationInvalidated',
  'apply','records','followup','followupHistory','followupDetail','favorites',
  'profile','profileEdit'
])

export function mobileRouteDecision({ name, hasToken=false, role=null }) {
  if (PUBLIC_ROUTES.has(name)) return { allow:true, redirect:null, clearSession:false }
  if (!hasToken) return { allow:false, redirect:'login', clearSession:false }
  if (!['USER','RESCUER'].includes(role)) return { allow:false, redirect:'login', clearSession:true }
  if (RESCUER_ONLY_ROUTES.has(name) && role !== 'RESCUER') return { allow:false, redirect:'home', clearSession:false }
  if (MEMBER_ROUTES.has(name) || RESCUER_ONLY_ROUTES.has(name)) return { allow:true, redirect:null, clearSession:false }
  return { allow:false, redirect:'home', clearSession:false }
}

export const mobileRouteSets = {
  public: PUBLIC_ROUTES,
  rescuerOnly: RESCUER_ONLY_ROUTES,
  member: MEMBER_ROUTES
}
