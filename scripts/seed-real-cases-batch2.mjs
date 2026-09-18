#!/usr/bin/env node

/**
 * Append the second curated real-source case batch.
 *
 * This script is intentionally separate from the first batch so it can be
 * rerun after a partial deployment without recreating the first five cases.
 * Existing animals with the same display name are skipped.
 */

import { readFile } from 'node:fs/promises'
import { basename, dirname, extname, join } from 'node:path'
import { randomUUID } from 'node:crypto'
import { fileURLToPath } from 'node:url'

const ROOT = dirname(dirname(fileURLToPath(import.meta.url)))
const API = (process.env.PETLINK_REAL_CASE_ORIGIN || 'http://127.0.0.1:8080/api').replace(/\/$/, '')
const PASSWORD = process.env.PETLINK_REAL_CASE_PASSWORD
const CONTENT = join(ROOT, 'petlink-pc/public/assets/content')

const CASES = [
  {
    name: '煤球', sourceName: 'Cashmere',
    files: ['animal-meiqiu-cover.jpg', 'animal-meiqiu-full.jpg', 'animal-meiqiu-life.jpg'],
    sourceUrl: 'https://bestfriends.org/new-york-city/adopt/214271912/cashmere',
    location: '南京市玄武区寄养家庭（已隐去精确位置）',
    description: '一只高龄玳瑁母猫正在寄养环境中适应，已完成基础照护，适合生活节奏稳定的家庭。',
    scene: '领养档案参考 Best Friends Animal Society 的 Cashmere 公开档案，照片来自同一只动物。',
    records: ['核对来源档案和连续照片，建立高龄猫基础档案。', '记录寄养反馈、互动边界和长期照护要求。'],
    fields: {
      species: 'CAT', sex: 'FEMALE', estimatedAgeMonths: 133, color: '玳瑁短毛',
      healthCondition: '来源档案显示已绝育、接种疫苗并植入芯片；需要高龄猫常规检查。',
      personality: '亲人、爱交流，适应后喜欢梳毛、探索环境和安静陪伴。',
      adoptionRequirements: '适合生活节奏稳定的室内家庭；接受高龄动物定期体检；给予充足适应时间。'
    },
    health: '公开资料记录：已绝育、接种疫苗并植入芯片；高龄照护以最新检查和兽医建议为准。'
  },
  {
    name: '栗子', sourceName: 'Sally',
    files: ['animal-lizi-cover.jpg', 'animal-lizi-full.jpg', 'animal-lizi-life.jpg'],
    sourceUrl: 'https://bestfriends.org/sanctuary/adopt/56683310/sally',
    location: '南京市秦淮区寄养家庭（已隐去精确位置）',
    description: '一只灰、米与白色淡三花母猫在安静寄养环境中状态稳定，需要尊重自己的互动节奏。',
    scene: '领养档案参考 Best Friends Animal Society 的 Sally 公开档案，照片来自同一只动物。',
    records: ['核对来源档案和连续照片，修正物种及毛色信息。', '记录互动边界和室内环境要求。'],
    fields: {
      species: 'CAT', sex: 'FEMALE', estimatedAgeMonths: 62, color: '灰、米与白色淡三花短毛',
      healthCondition: '来源档案显示已开放领养；平台展示不替代领养前的最新检查。',
      personality: '独立而有边界感，喜欢温和抚摸和安静休息。',
      adoptionRequirements: '室内喂养并做好门窗防护；不强行抱持；准备可躲藏的安全空间。'
    },
    health: '公开资料记录：当前状态稳定；领养前需根据交接资料复核疫苗、驱虫和绝育情况。'
  },
  {
    name: '米粒', sourceName: 'Moss',
    files: ['animal-mili-cover.jpg', 'animal-mili-full.jpg', 'animal-mili-life.jpg'],
    sourceUrl: 'https://bestfriends.org/sanctuary/adopt/214077523/moss',
    location: '南京市建邺区寄养家庭（已隐去精确位置）',
    description: '一只棕色虎斑加白公幼猫处于快速成长阶段，好奇、活跃并喜欢探索。',
    scene: '领养档案参考 Best Friends Animal Society 的 Moss 公开档案，照片来自同一只动物。',
    records: ['核对来源档案和连续照片，建立幼猫成长档案。', '记录后续免疫、绝育和环境防护要求。'],
    fields: {
      species: 'CAT', sex: 'MALE', estimatedAgeMonths: 4, color: '棕色虎斑加白短毛',
      healthCondition: '幼猫仍处于成长阶段；疫苗、驱虫和绝育应按实际记录继续完成。',
      personality: '好奇、活跃，喜欢探索和玩耍，需要稳定互动消耗精力。',
      adoptionRequirements: '阳台和窗户必须封网；完成后续免疫与绝育；每天提供安全探索时间。'
    },
    health: '公开资料记录：幼猫需要持续完成免疫、驱虫、绝育和社会化照护。'
  },
  {
    name: '阿布', sourceName: 'Bubs',
    files: ['animal-abu-cover.jpg', 'animal-abu-full.jpg', 'animal-abu-life.jpg'],
    sourceUrl: 'https://bestfriends.org/sanctuary/adopt/213764787/bubs',
    location: '南京市栖霞区寄养家庭（已隐去精确位置）',
    description: '一只浅棕色中型公犬活泼、亲人，喜欢其他狗狗和户外活动。',
    scene: '领养档案参考 Best Friends Animal Society 的 Bubs 公开档案，照片来自同一只动物。',
    records: ['核对来源档案和连续照片，建立行为观察。', '补充运动、牵引和家庭陪伴要求。'],
    fields: {
      species: 'DOG', sex: 'MALE', estimatedAgeMonths: 42, color: '浅棕短毛 / 中型',
      healthCondition: '来源档案显示已开放领养；具体健康状态以最新检查和交接记录为准。',
      personality: '活泼、亲人，喜欢其他狗狗，运动意愿强。',
      adoptionRequirements: '每天保证户外活动和正向训练；出门全程牵引；提供稳定陪伴。'
    },
    health: '公开资料记录：需要每日活动、正向训练和稳定陪伴；领养前复核最新体检。'
  }
]

