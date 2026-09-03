<template>
  <MobileShell :screen="screen" @reach-bottom="onReachBottom">
    <template v-if="screen==='tasks'">
      <view class="task-head"><view><text class="eyebrow">救助行动</text><text class="page-title">救助任务</text></view></view>
      <view v-if="role!=='RESCUER'" class="guest-note">只有救助人员可查看待接取线索和本人任务。</view>
      <template v-else>
        <view class="title">等待接取</view>
        <PageState :loading="waiting.loading.value" :error="waiting.error.value" :empty="!waiting.records.value.length" :has-data="!!waiting.records.value.length" :loading-more="waiting.loadingMore.value" :has-more="waiting.hasMore.value" @retry="waiting.retry">
          <view v-for="c in waiting.records.value" :key="c.id" class="card action-card"><StatusBadge :value="c.status"/><text class="card-title">{{c.animalDescription}}</text><view class="muted meta-line"><Icon name="pin" :size="18" /><text>{{c.location}} · {{c.foundTime}}</text></view><view class="actions"><button class="btn" @click="goto('waitingClue',{id:c.id})"><Icon name="eye" :size="18" />查看详情</button><button class="btn primary" :disabled="actionSubmit.submitting.value" @click="accept(c)"><Icon name="check" :size="18" />接取救助</button></view></view>
        </PageState>
        <view class="section-divider"/><view class="title">我的任务</view>
        <view class="filter-bar"><picker :range="taskStatusOptions.map(x=>x?displayText(x):'全部')" @change="taskStatus=taskStatusOptions[$event.detail.value];reloadTasks()"><view class="filter-item">状态：{{taskStatus?displayText(taskStatus):'全部'}}</view></picker><button class="btn" @click="refreshAll">刷新</button></view>
        <PageState :loading="tasks.loading.value" :error="tasks.error.value" :empty="!tasks.records.value.length" :has-data="!!tasks.records.value.length" :loading-more="tasks.loadingMore.value" :has-more="tasks.hasMore.value" @retry="tasks.retry">
          <view v-for="t in tasks.records.value" :key="t.id" class="card"><StatusBadge :value="t.status"/><text class="card-title">任务 #{{t.id}}</text><text class="muted">线索 #{{t.clueId}}</text><view class="actions"><button class="btn primary" @click="goto(t.status==='WAITING_START'?'taskWaitingStart':'taskProgress',{id:t.id})">任务详情</button></view></view>
        </PageState>
      </template>
    </template>

    <template v-else-if="screen==='taskWaitingStart'">
      <BackButton/><PageState :loading="detailState.loading.value" :error="detailState.error.value" :empty="!task" :has-data="!!task" @retry="loadTask"><TaskHeader :task="task"/><view class="card"><view class="notice">只有任务负责人可以开始救助。</view><button v-if="task?.status==='WAITING_START'" class="btn primary wide" :disabled="actionSubmit.submitting.value" @click="startTask">{{actionSubmit.submitting.value?'处理中…':'开始救助'}}</button></view></PageState>
    </template>

    <template v-else-if="screen==='taskProgress'">
      <BackButton/><PageState :loading="detailState.loading.value" :error="detailState.error.value" :empty="!task" :has-data="!!task" @retry="loadTask"><TaskHeader :task="task"/><view class="card"><text class="title">历史救助记录</text><view v-if="!records.length" class="subtle">暂无过程记录</view><view v-for="r in records" :key="r.id" class="timeline"><text>{{r.content}}</text><text class="muted">{{r.createdAt}}</text></view><view class="actions"><button v-if="task?.status==='IN_PROGRESS'" class="btn" @click="goto('addRescueRecord',{id:task.id})">追加救助记录</button><button v-if="task?.status==='IN_PROGRESS'" class="btn primary" @click="goto('submitRescueResult',{id:task.id})">提交救助结果</button></view></view></PageState>
    </template>

    <template v-else-if="screen==='addRescueRecord'">
      <BackButton/><view class="title">追加救助记录</view><view class="card"><textarea v-model="recordText" class="input area" maxlength="2000" placeholder="记录到场、转运、就医等过程"/><text v-if="recordError" class="inline-error">{{recordError}}</text><button class="btn primary wide" :disabled="actionSubmit.submitting.value" @click="saveRecord">{{actionSubmit.submitting.value?'保存中…':'保存记录'}}</button></view>
    </template>

    <template v-else-if="screen==='submitRescueResult'">
      <BackButton/><view class="title">提交救助结果 · 核心用例 04</view><view class="tabs"><button :class="['btn',mode==='SUCCESS'?'tab-active':'']" @click="mode='SUCCESS'">救助成功</button><button :class="['btn',mode==='FAILED'?'tab-active':'']" @click="mode='FAILED'">救助失败</button></view>
      <view v-if="mode==='FAILED'" class="card"><textarea v-model="failureReason" class="input area" maxlength="500" placeholder="失败原因必填，1～500"/><text v-if="failureError" class="inline-error">{{failureError}}</text><button class="btn danger wide" :disabled="actionSubmit.submitting.value" @click="submitFailed">{{actionSubmit.submitting.value?'提交中…':'提交救助失败'}}</button></view>
      <view v-else><view class="notice">救助成功时必须至少创建 1 个动物档案；所有动物图片合计最多 9 张。</view><view v-for="(a,i) in successAnimals" :key="i" class="card"><text class="title">动物 #{{i+1}}</text><FormField label="名称" :error="a.errors.name"><input v-model="a.name" class="input" maxlength="100"/></FormField><FormField label="种类" :error="a.errors.species"><input v-model="a.species" class="input" maxlength="50" placeholder="例如猫"/></FormField><picker :range="['公','母','未知']" @change="a.sex=['MALE','FEMALE','UNKNOWN'][$event.detail.value]"><view class="input">{{displayText(a.sex)}}</view></picker><input v-model.number="a.estimatedAgeMonths" class="input" type="number" placeholder="估算年龄（月）"/><input v-model="a.color" class="input" maxlength="100" placeholder="毛色（可空）"/><FormField label="当前健康情况" :error="a.errors.healthCondition"><textarea v-model="a.healthCondition" class="input area" maxlength="1000"/></FormField><textarea v-model="a.initialHealthRecord" class="input area" maxlength="2000" placeholder="初始健康记录（可空）"/><ImagePicker v-model="a.imageTokens"/><button v-if="successAnimals.length>1" class="btn danger" @click="successAnimals.splice(i,1)">删除该动物</button></view><text v-if="successError" class="inline-error">{{successError}}</text><button class="btn" @click="successAnimals.push(newAnimal())">+ 添加动物</button><button class="btn primary wide" :disabled="actionSubmit.submitting.value" @click="submitSuccess">{{actionSubmit.submitting.value?'提交中…':'提交救助成功并建档'}}</button></view>
    </template>
  </MobileShell>
