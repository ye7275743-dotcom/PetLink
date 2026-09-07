<template>
  <MobileShell :screen="screen" @reach-bottom="onReachBottom">
    <template v-if="['applicationDetail','applicationRejected','applicationInvalidated'].includes(screen)">
      <BackButton/><view class="title">领养申请详情</view>
      <PageState :loading="detailState.loading.value" :error="detailState.error.value" :empty="!application" :has-data="!!application" @retry="loadApplication">
        <view v-if="application" class="card"><text class="card-title">申请 #{{application.id}}</text><StatusBadge :value="application.status"/><view class="kv"><b>领养理由</b><text>{{application.adoptionReason}}</text></view><view class="kv"><b>住房条件</b><text>{{application.housingCondition}}</text></view><view class="kv"><b>家庭成员</b><text>{{application.familyMembers}}</text></view><view class="kv"><b>养宠经验</b><text>{{application.petExperience}}</text></view><view class="kv"><b>联系方式</b><text>{{application.contact}}</text></view><view v-if="application.rejectReason" class="dangerbox">{{application.rejectReason}}</view><view v-if="application.status==='INVALIDATED'" class="notice">同动物其他申请已获批准，本申请自动失效。</view><button v-if="application.status==='PENDING'" class="btn danger wide" :disabled="actionSubmit.submitting.value" @click="withdrawApplication">{{actionSubmit.submitting.value?'处理中…':'撤回申请'}}</button></view>
      </PageState>
    </template>

    <template v-else-if="screen==='adoptionOverview'">
      <BackButton/><view class="title">负责动物领养概览 · 只读</view>
      <PageState :loading="detailState.loading.value" :error="detailState.error.value" :empty="!overview" :has-data="!!overview" @retry="loadOverview"><view class="card"><view class="kv"><b>动物</b><text>#{{params.id}}</text></view><view class="kv"><b>状态</b><StatusBadge :value="overview?.animalStatus"/></view><view class="kv"><b>待审核申请</b><text>{{overview?.pendingApplicationCount??0}}</text></view><view class="kv"><b>批准申请</b><text>{{overview?.approvedApplicationId||'—'}}</text></view><view class="kv"><b>领养记录</b><text>{{overview?.adoptionRecordId||'—'}}</text></view><view class="kv"><b>领养时间</b><text>{{overview?.adoptedAt||'—'}}</text></view><view class="notice">不展示其他申请人的理由、住房、家庭、养宠经验和联系方式。</view></view></PageState>
    </template>

    <template v-else-if="screen==='apply'">
      <BackButton/><view class="title">领养申请</view><view class="card">
        <FormField label="领养理由" :error="errors.adoptionReason"><textarea v-model="form.adoptionReason" class="input area" maxlength="1000"/></FormField>
        <FormField label="住房条件" :error="errors.housingCondition"><textarea v-model="form.housingCondition" class="input area" maxlength="1000"/></FormField>
        <FormField label="家庭成员" :error="errors.familyMembers"><textarea v-model="form.familyMembers" class="input area" maxlength="1000"/></FormField>
        <FormField label="养宠经验" :error="errors.petExperience"><textarea v-model="form.petExperience" class="input area" maxlength="1000"/></FormField>
        <FormField label="联系方式" :error="errors.contact"><input v-model="form.contact" class="input" maxlength="100"/></FormField>
        <button class="btn primary wide" :disabled="actionSubmit.submitting.value" @click="submitApplication">{{actionSubmit.submitting.value?'提交中…':'提交申请'}}</button>
      </view>
    </template>

    <template v-else-if="screen==='records'">
      <view class="title">我的领养与回访</view><view class="title small">领养记录</view>
      <PageState :loading="recordsList.loading.value" :error="recordsList.error.value" :empty="!recordsList.records.value.length" :has-data="!!recordsList.records.value.length" :loading-more="recordsList.loadingMore.value" :has-more="recordsList.hasMore.value" empty-text="暂无领养记录" @retry="recordsList.retry">
        <view v-for="r in recordsList.records.value" :key="r.id" class="card archive"><AuthImage class="thumb" :src="r.animal?.coverImageUrl" fallback="/static/assets/little-orange-dog.png" mode="aspectFill"/><view class="grow"><text class="card-title">{{r.animal?.name||('动物 #'+r.animalId)}}</text><text class="muted">领养记录 #{{r.id}}</text><view class="actions"><button class="btn" @click="goto('followupHistory',{recordId:r.id})">历史回访</button><button class="btn primary" @click="goto('followup',{recordId:r.id})">提交回访</button></view></view></view>
      </PageState>
      <view class="section-divider"/><view class="title small">我的申请</view>
      <view class="filter-bar"><picker :range="applicationStatuses.map(x=>x?displayText(x):'全部')" @change="appStatus=applicationStatuses[$event.detail.value];reloadApplications()"><view class="filter-item">状态：{{appStatus?displayText(appStatus):'全部'}}</view></picker><button class="btn" @click="reloadApplications">刷新</button></view>
      <PageState :loading="applications.loading.value" :error="applications.error.value" :empty="!applications.records.value.length" :has-data="!!applications.records.value.length" :loading-more="applications.loadingMore.value" :has-more="applications.hasMore.value" empty-text="暂无领养申请" @retry="applications.retry">
        <view v-for="a in applications.records.value" :key="a.id" class="card"><text class="card-title">申请 #{{a.id}}</text><StatusBadge :value="a.status"/><view class="actions"><button class="btn" @click="goto(a.status==='REJECTED'?'applicationRejected':a.status==='INVALIDATED'?'applicationInvalidated':'applicationDetail',{id:a.id})">详情</button><button v-if="a.status==='PENDING'" class="btn danger" :disabled="actionSubmit.submitting.value" @click="withdrawById(a.id)">撤回申请</button></view></view>
      </PageState>
    </template>
  </MobileShell>
