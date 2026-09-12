import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile, readdir } from 'node:fs/promises'

const read = path => readFile(new URL(`../${path}`, import.meta.url), 'utf8')

test('mobile home includes rescue progress, follow-up letters and trust practices', async () => {
  const [screen, content] = await Promise.all([
    read('src/features/animal/AnimalScreens.vue'), read('src/content/publicContent.js')
  ])
  assert.match(screen, /救助纪实 · RESCUE LOG/)
  assert.match(screen, /新家来信 · FOLLOW-UP/)
  assert.match(screen, /mobile-timeline/)
  assert.match(screen, /trustPractices/)
  assert.match(content, /回访尊重隐私/)
  assert.match(screen, /demoNotice/)
  assert.match(screen, /图片来源/)
  assert.match(screen, /story\.images/)
  assert.match(screen, /openSource/)
})

test('all 36 mobile content assets are real JPEG files', async () => {
  const directory = new URL('../src/static/assets/content/', import.meta.url)
  const files = (await readdir(directory)).filter(name => name.endsWith('.jpg')).sort()
  assert.equal(files.length, 36)
  for (const name of files) {
    const bytes = await readFile(new URL(name, directory))
    assert.deepEqual([...bytes.subarray(0, 3)], [0xff, 0xd8, 0xff], `${name} is not a JPEG`)
  }
})

test('mobile logged-in screens show case photos and complete animal profiles', async () => {
  const [tasks, taskHeader, animals] = await Promise.all([
    read('src/features/rescue/RescueScreens.vue'), read('src/components/TaskHeader.vue'), read('src/features/animal/AnimalScreens.vue')
  ])
  assert.match(tasks, /c\.coverImageUrl/)
  assert.match(taskHeader, /task\.clue\?\.coverImageUrl/)
  assert.match(taskHeader, /task\.clue\?\.animalDescription/)
  assert.match(animals, /animal\.personality/)
  assert.match(animals, /animal\.adoptionRequirements/)
})
