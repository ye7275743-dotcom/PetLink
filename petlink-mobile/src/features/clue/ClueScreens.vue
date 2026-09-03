<template>
  <MobileShell :screen="screen" @reach-bottom="onReachBottom">
    <template v-if="screen==='clue'">
      <view class="tabs"><button :class="['btn',tab==='publish'?'tab-active':'']" @click="tab='publish'">发布线索</button><button :class="['btn',tab==='mine'?'tab-active':'']" @click="openMine">我的线索</button></view>
      <view v-if="tab==='publish'" class="card">
        <FormField label="发现地点" :error="errors.location"><input v-model="form.location" class="input" placeholder="请输入发现地点"/></FormField>
        <FormField label="发现时间" :error="errors.foundTime"><input v-model="form.foundTime" class="input" placeholder="2026-09-01T08:30:00+08:00"/></FormField>
        <FormField label="动物描述" :error="errors.animalDescription"><textarea v-model="form.animalDescription" class="input area" placeholder="描述动物体貌、受伤情况等"/></FormField>
        <FormField label="现场情况" :error="errors.sceneDescription"><textarea v-model="form.sceneDescription" class="input area" placeholder="可选，最多 1000 字"/></FormField>
        <FormField label="联系方式" :error="errors.contact"><input v-model="form.contact" class="input"/></FormField>
        <ImagePicker v-model="tokens" :max="9"/><FieldError :message="errors.images"/>
        <button class="btn primary wide" :disabled="publishSubmit.submitting.value" @click="publish">{{publishSubmit.submitting.value?'提交中…':'提交救助线索'}}</button>
      </view>
      <template v-else>
        <view class="filter-bar"><picker :range="statusOptions.map(x=>x?displayText(x):'全部')" @change="mineStatus=statusOptions[$event.detail.value];reloadMine()"><view class="filter-item">状态：{{mineStatus?displayText(mineStatus):'全部'}}</view></picker><button class="btn" @click="reloadMine">刷新</button></view>
        <PageState :loading="mine.loading.value" :error="mine.error.value" :empty="!mine.records.value.length" :has-data="!!mine.records.value.length" :loading-more="mine.loadingMore.value" :has-more="mine.hasMore.value" @retry="mine.retry">
          <view v-for="c in mine.records.value" :key="c.id" class="card"><text class="card-title">线索 #{{c.id}}</text><text class="muted">{{c.location}} · {{c.foundTime}}</text><StatusBadge :value="c.status"/><view class="actions"><button class="btn" @click="goto('waitingClue',{id:c.id})">详情</button><button v-if="c.status==='PENDING_REVIEW'" class="btn" @click="goto('clueEdit',{id:c.id})">修改</button><button v-if="c.status==='PENDING_REVIEW'" class="btn danger" :disabled="actionSubmit.submitting.value" @click="withdraw(c)">撤回</button></view></view>
        </PageState>
      </template>
    </template>

    <template v-else-if="screen==='clueEdit'">
      <BackButton/><view class="title">修改待审核线索 #{{params.id}}</view>
      <PageState :loading="detailState.loading.value" :error="detailState.error.value" :empty="!detail" :has-data="!!detail" @retry="loadEdit">
        <view class="card">
          <FormField label="发现地点" :error="editErrors.location"><input v-model="edit.location" class="input"/></FormField>
          <FormField label="发现时间" :error="editErrors.foundTime"><input v-model="edit.foundTime" class="input"/></FormField>
          <FormField label="动物描述" :error="editErrors.animalDescription"><textarea v-model="edit.animalDescription" class="input area"/></FormField>
          <FormField label="现场情况" :error="editErrors.sceneDescription"><textarea v-model="edit.sceneDescription" class="input area"/></FormField>
          <FormField label="联系方式" :error="editErrors.contact"><input v-model="edit.contact" class="input"/></FormField>
          <button class="btn primary wide" :disabled="editSubmit.submitting.value" @click="saveEdit">{{editSubmit.submitting.value?'保存中…':'保存资料'}}</button>
        </view>
        <view class="card"><text class="title">现场图片</text><view class="photo-grid"><view v-for="im in detail?.images||[]" :key="im.id" class="photo"><AuthImage :src="im.url" mode="aspectFill"/><button class="mini danger" aria-label="删除图片" :disabled="imageSubmit.submitting.value" @click="deleteImage(im)"><Icon name="trash" :size="18" /></button></view></view><ImagePicker v-model="appendTokens"/><button class="btn" :disabled="!appendTokens.length||imageSubmit.submitting.value" @click="appendImages"><Icon name="plus" :size="18" />{{imageSubmit.submitting.value?'处理中…':'追加图片'}}</button></view>
      </PageState>
    </template>

    <template v-else-if="screen==='waitingClue'">
      <BackButton/>
      <PageState :loading="detailState.loading.value" :error="detailState.error.value" :empty="!detail" :has-data="!!detail" @retry="loadDetail">
        <view v-if="detail" class="card"><StatusBadge :value="detail.status"/><text class="card-title">线索 #{{detail.id}}</text><text class="muted">{{detail.foundTime}}</text><view class="kv"><b>地点</b><text>{{detail.location}}</text></view><view class="kv"><b>描述</b><text>{{detail.animalDescription}}</text></view><view class="kv"><b>现场</b><text>{{detail.sceneDescription||'—'}}</text></view><view class="kv"><b>联系方式</b><text>{{detail.contact}}</text></view><view v-if="detail.images?.length" class="photo-grid"><AuthImage v-for="im in detail.images" :key="im.id" class="photo-img" :src="im.url" mode="aspectFill"/></view><button v-if="role==='RESCUER'&&detail.status==='WAITING_ACCEPT'" class="btn primary wide" :disabled="actionSubmit.submitting.value" @click="accept(detail)">{{actionSubmit.submitting.value?'接取中…':'接取任务'}}</button></view>
      </PageState>
    </template>
  </MobileShell>