</template>
<script setup>
import {ref,reactive,computed,onMounted} from 'vue'
import MobileShell from '../../components/MobileShell.vue';import PageState from '../../components/PageState.vue';import BackButton from '../../components/BackButton.vue';import StatusBadge from '../../components/StatusBadge.vue';import TaskHeader from '../../components/TaskHeader.vue';import FormField from '../../components/FormField.vue';import ImagePicker from '../../components/ImagePicker.vue';import Icon from '../../components/Icon.vue'
import {clueApi,rescueApi} from '../../api/index.js';import {useAuth} from '../../store/auth.js';import {useNavigation} from '../../composables/navigation.js';import {usePagedList} from '../../composables/pagedList.js';import {useAsyncState,useSubmit} from '../../composables/asyncState.js';import {currentPageParams} from '../../composables/routeParams.js';import {textRule} from '../../composables/validation.js';import {displayText} from '../../utils/displayText.js'
const props=defineProps({screen:{type:String,required:true}});const screen=props.screen;const params=currentPageParams();const role=computed(()=>useAuth().state.user?.roleCode);const {goto,switchMain}=useNavigation();const waiting=usePagedList(q=>clueApi.waiting(q),{pageSize:20});const tasks=usePagedList(q=>rescueApi.mine(q),{pageSize:20});const taskStatusOptions=['','WAITING_START','IN_PROGRESS','SUCCESS','FAILED','CANCELED'];const taskStatus=ref('');const detailState=useAsyncState();const actionSubmit=useSubmit();const task=ref(null),records=ref([]),recordText=ref(''),recordError=ref(''),failureReason=ref(''),failureError=ref(''),mode=ref('SUCCESS'),successError=ref('')
function newAnimal(){return reactive({name:'',species:'CAT',sex:'UNKNOWN',estimatedAgeMonths:null,color:'',healthCondition:'',initialHealthRecord:'',imageTokens:[],errors:{name:'',species:'',healthCondition:''}})}const successAnimals=ref([newAnimal()])
async function reloadTasks(){await tasks.refresh(taskStatus.value?{status:taskStatus.value}:{})}
async function refreshAll(){if(screen!=='tasks'||role.value!=='RESCUER')return;await Promise.all([waiting.refresh(),reloadTasks()])}
async function accept(c){await actionSubmit.submit(async()=>{const r=await rescueApi.accept(c.id);uni.showToast({title:'已接取'});goto('taskWaitingStart',{id:r.taskId})})}
async function loadTask(){await detailState.run(async()=>{task.value=await rescueApi.detail(params.id);records.value=await rescueApi.records(params.id)})}
async function startTask(){await actionSubmit.submit(async()=>{await rescueApi.start(params.id);uni.redirectTo({url:`/pages/taskProgress/index?id=${encodeURIComponent(params.id)}`})})}
async function saveRecord(){recordError.value=textRule(recordText.value,{label:'救助记录',max:2000});if(recordError.value)return;await actionSubmit.submit(async()=>{await rescueApi.addRecord(params.id,{content:recordText.value.trim()});uni.navigateBack()})}
async function submitFailed(){failureError.value=textRule(failureReason.value,{label:'失败原因',max:500});if(failureError.value)return;await actionSubmit.submit(async()=>{await rescueApi.result(params.id,{result:'FAILED',failureReason:failureReason.value.trim()});switchMain('tasks')})}
function validateSuccess(){successError.value='';let valid=successAnimals.value.length>0;let total=0;const seen=new Set();for(const a of successAnimals.value){a.errors.name=textRule(a.name,{label:'名称',max:100});a.errors.species=textRule(a.species,{label:'种类',max:50});a.errors.healthCondition=textRule(a.healthCondition,{label:'健康情况',max:1000});if(Object.values(a.errors).some(Boolean))valid=false;total+=a.imageTokens.length;for(const t of a.imageTokens){if(seen.has(t)){successError.value='同一次救助成功请求中图片标识不能重复';valid=false}seen.add(t)}if(a.estimatedAgeMonths!==null&&a.estimatedAgeMonths!==''&&(Number(a.estimatedAgeMonths)<0||Number(a.estimatedAgeMonths)>65535)){successError.value='估算年龄必须为 0～65535 个月';valid=false}}if(total>9){successError.value='所有动物图片合计最多 9 张';valid=false}return valid}
async function submitSuccess(){if(!validateSuccess())return;await actionSubmit.submit(async()=>{await rescueApi.result(params.id,{result:'SUCCESS',animals:successAnimals.value.map(a=>({name:a.name.trim(),species:a.species.trim(),sex:a.sex,estimatedAgeMonths:a.estimatedAgeMonths===''?null:a.estimatedAgeMonths,color:a.color?.trim()||null,healthCondition:a.healthCondition.trim(),initialHealthRecord:a.initialHealthRecord?.trim()||null,imageTokens:a.imageTokens}))});switchMain('tasks')})}
function onReachBottom(){if(screen==='tasks'){if(waiting.hasMore.value)waiting.loadMore();else tasks.loadMore()}}
async function initialize(){if(screen==='tasks'&&role.value==='RESCUER')await refreshAll();else if(['taskWaitingStart','taskProgress'].includes(screen))await loadTask()}
onMounted(()=>{void initialize().catch(()=>{})})
defineExpose({refresh:refreshAll})
</script>
