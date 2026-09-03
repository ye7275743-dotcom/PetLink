<template><ContentScreens screen="announcements" /></template>

<script setup>
import { useRouteGuard } from '../../composables/routeGuard.js'
import ContentScreens from '../../features/content/ContentScreens.vue'

useRouteGuard('announcements')
</script>
