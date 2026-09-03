#!/usr/bin/env node

/**
 * Create a complete, non-destructive PetLink demonstration dataset.
 *
 * Required environment:
 *   PETLINK_DEMO_PASSWORD=the shared demo password
 * Optional environment:
 *   PETLINK_DEMO_ORIGIN=http://127.0.0.1:8080/api
 *   PETLINK_DEMO_TAG=demo-batch-name
 *   PETLINK_DEMO_CAT=/absolute/path/to/cat.png
 *   PETLINK_DEMO_DOG=/absolute/path/to/dog.png
 *   PETLINK_DEMO_RABBIT=/absolute/path/to/rabbit.png
 */

import { readFile } from 'node:fs/promises'
import { basename, dirname, join } from 'node:path'
import { randomUUID } from 'node:crypto'
import { fileURLToPath } from 'node:url'

const PROJECT_ROOT = dirname(dirname(fileURLToPath(import.meta.url)))
const API = (process.env.PETLINK_DEMO_ORIGIN || 'http://127.0.0.1:8080/api').replace(/\/$/, '')
const PASSWORD = process.env.PETLINK_DEMO_PASSWORD
const TAG = process.env.PETLINK_DEMO_TAG || `演示批次-${new Date().toISOString().slice(0, 16).replace(/[T:]/g, '-')}`
const ASSETS = {
  cat: process.env.PETLINK_DEMO_CAT || join(PROJECT_ROOT, 'docs/demo-assets/demo-orange-cat.png'),
  dog: process.env.PETLINK_DEMO_DOG || join(PROJECT_ROOT, 'docs/demo-assets/demo-black-white-dog.png'),
  rabbit: process.env.PETLINK_DEMO_RABBIT || join(PROJECT_ROOT, 'docs/demo-assets/demo-gray-rabbit.png')
}

if (!PASSWORD) {
  console.error('缺少 PETLINK_DEMO_PASSWORD。')
  process.exit(2)
}

async function request(path, { method = 'GET', token, body, form, headers: extraHeaders = {} } = {}) {
  const headers = { Accept: 'application/json', ...extraHeaders }
  if (token) headers.Authorization = `Bearer ${token}`
  let requestBody = body
  if (form) requestBody = form
  else if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
    requestBody = JSON.stringify(body)
  }
  const response = await fetch(`${API}${path}`, { method, headers, body: requestBody })
  const text = await response.text()
  let parsed
  try { parsed = text ? JSON.parse(text) : null } catch { parsed = text }
  return { status: response.status, body: parsed }
}

function dataOf(response, expected, label) {
  if (response.status !== expected) {
    throw new Error(`${label}: expected ${expected}, got ${response.status}; ${JSON.stringify(response.body)}`)
  }
  if (response.body && typeof response.body === 'object' && response.body.code !== undefined && response.body.code !== 0) {
    throw new Error(`${label}: business code ${response.body.code}`)
  }
  return response.body?.data ?? response.body
}

async function login(account) {
  const result = dataOf(await request('/auth/login', {
    method: 'POST',
    body: { account, password: PASSWORD }
  }), 200, `${account} 登录`)
  if (!result.accessToken) throw new Error(`${account} 登录未返回 accessToken`)
  return result.accessToken
}

async function upload(token, filePath, label) {
  const bytes = await readFile(filePath)
  const form = new FormData()
  form.append('file', new Blob([bytes], { type: 'image/png' }), `${label}-${basename(filePath)}`)
  const result = dataOf(await request('/files/temporary', { method: 'POST', token, form }), 201, `${label} 图片上传`)
  if (!result.token) throw new Error(`${label} 上传未返回 token`)
  return result.token
}

async function createClue(userToken, imagePath, label, details) {
  const imageToken = await upload(userToken, imagePath, label)
  const key = dataOf(await request('/rescue-clues/idempotency-keys', {
    method: 'POST', token: userToken
  }), 201, `${label} 幂等键`).idempotencyKey
  const clue = dataOf(await request('/rescue-clues', {
    method: 'POST',
    token: userToken,
    body: {
      location: details.location,
      foundTime: new Date(Date.now() - 60_000).toISOString(),
      animalDescription: details.animalDescription,
      sceneDescription: details.sceneDescription,
      contact: '13800138000',
      imageTokens: [imageToken]
    },
    // The helper does not otherwise need to know about this header; add it below.
    headers: { 'Idempotency-Key': key }
  }), 201, `${label} 发布线索`)
  return clue.clueId
}

async function auditClue(adminToken, clueId, decision, rejectReason) {
  return dataOf(await request(`/admin/rescue-clues/${clueId}/audit`, {
    method: 'POST', token: adminToken,
    body: rejectReason ? { decision, rejectReason } : { decision }
  }), 200, `线索 ${clueId} 审核`)
}

