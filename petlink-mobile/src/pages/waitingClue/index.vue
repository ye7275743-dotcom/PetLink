<template><ClueScreens screen="waitingClue" /></template>

<script setup>
import { useRouteGuard } from '../../composables/routeGuard.js'
import ClueScreens from '../../features/clue/ClueScreens.vue'

useRouteGuard('waitingClue')
</script>
