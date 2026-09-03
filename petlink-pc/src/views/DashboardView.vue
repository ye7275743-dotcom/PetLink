<template>
  <PageHeader title="救助中心工作台" :subtitle="role==='ADMIN'?'待处理业务优先，统计信息位于第二层级':'查看本人救助任务、负责动物与回访'">
    <el-button :loading="loading" @click="load">刷新</el-button>
  </PageHeader>
  <ListError :message="loadError" @retry="load" />
  <div class="refresh-meta" role="status">{{ loading ? '正在同步最新数据…' : lastLoadedAt ? `最后更新：${lastLoadedAt}` : '尚未同步数据' }}</div>
  <div class="hero-panel">
    <div class="hero-copy"><span class="eyebrow">救助中心 · 今日</span><h2>每一次救助，都是改变命运的开始。</h2><p>{{ greeting }}</p></div>
    <div class="hero-art" aria-hidden="true"><img src="/assets/petlink-rescue-hero-v1.jpg" alt="" /></div>
  </div>
  <div v-if="role==='ADMIN'" v-loading="loading" class="content-grid" style="margin-top:20px">
    <div class="card metric-card"><div class="muted">待审核线索</div><div class="metric">{{ count('rescueClues','PENDING_REVIEW') }}</div></div>
    <div class="card metric-card"><div class="muted">进行中任务</div><div class="metric">{{ count('rescueTasks','IN_PROGRESS') }}</div></div>
    <div class="card metric-card"><div class="muted">待领养动物</div><div class="metric">{{ count('animals','AVAILABLE') }}</div></div>
    <div class="card metric-card"><div class="muted">待审核申请</div><div class="metric">{{ count('adoptionApplications','PENDING') }}</div></div>
  </div>
  <div v-else v-loading="loading" class="content-grid" style="margin-top:20px">
    <div class="card metric-card"><div class="muted">我的待开始任务</div><div class="metric">{{ tasks.filter(x=>x.status==='WAITING_START').length }}</div></div>
    <div class="card metric-card"><div class="muted">我的进行中任务</div><div class="metric">{{ tasks.filter(x=>x.status==='IN_PROGRESS').length }}</div></div>
    <div class="card metric-card"><div class="muted">负责动物</div><div class="metric">{{ animals.length }}</div></div>
    <div class="card metric-card"><div class="muted">近期回访</div><div class="metric">{{ followups.length }}</div></div>
  </div>
  <div v-loading="loading" class="two" style="margin-top:14px">
    <div class="card"><h3 class="section-title">{{ role==='ADMIN'?'近期趋势':'我的救助任务' }}</h3>
      <template v-if="role==='ADMIN'"><div v-if="trends.length" class="bars"><div v-for="p in trends" :key="p.date" class="bar-col"><div class="bar-a" :style="{height:(12+p.rescueSuccessCount*12)+'px'}"></div><div class="bar-b" :style="{height:(12+p.adoptionCount*12)+'px'}"></div><span>{{ p.date?.slice(5) }}</span></div></div><div v-else class="empty">暂无趋势数据</div></template>
      <template v-else><div v-for="t in tasks.slice(0,5)" :key="t.id" class="line"><StatusBadge :value="t.status"/><b>#{{t.id}}</b><span>{{t.clueId?'线索 #'+t.clueId:''}}</span></div></template>
    </div>
    <div class="card"><h3 class="section-title">今日操作提示</h3><div class="reason">状态按钮只展示合法动作；前端显示不替代后端权限与状态校验。</div><div class="timeline" style="margin-top:18px"><div class="timeline-item"><b>线索审核</b><div class="muted">待审核 → 待接取 / 已驳回</div></div><div class="timeline-item"><b>救助闭环</b><div class="muted">待开始 → 进行中 → 救助成功 / 救助失败</div></div><div class="timeline-item"><b>领养闭环</b><div class="muted">待审核 → 已批准 / 已驳回</div></div></div></div>
  </div>
</template>
<script setup>
import {ref,computed,onMounted} from 'vue'
import PageHeader from '../components/PageHeader.vue'
import StatusBadge from '../components/StatusBadge.vue'
import ListError from '../components/ListError.vue'
import {adminApi,rescueApi,animalApi,followupApi} from '../api/index.js'
import {useAuth} from '../store/auth.js'

const auth=useAuth()
const role=computed(()=>auth.state.user?.roleCode)
const stats=ref({});const trends=ref([]);const tasks=ref([]);const animals=ref([]);const followups=ref([])
const loading=ref(false);const loadError=ref('');const lastLoadedAt=ref('')
const greeting=computed(()=>`您好，${auth.state.user?.nickname||auth.state.user?.account||'宠链成员'}。`)
const count=(group,key)=>stats.value?.[group]?.[key]??0
const shanghaiDate=date=>new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Shanghai',year:'numeric',month:'2-digit',day:'2-digit'}).format(date)
const formatUpdatedAt=date=>new Intl.DateTimeFormat('zh-CN',{timeZone:'Asia/Shanghai',year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit'}).format(date)

async function load(){
  loading.value=true;loadError.value=''
  try{
    if(role.value==='ADMIN'){
      const now=new Date();const to=shanghaiDate(now);const from=shanghaiDate(new Date(now.getTime()-6*86400000))
      const [overview,trendData]=await Promise.all([adminApi.statsOverview(),adminApi.statsTrends({from,to,granularity:'DAY'})])
      stats.value=overview;trends.value=trendData?.points||[]
    }else{
      const [t,a,f]=await Promise.all([rescueApi.mine({page:1,size:20}),animalApi.responsible({page:1,size:20}),followupApi.rescuerList({page:1,size:20})])
      tasks.value=t?.records||[];animals.value=a?.records||[];followups.value=f?.records||[]
    }
    lastLoadedAt.value=formatUpdatedAt(new Date())
  }catch(e){loadError.value=e?.userMessage||'工作台数据加载失败，请稍后重试'}
  finally{loading.value=false}
}
onMounted(load)
</script>
<style scoped>.bars{height:190px;display:flex;gap:14px;align-items:flex-end;padding:14px 4px 0;border-bottom:1px solid rgba(216,209,195,.76)}.bar-col{flex:1;display:flex;align-items:flex-end;gap:4px;height:100%;position:relative;padding-bottom:22px}.bar-a,.bar-b{width:45%;min-height:4px;background:linear-gradient(180deg,#b7d4cd,var(--sky));border-radius:6px 6px 2px 2px;box-shadow:0 4px 10px rgba(155,194,186,.2)}.bar-b{background:linear-gradient(180deg,#f18b70,var(--orange));box-shadow:0 4px 10px rgba(228,104,74,.2)}.bar-col span{position:absolute;bottom:0;left:0;font:9px var(--mono);color:var(--muted)}.line{display:flex;align-items:center;gap:10px;padding:13px 0;border-bottom:1px solid rgba(216,209,195,.76)}.line:last-child{border-bottom:0}</style>
