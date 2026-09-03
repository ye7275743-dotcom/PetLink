<template><img :src="resolved||fallback" :alt="alt" loading="lazy" decoding="async" v-bind="$attrs"/></template>
<script setup>
import {ref,watch,onBeforeUnmount} from 'vue';import request from '../api/request.js';import {useAuth} from '../store/auth.js'
const props=defineProps({src:String,fallback:{type:String,default:''},alt:{type:String,default:''}});const resolved=ref('');let objectUrl=''
async function load(){if(objectUrl){URL.revokeObjectURL(objectUrl);objectUrl=''};const src=props.src;if(!src){resolved.value='';return}if(!src.startsWith('/api/')||!useAuth().state.token){resolved.value=src;return}try{const blob=await request.get(src.replace(/^\/api/,''),{responseType:'blob',silent:true});objectUrl=URL.createObjectURL(blob);resolved.value=objectUrl}catch{resolved.value=props.fallback}}
watch(()=>props.src,load,{immediate:true});onBeforeUnmount(()=>{if(objectUrl)URL.revokeObjectURL(objectUrl)})
</script>
