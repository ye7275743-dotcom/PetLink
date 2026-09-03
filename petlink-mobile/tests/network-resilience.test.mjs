import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'

const root = path.resolve('src/features')
const featureFiles = [
  'animal/AnimalScreens.vue',
  'content/ContentScreens.vue',
  'clue/ClueScreens.vue',
  'adoption/AdoptionScreens.vue',
  'rescue/RescueScreens.vue',
  'followup/FollowupScreens.vue'
]

test('mobile feature initial loads swallow handled network failures', () => {
  for (const relative of featureFiles) {
    const source = fs.readFileSync(path.join(root, relative), 'utf8')
    assert.doesNotMatch(source, /onMounted\(async\s*\(/, `${relative} must not expose a rejecting async mounted hook`)
    assert.match(source, /onMounted\(\(\)=>\{void initialize\(\)\.catch\(\(\)=>\{\}\)\}\)/, `${relative} must close initial-load errors`)
  }
})

test('Mobile API requests carry a correlation id for support tracing', () => {
  const request = fs.readFileSync(path.resolve('src/api/request.js'), 'utf8')
  assert.match(request, /X-Request-Id/)
  assert.match(request, /createRequestId\(\)/)
})
