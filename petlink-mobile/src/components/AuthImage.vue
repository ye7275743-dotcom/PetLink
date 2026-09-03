<template><image :src="resolved||fallback" lazy-load v-bind="$attrs" @error="resolved=fallback"/></template>
<script setup>
import { ref, watch } from 'vue'
import { API_BASE_URL } from '../config.js'
import { useAuth } from '../store/auth.js'

const props=defineProps({src:String,fallback:{type:String,default:''}})
const auth=useAuth()
const resolved=ref('')
let loadVersion=0

function absolute(src){ return src.startsWith('/api/') ? API_BASE_URL.replace(/\/api$/,'')+src : src }
function load(){
  const version=++loadVersion
  const src=props.src
  if(!src){resolved.value=props.fallback;return}
  if(!src.startsWith('/api/')||!auth.state.token){resolved.value=absolute(src);return}
  uni.downloadFile({
    url:absolute(src),
    header:{Authorization:`Bearer ${auth.state.token}`},
    success:r=>{if(version!==loadVersion)return;resolved.value=r.statusCode===200?r.tempFilePath:props.fallback},
    fail:()=>{if(version===loadVersion)resolved.value=props.fallback}
  })
}
watch([()=>props.src,()=>auth.state.token],load,{immediate:true})
</script>
