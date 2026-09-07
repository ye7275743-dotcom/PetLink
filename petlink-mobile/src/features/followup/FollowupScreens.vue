<template>
  <MobileShell :screen="screen" @reach-bottom="onReachBottom">
    <template v-if="screen==='followupHistory'">
      <BackButton/><view class="title">历史回访</view>
      <PageState :loading="detailState.loading.value" :error="detailState.error.value" :empty="!followups.length" :has-data="!!followups.length" empty-text="暂无回访记录" @retry="loadHistory"><view v-for="f in followups" :key="f.id" class="card"><text class="card-title">回访 #{{f.id}}</text><text>{{f.content||f.healthCondition||'仅上传图片'}}</text><text class="muted">{{f.createdAt}}</text><button class="btn" @click="goto('followupDetail',{id:f.id})">查看详情</button></view></PageState>
    </template>
    <template v-else-if="screen==='followupDetail'">
      <BackButton/><PageState :loading="detailState.loading.value" :error="detailState.error.value" :empty="!detail" :has-data="!!detail" @retry="loadDetail"><view v-if="detail" class="card"><text class="card-title">回访 #{{detail.id}}</text><view class="kv"><b>近况</b><text>{{detail.content||'—'}}</text></view><view class="kv"><b>健康</b><text>{{detail.healthCondition||'—'}}</text></view><view class="photo-grid"><AuthImage v-for="im in detail.images||[]" :key="im.id" class="photo-img" :src="im.url" mode="aspectFill"/></view></view></PageState>
    </template>
    <template v-else-if="screen==='rescuerFollowups'">
      <BackButton/><view class="title">负责动物回访 · 救助人员只读</view><PageState :loading="rescuer.loading.value" :error="rescuer.error.value" :empty="!rescuer.records.value.length" :has-data="!!rescuer.records.value.length" :loading-more="rescuer.loadingMore.value" :has-more="rescuer.hasMore.value" @retry="rescuer.retry"><view v-for="f in rescuer.records.value" :key="f.id" class="card"><text class="card-title">回访 #{{f.id}}</text><text>{{f.content||f.healthCondition||'仅上传图片'}}</text><button class="btn" @click="goto('followupDetail',{id:f.id})">查看回访详情</button></view></PageState>
    </template>
    <template v-else-if="screen==='followup'">
      <BackButton/><view class="title">提交回访</view><view class="card"><FormField label="近况" :error="errors.content"><textarea v-model="form.content" class="input area" maxlength="2000" placeholder="近况（可空）"/></FormField><FormField label="健康情况" :error="errors.healthCondition"><textarea v-model="form.healthCondition" class="input area" maxlength="1000" placeholder="健康情况（可空）"/></FormField><ImagePicker v-model="tokens"/><FieldError :message="errors.atLeastOne"/><view class="notice">请至少填写一项近况、健康情况或上传一张生活照片。</view><button class="btn primary wide" :disabled="submitState.submitting.value" @click="submitFollowup">{{submitState.submitting.value?'提交中…':'提交回访'}}</button></view>
    </template>
  </MobileShell>
</template>
<script setup>
import {ref,reactive,onMounted} from 'vue'
import MobileShell from '../../components/MobileShell.vue';import PageState from '../../components/PageState.vue';import BackButton from '../../components/BackButton.vue';import AuthImage from '../../components/AuthImage.vue';import ImagePicker from '../../components/ImagePicker.vue';import FormField from '../../components/FormField.vue';import FieldError from '../../components/FieldError.vue'
import {followupApi} from '../../api/index.js';import {uuid} from '../../utils/uuid.js';import {useNavigation} from '../../composables/navigation.js';import {usePagedList} from '../../composables/pagedList.js';import {useAsyncState,useSubmit} from '../../composables/asyncState.js';import {currentPageParams} from '../../composables/routeParams.js';import {textRule} from '../../composables/validation.js';import {normalizeFollowupPayload} from '../../composables/idempotency.js'
const props=defineProps({screen:{type:String,required:true}});const screen=props.screen;const params=currentPageParams();const {goto}=useNavigation();const detailState=useAsyncState(),submitState=useSubmit();const followups=ref([]),detail=ref(null);const rescuer=usePagedList(q=>followupApi.rescuer(q),{pageSize:20});const form=reactive({content:'',healthCondition:'',idempotencyKey:uuid()}),tokens=ref([]),errors=reactive({content:'',healthCondition:'',atLeastOne:''})
async function loadHistory(){await detailState.run(async()=>{followups.value=await followupApi.list(params.recordId)})}async function loadDetail(){await detailState.run(async()=>{detail.value=await followupApi.detail(params.id)})}
function validate(){errors.content=textRule(form.content,{label:'近况',required:false,max:2000});errors.healthCondition=textRule(form.healthCondition,{label:'健康情况',required:false,max:1000});errors.atLeastOne=(form.content.trim()||form.healthCondition.trim()||tokens.value.length)?'':'文字、健康情况、图片至少填写一项';return !Object.values(errors).some(Boolean)}
async function submitFollowup(){if(!validate())return;await submitState.submit(async()=>{await followupApi.create(params.recordId,normalizeFollowupPayload({content:form.content,healthCondition:form.healthCondition,imageTokens:tokens.value,idempotencyKey:form.idempotencyKey}));uni.showToast({title:'回访已提交'});uni.navigateBack()})}
function onReachBottom(){if(screen==='rescuerFollowups')rescuer.loadMore()}
async function initialize(){if(screen==='followupHistory')await loadHistory();else if(screen==='followupDetail')await loadDetail();else if(screen==='rescuerFollowups')await rescuer.refresh()}
onMounted(()=>{void initialize().catch(()=>{})})
</script>
