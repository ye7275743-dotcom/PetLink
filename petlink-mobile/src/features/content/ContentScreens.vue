<template>
  <MobileShell :screen="screen" @reach-bottom="onReachBottom">
    <template v-if="screen==='favorites'">
      <BackButton/><view class="title">我的收藏</view>
      <PageState :loading="favorites.loading.value" :error="favorites.error.value" :empty="!favorites.records.value.length" :has-data="!!favorites.records.value.length" :loading-more="favorites.loadingMore.value" :has-more="favorites.hasMore.value" empty-text="暂无收藏" @retry="favorites.retry">
        <view v-for="f in favorites.records.value" :key="f.favoriteId" class="card archive"><AuthImage v-if="f.animal?.coverImageUrl" class="thumb" :src="f.animal.coverImageUrl" mode="aspectFill"/><view v-else class="thumb placeholder">—</view><view class="grow"><text class="card-title">{{f.animal?.name}}</text><StatusBadge :value="f.animal?.status"/><view class="actions"><button class="btn" @click="goto('animal',{id:f.animal?.id})">查看档案</button><button class="btn danger" :disabled="submitState.submitting.value" @click="unfavorite(f)">取消收藏</button></view></view></view>
      </PageState>
    </template>
    <template v-else-if="screen==='announcements'">
      <BackButton/><view class="title">公告</view>
      <PageState :loading="announcements.loading.value" :error="announcements.error.value" :empty="!announcements.records.value.length" :has-data="!!announcements.records.value.length" :loading-more="announcements.loadingMore.value" :has-more="announcements.hasMore.value" empty-text="暂无已发布公告" @retry="announcements.retry">
        <view v-for="n in announcements.records.value" :key="n.id" class="card"><text class="card-title">{{n.title}}</text><text class="muted">{{n.publishedAt}}</text><button class="btn" @click="goto('announcementDetail',{id:n.id})">查看详情</button></view>
      </PageState>
    </template>
    <template v-else-if="screen==='announcementDetail'">
      <BackButton/><PageState :loading="detailState.loading.value" :error="detailState.error.value" :empty="!detail" :has-data="!!detail" @retry="loadDetail"><view v-if="detail" class="card"><text class="card-title big">{{detail.title}}</text><text class="muted">{{detail.publishedAt}}</text><text class="article">{{detail.content}}</text></view></PageState>
    </template>
  </MobileShell>
</template>
<script setup>
import {ref,onMounted} from 'vue'
import MobileShell from '../../components/MobileShell.vue';import PageState from '../../components/PageState.vue';import BackButton from '../../components/BackButton.vue';import StatusBadge from '../../components/StatusBadge.vue';import AuthImage from '../../components/AuthImage.vue'
import {contentApi} from '../../api/index.js';import {useNavigation} from '../../composables/navigation.js';import {usePagedList} from '../../composables/pagedList.js';import {useAsyncState,useSubmit} from '../../composables/asyncState.js';import {currentPageParams} from '../../composables/routeParams.js'
const props=defineProps({screen:{type:String,required:true}});const screen=props.screen;const params=currentPageParams();const {goto}=useNavigation();const favorites=usePagedList(q=>contentApi.favorites(q),{pageSize:20});const announcements=usePagedList(q=>contentApi.announcements(q),{pageSize:20});const detailState=useAsyncState(),submitState=useSubmit(),detail=ref(null)
async function unfavorite(f){await submitState.submit(async()=>{await contentApi.unfavorite(f.animal.id);await favorites.refresh()})}async function loadDetail(){await detailState.run(async()=>{detail.value=await contentApi.announcement(params.id)})}function onReachBottom(){if(screen==='favorites')favorites.loadMore();else if(screen==='announcements')announcements.loadMore()}
async function initialize(){if(screen==='favorites')await favorites.refresh();else if(screen==='announcements')await announcements.refresh();else if(screen==='announcementDetail')await loadDetail()}
onMounted(()=>{void initialize().catch(()=>{})})
</script>
