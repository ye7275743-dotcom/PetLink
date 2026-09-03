<template><FollowupScreens screen="rescuerFollowups" /></template>

<script setup>
import { useRouteGuard } from '../../composables/routeGuard.js'
import FollowupScreens from '../../features/followup/FollowupScreens.vue'

useRouteGuard('rescuerFollowups')
</script>