</template>
<script setup>
import {ref,reactive,computed,onMounted} from 'vue'
import MobileShell from '../../components/MobileShell.vue';import PageState from '../../components/PageState.vue';import BackButton from '../../components/BackButton.vue';import StatusBadge from '../../components/StatusBadge.vue';import FormField from '../../components/FormField.vue';import FieldError from '../../components/FieldError.vue';import ImagePicker from '../../components/ImagePicker.vue';import AuthImage from '../../components/AuthImage.vue';import Icon from '../../components/Icon.vue'
import {clueApi,rescueApi} from '../../api/index.js';import {useAuth} from '../../store/auth.js';import {useNavigation} from '../../composables/navigation.js';import {usePagedList} from '../../composables/pagedList.js';import {useAsyncState,useSubmit} from '../../composables/asyncState.js';import {currentPageParams} from '../../composables/routeParams.js';import {textRule,foundTimeRule} from '../../composables/validation.js';import {displayText} from '../../utils/displayText.js'
const props=defineProps({screen:{type:String,required:true}});const screen=props.screen;const params=currentPageParams();const auth=useAuth();const role=computed(()=>auth.state.user?.roleCode);const {goto}=useNavigation();const tab=ref('publish'),mineStatus=ref('');const statusOptions=['','PENDING_REVIEW','REJECTED','WITHDRAWN','WAITING_ACCEPT','CONVERTED','CLOSED'];const mine=usePagedList(q=>clueApi.mine(q),{pageSize:20});const detailState=useAsyncState();const publishSubmit=useSubmit(),editSubmit=useSubmit(),imageSubmit=useSubmit(),actionSubmit=useSubmit();const form=reactive({location:'',foundTime:'',animalDescription:'',sceneDescription:'',contact:''}),tokens=ref([]);const errors=reactive({location:'',foundTime:'',animalDescription:'',sceneDescription:'',contact:'',images:''});const detail=ref(null),edit=reactive({location:'',foundTime:'',animalDescription:'',sceneDescription:'',contact:''}),editErrors=reactive({location:'',foundTime:'',animalDescription:'',sceneDescription:'',contact:''}),appendTokens=ref([])
function validate(target,errs,requireImages=false){errs.location=textRule(target.location,{label:'发现地点',max:255});errs.foundTime=foundTimeRule(target.foundTime);errs.animalDescription=textRule(target.animalDescription,{label:'动物描述',max:1000});errs.sceneDescription=textRule(target.sceneDescription,{label:'现场情况',required:false,max:1000});errs.contact=textRule(target.contact,{label:'联系方式',max:100});if(requireImages)errs.images=tokens.value.length?'':'至少上传 1 张现场图片';return !Object.values(errs).some(Boolean)}
async function openMine(){tab.value='mine';if(!mine.records.value.length&&!mine.loading.value)await reloadMine()}
async function reloadMine(){await mine.refresh(mineStatus.value?{status:mineStatus.value}:{})}
async function publish(){if(!validate(form,errors,true))return;await publishSubmit.submit(async()=>{const key=await clueApi.key();await clueApi.create({location:form.location.trim(),foundTime:form.foundTime,animalDescription:form.animalDescription.trim(),sceneDescription:form.sceneDescription.trim()||null,contact:form.contact.trim(),imageTokens:tokens.value},key.idempotencyKey);Object.assign(form,{location:'',foundTime:'',animalDescription:'',sceneDescription:'',contact:''});tokens.value=[];uni.showToast({title:'线索已提交'});tab.value='mine';await reloadMine()})}
async function withdraw(c){await actionSubmit.submit(async()=>{await clueApi.withdraw(c.id);await reloadMine()})}
async function loadDetail(){await detailState.run(async()=>{detail.value=await clueApi.detail(params.id)})}
async function loadEdit(){await detailState.run(async()=>{detail.value=await clueApi.detail(params.id);Object.assign(edit,{location:detail.value.location,foundTime:detail.value.foundTime,animalDescription:detail.value.animalDescription,sceneDescription:detail.value.sceneDescription||'',contact:detail.value.contact})})}
async function saveEdit(){if(!validate(edit,editErrors,false))return;await editSubmit.submit(async()=>{detail.value=await clueApi.patch(params.id,{location:edit.location.trim(),foundTime:edit.foundTime,animalDescription:edit.animalDescription.trim(),sceneDescription:edit.sceneDescription.trim()||null,contact:edit.contact.trim()});Object.assign(edit,{location:detail.value.location,foundTime:detail.value.foundTime,animalDescription:detail.value.animalDescription,sceneDescription:detail.value.sceneDescription||'',contact:detail.value.contact});uni.showToast({title:'已保存'})})}
async function appendImages(){if(!appendTokens.value.length)return;await imageSubmit.submit(async()=>{detail.value=await clueApi.addImages(params.id,{imageTokens:appendTokens.value});appendTokens.value=[]})}
async function deleteImage(im){if((detail.value?.images||[]).length<=1){uni.showToast({title:'至少保留 1 张图片',icon:'none'});return}await imageSubmit.submit(async()=>{detail.value=await clueApi.deleteImage(params.id,im.id)})}
async function accept(c){await actionSubmit.submit(async()=>{const r=await rescueApi.accept(c.id);uni.showToast({title:'已接取'});goto('taskWaitingStart',{id:r.taskId})})}
function onReachBottom(){if(screen==='clue'&&tab.value==='mine')mine.loadMore()}
async function initialize(){if(screen==='clueEdit')await loadEdit();else if(screen==='waitingClue')await loadDetail()}
onMounted(()=>{void initialize().catch(()=>{})})
</script>
