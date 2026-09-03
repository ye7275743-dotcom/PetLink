<template><ProfileScreens screen="profileEdit" /></template>

<script setup>
import { useRouteGuard } from '../../composables/routeGuard.js'
import ProfileScreens from '../../features/profile/ProfileScreens.vue'

useRouteGuard('profileEdit')
</script>
