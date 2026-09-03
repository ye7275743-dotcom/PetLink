# PetLink 页面原型

| 目录 | 定位 | 状态 |
| --- | --- | --- |
| `v1.3-functional-frozen/` | M01～M08 功能、导航、权限和异常场景基线 | Frozen |
| `v2-high-fidelity/` | 基于 V1.3 的“城市救助现场档案”高保真视觉版本 | V2.0 |

V2 只改变视觉 Token、布局、响应式表现与微动效，不改变 V1.3 已冻结的页面数量、业务状态、角色权限和 JavaScript 交互逻辑。

两个版本中的 HTML 均可直接离线打开。正式前端实现阶段应以冻结 REST API 为依据，将 V2 的视觉规范迁移到 Vue 3 + Element Plus 与 UniApp。
