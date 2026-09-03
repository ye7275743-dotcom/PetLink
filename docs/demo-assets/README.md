# PetLink 演示图片

本目录保存本次服务器演示数据使用的宠物图片，均由 Codex 内置 ImageGen（gpt-image-2 路由）生成，未使用真实个人或动物照片：

| 文件 | 用途 |
|---|---|
| `demo-orange-cat.png` | 橘白猫档案、线索和领养回访 |
| `demo-black-white-dog.png` | 黑白狗档案和救助线索 |
| `demo-gray-rabbit.png` | 灰色垂耳兔档案和待接取线索 |

图片仅作为课程演示素材。通过业务接口上传后，服务器会把图片绑定到对应线索、动物档案或回访记录；不要直接把本地文件路径写入数据库。

## 重新生成演示数据

在本地项目根目录执行，脚本每次使用新的演示批次标签，不删除已有数据：

```bash
cd PetLink
PETLINK_DEMO_PASSWORD='你的演示密码' node scripts/seed-demo-data.mjs
PETLINK_DEMO_PASSWORD='你的演示密码' node scripts/seed-demo-task-states.mjs
```

如需换服务器地址，可设置 `PETLINK_DEMO_ORIGIN`；如需换图片，可设置 `PETLINK_DEMO_CAT`、`PETLINK_DEMO_DOG`、`PETLINK_DEMO_RABBIT` 的绝对路径。
