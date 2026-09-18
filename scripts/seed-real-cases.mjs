#!/usr/bin/env node

/**
 * Create a small, curated PetLink dataset that follows realistic user journeys.
 *
 * The photos are the real public-source assets already documented in
 * docs/content/PHOTO-ASSET-MAP.md.  The records below are de-identified
 * workflow examples derived from those public cases; they must not be shown as
 * PetLink's own rescue records or as a partnership claim.
 *
 * Required environment:
 *   PETLINK_REAL_CASE_PASSWORD=...
 * Optional:
 *   PETLINK_REAL_CASE_ORIGIN=http://127.0.0.1:8080/api
 */

import { readFile } from 'node:fs/promises'
import { basename, dirname, extname, join } from 'node:path'
import { randomUUID } from 'node:crypto'
import { fileURLToPath } from 'node:url'

const ROOT = dirname(dirname(fileURLToPath(import.meta.url)))
const API = (process.env.PETLINK_REAL_CASE_ORIGIN || 'http://127.0.0.1:8080/api').replace(/\/$/, '')
const PASSWORD = process.env.PETLINK_REAL_CASE_PASSWORD
const CONTENT = join(ROOT, 'petlink-pc/public/assets/content')

const FILES = {
  marble: ['rescue-xiaoman-process.jpg', 'rescue-xiaoman-recovery.jpg'],
  davidBowie: ['rescue-afu-process.jpg', 'rescue-afu-recovery.jpg'],
  coop: ['rescue-huidou-process.jpg', 'rescue-huidou-recovery.jpg'],
  paradise: ['animal-naitang-cover.jpg', 'animal-naitang-full.jpg', 'animal-naitang-life.jpg'],
  zuzu: ['animal-doubao-cover.jpg', 'animal-doubao-full.jpg', 'animal-doubao-life.jpg']
}

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
  if (response.status !== expected) {
    throw new Error(`${label}: expected ${expected}, got ${response.status}; ${JSON.stringify(response.body)}`)
  }
  if (response.body?.code !== undefined && response.body.code !== 0) {
    throw new Error(`${label}: business code ${response.body.code}`)
  }
  return response.body?.data ?? response.body
}

async function login(account) {
  return dataOf(await request('/auth/login', {
    method: 'POST', body: { account, password: PASSWORD }
  }), 200, `${account} 登录`).accessToken
}

async function upload(token, filePath, label) {
  const bytes = await readFile(filePath)
  const form = new FormData()
  const mime = ({ '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.png': 'image/png' })[extname(filePath).toLowerCase()] || 'application/octet-stream'
  form.append('file', new Blob([bytes], { type: mime }), `${label}-${basename(filePath)}`)
  return dataOf(await request('/files/temporary', {
    method: 'POST', token, form
  }), 201, `${label} 图片上传`).token
}

async function createClue(userToken, imagePath, details) {
  const imageToken = await upload(userToken, imagePath, details.label)
  const key = dataOf(await request('/rescue-clues/idempotency-keys', {
    method: 'POST', token: userToken
  }), 201, `${details.label} 幂等键`).idempotencyKey
  return dataOf(await request('/rescue-clues', {
    method: 'POST', token: userToken,
    headers: { 'Idempotency-Key': key },
    body: {
      location: details.location,
      foundTime: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString(),
      animalDescription: details.animalDescription,
      sceneDescription: details.sceneDescription,
      contact: '13800138000',
      imageTokens: [imageToken]
    }
  }), 201, `${details.label} 发布线索`).clueId
}

async function audit(adminToken, clueId, decision, rejectReason) {
  return dataOf(await request(`/admin/rescue-clues/${clueId}/audit`, {
    method: 'POST', token: adminToken,
    body: rejectReason ? { decision, rejectReason } : { decision }
  }), 200, `线索 ${clueId} 审核`)
}

async function accept(rescuerToken, clueId) {
  return dataOf(await request(`/rescue-clues/${clueId}/accept`, {
    method: 'POST', token: rescuerToken
  }), 201, `线索 ${clueId} 接取`).taskId
}

