#!/usr/bin/env node

/**
 * PetLink cross-platform E2E acceptance flow.
 *
 * The flow deliberately sends related requests through both frontend dev
 * proxies (5173 and 5174) while keeping independent role tokens in memory.
 * It creates uniquely named data and does not delete existing project data.
 */

import assert from 'node:assert/strict'
import { randomUUID } from 'node:crypto'

const PC = process.env.PETLINK_PC_ORIGIN || 'http://127.0.0.1:5173/api'
const MOBILE = process.env.PETLINK_MOBILE_ORIGIN || 'http://127.0.0.1:5174/api'
const BACKEND = process.env.PETLINK_BACKEND_ORIGIN || 'http://127.0.0.1:8080'
const operatorPassword = process.env.PETLINK_E2E_OPERATOR_PASSWORD
const adminAccount = process.env.PETLINK_E2E_ADMIN_ACCOUNT || 'admin'
const rescuerAccount = process.env.PETLINK_E2E_RESCUER_ACCOUNT || 'rescuer'
const mobileOriginHeader = process.env.PETLINK_E2E_MOBILE_ORIGIN_HEADER || 'http://127.0.0.1:5174'

if (!operatorPassword) {
  console.error('缺少 PETLINK_E2E_OPERATOR_PASSWORD；请传入本机 admin/rescuer 演示密码。')
  process.exit(2)
}

const suffix = `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 7)}`
const userAccount = `e2e${suffix}`.slice(0, 50)
const userPassword = `CrossPlatform!${suffix}`
const checks = []
const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=', 'base64')

function bodyOf(response) {
  if (!response.body || typeof response.body !== 'object') return response.body
  if ('code' in response.body && response.body.code !== 0) {
    throw new Error(`API business error ${response.status}: ${response.body.code}`)
  }
  return 'data' in response.body ? response.body.data : response.body
}

async function request(origin, path, { method = 'GET', token, body, headers = {}, originHeader } = {}) {
  const finalHeaders = { Accept: 'application/json', ...headers }
  if (token) finalHeaders.Authorization = `Bearer ${token}`
  if (body !== undefined && !(body instanceof FormData)) {
    finalHeaders['Content-Type'] = 'application/json'
    body = JSON.stringify(body)
  }
  if (originHeader) finalHeaders.Origin = originHeader
  const response = await fetch(`${origin}${path}`, { method, headers: finalHeaders, body })
  const text = await response.text()
  let parsed = null
  try { parsed = text ? JSON.parse(text) : null } catch { parsed = text }
  return { status: response.status, body: parsed }
}

function expectStatus(response, expected, label) {
  assert.equal(response.status, expected, `${label}: expected HTTP ${expected}, got ${response.status}`)
  if (response.status >= 400) return response.body
  return bodyOf(response)
}

async function login(origin, account, password, label) {
  const response = await request(origin, '/auth/login', { method: 'POST', body: { account, password } })
  const data = expectStatus(response, 200, `${label} login`)
  assert.ok(data.accessToken, `${label} token missing`)
  return data
}

async function upload(origin, token, label) {
  const form = new FormData()
  form.append('file', new Blob([png], { type: 'image/png' }), `${label}.png`)
  const response = await request(origin, '/files/temporary', { method: 'POST', token, body: form })
  const data = expectStatus(response, 201, `${label} upload`)
  assert.ok(data.token, `${label} upload token missing`)
  return data.token
}

async function step(name, action) {
  const detail = await action()
  checks.push({ name, detail })
  console.log(`PASS ${name}${detail ? ` — ${detail}` : ''}`)
  return detail
}

const health = await request(BACKEND, '/actuator/health')
await step('后端健康检查', () => {
  const data = expectStatus(health, 200, 'health')
  assert.equal(data.status, 'UP')
  return 'UP'
})

const mobileCorsLogin = await request(MOBILE, '/auth/login', {
  method: 'POST',
  body: { account: rescuerAccount, password: operatorPassword },
  originHeader: mobileOriginHeader
})
await step('手机端 Origin/CORS 登录', () => {
  const data = expectStatus(mobileCorsLogin, 200, 'mobile CORS login')
  return data.user.roleCode
})

