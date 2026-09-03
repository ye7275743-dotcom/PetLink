<template>
  <transition name="network-slide">
    <div v-if="!online" class="network-banner" role="status" aria-live="polite">
      网络连接已断开，当前显示的数据可能不是最新内容；网络恢复后请刷新页面。
    </div>
  </transition>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

const online = ref(typeof navigator === 'undefined' ? true : navigator.onLine)
function syncOnline(){ online.value = navigator.onLine }

onMounted(() => {
  window.addEventListener('online', syncOnline)
  window.addEventListener('offline', syncOnline)
})
onBeforeUnmount(() => {
  window.removeEventListener('online', syncOnline)
  window.removeEventListener('offline', syncOnline)
})
</script>

<style scoped>
.network-banner{position:fixed;top:14px;left:50%;z-index:2000;max-width:min(620px,calc(100vw - 32px));padding:11px 18px;color:#fff8e7;font-size:13px;line-height:1.45;text-align:center;background:linear-gradient(135deg,#b33b2e,#8f2e27);border:1px solid rgba(255,255,255,.35);border-radius:999px;box-shadow:0 12px 28px rgba(23,61,57,.24),inset 0 1px rgba(255,255,255,.18);transform:translateX(-50%)}
.network-slide-enter-active,.network-slide-leave-active{transition:opacity 180ms ease,transform 180ms ease}.network-slide-enter-from,.network-slide-leave-to{opacity:0;transform:translate(-50%,-10px)}
@media (max-width:560px){.network-banner{top:8px;font-size:12px;border-radius:12px}}
</style>
