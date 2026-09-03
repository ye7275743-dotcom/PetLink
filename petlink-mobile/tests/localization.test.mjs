import test from 'node:test'
import assert from 'node:assert/strict'
import { displayText, roleText } from '../src/utils/displayText.js'

test('移动端将角色、状态、物种和性别显示为中文', () => {
  const expected = {
    ADMIN:'管理员', RESCUER:'救助人员', USER:'普通用户', VISITOR:'访客',
    ENABLED:'已启用', DISABLED:'已禁用', PENDING_REVIEW:'待审核',
    WAITING_ACCEPT:'待接取', WAITING_START:'待开始', IN_PROGRESS:'进行中',
    SUCCESS:'救助成功', FAILED:'救助失败', AVAILABLE:'可领养', ADOPTED:'已领养',
    DRAFT:'草稿', PUBLISHED:'已发布', WITHDRAWN:'已撤回',
    CAT:'猫', DOG:'狗', OTHER:'其他', MALE:'公', FEMALE:'母', UNKNOWN:'未知'
  }
  for (const [code, text] of Object.entries(expected)) assert.equal(displayText(code), text, code)
  assert.equal(roleText('RESCUER'), '救助人员')
})
