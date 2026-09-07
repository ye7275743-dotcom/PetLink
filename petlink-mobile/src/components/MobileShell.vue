<template>
  <view class="app-shell-mobile">
    <NetworkStatus />
    <view class="statusbar">
      <view class="status-brand"><view class="status-led" /><text>宠链公益</text></view>
      <text class="status-role">{{ roleText(role||'VISITOR') }}</text>
    </view>
    <view class="mobile-header">
      <view class="brand-lockup">
        <view class="brand-mark"><Icon name="spark" :size="22" /></view>
        <view class="logo"><view class="brand-names"><text class="brand-cn">宠链</text><text class="brand-en">PetLink</text></view><text>救助与领养</text></view>
      </view>
      <view class="header-meta">
        <text class="header-kicker">关爱每个生命</text>
        <view class="role-chip"><view class="role-dot" />{{ roleText(role||'VISITOR') }}</view>
      </view>
    </view>
    <scroll-view :scroll-into-view="scrollTarget" scroll-with-animation class="mobile-content" scroll-y :show-scrollbar="false" @scrolltolower="$emit('reach-bottom')">
      <slot />
      <view class="safe-bottom-spacer" />
    </scroll-view>
    <view v-if="mainScreens.includes(screen)" class="bottom-nav">
      <button :class="screen==='home'?'active':''" aria-label="首页" @click="switchMain('home')"><Icon name="home" :size="26" /><text>首页</text></button>
      <button :class="screen==='clue'?'active':''" aria-label="救助线索" @click="requireLogin('clue')"><Icon name="pin" :size="26" /><text>线索</text></button>
      <button v-if="role==='RESCUER'" :class="screen==='tasks'?'active':''" aria-label="救助任务" @click="switchMain('tasks')"><Icon name="tasks" :size="26" /><text>救助</text></button>
      <button v-if="user" :class="screen==='records'?'active':''" aria-label="领养记录" @click="switchMain('records')"><Icon name="heart" :size="26" /><text>领养</text></button>
      <button :class="screen==='profile'?'active':''" aria-label="我的" @click="switchMain('profile')"><Icon name="users" :size="26" /><text>我的</text></button>
    </view>
  </view>
</template>
<script setup>
import { computed } from 'vue'
import { useAuth } from '../store/auth.js'
import { useNavigation } from '../composables/navigation.js'
import { roleText } from '../utils/displayText.js'
import Icon from './Icon.vue'
import NetworkStatus from './NetworkStatus.vue'
defineProps({screen:{type:String,required:true},scrollTarget:{type:String,default:''}})
defineEmits(['reach-bottom'])
const auth=useAuth(); const user=computed(()=>auth.state.user); const role=computed(()=>user.value?.roleCode)
const {switchMain,requireLogin}=useNavigation()
const mainScreens=['home','clue','tasks','records','profile']
</script>
<style scoped>
.app-shell-mobile {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100vh;
  height: 100dvh;
  overflow: hidden;
  color: #173d39;
  background: transparent;
}

.statusbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-sizing: content-box;
  min-height: 50rpx;
  padding: env(safe-area-inset-top) 30rpx 0;
  color: #e9f1ee;
  font: 17rpx/1 monospace;
  letter-spacing: 1.6rpx;
  background: linear-gradient(115deg, #0c2927 0%, #173d39 62%, #285b52 100%);
}

.status-brand {
  display: inline-flex;
  gap: 10rpx;
  align-items: center;
  opacity: .9;
}

.status-led,
.role-dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: #f1c74b;
  box-shadow: 0 0 0 5rpx rgba(241, 199, 75, .12), 0 0 12rpx rgba(241, 199, 75, .3);
}

.status-role {
  color: #cbd9d5;
  letter-spacing: 0;
}

.mobile-header {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-sizing: border-box;
  min-height: 132rpx;
  padding: 24rpx 30rpx;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(255, 253, 248, .96), rgba(250, 247, 239, .86));
  border-bottom: 1px solid rgba(216, 209, 195, .76);
  box-shadow: 0 12rpx 30rpx rgba(23, 61, 57, .08), inset 0 1px rgba(255, 255, 255, .88);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
}

.mobile-header::before {
  position: absolute;
  top: -76rpx;
  right: -54rpx;
  width: 220rpx;
  height: 180rpx;
  content: "";
  pointer-events: none;
  background: radial-gradient(circle, rgba(155, 194, 186, .24), transparent 68%);
}