async function acceptClue(rescuerToken, clueId) {
  return dataOf(await request(`/rescue-clues/${clueId}/accept`, {
    method: 'POST', token: rescuerToken
  }), 201, `线索 ${clueId} 接取`)
}

async function startTask(rescuerToken, taskId) {
  return dataOf(await request(`/rescue-tasks/${taskId}/start`, {
    method: 'POST', token: rescuerToken
  }), 200, `任务 ${taskId} 开始`)
}

async function addRecord(rescuerToken, taskId, content) {
  return dataOf(await request(`/rescue-tasks/${taskId}/records`, {
    method: 'POST', token: rescuerToken, body: { content }
  }), 201, `任务 ${taskId} 救助记录`)
}

async function submitSuccess(rescuerToken, taskId, animalRequests) {
  return dataOf(await request(`/rescue-tasks/${taskId}/result`, {
    method: 'POST', token: rescuerToken,
    body: { result: 'SUCCESS', animals: animalRequests }
  }), 200, `任务 ${taskId} 救助成功`)
}

async function animalDetail(token, animalId) {
  return dataOf(await request(`/animals/${animalId}`, { token }), 200, `动物 ${animalId} 详情`)
}

async function animalAction(token, animalId, action) {
  const current = await animalDetail(token, animalId)
  dataOf(await request(`/animals/${animalId}/status-actions`, {
    method: 'POST', token,
    body: { action, version: current.version }
  }), 200, `动物 ${animalId} ${action}`)
  return animalDetail(token, animalId)
}

async function createApplication(userToken, animalId, name) {
  return dataOf(await request(`/animals/${animalId}/adoption-applications`, {
    method: 'POST', token: userToken,
    body: {
      adoptionReason: `演示申请：希望长期照顾${name}，承担医疗和日常生活费用。`,
      housingCondition: '自有住房，已做好独立养宠空间和安全防护。',
      familyMembers: '两名家庭成员，已共同确认领养计划。',
      petExperience: '有多年猫狗照护经验，能够按时完成免疫和复诊。',
      contact: '13800138000'
    }
  }), 201, `动物 ${animalId} 领养申请`)
}

async function createAnnouncement(adminToken, title, content) {
  return dataOf(await request('/admin/announcements', {
    method: 'POST', token: adminToken, body: { title, content }
  }), 201, '公告草稿')
}

