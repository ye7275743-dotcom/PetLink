<template>
  <MobileShell :screen="screen" @reach-bottom="onReachBottom">
    <template v-if="screen==='home'">
      <view class="hero">
        <view class="hero-copy"><text class="eyebrow light">上海救助手记</text><text class="hero-title">发现需要帮助的它，\n也找到愿意接住它的人</text><text class="hero-sub">每一次救助，都是改变命运的开始。</text><button class="btn primary hero-btn" @click="requireLogin('clue')"><Icon name="send" :size="20" /><text>发布救助线索</text></button></view>
        <view class="hero-visual" aria-hidden="true"><image class="hero-image" src="/static/assets/petlink-rescue-hero-v1.jpg" mode="aspectFill" /></view>
      </view>
      <view class="title">等待一个家的它们</view>
      <view class="filter-bar">
        <picker :range="speciesOptions.map(x=>x?displayText(x):'全部')" @change="species=speciesOptions[$event.detail.value];reloadHome()"><view class="filter-item">物种：{{species?displayText(species):'全部'}}</view></picker>
        <picker :range="sexOptions.map(x=>x?displayText(x):'全部')" @change="sex=sexOptions[$event.detail.value];reloadHome()"><view class="filter-item">性别：{{sex?displayText(sex):'全部'}}</view></picker>
      </view>
      <PageState :loading="home.loading.value" :error="home.error.value" :empty="!home.records.value.length" :has-data="!!home.records.value.length" :loading-more="home.loadingMore.value" :has-more="home.hasMore.value" empty-text="暂无符合条件的可领养动物" @retry="home.retry">
        <view v-for="a in home.records.value" :key="a.id" class="card archive">
          <AuthImage class="thumb" :src="a.coverImageUrl" fallback="/static/assets/pudding-cat-hero.png" mode="aspectFill"/>
          <view class="grow"><text class="card-title">{{a.name}}</text><text class="muted">{{displayText(a.species)}} · {{displayText(a.sex)}} · {{age(a.estimatedAgeMonths)}}</text><view class="health"><Icon name="heart" :size="18" /><text>{{a.healthCondition}}</text></view><view class="actions"><button class="btn" @click="goto('animal',{id:a.id})"><Icon name="eye" :size="18" /><text>查看档案</text></button><button v-if="logged" class="btn primary" :disabled="favoriteBusy===a.id" @click="favorite(a)"><Icon name="heart" :size="18" />{{favoriteBusy===a.id?'处理中…':'收藏'}}</button></view></view>
        </view>
      </PageState>
      <view class="guide"><text class="eyebrow">救助指南</text><text class="card-title">第一次参与救助？</text><text class="muted">安全接近、现场记录、联系救助站，一步步陪你完成。</text></view>
    </template>

    <template v-else-if="screen==='animal'">
      <BackButton/>
      <PageState :loading="detailState.loading.value" :error="detailState.error.value" :empty="!animal" :has-data="!!animal" @retry="loadAnimal">
        <view v-if="animal" class="detail-hero"><AuthImage :src="animal.images?.[0]?.url||animal.coverImageUrl" fallback="/static/assets/pudding-cat-hero.png" mode="aspectFill"/><view class="stamp">{{displayText(animal.status)}}</view></view>
        <view v-if="animal" class="card"><text class="card-title big">{{animal.name}}</text><text class="muted">{{displayText(animal.species)}} · {{displayText(animal.sex)}} · {{age(animal.estimatedAgeMonths)}}</text><view class="health"><Icon name="heart" :size="18" /><text>{{animal.healthCondition}}</text></view><view class="kv"><b>毛色</b><text>{{animal.color||'—'}}</text></view><view class="kv"><b>状态</b><StatusBadge :value="animal.status"/></view><view class="actions"><button v-if="logged" class="btn" @click="favorite(animal)"><Icon name="heart" :size="18" />收藏</button><button v-if="animal.status==='AVAILABLE'" class="btn primary" @click="requireLogin('apply',{animalId:animal.id})"><Icon name="send" :size="18" />申请领养</button></view></view>
        <view class="card"><text class="title">健康记录</text><view v-if="!health.length" class="subtle">暂无公开健康记录</view><view v-for="h in health" :key="h.id" class="timeline"><text>{{h.content}}</text><text class="muted">{{h.createdAt}}</text></view></view>
      </PageState>
    </template>

    <template v-else-if="screen==='responsibleAnimals'">
      <BackButton/><view class="title">我负责的动物 · 第四模块</view>
      <PageState :loading="responsible.loading.value" :error="responsible.error.value" :empty="!responsible.records.value.length" :has-data="!!responsible.records.value.length" :loading-more="responsible.loadingMore.value" :has-more="responsible.hasMore.value" @retry="responsible.retry">
        <view v-for="a in responsible.records.value" :key="a.id" class="card archive"><AuthImage class="thumb" :src="a.coverImageUrl" fallback="/static/assets/pudding-cat-hero.png" mode="aspectFill"/><view class="grow"><text class="card-title">{{a.name}}</text><text class="muted">来源任务 #{{a.rescueTaskId||'—'}}</text><StatusBadge :value="a.status"/><view class="actions"><button class="btn" @click="goto('animalManage',{id:a.id})">管理档案</button><button class="btn" @click="goto('adoptionOverview',{id:a.id})">领养情况</button></view></view></view>
      </PageState>
    </template>

    <template v-else-if="screen==='animalManage'">
      <BackButton/>
      <PageState :loading="detailState.loading.value" :error="detailState.error.value" :empty="!animal" :has-data="!!animal" @retry="loadAnimalManage">
        <view v-if="animal" class="title">动物档案管理 · {{animal.name}} #{{animal.id}}</view>
        <view v-if="animal" class="card">
          <FormField label="名称" :error="animalErrors.name"><input v-model="animalEdit.name" class="input"/></FormField>
          <FormField label="种类" :error="animalErrors.species"><input v-model="animalEdit.species" class="input"/></FormField>
          <picker :range="['公','母','未知']" @change="animalEdit.sex=['MALE','FEMALE','UNKNOWN'][$event.detail.value]"><view class="input">{{displayText(animalEdit.sex)}}</view></picker>
          <input v-model.number="animalEdit.estimatedAgeMonths" class="input" type="number" placeholder="估算年龄（月）"/>
          <input v-model="animalEdit.color" class="input" placeholder="毛色"/>
          <FormField label="健康摘要" :error="animalErrors.healthCondition"><textarea v-model="animalEdit.healthCondition" class="input area"/></FormField>
          <button class="btn primary wide" :disabled="manageSubmit.submitting.value" @click="saveAnimal">{{manageSubmit.submitting.value?'保存中…':'保存基本资料'}}</button>
        </view>
        <view class="card"><text class="title">动物图片</text><ImagePicker v-model="animalTokens"/><button class="btn" :disabled="!animalTokens.length||imageSubmit.submitting.value" @click="addAnimalImages"><Icon name="plus" :size="18" />{{imageSubmit.submitting.value?'处理中…':'追加图片'}}</button><view class="photo-grid"><view v-for="im in animal?.images||[]" :key="im.id" class="photo"><AuthImage class="photo-img" :src="im.url" mode="aspectFill"/><button class="mini danger" :disabled="imageSubmit.submitting.value" aria-label="删除图片" @click="deleteAnimalImage(im)"><Icon name="trash" :size="18" /></button></view></view></view>
        <view class="card"><text class="title">健康记录</text><view v-for="h in health" :key="h.id" class="timeline">{{h.content}}<text class="muted">{{h.createdAt}}</text></view><textarea v-model="healthText" class="input area" placeholder="追加健康记录，最多 2000 字"/><text v-if="healthError" class="inline-error">{{healthError}}</text><button class="btn" :disabled="healthSubmit.submitting.value" @click="addHealth">{{healthSubmit.submitting.value?'提交中…':'追加健康记录'}}</button></view>
        <view v-if="animal" class="card"><text class="title">状态操作</text><view class="notice">“已领养”状态只能由领养批准事务产生。</view><button v-if="animal.status==='TREATING'" class="btn" :disabled="stateSubmit.submitting.value" @click="animalState('TO_OBSERVING')">进入观察</button><button v-if="animal.status==='OBSERVING'" class="btn primary" :disabled="stateSubmit.submitting.value" @click="animalState('OPEN_ADOPTION')">开放领养</button><button v-if="animal.status==='AVAILABLE'" class="btn danger" :disabled="stateSubmit.submitting.value" @click="suspendAnimal">暂停领养</button><button v-if="animal.status==='SUSPENDED'" class="btn primary" :disabled="stateSubmit.submitting.value" @click="animalState('RESUME_ADOPTION')">恢复领养</button></view>
      </PageState>
    </template>
  </MobileShell>
