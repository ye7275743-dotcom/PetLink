#!/usr/bin/env node
import assert from 'node:assert/strict'
const base=process.env.PETLINK_PC_ORIGIN||'http://127.0.0.1:5173/api'
const password=process.env.PETLINK_E2E_OPERATOR_PASSWORD
if(!password)throw Error('PETLINK_E2E_OPERATOR_PASSWORD required')
let count=0
function pass(name){count++;console.log('PASS '+name)}
async function req(path,token,body,status=200,method=body===undefined?'GET':'POST'){
 const r=await fetch(base+path,{method,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},body:body===undefined?undefined:JSON.stringify(body)})
 const b=r.status===204?{}:await r.json();assert.equal(r.status,status,path+': '+b.message);return b.data
}
const admin=await req('/auth/login',null,{account:process.env.PETLINK_E2E_ADMIN_ACCOUNT||'admin',password})
const at=admin.accessToken,account='deleteqa'+Date.now().toString(36),up='DeletionAcceptance!20260904'
await req('/auth/register',null,{account,password:up,nickname:'删除功能独立验收'},201)
const user=await req('/auth/login',null,{account,password:up}),ut=user.accessToken,uid=user.user.id
await req('/admin/users/'+admin.user.id,at,undefined,403,'DELETE');pass('管理员账号不能删除')
await req('/admin/users/'+uid,ut,undefined,403,'DELETE');pass('普通用户不能删除账号')
const a=await req('/admin/announcements',at,{title:'删除公告验收 '+account,content:'只用于自动化验收的公告草稿'},201)
const aid=a.id
assert.ok((await req('/admin/announcements?keyword='+account,at)).records.some(r=>r.id===aid));pass('公告关键词按服务端分页检索')
await req('/admin/announcements/'+aid+'?version='+a.version,ut,undefined,403,'DELETE');pass('普通用户不能删除公告')
const published=await req('/admin/announcements/'+aid+'/publish',at,{version:a.version})
await req('/admin/announcements/'+aid+'?version='+a.version,at,undefined,409,'DELETE');pass('旧版本不能删除已更新公告')
await req('/announcements/'+aid);pass('发布公告公众可读')
await req('/admin/announcements/'+aid+'?version='+published.version,at,undefined,200,'DELETE')
await req('/announcements/'+aid,null,undefined,404);await req('/admin/announcements/'+aid,at,undefined,404)
assert.ok(!(await req('/admin/announcements?keyword='+account,at)).records.some(r=>r.id===aid));pass('删除公告后公开详情和后台列表同时移除')
await req('/admin/announcements/'+aid+'?version='+published.version,at,undefined,404,'DELETE');pass('重复删除返回404')
await req('/admin/users/'+uid,at,undefined,200,'DELETE')
await req('/users/me',ut,undefined,401);pass('删除用户后旧令牌失效')
await req('/auth/login',null,{account,password:up},401);pass('删除用户后不能重新登录')
assert.equal((await req('/admin/users?keyword='+account,at)).total,0);pass('删除用户不再出现在管理列表')
await req('/admin/users/'+uid+'/enable',at,{},404);pass('删除用户不能通过启用接口恢复')
await req('/auth/register',null,{account,password:up},409);pass('历史账号不会被重复注册冒用')
const followups=await req('/admin/follow-ups?page=1&size=20',at)
if(followups.records.length){const row=followups.records[0];const filtered=await req('/admin/follow-ups?userId='+row.submitterId,at);assert.ok(filtered.records.every(r=>r.submitterId===row.submitterId));const d=await req('/follow-ups/'+row.id,at);assert.deepEqual(row.images,d.images)}
pass('回访筛选和批量图片与详情一致')
const records=await req('/admin/adoption-records?userId='+uid,at);assert.ok(records.records.every(r=>r.userId===uid));pass('领养记录按用户筛选')
console.log('UI_MANAGEMENT_E2E_PASSED checks='+count+' user='+uid+' announcement='+aid)
