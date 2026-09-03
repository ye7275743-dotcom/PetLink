<template><RescueScreens ref="screens" screen="tasks" /></template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useRouteGuard } from '../../composables/routeGuard.js'
import RescueScreens from '../../features/rescue/RescueScreens.vue'

const screens = ref(null)
useRouteGuard('tasks')
onShow(() => {
  const result = screens.value?.refresh?.()
  if (result) void result.catch(() => {})
})
</script>