async function main() {
  for (const [name, path] of Object.entries(ASSETS)) await readFile(path).catch(() => { throw new Error(`找不到 ${name} 图片：${path}`) })

  const [adminToken, rescuerToken, userToken] = await Promise.all([
    login('admin'), login('rescuer'), login('user')
  ])

  const pendingClue = await createClue(userToken, ASSETS.cat, 'pending', {
    location: `${TAG}｜城市公园南门`,
    animalDescription: '一只橘白猫在绿化带附近徘徊，精神尚可，需要救助人员进一步确认。',
    sceneDescription: '靠近长椅和饮水点，周边行人较多。'
  })

  const waitingClue = await createClue(userToken, ASSETS.rabbit, 'waiting', {
    location: `${TAG}｜社区活动中心东侧`,
    animalDescription: '一只灰色垂耳兔疑似走失，已被临时安置在安全区域。',
    sceneDescription: '现场有志愿者看护，等待救助人员接取。'
  })
  await auditClue(adminToken, waitingClue, 'APPROVE')

  const activeClue = await createClue(userToken, ASSETS.dog, 'active', {
    location: `${TAG}｜河畔步道 3 号入口`,
    animalDescription: '一只黑白狗狗受轻微擦伤，情绪紧张，需要专业救助。',
    sceneDescription: '已经联系附近志愿者，现场保持安静并准备饮水。'
  })
  await auditClue(adminToken, activeClue, 'APPROVE')
  const accepted = await acceptClue(rescuerToken, activeClue)
  const activeTask = accepted.taskId
  await startTask(rescuerToken, activeTask)
  await addRecord(rescuerToken, activeTask, '已到达河畔步道，完成现场评估并安抚动物。')
  await addRecord(rescuerToken, activeTask, '已联系合作医院，正在进行基础检查和伤口清洁。')

  const catToken = await upload(rescuerToken, ASSETS.cat, 'animal-cat')
  const dogToken = await upload(rescuerToken, ASSETS.dog, 'animal-dog')
  const rabbitToken = await upload(rescuerToken, ASSETS.rabbit, 'animal-rabbit')
  const success = await submitSuccess(rescuerToken, activeTask, [
    {
      name: `${TAG}·橘宝`, species: 'CAT', sex: 'MALE', estimatedAgeMonths: 18,
      color: '橘白', healthCondition: '精神和食欲良好，已完成基础检查。',
      initialHealthRecord: '完成体表检查和驱虫评估，建议观察一周。', imageTokens: [catToken]
    },
    {
      name: `${TAG}·墨墨`, species: 'DOG', sex: 'FEMALE', estimatedAgeMonths: 30,
      color: '黑白', healthCondition: '前肢轻微擦伤，正在护理观察。',
      initialHealthRecord: '已清洁伤口并完成基础体温检查，暂不开放领养。', imageTokens: [dogToken]
    },
    {
      name: `${TAG}·灰灰`, species: 'RABBIT', sex: 'FEMALE', estimatedAgeMonths: 10,
      color: '灰色', healthCondition: '状态稳定，食欲正常，适合进入观察期。',
      initialHealthRecord: '已完成基础健康检查，建议继续观察并补充免疫信息。', imageTokens: [rabbitToken]
    }
  ])
  const [catId, dogId, rabbitId] = success.animalIds

  await animalAction(rescuerToken, catId, 'TO_OBSERVING')
  await animalAction(rescuerToken, catId, 'OPEN_ADOPTION')
  await animalAction(rescuerToken, rabbitId, 'TO_OBSERVING')
  await animalAction(rescuerToken, rabbitId, 'OPEN_ADOPTION')

  const rejectedClue = await createClue(userToken, ASSETS.rabbit, 'rejected', {
    location: `${TAG}｜信息待补充的示例地点`,
    animalDescription: '一只需要进一步核实身份信息的示例动物。',
    sceneDescription: '演示管理员驳回分支，资料不足时要求补充。'
  })
  await auditClue(adminToken, rejectedClue, 'REJECT', '演示驳回：请补充更清晰的地点和动物情况说明。')

  await dataOf(await request(`/animals/${rabbitId}/favorite`, {
    method: 'POST', token: userToken
  }), 201, `动物 ${rabbitId} 收藏`)

  const approvedApplication = await createApplication(userToken, catId, `${TAG}·橘宝`)
  const auditedApplication = dataOf(await request(`/admin/adoption-applications/${approvedApplication.id}/audit`, {
    method: 'POST', token: adminToken, body: { decision: 'APPROVE' }
  }), 200, `申请 ${approvedApplication.id} 批准`)
  const adoptionRecordId = auditedApplication.adoptionRecord?.id
  if (!adoptionRecordId) throw new Error('批准领养申请未生成 AdoptionRecord')

  const followupImage = await upload(userToken, ASSETS.cat, 'follow-up')
  const followup = dataOf(await request(`/adoption-records/${adoptionRecordId}/follow-ups`, {
    method: 'POST', token: userToken,
    body: {
      idempotencyKey: randomUUID(),
      content: `演示回访：${TAG}·橘宝已适应新家，会主动亲近家人。`,
      healthCondition: '食欲正常，精神状态良好，排便规律。',
      imageTokens: [followupImage]
    }
  }), 201, `领养记录 ${adoptionRecordId} 回访`)

  const pendingApplication = await createApplication(userToken, rabbitId, `${TAG}·灰灰`)
  const draft = await createAnnouncement(adminToken, `${TAG}｜领养日开放公告`, '本批次演示动物已完成档案建立，欢迎在移动端查看可领养动物并提交申请。')
  const published = await createAnnouncement(adminToken, `${TAG}｜救助流程说明`, '管理员审核、救助人员接取和普通用户回访均已准备好，可按演示手册逐步操作。')
  await dataOf(await request(`/admin/announcements/${published.id}/publish`, {
    method: 'POST', token: adminToken, body: { version: published.version }
  }), 200, '公告发布')

  const output = {
    tag: TAG,
    clues: {
      pendingReview: pendingClue,
      waitingAcceptance: waitingClue,
      inProgressSource: activeClue,
      rejected: rejectedClue
    },
    tasks: { success: activeTask },
    animals: {
      adopted: catId,
      treating: dogId,
      available: rabbitId
    },
    adoption: {
      approvedRecord: adoptionRecordId,
      pendingApplication: pendingApplication.id
    },
    followUp: followup.id,
    announcements: { draft: draft.id, published: published.id },
    urls: {
      pc: 'http://103.233.254.186:8081/',
      mobile: 'http://103.233.254.186:8081/mobile/'
    }
  }
  console.log(JSON.stringify(output, null, 2))
}

main().catch(error => {
  console.error(`演示数据创建失败：${error.message}`)
  process.exitCode = 1
})
