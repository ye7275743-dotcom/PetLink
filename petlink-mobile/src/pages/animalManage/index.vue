<template><AnimalScreens screen="animalManage" /></template>

<script setup>
import { useRouteGuard } from '../../composables/routeGuard.js'
import AnimalScreens from '../../features/animal/AnimalScreens.vue'

useRouteGuard('animalManage')
</script>