const admin = await login(PC, adminAccount, operatorPassword, 'PC 管理员')
const rescuer = await login(PC, rescuerAccount, operatorPassword, 'PC 救助人员')
const registered = await request(MOBILE, '/auth/register', {
  method: 'POST',
  body: { account: userAccount, password: userPassword, nickname: `跨平台验收 ${suffix}`, phone: '13800138000' }
})
const user = await step('手机端注册唯一普通用户', () => {
  const data = expectStatus(registered, 201, 'register')
  assert.equal(data.roleCode, 'USER')
  return userAccount
})
const userSession = await login(MOBILE, userAccount, userPassword, '手机端普通用户')
const userToken = userSession.accessToken
const adminToken = admin.accessToken
const rescuerToken = rescuer.accessToken

const userMe = await request(PC, '/users/me', { token: userToken })
await step('同一普通用户令牌跨 PC 端查询资料', () => {
  const data = expectStatus(userMe, 200, 'cross-origin user me')
  assert.equal(data.account, userAccount)
  return data.roleCode
})

const clueImage = await upload(MOBILE, userToken, 'cross-platform-clue')
const keyResponse = await request(MOBILE, '/rescue-clues/idempotency-keys', { method: 'POST', token: userToken })
const clueKey = expectStatus(keyResponse, 201, 'clue idempotency key').idempotencyKey
const cluePayload = {
  location: `跨平台验收地点 ${suffix}`,
  foundTime: new Date().toISOString(),
  animalDescription: `跨平台验收动物 ${suffix}`,
  sceneDescription: '普通用户手机端发布，管理员 PC 端审核',
  contact: '13800138000',
  imageTokens: [clueImage]
}
const createdClueResponse = await request(MOBILE, '/rescue-clues', {
  method: 'POST', token: userToken, body: cluePayload,
  headers: { 'Idempotency-Key': clueKey }
})
const createdClue = await step('手机端普通用户发布救助线索', () => {
  const data = expectStatus(createdClueResponse, 201, 'create clue')
  assert.equal(data.status, 'PENDING_REVIEW')
  return data.clueId
})
const clueId = createdClue

const replayClueResponse = await request(PC, '/rescue-clues', {
  method: 'POST', token: userToken, body: cluePayload,
  headers: { 'Idempotency-Key': clueKey }
})
await step('PC 端重放同一线索幂等请求', () => {
  const data = expectStatus(replayClueResponse, 201, 'clue replay')
  assert.equal(data.clueId, clueId)
  return `clue #${clueId}`
})

const pendingAdmin = await request(PC, `/admin/rescue-clues?page=1&size=100&status=PENDING_REVIEW`, { token: adminToken })
await step('管理员 PC 端看到待审核线索', () => {
  const data = expectStatus(pendingAdmin, 200, 'admin clue queue')
  assert.ok(data.records.some(row => row.id === clueId))
  return `clue #${clueId}`
})

const approved = await request(PC, `/admin/rescue-clues/${clueId}/audit`, { method: 'POST', token: adminToken, body: { decision: 'APPROVE' } })
await step('管理员 PC 端审核通过线索', () => {
  const data = expectStatus(approved, 200, 'approve clue')
  assert.equal(data.status, 'WAITING_ACCEPT')
  return 'WAITING_ACCEPT'
})

const waitingPc = await request(PC, '/rescue-clues/waiting-acceptance?page=1&size=100', { token: rescuerToken })
const waitingMobile = await request(MOBILE, '/rescue-clues/waiting-acceptance?page=1&size=100', { token: rescuerToken })
await step('PC 与手机端救助人员同时看到待接取线索', () => {
  const pcData = expectStatus(waitingPc, 200, 'PC waiting queue')
  const mobileData = expectStatus(waitingMobile, 200, 'mobile waiting queue')
  assert.ok(pcData.records.some(row => row.id === clueId))
  assert.ok(mobileData.records.some(row => row.id === clueId))
  return `PC=${pcData.total}, Mobile=${mobileData.total}`
})

