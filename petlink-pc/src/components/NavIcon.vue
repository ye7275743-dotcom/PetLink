<template>
  <svg class="nav-svg" :style="iconStyle" :aria-hidden="!label" :aria-label="label || undefined" :role="label ? 'img' : undefined" viewBox="0 0 24 24">
    <path v-for="(d, i) in (iconPaths[name] || iconPaths.dashboard)" :key="i" :d="d" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
  </svg>
</template>
<script setup>
import { computed } from 'vue'
import { iconPaths } from './iconPaths.js'

const props = defineProps({ name: String, size: { type: [Number, String], default: 20 }, label: String })
const iconStyle = computed(() => ({ width: `${props.size}px`, height: `${props.size}px` }))
</script>
<style scoped>
.nav-svg{display:block;flex:none;color:currentColor;overflow:visible;transition:transform 160ms ease}.nav-svg:hover{transform:scale(1.06)}
</style>
