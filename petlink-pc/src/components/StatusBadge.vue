<template><span :class="['status-pill',tone]">{{ displayText(value) }}</span></template>
<script setup>
import { computed } from 'vue'
import { displayText } from '../utils/displayText.js'
const p=defineProps({value:String})
const tone=computed(()=>['REJECTED','FAILED','DISABLED'].includes(p.value)?'danger':['PENDING_REVIEW','WAITING_START','PENDING','SUSPENDED','DRAFT'].includes(p.value)?'warn':'')
</script>