async function start(rescuerToken, taskId) {
  return dataOf(await request(`/rescue-tasks/${taskId}/start`, {
    method: 'POST', token: rescuerToken
  }), 200, `任务 ${taskId} 开始`)
}

async function record(rescuerToken, taskId, content) {
  return dataOf(await request(`/rescue-tasks/${taskId}/records`, {
    method: 'POST', token: rescuerToken, body: { content }
  }), 201, `任务 ${taskId} 救助记录`)
}

async function submitSuccess(rescuerToken, taskId, animal) {
  const imageTokens = []
  for (const [index, file] of animal.files.entries()) {
    imageTokens.push(await upload(rescuerToken, pathFor(file), `${animal.name}-档案-${index + 1}`))
  }
  return dataOf(await request(`/rescue-tasks/${taskId}/result`, {
    method: 'POST', token: rescuerToken,
    body: { result: 'SUCCESS', animals: [{ ...animal.fields, imageTokens }] }
  }), 200, `${animal.name} 救助结果`).animalIds[0]
}

async function addHealth(rescuerToken, animalId, content) {
  return dataOf(await request(`/animals/${animalId}/health-records`, {
    method: 'POST', token: rescuerToken, body: { content }
  }), 201, `动物 ${animalId} 健康记录`)
}

async function animalAction(token, animalId, action) {
  const detail = dataOf(await request(`/animals/${animalId}`, { token }), 200, `动物 ${animalId} 详情`)
  return dataOf(await request(`/animals/${animalId}/status-actions`, {
    method: 'POST', token,
    body: { action, version: detail.version }
  }), 200, `动物 ${animalId} ${action}`)
}

async function createApplication(userToken, animalId, name) {
  return dataOf(await request(`/animals/${animalId}/adoption-applications`, {
    method: 'POST', token: userToken,
    body: {
      adoptionReason: `我希望为${name}提供稳定的室内生活，愿意承担日常照护和必要医疗费用。`,
      housingCondition: '已完成门窗和阳台防护，准备独立安静的适应空间。',
      familyMembers: '家庭成员已共同确认领养计划，并同意遵守回访安排。',
      petExperience: '有持续照护伴侣动物的经验，能够按期完成免疫、复查和驱虫。',
      contact: '13800138000'
    }
  }), 201, `${name} 领养申请`)
}

async function createFollowUp(userToken, recordId, file, content, healthCondition) {
  const imageToken = await upload(userToken, pathFor(file), '领养回访')
  return dataOf(await request(`/adoption-records/${recordId}/follow-ups`, {
    method: 'POST', token: userToken,
    body: { idempotencyKey: randomUUID(), content, healthCondition, imageTokens: [imageToken] }
  }), 201, `领养记录 ${recordId} 回访`)
}

async function completeCase({ adminToken, rescuerToken, userToken, files, label, clue, records, animal }) {
  const clueId = await createClue(userToken, pathFor(files[0]), { ...clue, label })
  await audit(adminToken, clueId, 'APPROVE')
  const taskId = await accept(rescuerToken, clueId)
  await start(rescuerToken, taskId)
  for (const item of records) await record(rescuerToken, taskId, item)
  const animalId = await submitSuccess(rescuerToken, taskId, { ...animal, files })
  return { clueId, taskId, animalId }
}

