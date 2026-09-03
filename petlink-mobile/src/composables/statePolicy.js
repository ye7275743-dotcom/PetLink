export function animalActions(status){
  return ({TREATING:['TO_OBSERVING'],OBSERVING:['OPEN_ADOPTION'],AVAILABLE:['SUSPEND_ADOPTION'],SUSPENDED:['RESUME_ADOPTION'],ADOPTED:[]})[status]||[]
}
export function rescueActions(status,role){
  if(role==='RESCUER') return ({WAITING_START:['START_RESCUE'],IN_PROGRESS:['ADD_RECORD','SUCCESS','FAILED']})[status]||[]
  if(role==='ADMIN') return ['WAITING_START','IN_PROGRESS'].includes(status)?['CANCEL_RESCUE']:status==='FAILED'?['REOPEN','CLOSE']:[]
  return []
}
