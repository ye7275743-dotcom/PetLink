<template><el-alert v-if="message" :title="message" type="error" show-icon :closable="false" style="margin-bottom:14px"><template #default><el-button size="small" @click="$emit('retry')">重试</el-button></template></el-alert></template>
<script setup>defineProps({message:String});defineEmits(['retry'])</script>
