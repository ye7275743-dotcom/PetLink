import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const request = readFileSync(new URL('../src/api/request.js', import.meta.url), 'utf8')
const authImage = readFileSync(new URL('../src/components/AuthImage.vue', import.meta.url), 'utf8')

test('missing protected images fall back without a global request error toast', () => {
  assert.match(authImage, /responseType:'blob',silent:true/)
  assert.match(authImage, /catch\{resolved\.value=props\.fallback\}/)
  assert.match(request, /!error\.config\?\.silent/)
})

test('non-JSON network errors use Chinese user-facing fallbacks', () => {
  assert.match(request, /statusMessages/)
  assert.match(request, /fallbackMessage\(error, status\)/)
  assert.match(request, /safeServerMessage\(body\?\.message/)
  assert.match(request, /无法连接服务器，请检查网络/)
  assert.doesNotMatch(request, /body\?\.message \|\| error\.message \|\|/)
})

test('PC API requests carry a correlation id for support tracing', () => {
  assert.match(request, /X-Request-Id/)
  assert.match(request, /randomUUID|web-/)
})
