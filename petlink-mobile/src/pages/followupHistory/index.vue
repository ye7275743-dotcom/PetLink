<template><FollowupScreens screen="followupHistory" /></template>

<script setup>
import { useRouteGuard } from '../../composables/routeGuard.js'
import FollowupScreens from '../../features/followup/FollowupScreens.vue'

useRouteGuard('followupHistory')
</script>