</template>
<script setup>
import {ref,reactive,onMounted} from 'vue'
import MobileShell from '../../components/MobileShell.vue';import PageState from '../../components/PageState.vue';import AuthImage from '../../components/AuthImage.vue';import BackButton from '../../components/BackButton.vue';import StatusBadge from '../../components/StatusBadge.vue';import FormField from '../../components/FormField.vue';import ImagePicker from '../../components/ImagePicker.vue';import Icon from '../../components/Icon.vue'
import {animalApi,contentApi} from '../../api/index.js';import {useNavigation} from '../../composables/navigation.js';import {usePagedList} from '../../composables/pagedList.js';import {useAsyncState,useSubmit} from '../../composables/asyncState.js';import {currentPageParams} from '../../composables/routeParams.js';import {textRule} from '../../composables/validation.js';import {displayText} from '../../utils/displayText.js'
const props=defineProps({screen:{type:String,required:true}});const screen=props.screen;const params=currentPageParams();const {goto,requireLogin,logged}=useNavigation();const speciesOptions=['','CAT','DOG','OTHER'];const sexOptions=['','MALE','FEMALE','UNKNOWN'];const species=ref(''),sex=ref(''),favoriteBusy=ref(null)
const home=usePagedList(q=>animalApi.list(q),{pageSize:20});const responsible=usePagedList(q=>animalApi.responsible(q),{pageSize:20});const detailState=useAsyncState();const animal=ref(null),health=ref([]);const animalEdit=reactive({name:'',species:'',sex:'UNKNOWN',estimatedAgeMonths:null,color:'',healthCondition:''});const animalTokens=ref([]),healthText=ref(''),healthError=ref('');const manageSubmit=useSubmit(),imageSubmit=useSubmit(),healthSubmit=useSubmit(),stateSubmit=useSubmit();const animalErrors=reactive({name:'',species:'',healthCondition:''})
function age(m){return m==null?'年龄未知':m<12?`${m}个月`:`${Math.floor(m/12)}岁${m%12?m%12+'个月':''}`}
async function reloadHome(){await home.refresh({...(species.value?{species:species.value}:{}),...(sex.value?{sex:sex.value}:{})})}
async function loadAnimal(){await detailState.run(async()=>{animal.value=await animalApi.detail(params.id);health.value=await animalApi.health(params.id)})}
async function loadAnimalManage(){await detailState.run(async()=>{animal.value=await animalApi.detail(params.id);Object.assign(animalEdit,animal.value);health.value=await animalApi.health(params.id)})}
async function favorite(a){favoriteBusy.value=a.id;try{await contentApi.favorite(a.id);uni.showToast({title:'已收藏'})}finally{favoriteBusy.value=null}}
function validateAnimal(){animalErrors.name=textRule(animalEdit.name,{label:'名称',max:100});animalErrors.species=textRule(animalEdit.species,{label:'种类',max:50});animalErrors.healthCondition=textRule(animalEdit.healthCondition,{label:'健康摘要',max:1000});return !Object.values(animalErrors).some(Boolean)}
async function saveAnimal(){if(!validateAnimal())return;await manageSubmit.submit(async()=>{animal.value=await animalApi.patch(params.id,{name:animalEdit.name.trim(),species:animalEdit.species.trim(),sex:animalEdit.sex,estimatedAgeMonths:animalEdit.estimatedAgeMonths??null,color:animalEdit.color?.trim()||null,healthCondition:animalEdit.healthCondition.trim(),version:animal.value.version});Object.assign(animalEdit,animal.value);uni.showToast({title:'已保存'})})}
async function addAnimalImages(){if(!animalTokens.value.length)return;await imageSubmit.submit(async()=>{animal.value=await animalApi.addImages(params.id,{imageTokens:animalTokens.value});animalTokens.value=[]})}
async function deleteAnimalImage(im){await imageSubmit.submit(async()=>{animal.value=await animalApi.deleteImage(params.id,im.id)})}
async function addHealth(){healthError.value=textRule(healthText.value,{label:'健康记录',max:2000});if(healthError.value)return;await healthSubmit.submit(async()=>{await animalApi.addHealth(params.id,{content:healthText.value.trim()});healthText.value='';health.value=await animalApi.health(params.id)})}
async function animalState(action,extra={}){await stateSubmit.submit(async()=>{await animalApi.status(params.id,{action,version:animal.value.version,...extra});await loadAnimalManage()})}
function suspendAnimal(){uni.showModal({title:'暂停领养',editable:true,placeholderText:'请输入暂停原因',success:r=>{if(r.confirm){const reason=String(r.content||'').trim();if(!reason||reason.length>500){uni.showToast({title:'暂停原因需为 1～500 字',icon:'none'});return}animalState('SUSPEND_ADOPTION',{suspendReason:reason})}}})}
function onReachBottom(){if(screen==='home')home.loadMore();else if(screen==='responsibleAnimals')responsible.loadMore()}
async function initialize(){if(screen==='home')await reloadHome();else if(screen==='animal')await loadAnimal();else if(screen==='responsibleAnimals')await responsible.refresh();else if(screen==='animalManage')await loadAnimalManage()}
onMounted(()=>{void initialize().catch(()=>{})})
</script>
