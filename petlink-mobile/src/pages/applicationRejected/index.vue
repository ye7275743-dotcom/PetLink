<template><AdoptionScreens screen="applicationRejected" /></template>

<script setup>
import { useRouteGuard } from '../../composables/routeGuard.js'
import AdoptionScreens from '../../features/adoption/AdoptionScreens.vue'

useRouteGuard('applicationRejected')
</script>
