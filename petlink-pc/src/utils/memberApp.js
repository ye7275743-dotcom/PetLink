export function memberUrl(screen='home',params={}) {
  const base=import.meta.env.DEV?`${location.protocol}//${location.hostname}:5174/`:'/mobile/'
  const query=new URLSearchParams(params).toString()
  return `${base}#/pages/${screen}/index${query?'?'+query:''}`
}

// Explicit, same-origin USER handoff only; never put credentials in URLs.
export function openMemberApp(session,screen='home',params={}) {
  const destination=new URL(memberUrl(screen,params),location.href)
  if(session?.user?.roleCode==='USER' && destination.origin===location.origin){
    localStorage.setItem('petlink_mobile_token',session.accessToken)
    localStorage.setItem('petlink_mobile_user',JSON.stringify({type:'object',data:session.user}))
  }
  location.assign(destination.href)
}
