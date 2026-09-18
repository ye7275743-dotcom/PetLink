#!/usr/bin/env node

/** Add visible WAITING_START and IN_PROGRESS task samples without deleting data. */
import { readFile } from 'node:fs/promises'
import { basename, dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const PROJECT_ROOT = dirname(dirname(fileURLToPath(import.meta.url)))
const API = (process.env.PETLINK_DEMO_ORIGIN || 'http://127.0.0.1:8080/api').replace(/\/$/, '')
const PASSWORD = process.env.PETLINK_DEMO_PASSWORD
const TAG = process.env.PETLINK_DEMO_TAG || `演示任务状态-${new Date().toISOString().slice(0, 16).replace(/[T:]/g, '-')}`
const IMAGE = process.env.PETLINK_DEMO_DOG || join(PROJECT_ROOT, 'docs/demo-assets/demo-black-white-dog.png')

if (process.env.PETLINK_ALLOW_LEGACY_DEMO_DATA !== '1') {
  console.error('该脚本仅保留作历史回归，已停止追加旧演示数据；请改用 scripts/seed-real-cases.mjs。')
  process.exit(3)
}

if (!PASSWORD) { console.error('缺少 PETLINK_DEMO_PASSWORD。'); process.exit(2) }

async function request(path, { method = 'GET', token, body, form, headers: extra = {} } = {}) {
  const headers = { Accept: 'application/json', ...extra }
  if (token) headers.Authorization = `Bearer ${token}`
  let requestBody = form || body
  if (!form && body !== undefined) { headers['Content-Type'] = 'application/json'; requestBody = JSON.stringify(body) }
  const response = await fetch(`${API}${path}`, { method, headers, body: requestBody })
  const text = await response.text()
  let parsed
  try { parsed = text ? JSON.parse(text) : null } catch { parsed = text }
  return { status: response.status, body: parsed }
}

function dataOf(response, expected, label) {
  if (response.status !== expected) throw new Error(`${label}: expected ${expected}, got ${response.status}; ${JSON.stringify(response.body)}`)
  if (response.body?.code !== undefined && response.body.code !== 0) throw new Error(`${label}: business code ${response.body.code}`)
  return response.body?.data ?? response.body
}

async function login(account) {
  return dataOf(await request('/auth/login', { method: 'POST', body: { account, password: PASSWORD } }), 200, `${account} 登录`).accessToken
}

async function upload(token, label) {
  const bytes = await readFile(IMAGE)
  const form = new FormData()
  form.append('file', new Blob([bytes], { type: 'image/png' }), `${label}-${basename(IMAGE)}`)
  return dataOf(await request('/files/temporary', { method: 'POST', token, form }), 201, `${label} 图片上传`).token
}

async function clue(userToken, label) {
  const imageToken = await upload(userToken, label)
  const key = dataOf(await request('/rescue-clues/idempotency-keys', { method: 'POST', token: userToken }), 201, `${label} 幂等键`).idempotencyKey
  return dataOf(await request('/rescue-clues', {
    method: 'POST', token: userToken, headers: { 'Idempotency-Key': key },
    body: {
      location: `${TAG}｜${label === 'waiting-start' ? '社区服务站' : '河畔步道'} `,
      foundTime: new Date(Date.now() - 60_000).toISOString(),
      animalDescription: '一只黑白狗狗的演示救助线索，用于展示任务状态流转。',
      sceneDescription: '已安排救助人员跟进，现场保持安静并准备饮水。',
      contact: '13800138000', imageTokens: [imageToken]
    }
  }), 201, `${label} 发布线索`).clueId
}

async function audit(adminToken, clueId) {
  dataOf(await request(`/admin/rescue-clues/${clueId}/audit`, { method: 'POST', token: adminToken, body: { decision: 'APPROVE' } }), 200, `线索 ${clueId} 审核`)
}

async function createTask(rescuerToken, clueId, start) {
  const accepted = dataOf(await request(`/rescue-clues/${clueId}/accept`, { method: 'POST', token: rescuerToken }), 201, `线索 ${clueId} 接取`)
  const taskId = accepted.taskId
  if (start) {
    dataOf(await request(`/rescue-tasks/${taskId}/start`, { method: 'POST', token: rescuerToken }), 200, `任务 ${taskId} 开始`)
    dataOf(await request(`/rescue-tasks/${taskId}/records`, {
      method: 'POST', token: rescuerToken,
      body: { content: '已到达现场，完成初步评估并持续观察动物状态。' }
    }), 201, `任务 ${taskId} 过程记录`)
  }
  return taskId
}

async function main() {
  await readFile(IMAGE)
  const [adminToken, rescuerToken, userToken] = await Promise.all([login('admin'), login('rescuer'), login('user')])
  const waitingClue = await clue(userToken, 'waiting-start')
  await audit(adminToken, waitingClue)
  const waitingTask = await createTask(rescuerToken, waitingClue, false)
  const activeClue = await clue(userToken, 'in-progress')
  await audit(adminToken, activeClue)
  const activeTask = await createTask(rescuerToken, activeClue, true)
  console.log(JSON.stringify({ tag: TAG, waitingStart: { clueId: waitingClue, taskId: waitingTask }, inProgress: { clueId: activeClue, taskId: activeTask } }, null, 2))
}

main().catch(error => { console.error(`演示任务状态创建失败：${error.message}`); process.exitCode = 1 })
