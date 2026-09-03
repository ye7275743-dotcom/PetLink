<template><FollowupScreens screen="followupDetail" /></template>

<script setup>
import { useRouteGuard } from '../../composables/routeGuard.js'
import FollowupScreens from '../../features/followup/FollowupScreens.vue'

useRouteGuard('followupDetail')
</script>