.mobile-header::after {
  position: absolute;
  right: 0;
  bottom: -4rpx;
  left: 0;
  height: 4rpx;
  content: "";
  background: linear-gradient(90deg, #e4684a 0 28%, #f1c74b 28% 52%, #6f8f6b 52% 76%, #9bc2ba 76%);
}

.brand-lockup {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 16rpx;
  align-items: center;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 68rpx;
  height: 68rpx;
  color: #f8e9b0;
  background: linear-gradient(145deg, #1b5149, #102f2d);
  border: 1px solid rgba(255, 255, 255, .22);
  border-radius: 22rpx 22rpx 22rpx 8rpx;
  box-shadow: 0 8rpx 18rpx rgba(16, 47, 45, .2), inset 0 1px rgba(255, 255, 255, .16);
}

.logo {
  color: #102f2d;
  font: 700 40rpx/1 "Songti SC", "STSong", Georgia, serif;
  letter-spacing: -1rpx;
}

.logo text {
  display: block;
  margin-top: 9rpx;
  color: #71817a;
  font: 14rpx/1 monospace;
  letter-spacing: 2.5rpx;
}

.header-meta {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 9rpx;
  align-items: flex-end;
}

.header-kicker {
  color: #93a49e;
  font: 13rpx/1 monospace;
  letter-spacing: 1.8rpx;
}

.role-chip {
  display: inline-flex;
  gap: 9rpx;
  align-items: center;
  padding: 9rpx 14rpx 9rpx 11rpx;
  color: #315650;
  font: 17rpx/1 monospace;
  background: rgba(255, 255, 255, .76);
  border: 1px solid rgba(198, 188, 169, .72);
  border-radius: 999rpx;
  box-shadow: 0 4rpx 12rpx rgba(23, 61, 57, .06), inset 0 1px rgba(255, 255, 255, .86);
}

.role-chip .role-dot {
  width: 8rpx;
  height: 8rpx;
  box-shadow: 0 0 0 4rpx rgba(241, 199, 75, .13);
}

.mobile-content {
  flex: 1;
  box-sizing: border-box;
  min-height: 0;
  padding: 34rpx 28rpx 0;
  overscroll-behavior: contain;
}

.safe-bottom-spacer {
  height: calc(202rpx + env(safe-area-inset-bottom));
}

.bottom-nav {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 20;
  display: flex;
  justify-content: space-around;
  padding: 13rpx 10rpx calc(17rpx + env(safe-area-inset-bottom));
  background: rgba(12, 41, 39, .96);
  border-top: 1px solid rgba(255, 255, 255, .14);
  box-shadow: 0 -16rpx 34rpx rgba(23, 61, 57, .2), inset 0 1px rgba(255, 255, 255, .06);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.bottom-nav::before {
  position: absolute;
  top: -1px;
  right: 18%;
  left: 18%;
  height: 1px;
  content: "";
  background: linear-gradient(90deg, transparent, rgba(241, 199, 75, .7), transparent);
}

.bottom-nav button {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 92rpx;
  min-height: 72rpx;
  padding: 8rpx 18rpx;
  color: #a9bfba;
  font-size: 17rpx;
  line-height: 1.25;
  background: transparent;
  border: 0;
  border-radius: 20rpx;
  transition: background 180ms ease, color 180ms ease, transform 180ms ease, box-shadow 180ms ease;
}

.bottom-nav button text {
  display: block;
  margin-top: 5rpx;
}

.bottom-nav button .ui-icon {
  display: block;
  margin: 0 auto;
  opacity: .82;
  transition: transform 180ms ease, opacity 180ms ease;
}

.bottom-nav button.active {
  color: #f1c74b;
  transform: translateY(-7rpx);
  background: linear-gradient(145deg, rgba(241, 199, 75, .2), rgba(241, 199, 75, .06));
  box-shadow: 0 10rpx 20rpx rgba(5, 24, 23, .18), inset 0 1px rgba(255, 255, 255, .1);
}

.bottom-nav button.active .ui-icon {
  opacity: 1;
  transform: translateY(-2rpx) scale(1.06);
}

.bottom-nav button::after {
  border: 0;
}
.brand-names{display:flex;align-items:baseline;gap:10rpx}.logo .brand-cn{margin:0;color:#102f2d;font:700 36rpx/1 "Songti SC","STSong",serif;letter-spacing:-1rpx}.logo .brand-en{margin:0;color:#102f2d;font:italic 600 34rpx/1 "Baskerville",Georgia,serif;letter-spacing:-1.4rpx}
</style>
