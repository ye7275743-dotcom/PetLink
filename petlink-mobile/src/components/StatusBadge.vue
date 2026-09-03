<template><text :class="['badge', tone]" :aria-label="displayText(value)">{{displayText(value)}}</text></template>
<script setup>
import { computed } from 'vue'
import { displayText } from '../utils/displayText.js'

const props = defineProps({ value:String })
const tone = computed(() => {
  if (['REJECTED','FAILED','DISABLED','CANCELED','INVALIDATED'].includes(props.value)) return 'bad'
  if (['WAITING_ACCEPT','WAITING_START','PENDING_REVIEW','PENDING'].includes(props.value)) return 'waiting'
  if (['SUCCESS','APPROVED','PUBLISHED','AVAILABLE','IN_PROGRESS'].includes(props.value)) return 'positive'
  return 'neutral'
})
</script>
<style scoped>
.badge{display:inline-flex;align-items:center;gap:9rpx;margin:10rpx 0;padding:9rpx 14rpx;color:#315650;font:700 17rpx/1 monospace;letter-spacing:.4rpx;background:rgba(234,241,230,.9);border:1px solid rgba(23,61,57,.16);border-radius:999rpx;box-shadow:0 4rpx 10rpx rgba(23,61,57,.04),inset 0 1px rgba(255,255,255,.9)}
.badge:before{width:8rpx;height:8rpx;content:"";background:#6f8f6b;border-radius:50%;box-shadow:0 0 0 4rpx rgba(111,143,107,.13)}
.badge.waiting{color:#7a5a16;background:rgba(255,248,221,.96);border-color:rgba(241,199,75,.46)}
.badge.waiting:before{background:#e2aa2e;box-shadow:0 0 0 4rpx rgba(241,199,75,.16)}
.badge.positive{color:#28645b;background:rgba(226,241,233,.96);border-color:rgba(111,143,107,.34)}
.badge.bad{color:#b33b2e;background:rgba(255,240,236,.94);border-color:rgba(179,59,46,.25)}
.badge.bad:before{background:#b33b2e;box-shadow:0 0 0 4rpx rgba(179,59,46,.12)}
</style>
