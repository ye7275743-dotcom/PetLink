<template><view class="field"><text class="label">{{label}}</text><slot/><FieldError :message="error"/></view></template>
<script setup>import FieldError from './FieldError.vue';defineProps({label:String,error:String})</script>
<style scoped>
.field{margin-bottom:8rpx}.label{display:flex;align-items:center;gap:8rpx;margin-bottom:7rpx;color:#315650;font:700 17rpx/1 monospace;letter-spacing:1.8rpx}.label:before{width:6rpx;height:6rpx;content:"";background:#e4684a;border-radius:50%;box-shadow:0 0 0 3rpx rgba(228,104,74,.12)}
</style>
