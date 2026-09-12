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
import { basename, dirname, extname, join } from 'node:path'
import { randomUUID } from 'node:crypto'
import { fileURLToPath } from 'node:url'

const PROJECT_ROOT = dirname(dirname(fileURLToPath(import.meta.url)))
const API = (process.env.PETLINK_DEMO_ORIGIN || 'http://127.0.0.1:8080/api').replace(/\/$/, '')
const PASSWORD = process.env.PETLINK_DEMO_PASSWORD
const TAG = process.env.PETLINK_DEMO_TAG || `演示批次-${new Date().toISOString().slice(0, 16).replace(/[T:]/g, '-')}`
const CONTENT_ASSETS = join(PROJECT_ROOT, 'petlink-pc/public/assets/content')
const contentFiles = (...names) => names.map(name => join(CONTENT_ASSETS, name))
const ASSETS = {
  cat: process.env.PETLINK_DEMO_CAT ? [process.env.PETLINK_DEMO_CAT] : contentFiles('rescue-xiaoman-process.jpg', 'rescue-xiaoman-recovery.jpg'),
  dog: process.env.PETLINK_DEMO_DOG ? [process.env.PETLINK_DEMO_DOG] : contentFiles('rescue-afu-process.jpg', 'rescue-afu-recovery.jpg'),
  rabbit: process.env.PETLINK_DEMO_RABBIT ? [process.env.PETLINK_DEMO_RABBIT] : contentFiles('rescue-huidou-process.jpg', 'rescue-huidou-recovery.jpg'),
  naitang: contentFiles('animal-naitang-cover.jpg', 'animal-naitang-full.jpg', 'animal-naitang-life.jpg'),
  meiqiu: contentFiles('animal-meiqiu-cover.jpg', 'animal-meiqiu-full.jpg', 'animal-meiqiu-life.jpg'),
  doubao: contentFiles('animal-doubao-cover.jpg', 'animal-doubao-full.jpg', 'animal-doubao-life.jpg'),
  lizi: contentFiles('animal-lizi-cover.jpg', 'animal-lizi-full.jpg', 'animal-lizi-life.jpg'),
  mili: contentFiles('animal-mili-cover.jpg', 'animal-mili-full.jpg', 'animal-mili-life.jpg'),
  abu: contentFiles('animal-abu-cover.jpg', 'animal-abu-full.jpg', 'animal-abu-life.jpg')
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
  const mime = ({ '.jpg':'image/jpeg', '.jpeg':'image/jpeg', '.png':'image/png', '.webp':'image/webp', '.avif':'image/avif' })[extname(filePath).toLowerCase()] || 'application/octet-stream'
  form.append('file', new Blob([bytes], { type: mime }), `${label}-${basename(filePath)}`)
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

async function addHealth(rescuerToken, animalId, content) {
  return dataOf(await request(`/animals/${animalId}/health-records`, {
    method: 'POST', token: rescuerToken, body: { content }
  }), 201, `动物 ${animalId} 健康记录`)
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

async function publishAnnouncement(adminToken, announcement) {
  return dataOf(await request(`/admin/announcements/${announcement.id}/publish`, {
    method: 'POST', token: adminToken, body: { version: announcement.version }
  }), 200, `公告 ${announcement.id} 发布`)
}

async function createFollowUp(userToken, adoptionRecordId, imagePath, label, content, healthCondition) {
  const imageToken = await upload(userToken, imagePath, label)
  return dataOf(await request(`/adoption-records/${adoptionRecordId}/follow-ups`, {
    method: 'POST', token: userToken,
    body: { idempotencyKey: randomUUID(), content, healthCondition, imageTokens: [imageToken] }
  }), 201, `领养记录 ${adoptionRecordId} 回访`)
}

async function rescueOneAnimal({ adminToken, rescuerToken, userToken, imagePaths, label, clue, records, animal }) {
  if (!imagePaths?.length) throw new Error(`${label} 至少需要一张图片`)
  const clueId = await createClue(userToken, imagePaths[0], `${label}-clue`, clue)
  await auditClue(adminToken, clueId, 'APPROVE')
  const taskId = (await acceptClue(rescuerToken, clueId)).taskId
  await startTask(rescuerToken, taskId)
  for (const record of records) await addRecord(rescuerToken, taskId, record)
  const imageTokens = []
  for (const [index, imagePath] of imagePaths.entries()) imageTokens.push(await upload(rescuerToken, imagePath, `${label}-animal-${index + 1}`))
  const result = await submitSuccess(rescuerToken, taskId, [{ ...animal, imageTokens }])
  return { clueId, taskId, animalId: result.animalIds[0] }
}

async function main() {
  for (const [name, paths] of Object.entries(ASSETS)) for (const path of paths) await readFile(path).catch(() => { throw new Error(`找不到 ${name} 图片：${path}`) })

  const [adminToken, rescuerToken, userToken] = await Promise.all([
    login('admin'), login('rescuer'), login('user')
  ])

  const pendingClue = await createClue(userToken, ASSETS.cat[0], 'pending', {
    location: `${TAG}｜待补充的猫咪医疗转介`,
    animalDescription: '一只棕色虎斑猫后肢活动异常，已提供初步医疗照片，需要审核转诊信息。',
    sceneDescription: '演示线索：需要补充发现地点、时间和现场安全情况后再安排接取。'
  })

  const waitingClue = await createClue(userToken, ASSETS.rabbit[0], 'waiting', {
    location: `${TAG}｜兔类临时安置点`,
    animalDescription: '一只黑色兔子后肢无力、体况偏瘦，已被临时安置并等待专业接取。',
    sceneDescription: '现场有志愿者看护，提供干草、饮水和实心垫面。'
  })
  await auditClue(adminToken, waitingClue, 'APPROVE')

  const activeClue = await createClue(userToken, ASSETS.dog[0], 'active', {
    location: `${TAG}｜犬只医疗转介`,
    animalDescription: '一只棕灰色犬皮肤发炎、毛发稀疏且身体消瘦，需要进一步检查。',
    sceneDescription: '已经联系医疗协作人员，当前保持安静、保暖并提供清水。'
  })
  await auditClue(adminToken, activeClue, 'APPROVE')
  const accepted = await acceptClue(rescuerToken, activeClue)
  const activeTask = accepted.taskId
  await startTask(rescuerToken, activeTask)
  await addRecord(rescuerToken, activeTask, '已完成接取和基础体况评估，记录皮肤与毛发状态。')
  await addRecord(rescuerToken, activeTask, '已联系医疗协作人员，正在排查常见皮肤病原因。')

  const catCase = await rescueOneAnimal({
    adminToken, rescuerToken, userToken, imagePaths: ASSETS.cat, label: 'xiaoman',
    clue: {
      location: `${TAG}｜外部案例演示：Marble`,
      animalDescription: '幼猫由合作救助网络转入时两条后腿严重骨折，需要尽快完成影像检查和专科转诊。',
      sceneDescription: '本条为来源机构公开案例的流程演示，不是 PetLink 自有现场记录。'
    },
    records: ['完成止痛、补液与影像检查，确认复杂骨折并转诊专科。', '左后腿实施截肢，右腿使用金属针和夹板固定；随后进入换药和激光治疗阶段。'],
    animal: {
      name: '小满', species: 'CAT', sex: 'FEMALE', estimatedAgeMonths: 12, color: '棕色虎斑',
      healthCondition: '复杂后肢骨折术后恢复中，需要持续换药、复查和活动管理。',
      personality: '治疗期间能够配合换药，喜欢梳毛和互动，恢复后活动兴趣逐步增加。',
      adoptionRequirements: '有术后动物照护能力；能够按期复查并做好室内防护；接受持续回访。',
      initialHealthRecord: '演示资料来源：Best Friends Animal Society 的 Marble 公开案例；左后腿截肢，右腿完成内固定和夹板保护。'
    }
  })
  const dogCase = await rescueOneAnimal({
    adminToken, rescuerToken, userToken, imagePaths: ASSETS.dog, label: 'afu',
    clue: {
      location: `${TAG}｜外部案例演示：David Bowie`,
      animalDescription: '犬只皮肤明显发炎、毛发稀疏，身体消瘦且几乎不愿活动，需要进一步排查病因。',
      sceneDescription: '本条为来源机构公开案例的流程演示，不是 PetLink 自有现场记录。'
    },
    records: ['常规螨虫治疗效果不佳，进一步检查后确认为自身免疫性皮肤病。', '调整为针对性用药、规律洗护和营养照护，体重、毛发与活动状态逐步改善。'],
    animal: {
      name: '阿福', species: 'DOG', sex: 'MALE', estimatedAgeMonths: 48, color: '棕灰色',
      healthCondition: '自身免疫性皮肤病已进入稳定管理期，仍需规律用药和定期复查。',
      personality: '恢复后重新愿意玩玩具、探索环境，也能与寄养家庭的其他狗狗友好互动。',
      adoptionRequirements: '理解慢性病长期管理；按期复诊和用药；能够提供稳定、低压力的生活环境。',
      initialHealthRecord: '演示资料来源：Best Friends Animal Society 的 David Bowie 公开案例；治疗不能表述为彻底治愈。'
    }
  })
  const rabbitCase = await rescueOneAnimal({
    adminToken, rescuerToken, userToken, imagePaths: ASSETS.rabbit, label: 'huidou',
    clue: {
      location: `${TAG}｜外部案例演示：Coop`,
      animalDescription: '黑色兔子被发现时后肢瘫痪、营养不良，无法正常移动和清洁身体。',
      sceneDescription: '本条为来源机构公开案例的流程演示，不是 PetLink 自有现场记录。'
    },
    records: ['完成清洁、止痛、抗生素、皮下补液和感染原因排查。', '约一周后开始重新站立，随后通过腿部物理治疗和安全草地活动恢复跳跃。'],
    animal: {
      name: '灰豆', species: 'RABBIT', sex: 'MALE', estimatedAgeMonths: 18, color: '黑色',
      healthCondition: '后肢活动能力已明显恢复，仍有轻微步态不稳，需要持续环境与体重管理。',
      personality: '恢复后亲人，愿意接受抚摸，也能在安全草地环境中主动探索。',
      adoptionRequirements: '具备兔类科学饲养经验；提供实心地面和安全活动区；愿意继续康复观察。',
      initialHealthRecord: '演示资料来源：Best Friends Animal Society 的 Coop 公开案例；曾接受止痛、补液和物理治疗。'
    }
  })
  const demoProfileInputs = [
    { label:'naitang', imagePaths:ASSETS.naitang, location:'外部领养档案演示：Paradise', description:'黑白短毛母猫进入寄养环境后正在逐步适应，已具备开放领养条件。', scene:'三张照片来自同一只动物的公开领养档案；中文名和任务地点为功能演示。', records:['核对来源档案和连续照片，建立动物基础信息。','补充性格、家庭匹配与长期照护要求。'], followup:'寄养观察显示适应后愿意互动、依偎和玩耍。', animal:{name:'奶糖',species:'CAT',sex:'FEMALE',estimatedAgeMonths:39,color:'黑白短毛',healthCondition:'来源档案显示已绝育、接种疫苗并植入芯片。',personality:'初到陌生环境时需要适应，熟悉后喜欢陪伴、抚摸和互动游戏。',adoptionRequirements:'室内科学喂养；安装纱窗；允许循序适应；每天安排稳定互动时间。',initialHealthRecord:'演示资料来源：Best Friends Animal Society 的 Paradise 公开领养档案。'} },
    { label:'meiqiu', imagePaths:ASSETS.meiqiu, location:'外部领养档案演示：Cashmere', description:'高龄玳瑁母猫正在寄养，适应后亲人、爱交流并喜欢梳毛。', scene:'三张照片来自同一只动物的公开领养档案；中文名和任务地点为功能演示。', records:['核对来源档案和连续照片，建立高龄动物档案。','记录寄养反馈和高龄照护要求。'], followup:'适应后会主动打招呼、探索环境，并在下巴挠痒时持续呼噜。', animal:{name:'煤球',species:'CAT',sex:'FEMALE',estimatedAgeMonths:133,color:'玳瑁短毛',healthCondition:'来源档案显示已绝育、接种疫苗并植入芯片；需要高龄猫常规检查。',personality:'亲人、爱交流，适应后喜欢梳毛、探索环境和安静陪伴。',adoptionRequirements:'适合生活节奏稳定的室内家庭；接受高龄动物定期体检；给予充足适应时间。',initialHealthRecord:'演示资料来源：Best Friends Animal Society 的 Cashmere 公开领养档案。'} },
    { label:'doubao', imagePaths:ASSETS.doubao, location:'外部领养档案演示：Zuzu', description:'黑色大型高龄公犬性格温和，喜欢人与零食，也能接受合适的犬类伙伴。', scene:'三张照片来自同一只动物的公开领养档案；中文名和任务地点为功能演示。', records:['核对来源档案和连续照片，建立高龄犬档案。','记录活动偏好和高龄家庭匹配条件。'], followup:'精神状态稳定，仍愿意参与轻松活动和与照护者互动。', animal:{name:'豆包',species:'DOG',sex:'MALE',estimatedAgeMonths:149,color:'黑色短毛 / 大型',healthCondition:'来源档案显示已开放领养；具体健康记录以来源机构交接资料为准。',personality:'性格温和，喜欢人与零食，也能接受合适的犬类伙伴。',adoptionRequirements:'提供防滑舒适的休息环境；控制运动强度；愿意进行高龄犬常规检查。',initialHealthRecord:'演示资料来源：Best Friends Animal Society 的 Zuzu 公开领养档案。'} },
    { label:'lizi', imagePaths:ASSETS.lizi, location:'外部领养档案演示：Sally', description:'灰、米与白色淡三花母猫独立而有边界感，需要尊重自己的互动节奏。', scene:'三张照片来自同一只动物的公开领养档案；中文名和任务地点为功能演示。', records:['核对来源档案和连续照片，修正物种及毛色信息。','记录互动边界和室内环境要求。'], followup:'在安静环境中状态稳定，喜欢温和抚摸和独处休息。', animal:{name:'栗子',species:'CAT',sex:'FEMALE',estimatedAgeMonths:62,color:'灰、米与白色淡三花短毛',healthCondition:'来源档案显示已开放领养；平台展示不替代领养前的最新检查。',personality:'独立而有边界感，喜欢温和抚摸和安静休息。',adoptionRequirements:'室内喂养并做好门窗防护；不强行抱持；准备可躲藏的安全空间。',initialHealthRecord:'演示资料来源：Best Friends Animal Society 的 Sally 公开领养档案。'} },
    { label:'mili', imagePaths:ASSETS.mili, location:'外部领养档案演示：Moss', description:'棕色虎斑加白公幼猫处于快速成长阶段，好奇、活跃并喜欢探索。', scene:'三张照片来自同一只动物的公开领养档案；中文名和任务地点为功能演示。', records:['核对来源档案和连续照片，建立幼猫成长档案。','记录后续免疫、绝育和环境防护要求。'], followup:'日常进食和活动稳定，持续进行社会化与环境适应。', animal:{name:'米粒',species:'CAT',sex:'MALE',estimatedAgeMonths:4,color:'棕色虎斑加白短毛',healthCondition:'幼猫仍处于成长阶段；疫苗、驱虫和绝育应按实际记录继续完成。',personality:'好奇、活跃，喜欢探索和玩耍，需要稳定互动消耗精力。',adoptionRequirements:'阳台和窗户必须封网；完成后续免疫与绝育；每天提供安全探索时间。',initialHealthRecord:'演示资料来源：Best Friends Animal Society 的 Moss 公开领养档案。'} },
    { label:'abu', imagePaths:ASSETS.abu, location:'外部领养档案演示：Bubs', description:'浅棕色中型公犬活泼、亲人，喜欢其他狗狗和户外活动。', scene:'三张照片来自同一只动物的公开领养档案；中文名和任务地点为功能演示。', records:['核对来源档案和连续照片，建立行为观察。','补充运动、牵引和家庭陪伴要求。'], followup:'活动状态良好，愿意与照护者和其他狗狗互动。', animal:{name:'阿布',species:'DOG',sex:'MALE',estimatedAgeMonths:42,color:'浅棕短毛 / 中型',healthCondition:'来源档案显示已开放领养；具体健康状态以最新检查和交接记录为准。',personality:'活泼、亲人，喜欢其他狗狗，运动意愿强。',adoptionRequirements:'每天保证户外活动和正向训练；出门全程牵引；提供稳定陪伴。',initialHealthRecord:'演示资料来源：Best Friends Animal Society 的 Bubs 公开领养档案。'} }
  ]
  const demoProfileCases = []
  for (const profile of demoProfileInputs) {
    const created = await rescueOneAnimal({adminToken,rescuerToken,userToken,imagePaths:profile.imagePaths,label:profile.label,clue:{location:`${TAG}｜${profile.location}`,animalDescription:profile.description,sceneDescription:profile.scene},records:profile.records,animal:profile.animal})
    await addHealth(rescuerToken, created.animalId, profile.followup)
    await animalAction(rescuerToken, created.animalId, 'TO_OBSERVING')
    await animalAction(rescuerToken, created.animalId, 'OPEN_ADOPTION')
    demoProfileCases.push({...created,name:profile.animal.name})
  }
  const { animalId: catId } = catCase
  const { animalId: dogId } = dogCase
  const { animalId: rabbitId } = rabbitCase

  await addHealth(rescuerToken, catId, '恢复记录：持续完成换药与激光治疗，食欲和活动兴趣逐步改善。')
  await animalAction(rescuerToken, catId, 'TO_OBSERVING')
  await animalAction(rescuerToken, catId, 'OPEN_ADOPTION')
  await addHealth(rescuerToken, dogId, '稳定期记录：毛发与体重逐步恢复，重新开始玩耍；仍需规律用药和复查。')
  await addHealth(rescuerToken, rabbitId, '康复记录：后肢力量明显恢复，可在安全草地活动，仍有轻微步态不稳。')
  await animalAction(rescuerToken, rabbitId, 'TO_OBSERVING')
  await animalAction(rescuerToken, rabbitId, 'OPEN_ADOPTION')

  const rejectedClue = await createClue(userToken, ASSETS.rabbit[0], 'rejected', {
    location: `${TAG}｜信息待补充的示例地点`,
    animalDescription: '一只需要进一步核实身份信息的示例动物。',
    sceneDescription: '演示管理员驳回分支，资料不足时要求补充。'
  })
  await auditClue(adminToken, rejectedClue, 'REJECT', '演示驳回：请补充更清晰的地点和动物情况说明。')

  await dataOf(await request(`/animals/${rabbitId}/favorite`, {
    method: 'POST', token: userToken
  }), 201, `动物 ${rabbitId} 收藏`)

  const adoptionProfile = demoProfileCases.find(item => item.name === '奶糖')
  if (!adoptionProfile) throw new Error('找不到奶糖演示档案')
  const adoptedAnimalId = adoptionProfile.animalId
  const approvedApplication = await createApplication(userToken, adoptedAnimalId, '奶糖')
  const auditedApplication = dataOf(await request(`/admin/adoption-applications/${approvedApplication.id}/audit`, {
    method: 'POST', token: adminToken, body: { decision: 'APPROVE' }
  }), 200, `申请 ${approvedApplication.id} 批准`)
  const adoptionRecordId = auditedApplication.adoptionRecord?.id
  if (!adoptionRecordId) throw new Error('批准领养申请未生成 AdoptionRecord')

  const followups = []
  followups.push(await createFollowUp(userToken, adoptionRecordId, ASSETS.naitang[0], 'follow-up-7d',
    '到家第一周主要在安静房间适应，已经愿意在固定时间进食，并开始接受家人在近处陪伴。',
    '食欲、饮水和排便规律；没有强迫互动，继续观察适应状态。'))
  followups.push(await createFollowUp(userToken, adoptionRecordId, ASSETS.naitang[1], 'follow-up-30d',
    '到家一个月后会主动走近家人，也开始使用猫抓板和玩具。门窗防护已经全部完成。',
    '体重稳定，精神状态正常；按交接计划完成常规健康观察。'))
  followups.push(await createFollowUp(userToken, adoptionRecordId, ASSETS.naitang[2], 'follow-up-90d',
    '三个月回访：作息已经稳定，会在熟悉的人身边休息，也能按自己的节奏与家庭互动。',
    '精神、食欲和排便正常；家庭继续按计划完成预防性照护。'))

  const pendingApplication = await createApplication(userToken, rabbitId, '灰豆')
  const draft = await createAnnouncement(adminToken, `${TAG}｜周末领养见面日筹备`, '本周末拟安排预约制见面，请工作人员先核对动物状态、申请家庭信息和现场消毒物资。本文暂为草稿。')
  const announcementDrafts = [
    await createAnnouncement(adminToken, `${TAG}｜新伙伴开放领养`, '小满和灰豆已完成观察期。申请前请仔细阅读动物健康档案、居住防护和长期照护要求；领养不以提交先后作为唯一依据。'),
    await createAnnouncement(adminToken, `${TAG}｜阿福治疗进展`, '阿福的皮肤与体重状态已经明显改善，目前仍需规律用药、洗护和复查，暂不接受领养申请。感谢大家给它一点时间。'),
    await createAnnouncement(adminToken, `${TAG}｜救助线索拍摄提示`, '提交线索时请优先保证自身安全，拍摄动物全身、受伤部位和周边环境，并提供可定位的地标。不要追赶或强行抓捕。'),
    await createAnnouncement(adminToken, `${TAG}｜领养后回访说明`, '回访用于持续了解适应与健康情况。公开展示只采用获得许可并完成去身份化处理的内容，家庭住址和联系方式不会公开。')
  ]
  const publishedAnnouncements = []
  for (const announcement of announcementDrafts) publishedAnnouncements.push(await publishAnnouncement(adminToken, announcement))

  const output = {
    tag: TAG,
    clues: {
      pendingReview: pendingClue,
      waitingAcceptance: waitingClue,
      inProgressSource: activeClue,
      rejected: rejectedClue
    },
    tasks: { inProgress: activeTask, completed: [catCase.taskId, dogCase.taskId, rabbitCase.taskId, ...demoProfileCases.map(item => item.taskId)] },
    animals: {
      adopted: adoptedAnimalId,
      treating: dogId,
      available: [catId, rabbitId, ...demoProfileCases.filter(item => item.animalId !== adoptedAnimalId).map(item => item.animalId)],
      demoProfiles: Object.fromEntries(demoProfileCases.map(item => [item.name, item.animalId]))
    },
    adoption: {
      approvedRecord: adoptionRecordId,
      pendingApplication: pendingApplication.id
    },
    followUps: followups.map(item => item.id),
    announcements: { draft: draft.id, published: publishedAnnouncements.map(item => item.id) },
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
