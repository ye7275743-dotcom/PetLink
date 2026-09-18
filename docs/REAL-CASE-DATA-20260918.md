# PetLink 真实来源案例数据记录

更新时间：2026-09-18

本批数据用于展示真实用户可能经历的救助与领养流程。数据通过 PetLink 正式 REST API 创建，图片来自 Best Friends Animal Society 公开页面；位置、联系方式和动物中文名均已去标识化，不应被解释为 PetLink 自有救助记录或与来源机构存在合作关系。

## 当前服务器批次

服务器：`103.233.254.186:8081`

| 场景 | 业务对象 | 当前状态 | 图片/来源 |
|---|---|---|---|
| 居民提交受伤幼猫线索 | 线索 28 | 待审核 | Marble 公开救助案例 |
| 兔类临时安置 | 线索 29 | 待接取 | Coop 公开救助案例 |
| 皮肤病犬医疗转介 | 线索 30、任务 15 | 进行中 | David Bowie 公开救助案例 |
| 兔类康复后开放领养 | 动物 12 | 可领养 | Coop 公开救助案例 |
| 寄养猫领养闭环 | 动物 13、领养记录 5 | 已领养并回访 | Paradise 公开领养档案 |
| 高龄犬寄养评估 | 动物 14 | 可领养 | Zuzu 公开领养档案 |

## 来源

- Marble：<https://bestfriends.org/stories/features/faces-no-kill-cat-heals-injury-style>
- David Bowie：<https://bestfriends.org/stories/features/faces-no-kill-dog-goes-shutdown-smiling>
- Coop：<https://bestfriends.org/stories/features/partially-paralyzed-rabbit-gets-hopping-again>
- Paradise：<https://bestfriends.org/new-york-city/adopt/213183902/paradise>
- Zuzu：<https://bestfriends.org/sanctuary/adopt/213258367/zuzu>

图片文件、原动物名称和授权边界见 [PHOTO-ASSET-MAP.md](content/PHOTO-ASSET-MAP.md)。公开部署或商业使用前，应取得图片权利方许可，或替换为 PetLink 自有且明确授权的图片。

## 重新生成

```bash
PETLINK_REAL_CASE_ORIGIN='http://127.0.0.1:8080/api' \
PETLINK_REAL_CASE_PASSWORD='本地演示密码' \
node scripts/seed-real-cases.mjs
```

脚本会创建完整状态链，但不会删除旧数据。清理历史自动化案例前，先备份数据库，再按当前快照审阅并执行 `sql/cleanup-20260918-test-cases.sql`；该脚本默认 `ROLLBACK`，确认选中行后才可改为 `COMMIT`。
