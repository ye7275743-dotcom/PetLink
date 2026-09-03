<template><view v-if="task" class="card"><text class="card-title">任务详情 #{{task.id}}</text><StatusBadge :value="task.status"/><text class="muted">线索 #{{task.clueId}}</text><text class="muted">{{task.startedAt?`开始时间 ${task.startedAt}`:'等待开始'}}</text></view></template>
<script setup>import StatusBadge from './StatusBadge.vue';defineProps({task:Object})</script>
