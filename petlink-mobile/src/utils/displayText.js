const TEXT = Object.freeze({
  VISITOR:'访客', USER:'普通用户', RESCUER:'救助人员', ADMIN:'管理员',
  ENABLED:'已启用', DISABLED:'已禁用',
  PENDING_REVIEW:'待审核', WAITING_ACCEPT:'待接取', REJECTED:'已驳回', WITHDRAWN:'已撤回', CONVERTED:'已转为任务', CLOSED:'已关闭',
  WAITING_START:'待开始', IN_PROGRESS:'进行中', SUCCESS:'救助成功', FAILED:'救助失败', CANCELED:'已取消',
  TREATING:'治疗中', OBSERVING:'观察中', AVAILABLE:'可领养', SUSPENDED:'暂停领养', ADOPTED:'已领养',
  PENDING:'待审核', APPROVED:'已批准', INVALIDATED:'已失效',
  DRAFT:'草稿', PUBLISHED:'已发布',
  RABBIT:'兔', BIRD:'鸟', HAMSTER:'仓鼠',
  CAT:'猫', DOG:'狗', OTHER:'其他', MALE:'公', FEMALE:'母', UNKNOWN:'未知'
})

export const displayText=value=>value==null||value===''?'—':(TEXT[value]||String(value))
export const roleText=displayText
export const statusText=displayText
export const speciesText=displayText
export const sexText=displayText
