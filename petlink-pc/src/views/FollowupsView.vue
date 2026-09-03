<template>
  <PageHeader title="回访记录" :subtitle="role==='ADMIN'?'第六模块 · 管理员查看全部回访':'第六模块 · 救助人员查看负责动物回访'"><el-button :loading="pager.loading.value" @click="load">刷新</el-button></PageHeader>
  <ListError :message="pager.error.value" @retry="load"/>
  <div class="card table-card"><el-table :data="pager.rows.value" v-loading="pager.loading.value" empty-text="暂无回访记录"><el-table-column prop="id" label="回访编号" width="100"/><el-table-column prop="adoptionRecordId" label="领养记录" width="120"/><el-table-column prop="submitterId" label="提交人" width="110"/><el-table-column prop="content" label="生活情况"/><el-table-column prop="healthCondition" label="健康情况"/><el-table-column prop="createdAt" label="时间" width="190"/><el-table-column label="操作" width="100"><template #default="{row}"><el-button size="small" @click="open(row)">详情</el-button></template></el-table-column></el-table></div>
  <el-pagination class="pager" background layout="sizes,prev,pager,next,total" :page-sizes="[10,20,50]" :total="pager.total.value" v-model:page-size="pager.size.value" v-model:current-page="pager.page.value" @size-change="changePageSize" @current-change="load"/>
  <el-drawer v-model="drawer" size="580px" title="回访详情"><template v-if="detail"><div class="reason"><b>回访 #{{detail.id}}</b><div>{{detail.content||'—'}}</div><div>健康：{{detail.healthCondition||'—'}}</div><div class="muted">{{detail.createdAt}}</div></div><div class="photos"><AuthImage v-for="img in detail.images||[]" :key="img.id" :src="img.url"/></div></template></el-drawer>
</template>
<script setup>
import{ref,computed,onMounted}from'vue';import PageHeader from'../components/PageHeader.vue';import AuthImage from'../components/AuthImage.vue';import ListError from '../components/ListError.vue';import{followupApi}from'../api/index.js';import{useAuth}from'../store/auth.js';import {usePagination} from '../composables/usePagination.js'
const role=computed(()=>useAuth().state.user?.roleCode),pager=usePagination({pageSize:20}),drawer=ref(false),detail=ref(null)
async function load(){await pager.load(({page,size})=>role.value==='ADMIN'?followupApi.adminList({page,size}):followupApi.rescuerList({page,size})).catch(()=>{})}
function changePageSize(){pager.reset();load()}async function open(row){detail.value=await followupApi.detail(row.id);drawer.value=true}onMounted(load)
</script>
<style scoped>.photos{display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin-top:16px}.photos img{width:100%;height:118px;object-fit:cover;border:1px solid rgba(216,209,195,.86);border-radius:12px;box-shadow:0 8px 18px rgba(23,61,57,.08)}</style>
