<template><ClueScreens screen="clue" /></template>

<script setup>
import { useRouteGuard } from '../../composables/routeGuard.js'
import ClueScreens from '../../features/clue/ClueScreens.vue'

useRouteGuard('clue')
</script>
