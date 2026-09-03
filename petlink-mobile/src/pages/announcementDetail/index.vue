<template><ContentScreens screen="announcementDetail" /></template>

<script setup>
import { useRouteGuard } from '../../composables/routeGuard.js'
import ContentScreens from '../../features/content/ContentScreens.vue'

useRouteGuard('announcementDetail')
</script>
