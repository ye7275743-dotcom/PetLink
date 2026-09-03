<template><ClueScreens screen="clueEdit" /></template>

<script setup>
import { useRouteGuard } from '../../composables/routeGuard.js'
import ClueScreens from '../../features/clue/ClueScreens.vue'

useRouteGuard('clueEdit')
</script>
