export const mainlandPhone = /^1[3-9]\d{9}$/
export const accountPattern = /^[A-Za-z0-9_]{4,50}$/

export function textRule(value,{label='字段',required=true,min=1,max=Infinity}={}){
  const text = String(value ?? '').trim()
  if(required && !text) return `${label}不能为空`
  if(!text) return ''
  if(text.length < min) return `${label}至少 ${min} 个字符`
  if(text.length > max) return `${label}不能超过 ${max} 个字符`
  return ''
}
export function phoneRule(value,{required=false}={}){
  const text = String(value ?? '').trim()
  if(!text && !required) return ''
  if(!mainlandPhone.test(text)) return '请输入正确的中国大陆手机号'
  return ''
}
export function accountRule(value){ return accountPattern.test(String(value??'').trim()) ? '' : '账号需为 4～50 位字母、数字或下划线' }
export function passwordRule(value){
  const text = String(value ?? '')
  if(text.length < 8 || text.length > 64) return '密码长度必须为 8～64 个字符'
  try{ if(new TextEncoder().encode(text).length > 72) return '密码编码后不能超过 72 字节' }catch{}
  return ''
}
export function foundTimeRule(value){
  if(!value) return '发现时间不能为空'
  const t = Date.parse(value)
  if(Number.isNaN(t)) return '发现时间必须为合法的标准日期时间'
  if(t > Date.now()+1000) return '发现时间不能晚于当前时间'
  return ''
}
export function firstError(errors){ return Object.values(errors).find(Boolean) || '' }
