export function validateRegistration(form){
  const password=String(form.password||'')
  return {
    account:/^[A-Za-z0-9_]{4,50}$/.test(form.account.trim())?'':'账号需为 4～50 位字母、数字或下划线',
    nickname:form.nickname.trim().length<=50?'':'昵称不能超过 50 字',
    password:[...password].length>=8&&[...password].length<=64&&new TextEncoder().encode(password).length<=72?'':'密码需为 8～64 个字符，请减少过长的中文或表情组合',
    confirm:password===form.confirm?'':'两次输入的密码不一致',
    phone:!form.phone.trim()||/^1[3-9]\d{9}$/.test(form.phone.trim())?'':'请输入正确的手机号'
  }
}
