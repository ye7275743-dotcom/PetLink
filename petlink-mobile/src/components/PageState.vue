<template>
  <view v-if="loading && !hasData" class="state-box"><view class="spinner"/><text>{{ loadingText }}</text></view>
  <view v-else-if="error && !hasData" class="state-box error"><text>{{ error }}</text><button class="btn" @click="$emit('retry')">重试</button></view>
  <view v-else-if="empty && !hasData" class="state-box"><text>{{ emptyText }}</text></view>
  <slot v-else />
  <view v-if="loadingMore" class="load-more">正在加载更多…</view>
  <view v-else-if="error && hasData" class="load-more error"><text>{{error}}</text><button class="mini" @click="$emit('retry')">重试</button></view>
  <view v-else-if="hasData && !hasMore" class="load-more">已经到底了</view>
</template>
<script setup>
defineProps({loading:Boolean,error:String,empty:Boolean,hasData:Boolean,loadingMore:Boolean,hasMore:{type:Boolean,default:true},emptyText:{type:String,default:'暂无数据'},loadingText:{type:String,default:'正在加载…'}})
defineEmits(['retry'])
</script>
<style scoped>
.state-box{position:relative;display:grid;justify-items:center;gap:20rpx;margin:22rpx 0;padding:82rpx 28rpx;text-align:center;color:#71817a;background:rgba(255,253,248,.66);border:1px dashed rgba(198,188,169,.86);border-radius:22rpx;box-shadow:0 10rpx 22rpx rgba(23,61,57,.04),inset 0 1px rgba(255,255,255,.86)}
.state-box::before{position:absolute;top:0;left:28rpx;width:56rpx;height:4rpx;content:"";background:linear-gradient(90deg,#e4684a,#f1c74b);border-radius:0 0 999rpx 999rpx}
.state-box.error{color:#a9402b;background:rgba(255,246,243,.78);border-style:solid;border-color:rgba(179,59,46,.24)}
.spinner{width:34rpx;height:34rpx;border:5rpx solid #d8d2c6;border-top-color:#e85f3e;border-radius:50%;animation:spin .8s linear infinite}
.load-more{padding:30rpx;color:#71817a;font-size:20rpx;text-align:center}
.load-more.error{display:flex;gap:12rpx;align-items:center;justify-content:center;color:#a9402b}
@keyframes spin{to{transform:rotate(360deg)}}
.btn,.mini{min-height:66rpx;padding:10rpx 18rpx;color:#315650;background:rgba(255,253,248,.92);border:1px solid rgba(198,188,169,.76);border-radius:14rpx;box-shadow:0 5rpx 12rpx rgba(23,61,57,.05),inset 0 1px rgba(255,255,255,.84)}
.mini{min-height:48rpx;padding:6rpx 12rpx;font-size:19rpx}
</style>
