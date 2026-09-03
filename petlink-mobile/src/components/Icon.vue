<template>
  <svg class="ui-icon" :style="iconStyle" :aria-hidden="!label" :aria-label="label || undefined" :role="label ? 'img' : undefined" viewBox="0 0 24 24">
    <path v-for="(d, i) in (iconPaths[name] || iconPaths.spark)" :key="i" :d="d" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
  </svg>
</template>
<script setup>
import { computed } from 'vue'
import { iconPaths } from './iconPaths.js'

const props = defineProps({ name: String, size: { type: [Number, String], default: 24 }, label: String })
const iconStyle = computed(() => ({ width: `${props.size}rpx`, height: `${props.size}rpx` }))
</script>
<style scoped>
.ui-icon{display:inline-block;flex:none;overflow:visible;vertical-align:middle;transition:transform 160ms ease,filter 160ms ease}.ui-icon:active{transform:scale(.94);filter:brightness(1.12)}
</style>
