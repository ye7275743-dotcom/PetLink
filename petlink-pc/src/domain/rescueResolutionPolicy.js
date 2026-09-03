export function failureResolutionFromDialog(action){
  if(action === 'confirm') return 'REOPEN'
  if(action === 'cancel') return 'CLOSE'
  return null
}