const accepted = await request(MOBILE, `/rescue-clues/${clueId}/accept`, { method: 'POST', token: rescuerToken })
const task = await step('手机端救助人员接取线索', () => {
  const data = expectStatus(accepted, 201, 'accept clue')
  assert.equal(data.status, 'WAITING_START')
  assert.ok(data.taskId)
  return data.taskId
})
const taskId = task

const waitingAfterAccept = await request(PC, '/rescue-clues/waiting-acceptance?page=1&size=100', { token: rescuerToken })
await step('接取后 PC 待接取列表同步移除线索', () => {
  const data = expectStatus(waitingAfterAccept, 200, 'waiting after accept')
  assert.ok(!data.records.some(row => row.id === clueId))
  return `remaining=${data.total}`
})

const started = await request(PC, `/rescue-tasks/${taskId}/start`, { method: 'POST', token: rescuerToken })
await step('PC 端开始救助任务', () => {
  const data = expectStatus(started, 200, 'start task')
  assert.equal(data.status, 'IN_PROGRESS')
  return 'IN_PROGRESS'
})

const rescueRecord = await request(MOBILE, `/rescue-tasks/${taskId}/records`, {
  method: 'POST', token: rescuerToken, body: { content: '手机端记录到场、转运和初步检查过程' }
})
await step('手机端追加救助过程记录', () => {
  const data = expectStatus(rescueRecord, 201, 'rescue record')
  assert.ok(data.id)
  return `record #${data.id}`
})

const success = await request(PC, `/rescue-tasks/${taskId}/result`, {
  method: 'POST', token: rescuerToken,
  body: {
    result: 'SUCCESS',
    animals: [{
      name: `跨平台动物 ${suffix}`, species: 'CAT', sex: 'UNKNOWN', estimatedAgeMonths: 12,
      color: 'orange', healthCondition: '状态稳定', initialHealthRecord: '已完成初步检查', imageTokens: []
    }]
  }
})
const animal = await step('PC 端提交救助成功并创建动物档案', () => {
  const data = expectStatus(success, 200, 'rescue success')
  assert.equal(data.status, 'SUCCESS')
  assert.equal(data.animalIds.length, 1)
  return data.animalIds[0]
})
const animalId = animal

const taskDetailMobile = await request(MOBILE, `/rescue-tasks/${taskId}`, { token: rescuerToken })
await step('手机端读取已完成任务和动物档案', () => {
  const data = expectStatus(taskDetailMobile, 200, 'mobile task detail')
  assert.equal(data.status, 'SUCCESS')
  assert.ok(data.animals.some(row => row.id === animalId))
  return `task #${taskId}`
})

const clueClosed = await request(PC, `/rescue-clues/${clueId}`, { token: adminToken })
await step('救助成功后管理员看到线索闭环为已关闭', () => {
  const data = expectStatus(clueClosed, 200, 'closed clue')
  assert.equal(data.status, 'CLOSED')
  return 'CLOSED'
})

const animalBefore = await request(PC, `/animals/${animalId}`, { token: rescuerToken })
const animalBeforeData = await step('救助人员 PC 端看到负责动物', () => {
  const data = expectStatus(animalBefore, 200, 'responsible animal')
  assert.equal(data.status, 'TREATING')
  assert.equal(data.rescueTaskId, taskId)
  return data
})

const observing = await request(PC, `/animals/${animalId}/status-actions`, {
  method: 'POST', token: rescuerToken, body: { action: 'TO_OBSERVING', version: animalBeforeData.version }
})
const observingData = await step('PC 端推进动物到观察期', () => {
  const data = expectStatus(observing, 200, 'to observing')
  assert.equal(data.status, 'OBSERVING')
  return data.version
})
const available = await request(MOBILE, `/animals/${animalId}/status-actions`, {
  method: 'POST', token: rescuerToken, body: { action: 'OPEN_ADOPTION', version: observingData }
})
await step('手机端救助人员开放动物领养', () => {
  const data = expectStatus(available, 200, 'open adoption')
  assert.equal(data.status, 'AVAILABLE')
  return 'AVAILABLE'
})

