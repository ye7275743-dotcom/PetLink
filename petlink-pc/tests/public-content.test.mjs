import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile, readdir } from 'node:fs/promises'

const read = path => readFile(new URL(`../${path}`, import.meta.url), 'utf8')

test('public site exposes rescue stories and follow-up letters as a first-class route', async () => {
  const [view, router, content] = await Promise.all([
    read('src/views/PublicView.vue'), read('src/router/index.js'), read('src/content/publicContent.js')
  ])
  assert.match(router, /path:'\/stories'/)
  assert.match(view, /RESCUE LOG · 救助纪实/)
  assert.match(view, /LETTERS FROM HOME · 新家来信/)
  assert.match(view, /trustPractices/)
  assert.equal((content.match(/slug\s*:\s*'/g) || []).length, 3)
  assert.equal((content.match(/period\s*:\s*'/g) || []).length, 3)
  assert.equal((content.match(/id\s*:\s*'PL-/g) || []).length, 6)
  assert.equal((content.match(/animal-[a-z]+-full\.jpg/g) || []).length, 6)
  assert.equal((content.match(/animal-[a-z]+-life\.jpg/g) || []).length, 6)
  assert.match(view, /story\.images/)
  assert.match(view, /animal\.images/)
  assert.doesNotMatch(view, /演示内容说明|demoNotice/)
  assert.match(view, /图片来源/)
  assert.match(view, /sourceUrl/)
  assert.doesNotMatch(content, /五星好评|★★★★★/)
})

test('all 36 desktop content assets are real JPEG files', async () => {
  const directory = new URL('../public/assets/content/', import.meta.url)
  const files = (await readdir(directory)).filter(name => name.endsWith('.jpg')).sort()
  assert.equal(files.length, 36)
  for (const name of files) {
    const bytes = await readFile(new URL(name, directory))
    assert.deepEqual([...bytes.subarray(0, 3)], [0xff, 0xd8, 0xff], `${name} is not a JPEG`)
  }
})

test('missing local story photos fall back without a broken image', async () => {
  const image = await read('src/components/AuthImage.vue')
  assert.match(image, /@error="onError"/)
  assert.match(image, /resolved\.value=props\.fallback/)
})

test('logged-in workbench keeps case images and complete animal profiles', async () => {
  const [tasks, animals, dashboard] = await Promise.all([
    read('src/views/TasksView.vue'), read('src/views/AnimalsView.vue'), read('src/views/DashboardView.vue')
  ])
  assert.match(tasks, /detail\.clue\?\.coverImageUrl/)
  assert.match(tasks, /detail\.clue\?\.animalDescription/)
  assert.match(animals, /edit\.personality/)
  assert.match(animals, /edit\.adoptionRequirements/)
  assert.match(dashboard, /recentAnimals/)
  assert.match(dashboard, /animal\.coverImageUrl/)
})
