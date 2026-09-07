#!/usr/bin/env node
// Real API/MySQL acceptance. Creates isolated fixtures; never alters existing users.
import assert from 'node:assert/strict'
const base=process.env.PETLINK_PC_ORIGIN||'http://127.0.0.1:5173/api'
const password=process.env.PETLINK_E2E_OPERATOR_PASSWORD
if(!password)throw Error('PETLINK_E2E_OPERATOR_PASSWORD is required')
let count=0
async function req(path,token,body,expected=200){
  const r=await fetch(base+path,{method:body===undefined?'GET':'POST',headers:{'Content-Type':'application/json',...(token?{Authorization:`Bearer ${token}`}:{})},body:body===undefined?undefined:JSON.stringify(body)})
  const b=await r.json();assert.equal(r.status,expected,`${path}: ${JSON.stringify(b)}`);return b.data
}
function pass(name){count++;console.log('PASS '+name)}
const admin=await req('/auth/login',null,{account:process.env.PETLINK_E2E_ADMIN_ACCOUNT||'admin',password})
const at=admin.accessToken,account=`roleqa${Date.now().toString(36)}`,up='RoleAcceptance!20260904'
await req('/auth/register',null,{account,password:up,nickname:'权限生命周期验收',roleCode:'ADMIN'},400)
const user=await req('/auth/register',null,{account,password:up,nickname:'权限生命周期验收'},201)
assert.equal(user.roleCode,'USER');pass('注册拒绝越权角色输入，新账号固定普通用户')
await req('/auth/register',null,{account,password:up},409);pass('重复账号拒绝')
const ut=(await req('/auth/login',null,{account,password:up})).accessToken
const users=await req('/admin/users?keyword='+account,at);const uid=users.records.find(u=>u.account===account).id
pass('注册用户进入管理员列表')
const role=(roleCode,expected=200,token=at,reason='验收角色生命周期')=>req(`/admin/users/${uid}/role`,token,{roleCode,reason},expected)
await role('RESCUER',403,ut);pass('普通用户不可调整角色')
await role('ADMIN',400);pass('不可通过角色接口授予管理员')
await role('RESCUER',400,at,'');pass('调整原因必填')
await role('RESCUER');assert.equal((await req('/users/me',ut)).roleCode,'RESCUER');await req('/rescue-clues/waiting-acceptance',ut);pass('升级后旧会话即时获得救助权限')
await role('RESCUER',409);pass('重复升级拒绝')
await role('USER');assert.equal((await req('/users/me',ut)).roleCode,'USER');await req('/rescue-clues/waiting-acceptance',ut,undefined,403);pass('降级后旧会话即时失去救助权限')
await req(`/admin/users/${admin.user.id}/role`,at,{roleCode:'USER',reason:'禁止管理员自降级'},403);pass('管理员账号保护')
await req(`/admin/users/${uid}/disable`,at,{});await req('/users/me',ut,undefined,403);await req('/auth/login',null,{account,password:up},403);pass('禁用同时阻止旧会话和新登录')
await req(`/admin/users/${uid}/enable`,at,{});await req('/users/me',ut);pass('启用后恢复访问')
await req('/admin/stats/trends?from=2020-01-01&to=2026-09-04',at,undefined,400);pass('过大统计日期范围拒绝')
// Exercise demotion versus task acceptance against real transactions, not mocks.
for(let i=0;i<5;i++){
  const form=new FormData();form.append('file',new Blob([Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=','base64')],{type:'image/png'}),'acceptance.png')
  const uploaded=await fetch(base+'/files/temporary',{method:'POST',headers:{Authorization:`Bearer ${ut}`},body:form});assert.equal(uploaded.status,201);const image=(await uploaded.json()).data.token
  const key=await req('/rescue-clues/idempotency-keys',ut,{},201)
  const cr=await fetch(base+'/rescue-clues',{method:'POST',headers:{Authorization:`Bearer ${ut}`,'Content-Type':'application/json','Idempotency-Key':key.idempotencyKey},body:JSON.stringify({location:'本地并发验收地点',foundTime:new Date().toISOString(),animalDescription:'角色并发验收',contact:'13800138000',imageTokens:[image]})})
  const crBody=await cr.json();assert.equal(cr.status,201,`create clue: ${JSON.stringify(crBody)}`);const clueId=crBody.data.clueId
  await req(`/admin/rescue-clues/${clueId}/audit`,at,{decision:'APPROVE'})
  await role('RESCUER')
  const post=(path,token,body)=>fetch(base+path,{method:'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},body:JSON.stringify(body)})
  const [accept,demote]=await Promise.all([post(`/rescue-clues/${clueId}/accept`,ut,{}),post(`/admin/users/${uid}/role`,at,{roleCode:'USER',reason:'并发降级验收'})])
  if(accept.status===201){
    assert.equal(demote.status,409,'Accepted task must block concurrent demotion')
    const taskId=(await accept.json()).data.taskId
    await role('USER',409);pass(`活动任务阻止降级 ${i+1}`)
    await req(`/rescue-tasks/${taskId}/start`,ut,{})
    await req(`/rescue-tasks/${taskId}/result`,ut,{result:'FAILED',failureReason:'本地并发验收结束'})
    await role('USER')
  }else{assert.equal(accept.status,403);assert.equal(demote.status,200)}
  assert.equal((await req('/users/me',ut)).roleCode,'USER')
  pass(`接取与降级并发一致性 ${i+1}`)
}
console.log(`ACCOUNT_LIFECYCLE_PASSED checks=${count} account=${account} id=${uid}`)
