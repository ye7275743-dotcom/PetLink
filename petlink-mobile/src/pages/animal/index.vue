<template><AnimalScreens screen="animal" /></template>

<script setup>
import { useRouteGuard } from '../../composables/routeGuard.js'
import AnimalScreens from '../../features/animal/AnimalScreens.vue'

useRouteGuard('animal')
</script>
