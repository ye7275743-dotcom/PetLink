<template><RescueScreens screen="taskWaitingStart" /></template>

<script setup>
import { useRouteGuard } from '../../composables/routeGuard.js'
import RescueScreens from '../../features/rescue/RescueScreens.vue'

useRouteGuard('taskWaitingStart')
</script>
