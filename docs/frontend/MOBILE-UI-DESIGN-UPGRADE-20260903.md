# PetLink 移动端 UI 设计升级记录（2026-09-03）

## 变更原因

现有移动端功能与交互契约已通过验收，但公共壳层和业务页面的视觉层次仍偏平，品牌识别、状态反馈和触控层级不够集中。本次仅优化展示与交互反馈，不改变 API、路由、权限、状态机或数据结构。

## 变更范围

| 区域 | 升级内容 |
|---|---|
| 公共壳层 | 增加 PETLINK CARE 状态栏、品牌图形标记、CARE NETWORK 辅助信息和角色状态点；头部采用玻璃渐变层与品牌色分隔线 |
| 底部导航 | 增加半透明悬浮层、顶部高光线、激活项上浮和图标强调，保留原有路由及无障碍标签 |
| 首页 Hero | 调整文案节奏、主视觉高度、图片裁切和主操作按钮，保持既有 ImageGen 主视觉资源 |
| 内容表面 | 统一卡片、筛选器、按钮、输入框、详情封面、时间线、状态徽章和空状态的圆角、阴影、边框与强调色 |
| 表单与上传 | 统一字段标签、错误提示、返回按钮和图片上传行的触控尺寸及反馈样式 |

## 兼容性影响

- 保持 29 个 UniApp 页面和现有页面级路由守卫不变。
- 保持所有业务 CSS 类名和组件 props/events 契约；仅调整样式与 `StatusBadge` 的展示 tone 分类。
- H5 无横向滚动；安全区底部留白仍覆盖固定底部导航。
- 遵循 `prefers-reduced-motion`，按钮和导航保留可见焦点与按下反馈。

## 验收证据

```text
Mobile tests       18/18 PASSED
Mobile H5 build    PASSED
Browser H5         首页/登录页视觉复核通过
Viewport           476 x 903，document scrollWidth = 476（无横向溢出）
``` 

涉及文件：

- `petlink-mobile/src/uni.scss`
- `petlink-mobile/src/components/MobileShell.vue`
- `petlink-mobile/src/components/StatusBadge.vue`
- `petlink-mobile/src/components/PageState.vue`
- `petlink-mobile/src/components/BackButton.vue`
- `petlink-mobile/src/components/FormField.vue`
- `petlink-mobile/src/components/FieldError.vue`
- `petlink-mobile/src/components/ImagePicker.vue`
- `petlink-mobile/src/components/Icon.vue`
- `petlink-mobile/tests/ui-visual-contract.test.mjs`