const publicAnimal = await request(MOBILE, `/animals/${animalId}`, { token: userToken })
await step('普通用户手机端看到可领养动物', () => {
  const data = expectStatus(publicAnimal, 200, 'public animal')
  assert.equal(data.status, 'AVAILABLE')
  assert.ok(!Object.hasOwn(data, 'rescueTaskId'))
  return `animal #${animalId}`
})

const favorite = await request(MOBILE, `/animals/${animalId}/favorite`, { method: 'POST', token: userToken })
await step('普通用户手机端收藏动物', () => {
  const data = expectStatus(favorite, 201, 'favorite')
  assert.ok(data.favoriteId)
  return `favorite #${data.favoriteId}`
})
const favoritesPc = await request(PC, '/favorites/me?page=1&size=100', { token: userToken })
await step('PC 端读取同一普通用户收藏结果', () => {
  const data = expectStatus(favoritesPc, 200, 'cross-origin favorites')
  assert.ok(data.records.some(row => row.animal?.id === animalId))
  return `total=${data.total}`
})

const application = await request(MOBILE, `/animals/${animalId}/adoption-applications`, {
  method: 'POST', token: userToken,
  body: { adoptionReason: '跨平台验收领养理由', housingCondition: '有安全住房', familyMembers: '两名家庭成员', petExperience: '有养宠经验', contact: '13800138000' }
})
const applicationData = await step('普通用户手机端提交领养申请', () => {
  const data = expectStatus(application, 201, 'adoption application')
  assert.equal(data.status, 'PENDING')
  return data.id
})
const applicationId = applicationData

const adminQueue = await request(PC, '/admin/adoption-applications?page=1&size=100&status=PENDING', { token: adminToken })
await step('管理员 PC 端看到待审核领养申请', () => {
  const data = expectStatus(adminQueue, 200, 'adoption admin queue')
  assert.ok(data.records.some(row => row.id === applicationId))
  return `application #${applicationId}`
})
const audited = await request(PC, `/admin/adoption-applications/${applicationId}/audit`, {
  method: 'POST', token: adminToken, body: { decision: 'APPROVE' }
})
const adoptionRecord = await step('管理员 PC 端批准领养并生成记录', () => {
  const data = expectStatus(audited, 200, 'approve adoption')
  assert.equal(data.application.status, 'APPROVED')
  assert.ok(data.adoptionRecord?.id)
  return data.adoptionRecord.id
})
const recordId = adoptionRecord

const userRecords = await request(MOBILE, '/adoption-records/me?page=1&size=100', { token: userToken })
await step('普通用户手机端看到领养记录', () => {
  const data = expectStatus(userRecords, 200, 'user adoption records')
  assert.ok(data.records.some(row => row.id === recordId))
  return `record #${recordId}`
})

const followKey = randomUUID()
const followPayload = { idempotencyKey: followKey, content: '已适应新环境，进食正常', healthCondition: '状态良好', imageTokens: [] }
const followup = await request(MOBILE, `/adoption-records/${recordId}/follow-ups`, { method: 'POST', token: userToken, body: followPayload })
const followupData = await step('普通用户手机端提交领养回访', () => {
  const data = expectStatus(followup, 201, 'follow-up create')
  assert.ok(data.id)
  return data.id
})
const followupId = followupData
const followupReplay = await request(PC, `/adoption-records/${recordId}/follow-ups`, { method: 'POST', token: userToken, body: followPayload })
await step('PC 端重放回访幂等请求', () => {
  const data = expectStatus(followupReplay, 200, 'follow-up replay')
  assert.equal(data.id, followupId)
  return `follow-up #${followupId}`
})
const rescuerFollowups = await request(PC, `/rescuer/follow-ups?page=1&size=100&animalId=${animalId}`, { token: rescuerToken })
await step('救助人员 PC 端看到普通用户回访', () => {
  const data = expectStatus(rescuerFollowups, 200, 'rescuer follow-ups')
  assert.ok(data.records.some(row => row.id === followupId))
  return `total=${data.total}`
})

