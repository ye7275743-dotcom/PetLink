<template><AdoptionScreens screen="apply" /></template>

<script setup>
import { useRouteGuard } from '../../composables/routeGuard.js'
import AdoptionScreens from '../../features/adoption/AdoptionScreens.vue'

useRouteGuard('apply')
</script>