</template>
<script setup>
import {ref,reactive,onMounted} from 'vue'
import MobileShell from '../../components/MobileShell.vue';import PageState from '../../components/PageState.vue';import BackButton from '../../components/BackButton.vue';import StatusBadge from '../../components/StatusBadge.vue';import AuthImage from '../../components/AuthImage.vue';import FormField from '../../components/FormField.vue'
import {adoptionApi} from '../../api/index.js';import {useNavigation} from '../../composables/navigation.js';import {usePagedList} from '../../composables/pagedList.js';import {useAsyncState,useSubmit} from '../../composables/asyncState.js';import {currentPageParams} from '../../composables/routeParams.js';import {textRule} from '../../composables/validation.js';import {displayText} from '../../utils/displayText.js'
const props=defineProps({screen:{type:String,required:true}});const screen=props.screen;const params=currentPageParams();const {goto,switchMain}=useNavigation();const detailState=useAsyncState(),actionSubmit=useSubmit();const application=ref(null),overview=ref(null);const form=reactive({adoptionReason:'',housingCondition:'',familyMembers:'',petExperience:'',contact:''}),errors=reactive({adoptionReason:'',housingCondition:'',familyMembers:'',petExperience:'',contact:''});const recordsList=usePagedList(q=>adoptionApi.records(q),{pageSize:20});const applications=usePagedList(q=>adoptionApi.mine(q),{pageSize:20});const applicationStatuses=['','PENDING','APPROVED','REJECTED','WITHDRAWN','INVALIDATED'],appStatus=ref('')
function validate(){for(const k of ['adoptionReason','housingCondition','familyMembers','petExperience'])errors[k]=textRule(form[k],{label:{adoptionReason:'领养理由',housingCondition:'住房条件',familyMembers:'家庭成员',petExperience:'养宠经验'}[k],max:1000});errors.contact=textRule(form.contact,{label:'联系方式',max:100});return !Object.values(errors).some(Boolean)}
async function submitApplication(){if(!validate())return;await actionSubmit.submit(async()=>{await adoptionApi.apply(params.animalId,{adoptionReason:form.adoptionReason.trim(),housingCondition:form.housingCondition.trim(),familyMembers:form.familyMembers.trim(),petExperience:form.petExperience.trim(),contact:form.contact.trim()});uni.showToast({title:'申请已提交'});switchMain('records')})}
async function loadApplication(){await detailState.run(async()=>{application.value=await adoptionApi.application(params.id)})}
async function loadOverview(){await detailState.run(async()=>{overview.value=await adoptionApi.overview(params.id)})}
async function withdrawApplication(){await actionSubmit.submit(async()=>{await adoptionApi.withdraw(application.value.id);await loadApplication()})}
async function withdrawById(id){await actionSubmit.submit(async()=>{await adoptionApi.withdraw(id);await reloadApplications()})}
async function reloadApplications(){await applications.refresh(appStatus.value?{status:appStatus.value}:{})}
function onReachBottom(){if(screen==='records'){if(recordsList.hasMore.value)recordsList.loadMore();else applications.loadMore()}}
async function initialize(){if(['applicationDetail','applicationRejected','applicationInvalidated'].includes(screen))await loadApplication();else if(screen==='adoptionOverview')await loadOverview();else if(screen==='records')await Promise.all([recordsList.refresh(),reloadApplications()])}
onMounted(()=>{void initialize().catch(()=>{})})
</script>