const announcement = await request(PC, '/admin/announcements', {
  method: 'POST', token: adminToken, body: { title: `跨平台公告 ${suffix}`, content: '管理员 PC 发布，手机端用户可见' }
})
const announcementData = await step('管理员 PC 端创建公告', () => {
  const data = expectStatus(announcement, 201, 'announcement create')
  assert.equal(data.status, 'DRAFT')
  return data.id
})
const announcementId = announcementData
const published = await request(PC, `/admin/announcements/${announcementId}/publish`, {
  method: 'POST', token: adminToken, body: { version: 0 }
})
await step('管理员 PC 端发布公告', () => {
  const data = expectStatus(published, 200, 'announcement publish')
  assert.equal(data.status, 'PUBLISHED')
  return 'PUBLISHED'
})
const publicAnnouncements = await request(MOBILE, '/announcements?page=1&size=100', { token: userToken })
await step('普通用户手机端看到 PC 发布的公告', () => {
  const data = expectStatus(publicAnnouncements, 200, 'public announcements')
  assert.ok(data.records.some(row => row.id === announcementId))
  return `announcement #${announcementId}`
})

const adminUsers = await request(PC, `/admin/users?page=1&size=100&keyword=${encodeURIComponent(userAccount)}`, { token: adminToken })
const managedUser = await step('管理员 PC 端查询跨平台普通用户', () => {
  const data = expectStatus(adminUsers, 200, 'admin user list')
  const row = data.records.find(item => item.account === userAccount)
  assert.ok(row)
  assert.equal(row.roleCode, 'USER')
  assert.ok(!Object.hasOwn(row, 'passwordHash'))
  return row.id
})
const managedUserId = managedUser
const disabled = await request(PC, `/admin/users/${managedUserId}/disable`, { method: 'POST', token: adminToken })
await step('管理员 PC 禁用用户后业务令牌立即失效', () => {
  const data = expectStatus(disabled, 200, 'disable user')
  assert.equal(data.status, 'DISABLED')
  return 'DISABLED'
})
const disabledMe = await request(MOBILE, '/users/me', { token: userToken })
await step('手机端验证禁用用户不能继续访问', () => {
  expectStatus(disabledMe, 403, 'disabled user')
  return '403'
})
const enabled = await request(PC, `/admin/users/${managedUserId}/enable`, { method: 'POST', token: adminToken })
await step('管理员 PC 恢复用户后跨平台令牌恢复', () => {
  const data = expectStatus(enabled, 200, 'enable user')
  assert.equal(data.status, 'ENABLED')
  return 'ENABLED'
})
const restoredMe = await request(MOBILE, '/users/me', { token: userToken })
await step('手机端验证恢复后的用户可继续访问', () => {
  const data = expectStatus(restoredMe, 200, 'restored user')
  assert.equal(data.account, userAccount)
  return '200'
})

const forbiddenAdmin = await request(MOBILE, '/admin/users?page=1&size=1', { token: userToken })
await step('普通用户不能越权访问管理员接口', () => {
  expectStatus(forbiddenAdmin, 403, 'user admin access')
  return '403'
})
const forbiddenWaiting = await request(MOBILE, '/rescue-clues/waiting-acceptance?page=1&size=1', { token: userToken })
await step('普通用户不能访问救助人员待接取接口', () => {
  expectStatus(forbiddenWaiting, 403, 'user waiting access')
  return '403'
})

const stats = await request(PC, '/admin/stats/overview', { token: adminToken })
await step('管理员 PC 端统计覆盖全业务状态', () => {
  const data = expectStatus(stats, 200, 'admin stats')
  for (const group of ['users', 'rescueClues', 'rescueTasks', 'animals', 'adoptionApplications', 'adoptionRecords', 'followUps']) assert.ok(data[group], `${group} missing`)
  return '7 groups'
})

console.log(`CROSS_PLATFORM_E2E_PASSED checks=${checks.length} user=${userAccount} clue=${clueId} task=${taskId} animal=${animalId} application=${applicationId} record=${recordId} followUp=${followupId} announcement=${announcementId}`)
