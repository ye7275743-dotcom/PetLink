<template><ProfileScreens screen="register" /></template>

<script setup>
import { useRouteGuard } from '../../composables/routeGuard.js'
import ProfileScreens from '../../features/profile/ProfileScreens.vue'

useRouteGuard('register')
</script>