async function main() {
  for (const files of Object.values(FILES)) {
    for (const file of files) await readFile(pathFor(file))
  }
  const [adminToken, rescuerToken, userToken] = await Promise.all([
    login('admin'), login('rescuer'), login('user')
  ])

  // 1. A resident submits a detailed clue and it remains pending review.
  const pendingClue = await createClue(userToken, pathFor(FILES.marble[0]), {
    label: '待审核线索',
    location: '南京市玄武区某社区公园（已隐去精确位置）',
    animalDescription: '一只幼猫后肢疑似严重受伤，精神较弱，暂时无法正常行走；发布人已保持距离并拍摄了现场情况。',
    sceneDescription: '资料背景参考 Best Friends Animal Society 公开案例 Marble；当前仅用于演示居民提交线索后的审核等待状态。'
  })

  // 2. A real-world style handoff: approved but waiting for a rescuer.
  const waitingClue = await createClue(userToken, pathFor(FILES.coop[0]), {
    label: '待接取线索',
    location: '南京市鼓楼区河畔步道（已隐去精确位置）',
    animalDescription: '一只黑色兔子后肢无力、体况偏瘦，无法自行清洁；附近居民已提供干草、饮水和实心垫面。',
    sceneDescription: '情况参考 Best Friends Animal Society 的 Coop 康复案例；已完成初步安置，等待救助人员接取。'
  })
  await audit(adminToken, waitingClue, 'APPROVE')

  // 3. A rescuer has accepted a case and is recording the medical handoff.
  const inProgressClue = await createClue(userToken, pathFor(FILES.davidBowie[0]), {
    label: '进行中线索',
    location: '南京市秦淮区某街道服务点（已隐去精确位置）',
    animalDescription: '一只中型犬皮肤红肿、毛发稀疏且体况偏弱，需要进一步检查并避免强光暴晒。',
    sceneDescription: '资料背景参考 Best Friends Animal Society 的 David Bowie 公开治疗案例；当前已联系救助人员和医疗协作方。'
  })
  await audit(adminToken, inProgressClue, 'APPROVE')
  const inProgressTask = await accept(rescuerToken, inProgressClue)
  await start(rescuerToken, inProgressTask)
  await record(rescuerToken, inProgressTask, '已到达现场，完成身份核对、基础体况观察并转移到阴凉安静区域。')
  await record(rescuerToken, inProgressTask, '已联系医疗协作方，准备进行皮肤检查；在明确诊断前不承诺治愈结果。')

  const coopCase = await completeCase({
    adminToken, rescuerToken, userToken, files: FILES.coop, label: '灰豆完整案例',
    clue: {
      location: '南京市雨花台区临时安置点（已隐去精确位置）',
      animalDescription: '黑色兔子曾出现后肢瘫痪和营养不良，无法正常移动和清洁身体。',
      sceneDescription: '案例细节来自 Best Friends Animal Society 的 Coop 公开文章，平台记录已去标识化。'
    },
    records: ['完成清洁、止痛、抗生素、皮下补液和感染原因排查。', '约一周后开始重新站立，随后通过腿部物理治疗和安全草地活动逐步恢复。'],
    animal: {
      fields: {
        name: '灰豆', species: 'RABBIT', sex: 'MALE', estimatedAgeMonths: 18, color: '黑色',
        healthCondition: '后肢活动能力明显恢复，仍有轻微步态不稳，需要持续环境与体重管理。',
        personality: '恢复后亲人，愿意接受抚摸，也能在安全草地环境中主动探索。',
        adoptionRequirements: '具备兔类科学饲养经验；提供实心地面和安全活动区；愿意继续康复观察。'
      }
    }
  })
  await addHealth(rescuerToken, coopCase.animalId, '公开资料记录：曾接受止痛、补液、营养支持和物理治疗；平台展示不替代领养前的最新检查。')
  await animalAction(rescuerToken, coopCase.animalId, 'TO_OBSERVING')
  await animalAction(rescuerToken, coopCase.animalId, 'OPEN_ADOPTION')

  const paradiseCase = await completeCase({
    adminToken, rescuerToken, userToken, files: FILES.paradise, label: '奶糖完整案例',
    clue: {
      location: '南京市建邺区寄养家庭（已隐去精确位置）',
      animalDescription: '一只黑白短毛母猫完成基础照护后正在寄养环境中适应，已准备进入领养评估。',
      sceneDescription: '领养档案参考 Best Friends Animal Society 的 Paradise 公开档案，照片来自同一只动物。'
    },
    records: ['核对连续照片和来源档案，建立动物基础信息。', '观察其在安静室内环境中的进食、互动和休息情况。'],
    animal: {
      fields: {
        name: '奶糖', species: 'CAT', sex: 'FEMALE', estimatedAgeMonths: 39, color: '黑白短毛',
        healthCondition: '来源档案显示已绝育、接种疫苗并植入芯片；领养前仍应复核最新健康记录。',
        personality: '初到陌生环境时需要适应，熟悉后喜欢陪伴、抚摸和互动游戏。',
        adoptionRequirements: '室内科学喂养；安装纱窗；允许循序适应；每天安排稳定互动时间。'
      }
    }
  })
  await addHealth(rescuerToken, paradiseCase.animalId, '公开资料记录：已绝育、接种疫苗并植入芯片；后续照护以领养交接资料和兽医建议为准。')
  await animalAction(rescuerToken, paradiseCase.animalId, 'TO_OBSERVING')
  await animalAction(rescuerToken, paradiseCase.animalId, 'OPEN_ADOPTION')

  const zuzuCase = await completeCase({
    adminToken, rescuerToken, userToken, files: FILES.zuzu, label: '豆包完整案例',
    clue: {
      location: '南京市栖霞区寄养家庭（已隐去精确位置）',
      animalDescription: '一只黑色大型高龄公犬在寄养环境中状态稳定，喜欢与人互动并需要低强度活动。',
      sceneDescription: '领养档案参考 Best Friends Animal Society 的 Zuzu 公开档案，照片来自同一只动物。'
    },
    records: ['核对来源档案和连续照片，建立高龄犬健康与行为观察。', '记录防滑休息区、活动强度和长期复查要求。'],
    animal: {
      fields: {
        name: '豆包', species: 'DOG', sex: 'MALE', estimatedAgeMonths: 149, color: '黑色短毛 / 大型',
        healthCondition: '来源档案显示已开放领养；具体健康状态以最新检查和交接记录为准。',
        personality: '性格温和，喜欢人与零食，也能接受合适的犬类伙伴。',
        adoptionRequirements: '提供防滑舒适的休息环境；控制运动强度；愿意进行高龄犬常规检查。'
      }
    }
  })
  await addHealth(rescuerToken, zuzuCase.animalId, '公开资料记录：高龄犬需要稳定生活、适度活动和规律健康复查；平台信息不替代领养前面诊。')
  await animalAction(rescuerToken, zuzuCase.animalId, 'TO_OBSERVING')
  await animalAction(rescuerToken, zuzuCase.animalId, 'OPEN_ADOPTION')

  const application = await createApplication(userToken, paradiseCase.animalId, '奶糖')
  const approved = dataOf(await request(`/admin/adoption-applications/${application.id}/audit`, {
    method: 'POST', token: adminToken, body: { decision: 'APPROVE' }
  }), 200, '奶糖领养审核')
  const adoptionRecordId = approved.adoptionRecord?.id
  if (!adoptionRecordId) throw new Error('奶糖领养审核未生成领养记录')
  await createFollowUp(userToken, adoptionRecordId, FILES.paradise[2],
    '到家第一周在安静房间适应，已按固定时间进食并开始接受家人陪伴。',
    '食欲、饮水和排便基本规律；继续保持低压力适应环境。')
  await createFollowUp(userToken, adoptionRecordId, FILES.paradise[1],
    '到家一个月后会主动走近家人，也开始使用猫抓板和玩具。',
    '体重和精神状态稳定，门窗防护已完成。')

  console.log(JSON.stringify({
    source: 'Best Friends Animal Society public case/adoption pages',
    pendingClue,
    waitingClue,
    inProgress: { clueId: inProgressClue, taskId: inProgressTask },
    available: { rabbit: coopCase.animalId, dog: zuzuCase.animalId },
    adopted: { animalId: paradiseCase.animalId, adoptionRecordId },
    note: '以上记录为去标识化流程案例，图片和来源链接见 docs/content/PHOTO-ASSET-MAP.md。'
  }, null, 2))
}

main().catch(error => {
  console.error(`真实案例创建失败：${error.message}`)
  process.exitCode = 1
})
