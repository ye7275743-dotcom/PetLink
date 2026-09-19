import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const login = readFileSync(new URL('../src/views/LoginView.vue', import.meta.url), 'utf8')
const api = readFileSync(new URL('../src/api/index.js', import.meta.url), 'utf8')
const request = readFileSync(new URL('../src/api/request.js', import.meta.url), 'utf8')

test('登录页提交失败时显示可理解的页面内提示', () => {
  assert.match(login, /@submit\.prevent="submit"/)
  assert.match(login, /errorMessage/)
  assert.match(login, /catch\(e\)/)
  assert.match(login, /e\?\.userMessage\|\|'登录失败，请检查账号和密码'/)
  assert.match(login, /silent:true/)
  assert.match(login, /native-type="submit"/)
  assert.match(api, /login: \(data, config = \{\}\) => request\.post\('\/auth\/login', data, config\)/)
})

test('登录与注册请求不携带旧会话，避免旧 token 阻塞重新登录', () => {
  assert.match(request, /publicAuthPath/)
  assert.match(request, /isPublicAuthRequest\(config\)/)
  assert.match(request, /state\.token && !isPublicAuthRequest\(config\)/)
})
