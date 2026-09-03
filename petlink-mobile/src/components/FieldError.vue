<template><text v-if="message" class="field-error">{{message}}</text></template>
<script setup>defineProps({message:String})</script>
<style scoped>.field-error{display:block;margin:-6rpx 0 14rpx;padding-left:14rpx;color:#b33b2e;font-size:19rpx;line-height:1.45}.field-error:before{content:"!";display:inline-grid;place-items:center;width:22rpx;height:22rpx;margin-right:6rpx;color:#fff;font:700 15rpx/1 sans-serif;background:#b33b2e;border-radius:50%}</style>
