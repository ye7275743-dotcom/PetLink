<template>
  <div class="upload-box">
    <el-upload :show-file-list="false" :http-request="upload" accept="image/jpeg,image/png" :disabled="modelValue.length>=max">
      <el-button :disabled="modelValue.length>=max">上传图片</el-button>
    </el-upload>
    <div class="muted">JPG / JPEG / PNG · 单张 ≤ 5MB · 本次最多 {{max}} 张</div>
    <div class="upload-list" v-if="items.length">
      <div v-for="item in items" :key="item.key" class="upload-item">
        <img v-if="item.preview" :src="item.preview" alt="待上传图片预览"/>
        <div class="grow"><div class="token-text">{{item.token?'图片标识 '+item.token.slice(0,8)+'…':item.file?.name}}</div><el-progress v-if="item.status==='uploading'" :percentage="item.progress" :stroke-width="5"/><div v-if="item.status==='error'" class="error-text">{{item.error}}</div></div>
        <el-button v-if="item.status==='error'" size="small" @click="retry(item)">重试</el-button>
        <el-button size="small" type="danger" text @click="removeItem(item)">移除</el-button>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref,watch,onBeforeUnmount } from 'vue'; import { authApi } from '../api/index.js'; import { ElMessage } from 'element-plus'
const props=defineProps({modelValue:{type:Array,default:()=>[]},max:{type:Number,default:9}}); const emit=defineEmits(['update:modelValue']); const items=ref([])
watch(()=>props.modelValue,(tokens)=>{const known=new Set(items.value.map(i=>i.token).filter(Boolean));for(const token of tokens){if(!known.has(token))items.value.push({key:token,token,status:'done',progress:100,preview:'',file:null,error:''})}items.value=items.value.filter(i=>!i.token||tokens.includes(i.token))},{immediate:true,deep:true})
function sync(){emit('update:modelValue',items.value.filter(i=>i.status==='done'&&i.token).map(i=>i.token))}
async function doUpload(item){item.status='uploading';item.error='';item.progress=0;try{const r=await authApi.uploadTemporary(item.file,{onUploadProgress:e=>{if(e.total)item.progress=Math.round(e.loaded/e.total*100)}});item.token=r.token;item.status='done';item.progress=100;sync()}catch(e){item.status='error';item.error=e?.userMessage||'上传失败，请稍后重试'}}
async function upload({file}){if(props.modelValue.length>=props.max){ElMessage.warning(`最多 ${props.max} 张`);return}if(file.size>5*1024*1024){ElMessage.warning('单张图片不能超过 5MB');return}if(!['image/jpeg','image/png'].includes(file.type)){ElMessage.warning('仅支持 JPG / JPEG / PNG');return}const item={key:`${Date.now()}-${Math.random()}`,file,preview:URL.createObjectURL(file),token:'',status:'queued',progress:0,error:''};items.value.push(item);await doUpload(item)}
function retry(item){if(item.file)doUpload(item)}function removeItem(item){if(item.preview)URL.revokeObjectURL(item.preview);items.value=items.value.filter(x=>x!==item);sync()}
onBeforeUnmount(()=>items.value.forEach(i=>i.preview&&URL.revokeObjectURL(i.preview)))
</script>
<style scoped>.upload-box{padding:14px;border:1px dashed rgba(198,188,169,.94);background:linear-gradient(135deg,rgba(247,242,231,.92),rgba(255,253,248,.72));border-radius:12px;box-shadow:inset 0 1px rgba(255,255,255,.78)}.upload-list{display:grid;gap:10px;margin-top:12px}.upload-item{display:flex;align-items:center;gap:12px;background:rgba(255,255,255,.82);border:1px solid rgba(216,209,195,.9);border-radius:10px;padding:10px;box-shadow:0 5px 14px rgba(23,61,57,.045)}.upload-item img{width:58px;height:58px;object-fit:cover;border-radius:8px}.grow{flex:1;min-width:0}.token-text{font:11px var(--mono);overflow:hidden;text-overflow:ellipsis}.error-text{color:#b33b2e;font-size:11px;margin-top:4px}.muted{font-size:11px;margin-top:8px}</style>
