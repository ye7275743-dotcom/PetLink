const credit=subject=>`Best Friends Animal Society · 原照片动物：${subject}`

export const rescueStories=[
  {slug:'xiaoman',name:'小满',profile:'猫 · 康复档案',area:'来源机构公开记录',status:'恢复照护中',summary:'幼猫转入救助机构时两条后腿严重骨折。经过专科手术、固定、换药、激光治疗和持续照护后逐步恢复。',cover:'/assets/content/rescue-xiaoman-process.jpg',images:[{src:'/assets/content/rescue-xiaoman-process.jpg',label:'治疗与换药'},{src:'/assets/content/rescue-xiaoman-recovery.jpg',label:'恢复期照护'}],fallback:'/assets/pudding-cat-hero.png',credit:credit('Marble'),sourceUrl:'https://bestfriends.org/stories/features/faces-no-kill-cat-heals-injury-style',note:'图片与病例信息可通过来源链接查看。',milestones:[{time:'接诊',title:'评估复杂骨折',text:'完成止痛、补液、影像检查并转诊专科。'},{time:'手术',title:'稳定受伤肢体',text:'左后腿截肢，右腿使用金属针和夹板固定。'},{time:'恢复',title:'持续换药与康复',text:'通过换药、激光治疗、营养和互动照护逐步恢复。'}]},
  {slug:'afu',name:'阿福',profile:'狗 · 康复档案',area:'来源机构公开记录',status:'长期健康管理',summary:'Bowie 到来时皮肤发炎、毛发稀疏且几乎不愿活动。确诊自身免疫性皮肤病后，通过针对性用药、规律洗护和寄养照护恢复活力。',cover:'/assets/content/rescue-afu-process.jpg',images:[{src:'/assets/content/rescue-afu-process.jpg',label:'治疗与洗护'},{src:'/assets/content/rescue-afu-recovery.jpg',label:'恢复后活动'}],fallback:'/assets/petlink-rescue-hero-v1.jpg',credit:credit('David Bowie'),sourceUrl:'https://bestfriends.org/stories/features/faces-no-kill-dog-goes-shutdown-smiling',note:'图片与病例信息可通过来源链接查看。该病仍需持续用药和复查。',milestones:[{time:'初诊',title:'排查皮肤病因',text:'常规治疗效果不佳后继续检查，确认自身免疫性疾病。'},{time:'治疗',title:'用药与规律洗护',text:'接受针对性药物、舒缓洗护和营养照护。'},{time:'稳定',title:'恢复活动与社交',text:'体重和毛发逐步恢复，重新开始玩耍。'}]},
  {slug:'huidou',name:'灰豆',profile:'兔 · 康复档案',area:'来源机构公开记录',status:'已恢复活动',summary:'Coop 被发现时后肢瘫痪且营养不良。通过药物、补液、物理治疗和安全草地活动，逐渐重新站立和跳跃。',cover:'/assets/content/rescue-huidou-process.jpg',images:[{src:'/assets/content/rescue-huidou-process.jpg',label:'兽医检查'},{src:'/assets/content/rescue-huidou-recovery.jpg',label:'草地康复'}],fallback:'/assets/pudding-cat-hero.png',credit:credit('Coop'),sourceUrl:'https://bestfriends.org/stories/features/partially-paralyzed-rabbit-gets-hopping-again',note:'图片与病例信息可通过来源链接查看。案例展示兔类救助对医疗、环境和康复记录的衔接。',milestones:[{time:'接诊',title:'稳定身体状况',text:'完成清洁、营养支持、止痛、抗生素和补液。'},{time:'观察',title:'首次重新站立',text:'约一周后开始抬起后躯并尝试迈步。'},{time:'康复',title:'物理治疗与练习',text:'通过腿部活动训练和草地练习逐步恢复跳跃。'}]}
]

export const familyLetters=[
  {name:'Chip 的新家近况',period:'领养后约 5 年',quote:'从最初躲藏，到主动依偎家人并融入有猫有狗的家庭，它终于拥有稳定而长久的陪伴。',detail:'来源文章记录了 Chet（到家后改名 Chip）适应家庭并与其他动物建立关系的过程。',image:'/assets/content/family-xiaoman-02.jpg',fallback:'/assets/pudding-cat-hero.png',credit:credit('Chet / Chip'),sourceUrl:'https://bestfriends.org/stories/features/adoption-update-worlds-cuddliest-cat'},
  {name:'Nina 的新家近况',period:'训练与领养回访',quote:'从害怕陌生人、难以牵引，到能参加外出活动并被家庭接纳，耐心和训练让它逐渐建立信任。',detail:'来源文章用连续照片记录了 Nina 从收容、训练到新家生活的变化。',image:'/assets/content/family-doubao-02.jpg',fallback:'/assets/petlink-rescue-hero-v1.jpg',credit:credit('Nina'),sourceUrl:'https://bestfriends.org/stories/features/shelter-dogs-journey-home-photos'},
  {name:'Bruce 的四年后',period:'领养后 4 年+',quote:'幼猫和家中的孩子一起长大，从最初的玩耍伙伴变成彼此日常生活里稳定的陪伴。',detail:'来源文章记录 Bruce 从约三个月大被领养，到四年后仍与家庭共同成长。',image:'/assets/content/family-mili-02.jpg',fallback:'/assets/pudding-cat-hero.png',credit:credit('Bruce'),sourceUrl:'https://bestfriends.org/stories/videos/adoption-update-baby-and-kitten-grow-together'}
]

export const trustPractices=[
  {number:'01',title:'过程有迹可循',text:'线索审核、任务接取和每次救助记录都有时间与责任人。'},
  {number:'02',title:'健康情况不美化',text:'已知病史、治疗进展和后续照护要求如实进入动物档案。'},
  {number:'03',title:'领养不是先到先得',text:'结合居住条件、家庭共识、照护经验和动物需求综合审核。'},
  {number:'04',title:'回访尊重隐私',text:'公开故事只使用经许可、去身份化的内容，完整资料仅授权角色可见。'}
]