if (!PASSWORD) {
  console.error('缺少 PETLINK_REAL_CASE_PASSWORD。')
  process.exit(2)
}

function pathFor(name) { return join(CONTENT, name) }

async function request(path, { method = 'GET', token, body, form, headers: extra = {} } = {}) {
  const headers = { Accept: 'application/json', ...extra }
  if (token) headers.Authorization = `Bearer ${token}`
  let requestBody = form || body
  if (!form && body !== undefined) {
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
  if (response.status !== expected) throw new Error(`${label}: expected ${expected}, got ${response.status}; ${JSON.stringify(response.body)}`)
  if (response.body?.code !== undefined && response.body.code !== 0) throw new Error(`${label}: business code ${response.body.code}`)
  return response.body?.data ?? response.body
}

async function login(account) {
  return dataOf(await request('/auth/login', { method: 'POST', body: { account, password: PASSWORD } }), 200, `${account} 登录`).accessToken
}

async function upload(token, filePath, label) {
  const bytes = await readFile(filePath)
  const form = new FormData()
  const mime = ({ '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg' })[extname(filePath).toLowerCase()] || 'application/octet-stream'
  form.append('file', new Blob([bytes], { type: mime }), `${label}-${basename(filePath)}`)
  return dataOf(await request('/files/temporary', { method: 'POST', token, form }), 201, `${label} 图片上传`).token
}

async function createClue(userToken, item) {
  const imageToken = await upload(userToken, pathFor(item.files[0]), `${item.name}-线索`)
  const key = dataOf(await request('/rescue-clues/idempotency-keys', { method: 'POST', token: userToken }), 201, `${item.name} 幂等键`).idempotencyKey
  return dataOf(await request('/rescue-clues', {
    method: 'POST', token: userToken, headers: { 'Idempotency-Key': key },
    body: {
      location: item.location,
      foundTime: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toISOString(),
      animalDescription: item.description,
      sceneDescription: `${item.scene} 来源链接：${item.sourceUrl}`,
      contact: '13800138000', imageTokens: [imageToken]
    }
  }), 201, `${item.name} 发布线索`).clueId
}

async function audit(adminToken, clueId) {
  return dataOf(await request(`/admin/rescue-clues/${clueId}/audit`, { method: 'POST', token: adminToken, body: { decision: 'APPROVE' } }), 200, `线索 ${clueId} 审核`)
}

async function accept(rescuerToken, clueId) {
  return dataOf(await request(`/rescue-clues/${clueId}/accept`, { method: 'POST', token: rescuerToken }), 201, `线索 ${clueId} 接取`).taskId
}

async function completeCase(adminToken, rescuerToken, userToken, item) {
  const clueId = await createClue(userToken, item)
  await audit(adminToken, clueId)
  const taskId = await accept(rescuerToken, clueId)
  dataOf(await request(`/rescue-tasks/${taskId}/start`, { method: 'POST', token: rescuerToken }), 200, `${item.name} 任务开始`)
  for (const content of item.records) dataOf(await request(`/rescue-tasks/${taskId}/records`, { method: 'POST', token: rescuerToken, body: { content } }), 201, `${item.name} 救助记录`)
  const imageTokens = []
  for (const [index, file] of item.files.entries()) imageTokens.push(await upload(rescuerToken, pathFor(file), `${item.name}-档案-${index + 1}`))
  const result = dataOf(await request(`/rescue-tasks/${taskId}/result`, {
    method: 'POST', token: rescuerToken,
    body: { result: 'SUCCESS', animals: [{ name: item.name, ...item.fields, imageTokens }] }
  }), 200, `${item.name} 救助结果`)
  const animalId = result.animalIds[0]
  dataOf(await request(`/animals/${animalId}/health-records`, { method: 'POST', token: rescuerToken, body: { content: `${item.health} 来源链接：${item.sourceUrl}` } }), 201, `${item.name} 健康记录`)
  for (const action of ['TO_OBSERVING', 'OPEN_ADOPTION']) {
    const detail = dataOf(await request(`/animals/${animalId}`, { token: rescuerToken }), 200, `${item.name} 详情`)
    dataOf(await request(`/animals/${animalId}/status-actions`, { method: 'POST', token: rescuerToken, body: { action, version: detail.version } }), 200, `${item.name} ${action}`)
  }
  return { clueId, taskId, animalId }
}

async function existingNames(adminToken) {
  const page = dataOf(await request('/admin/animals?page=1&size=100', { token: adminToken }), 200, '读取现有动物档案')
  return new Set((page.records || []).map(item => item.name))
}

async function apply(userToken, adminToken, animalId, item, decision) {
  const application = dataOf(await request(`/animals/${animalId}/adoption-applications`, {
    method: 'POST', token: userToken,
    body: {
      adoptionReason: `希望为${item.name}提供稳定的家庭生活，愿意承担长期照护、复诊和日常费用。`,
      housingCondition: '已完成门窗和阳台防护，准备安静的独立适应空间。',
      familyMembers: '家庭成员已共同确认领养计划，并同意配合回访。',
      petExperience: '有持续照护伴侣动物的经验，能够按期完成免疫、驱虫和复查。',
      contact: '13800138000'
    }
  }), 201, `${item.name} 领养申请`)
  let adoptionRecordId = null
  if (decision) {
    const audited = dataOf(await request(`/admin/adoption-applications/${application.id}/audit`, {
      method: 'POST', token: adminToken,
      body: decision === 'APPROVE' ? { decision } : { decision, rejectReason: '当前家庭条件与该动物的照护需求仍需进一步确认。' }
    }), 200, `${item.name} 领养审核`)
    adoptionRecordId = audited.adoptionRecord?.id || null
  }
  return { applicationId: application.id, adoptionRecordId }
}

async function followUp(userToken, adoptionRecordId, item) {
  const imageToken = await upload(userToken, pathFor(item.files[2]), `${item.name}-回访`)
  return dataOf(await request(`/adoption-records/${adoptionRecordId}/follow-ups`, {
    method: 'POST', token: userToken,
    body: {
      idempotencyKey: randomUUID(), imageTokens: [imageToken],
      content: `领养后回访：${item.name} 已逐步适应家庭节奏，照护者按计划记录饮食、活动和互动变化。`,
      healthCondition: '精神和食欲稳定，继续按交接计划完成预防性照护和必要复查。'
    }
  }), 201, `${item.name} 领养回访`)
}

async function main() {
  for (const item of CASES) for (const file of item.files) await readFile(pathFor(file))
  const [adminToken, rescuerToken, userToken] = await Promise.all([login('admin'), login('rescuer'), login('user')])
  const existing = await existingNames(adminToken)
  const created = []
  for (const item of CASES) {
    if (existing.has(item.name)) continue
    const result = await completeCase(adminToken, rescuerToken, userToken, item)
    created.push({ ...result, name: item.name })
    existing.add(item.name)
  }
  const byName = Object.fromEntries(created.map(item => [item.name, item]))
  if (byName['煤球']) await apply(userToken, adminToken, byName['煤球'].animalId, CASES[0], null)
  if (byName['栗子']) {
    const approved = await apply(userToken, adminToken, byName['栗子'].animalId, CASES[1], 'APPROVE')
    if (approved.adoptionRecordId) await followUp(userToken, approved.adoptionRecordId, CASES[1])
  }
  if (byName['米粒']) await apply(userToken, adminToken, byName['米粒'].animalId, CASES[2], 'REJECT')
  for (const name of ['栗子', '米粒']) {
    const record = byName[name]
    if (record) await request(`/animals/${record.animalId}/favorite`, { method: 'POST', token: userToken })
  }
  console.log(JSON.stringify({ source: 'Best Friends Animal Society public adoption pages', created }, null, 2))
}

main().catch(error => {
  console.error(`第二批真实案例创建失败：${error.message}`)
  process.exitCode = 1
})
