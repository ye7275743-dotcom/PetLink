<template>
  <view v-if="!online" class="network-banner" role="status" aria-live="polite">
    <text>网络连接已断开，网络恢复后请刷新页面。</text>
  </view>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

const online = ref(true)
const onNetworkChange = result => { online.value = result?.isConnected !== false && result?.networkType !== 'none' }
function syncBrowserOnline(){
  if(typeof navigator !== 'undefined' && typeof navigator.onLine === 'boolean') online.value = navigator.onLine
}
function syncUniNetwork(){
  if(typeof uni !== 'undefined' && typeof uni.getNetworkType === 'function') uni.getNetworkType({success: onNetworkChange})
}

onMounted(() => {
  syncBrowserOnline()
  syncUniNetwork()
  if(typeof window !== 'undefined'){
    window.addEventListener('online', syncBrowserOnline)
    window.addEventListener('offline', syncBrowserOnline)
  }
  if(typeof uni !== 'undefined' && typeof uni.onNetworkStatusChange === 'function') uni.onNetworkStatusChange(onNetworkChange)
})
onBeforeUnmount(() => {
  if(typeof window !== 'undefined'){
    window.removeEventListener('online', syncBrowserOnline)
    window.removeEventListener('offline', syncBrowserOnline)
  }
  if(typeof uni !== 'undefined' && typeof uni.offNetworkStatusChange === 'function') uni.offNetworkStatusChange(onNetworkChange)
})
</script>

<style scoped>
.network-banner{position:fixed;top:90rpx;right:24rpx;left:24rpx;z-index:100;color:#fff8e7;padding:16rpx 22rpx;font-size:21rpx;line-height:1.45;text-align:center;background:linear-gradient(135deg,#b33b2e,#8f2e27);border:1px solid rgba(255,255,255,.35);border-radius:14rpx;box-shadow:0 12rpx 28rpx rgba(23,61,57,.24),inset 0 1px rgba(255,255,255,.16)}
</style>
