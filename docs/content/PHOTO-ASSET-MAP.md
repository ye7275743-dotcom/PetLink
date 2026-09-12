# PetLink 演示素材映射与来源

> 当前共接入 36 张网页图片，PC 与移动端各一份。第三方真实动物照片只用于课程原型和功能演示，不代表照片中的动物、人员或机构与 PetLink 存在合作、授权或背书关系。

## 公开展示规则

- 中文动物名和 PetLink 医疗/领养字段是功能演示数据；来源动物的真实身份会在图片来源处单独标明。
- 不得把 Best Friends Animal Society 的图片写成“PetLink 志愿者拍摄”或“PetLink 自有案例”。
- 来源标注不等于取得再使用授权。公开部署或商业使用前必须获得权利方许可，或替换成 PetLink 自有且已获公开授权的照片。
- 直接下载时返回 AVIF、MPO 或其他格式的文件，已统一转为真正的 RGB JPEG，避免静态服务器按 `.jpg` MIME 类型发送时显示失败。

## 同一动物连续照片（18 张）

每组的封面、全身照和生活照均来自同一个来源档案。

| PetLink 演示名 | 原照片动物 | 文件 | 来源档案 |
|---|---|---|---|
| 奶糖 | Paradise | `animal-naitang-cover.jpg`、`animal-naitang-full.jpg`、`animal-naitang-life.jpg` | <https://bestfriends.org/new-york-city/adopt/213183902/paradise> |
| 煤球 | Cashmere | `animal-meiqiu-cover.jpg`、`animal-meiqiu-full.jpg`、`animal-meiqiu-life.jpg` | <https://bestfriends.org/new-york-city/adopt/214271912/cashmere> |
| 豆包 | Zuzu | `animal-doubao-cover.jpg`、`animal-doubao-full.jpg`、`animal-doubao-life.jpg` | <https://bestfriends.org/sanctuary/adopt/213258367/zuzu> |
| 栗子 | Sally | `animal-lizi-cover.jpg`、`animal-lizi-full.jpg`、`animal-lizi-life.jpg` | <https://bestfriends.org/sanctuary/adopt/56683310/sally> |
| 米粒 | Moss | `animal-mili-cover.jpg`、`animal-mili-full.jpg`、`animal-mili-life.jpg` | <https://bestfriends.org/sanctuary/adopt/214077523/moss> |
| 阿布 | Bubs | `animal-abu-cover.jpg`、`animal-abu-full.jpg`、`animal-abu-life.jpg` | <https://bestfriends.org/sanctuary/adopt/213764787/bubs> |

## 救助过程照片（6 张）

| PetLink 演示名 | 原案例动物 | 文件 | 来源文章 |
|---|---|---|---|
| 小满 | Marble | `rescue-xiaoman-process.jpg`、`rescue-xiaoman-recovery.jpg` | <https://bestfriends.org/stories/features/faces-no-kill-cat-heals-injury-style> |
| 阿福 | David Bowie | `rescue-afu-process.jpg`、`rescue-afu-recovery.jpg` | <https://bestfriends.org/stories/features/faces-no-kill-dog-goes-shutdown-smiling> |
| 灰豆 | Coop | `rescue-huidou-process.jpg`、`rescue-huidou-recovery.jpg` | <https://bestfriends.org/stories/features/partially-paralyzed-rabbit-gets-hopping-again> |

## 领养回访照片（3 张）

| 展示对象 | 文件 | 来源文章 |
|---|---|---|
| Chet / Chip | `family-xiaoman-02.jpg` | <https://bestfriends.org/stories/features/adoption-update-worlds-cuddliest-cat> |
| Nina | `family-doubao-02.jpg` | <https://bestfriends.org/stories/features/shelter-dogs-journey-home-photos> |
| Bruce | `family-mili-02.jpg` | <https://bestfriends.org/stories/videos/adoption-update-baby-and-kitten-grow-together> |

## 其余场景素材（9 张）

| 页面用途 | 文件 | 公开来源文案 |
|---|---|---|
| 首页主图 | `home-hero.jpg` | 用户提供 · 场景示意 |
| 原小满场景图 | `rescue-xiaoman-cover.jpg` | Gustavo Fring / Pexels · 场景示意 |
| 原阿福场景图 | `rescue-afu-cover.jpg` | 用户提供 · 场景示意 |
| 原灰豆场景图 | `rescue-huidou-cover.jpg` | 用户提供 · 场景示意 |
| 原猫咪回访场景 | `family-xiaoman.jpg` | 用户提供 · 场景示意 |
| 原犬只回访场景 | `family-doubao.jpg` | Samson Katt / Pexels · 场景示意 |
| 原猫咪回访场景 | `family-mili.jpg` | 用户提供 · 场景示意 |
| 志愿者服务 | `service-volunteers.jpg` | 用户提供 · 场景示意 |
| 医疗服务 | `service-medical.jpg` | Tima Miroshnichenko / Pexels · 场景示意 |

后三张回访场景旧图和三张救助场景旧图仍保留在素材目录，便于缺图回退，但公开故事页已经使用来源清晰的过程/回访图片。

## 同步目录

- PC：`petlink-pc/public/assets/content/`
- 移动端：`petlink-mobile/src/static/assets/content/`

两个目录中的 36 个文件名和文件内容应保持一致。
