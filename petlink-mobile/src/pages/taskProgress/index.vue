<template><RescueScreens screen="taskProgress" /></template>

<script setup>
import { useRouteGuard } from '../../composables/routeGuard.js'
import RescueScreens from '../../features/rescue/RescueScreens.vue'

useRouteGuard('taskProgress')
</script>
