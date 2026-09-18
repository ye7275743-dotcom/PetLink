# PetLink 演示图片

本目录保存早期自动化回归使用的占位图片。它们不是当前服务器真实来源案例的素材，不能用于对外展示或冒充救助记录：

| 文件 | 用途 |
|---|---|
| `demo-orange-cat.png` | 橘白猫档案、线索和领养回访 |
| `demo-black-white-dog.png` | 黑白狗档案和救助线索 |
| `demo-gray-rabbit.png` | 灰色垂耳兔档案和待接取线索 |

当前真实来源案例使用 `petlink-pc/public/assets/content/` 中的公开来源图片，并通过 `scripts/seed-real-cases.mjs` 经正式业务接口上传。不要把本目录的占位图写入线上数据库，也不要直接把本地文件路径写入数据库。

## 早期回归脚本（不再用于线上数据）

`seed-demo-data.mjs` 和 `seed-demo-task-states.mjs` 会产生历史演示/回归批次，不能对当前服务器重复执行。真实案例请使用项目根目录的 `scripts/seed-real-cases.mjs`。
