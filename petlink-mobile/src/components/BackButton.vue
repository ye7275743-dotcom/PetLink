<script setup>
import Icon from './Icon.vue'

function goBack() {
  if (window.uni?.navigateBack) {
    window.uni.navigateBack()
    return
  }
  window.history.back()
}
</script>

<template><button class="back-btn" aria-label="返回" @click="goBack"><Icon name="back" :size="20" /> <text>返回</text></button></template>
<style scoped>
.back-btn{display:inline-flex;align-items:center;gap:8rpx;min-height:64rpx;margin:0 0 14rpx;padding:10rpx 18rpx 10rpx 12rpx;color:#315650;font-size:21rpx;font-weight:650;text-align:left;background:rgba(255,253,248,.68);border:1px solid rgba(198,188,169,.62);border-radius:999rpx;box-shadow:0 5rpx 14rpx rgba(23,61,57,.05),inset 0 1px rgba(255,255,255,.84);transition:transform 160ms ease,background 160ms ease}
.back-btn:after{border:0}.back-btn .ui-icon{color:#e4684a}.back-btn:active{opacity:.82;transform:translateX(-3rpx);background:#fffdf8}
</style>
