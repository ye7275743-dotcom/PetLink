export const WORKBENCH_ROLES = ['ADMIN','RESCUER']
export function isWorkbenchRole(role){ return WORKBENCH_ROLES.includes(role) }

export function routeDecision({isPublic=false,isLogin=false,hasToken=false,role=null,allowedRoles=null}){
  const workbench = isWorkbenchRole(role)
  if(isPublic) return isLogin && hasToken && workbench ? '/dashboard' : true
  if(!hasToken) return '/login'
  if(!workbench) return '/login'
  if(Array.isArray(allowedRoles) && allowedRoles.length && !allowedRoles.includes(role)) return '/dashboard'
  return true
}
