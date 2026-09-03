<template>
  <view class="file-box">
    <text class="muted">图片 {{ modelValue.length }}/{{ max }} · JPG/JPEG/PNG · 单张≤5MB</text>
    <button class="btn" :disabled="choosing || modelValue.length>=max" @click="choose"><Icon name="plus" :size="18" />{{choosing?'上传中…':'添加图片'}}</button>
    <view v-for="item in items" :key="item.key" class="upload-row">
      <image v-if="item.localPath" class="preview" :src="item.localPath" mode="aspectFill"/>
      <view class="grow"><text>{{item.token ? '图片标识 '+item.token.slice(0,8)+'…' : item.name}}</text><progress v-if="item.status==='uploading'" :percent="item.progress" stroke-width="4" active/><text v-if="item.status==='error'" class="error-text">{{item.error||'上传失败'}}</text></view>
      <button v-if="item.status==='error'" class="mini" @click="retry(item)">重试</button>
      <button class="mini danger" aria-label="删除图片" @click="remove(item)"><Icon name="trash" :size="18" /></button>
    </view>
  </view>
</template>
<script setup>
import {ref,watch} from 'vue'
import {authApi} from '../api/index.js';import {validateImageMeta} from '../composables/uploadPolicy.js';import Icon from './Icon.vue'
const props=defineProps({modelValue:{type:Array,default:()=>[]},max:{type:Number,default:9}});const emit=defineEmits(['update:modelValue']);const items=ref([]),choosing=ref(false)
watch(()=>props.modelValue,(tokens)=>{const known=new Set(items.value.map(x=>x.token).filter(Boolean));for(const token of tokens){if(!known.has(token))items.value.push({key:token,token,status:'done',progress:100,name:token})}items.value=items.value.filter(x=>!x.token||tokens.includes(x.token))},{immediate:true,deep:true})
function sync(){emit('update:modelValue',items.value.filter(x=>x.status==='done'&&x.token).map(x=>x.token))}
async function uploadItem(item){item.status='uploading';item.error='';item.progress=0;try{const r=await authApi.uploadTemporary(item.localPath,{onProgress:p=>item.progress=p});item.token=r.token;item.status='done';item.progress=100;sync()}catch(e){item.status='error';item.error=e?.userMessage||'上传失败，请稍后重试'}}
async function choose(){if(props.modelValue.length>=props.max||choosing.value)return;choosing.value=true;try{const r=await new Promise((resolve,reject)=>uni.chooseImage({count:Math.min(9,props.max-props.modelValue.length),sizeType:['compressed'],success:resolve,fail:reject}));for(let i=0;i<(r.tempFilePaths||[]).length;i++){const fp=r.tempFilePaths[i];const meta=r.tempFiles?.[i]||{};const ext=(fp.split('.').pop()||'').toLowerCase();const inferred=meta.type||((ext==='png')?'image/png':(['jpg','jpeg'].includes(ext)?'image/jpeg':''));const validation=validateImageMeta({size:meta.size||0,type:inferred});const item={key:`${Date.now()}-${Math.random()}`,localPath:fp,name:fp.split('/').pop()||'图片',status:validation?'error':'queued',progress:0,token:'',error:validation};items.value.push(item);if(!validation)await uploadItem(item)}}finally{choosing.value=false}}
function retry(item){if(item.localPath)uploadItem(item)}function remove(item){items.value=items.value.filter(x=>x!==item);sync()}
</script>
<style scoped>
.file-box{position:relative;margin:16rpx 0;padding:22rpx;background:linear-gradient(135deg,rgba(241,234,219,.78),rgba(255,253,248,.72));border:1px dashed rgba(158,150,133,.8);border-radius:20rpx;box-shadow:0 8rpx 18rpx rgba(23,61,57,.04),inset 0 1px rgba(255,255,255,.84)}
.file-box::before{position:absolute;top:0;left:22rpx;width:52rpx;height:4rpx;content:"";background:linear-gradient(90deg,#e4684a,#f1c74b);border-radius:0 0 999rpx 999rpx}
.file-box>.btn{display:inline-flex;margin-top:14rpx}
.upload-row{display:flex;gap:12rpx;align-items:center;margin-top:12rpx;padding:12rpx;background:rgba(255,255,255,.82);border:1px solid rgba(216,209,195,.82);border-radius:16rpx;box-shadow:0 7rpx 15rpx rgba(23,61,57,.05)}
.preview{width:82rpx;height:82rpx;border-radius:14rpx;box-shadow:0 4rpx 10rpx rgba(23,61,57,.1)}
.grow{flex:1;min-width:0;color:#315650;font:17rpx/1.45 monospace}.error-text{display:block;margin-top:6rpx;color:#b33b2e;font-size:18rpx}
.btn,.mini{min-height:66rpx;padding:12rpx 18rpx;color:#315650;background:rgba(255,253,248,.92);border:1px solid rgba(198,188,169,.76);border-radius:14rpx;box-shadow:0 5rpx 12rpx rgba(23,61,57,.05),inset 0 1px rgba(255,255,255,.84)}
.mini{min-height:48rpx;padding:6rpx 11rpx;font-size:19rpx}.mini.danger{color:#b33b2e;background:#fff0ec;border-color:#dfb4aa}
</style>
