<template><div class="head"><div><span class="eyebrow">{{ eyebrow }}</span><h1 class="page-title">{{ title }}</h1><div class="page-sub">{{ subtitle }}</div></div><div class="actions"><slot/></div></div></template>
<script setup>defineProps({title:String,subtitle:String,eyebrow:{type:String,default:'宠链 / 流浪动物救助中心'}})</script>
<style scoped>
.head{display:flex;justify-content:space-between;align-items:flex-end;gap:24px;margin-bottom:28px;padding:2px 0 22px;border-bottom:1px solid rgba(198,188,169,.72);position:relative}.head::before{content:"";position:absolute;left:0;top:-2px;width:42px;height:2px;background:var(--yellow);border-radius:999px}.head::after{content:"";position:absolute;left:0;bottom:-2px;width:76px;height:3px;background:var(--orange);border-radius:999px}.actions{display:flex;gap:8px;flex-wrap:wrap;align-items:center}
</style>
