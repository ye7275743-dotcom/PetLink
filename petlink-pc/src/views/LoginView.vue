<template>
  <NetworkStatus />
  <div class="login-page">
    <div class="login-shell">
      <div class="login-visual"><img src="/assets/petlink-rescue-hero-v1.jpg" alt="猫狗在救助中心相互依偎" /><div class="visual-copy"><span class="eyebrow">PETLINK · 2026</span><strong>把需要帮助的它，交给愿意接住它的人。</strong></div></div>
      <div class="login-card">
      <div class="brand">宠链 <small>流浪动物救助中心</small></div>
      <span class="eyebrow">城市救助档案</span><h1>登录救助中心</h1>
      <el-form label-position="top" @submit.prevent="submit">
        <el-alert v-if="errorMessage" class="login-error" type="error" :closable="false" show-icon>{{ errorMessage }}</el-alert>
        <el-form-item label="账号" :error="fieldErrors.account"><el-input v-model="form.account" autocomplete="username" @input="clearError('account')" /></el-form-item>
        <el-form-item label="密码" :error="fieldErrors.password"><el-input v-model="form.password" type="password" show-password autocomplete="current-password" @input="clearError('password')" /></el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" style="width:100%">登录</el-button>
      </el-form>
      <p class="muted">电脑工作台仅供救助人员和管理员使用；普通用户请使用移动端。</p>
      </div>
    </div>
  </div>
</template>
<script setup>
import { reactive,ref } from 'vue'; import { useRouter } from 'vue-router'; import { authApi } from '../api/index.js'; import { useAuth } from '../store/auth.js'; import NetworkStatus from '../components/NetworkStatus.vue'
const form=reactive({account:'',password:''});const fieldErrors=reactive({account:'',password:''});const errorMessage=ref('');const loading=ref(false);const router=useRouter();const auth=useAuth()
function clearError(field){errorMessage.value='';if(field)fieldErrors[field]=''}
function validate(){
  fieldErrors.account=/^[A-Za-z0-9_]{4,50}$/.test(form.account.trim())?'':'账号需为 4～50 位字母、数字或下划线'
  const chars=[...form.password].length;const bytes=new TextEncoder().encode(form.password).length
  fieldErrors.password=chars>=8&&chars<=64&&bytes<=72?'':'密码需为 8～64 个字符，UTF-8 不超过 72 字节'
  return !fieldErrors.account&&!fieldErrors.password
}
async function submit(){if(loading.value||!validate())return;errorMessage.value='';loading.value=true;try{const r=await authApi.login({account:form.account.trim(),password:form.password},{silent:true});if(!['ADMIN','RESCUER'].includes(r.user?.roleCode)){errorMessage.value='该账号无权进入电脑工作台，请使用移动端';return}auth.setSession(r.accessToken,r.user);router.replace('/dashboard')}catch(e){errorMessage.value=e?.userMessage||'登录失败，请检查账号和密码'}finally{loading.value=false}}
</script>
<style scoped>
.login-page{min-height:100vh;display:grid;place-items:center;padding:42px;overflow-x:hidden;background:radial-gradient(circle at 18% 12%,rgba(241,199,75,.24),transparent 26%),radial-gradient(circle at 88% 86%,rgba(155,194,186,.2),transparent 28%),linear-gradient(135deg,var(--ink-deep) 0 43%,var(--paper) 43%)}
.login-shell{width:min(980px,96vw);display:grid;grid-template-columns:minmax(0,1.04fr) minmax(360px,.96fr);background:var(--panel);border:1px solid rgba(198,188,169,.78);border-radius:28px;overflow:hidden;box-shadow:0 30px 80px rgba(16,47,45,.22),0 12px 0 rgba(241,199,75,.56)}
.login-visual{position:relative;min-height:590px;overflow:hidden;background:var(--ink-deep)}.login-visual::before{content:"";position:absolute;inset:0;z-index:1;background:linear-gradient(135deg,rgba(16,47,45,.12),rgba(16,47,45,.72) 78%)}.login-visual::after{content:"";position:absolute;right:28px;bottom:28px;left:28px;z-index:2;height:1px;background:linear-gradient(90deg,var(--yellow),transparent);opacity:.75}.login-visual img{width:100%;height:100%;object-fit:cover;object-position:left center;opacity:.88;filter:saturate(.86) contrast(1.04)}.visual-copy{position:absolute;left:38px;right:38px;bottom:46px;z-index:3;color:#fff8e7}.visual-copy strong{display:block;max-width:420px;margin-top:14px;font:700 clamp(28px,3vw,38px)/1.2 var(--display);letter-spacing:-.04em}.login-card{display:flex;flex-direction:column;justify-content:center;padding:56px 52px;background:linear-gradient(145deg,rgba(255,255,255,.95),rgba(255,253,248,.92))}.brand{font:700 38px var(--display);letter-spacing:-.04em;margin-bottom:38px}.brand small{display:block;margin-top:8px;color:var(--muted);font:10px var(--mono);letter-spacing:.16em}.login-card h1{margin:6px 0 28px;font:700 32px var(--display);letter-spacing:-.035em}.muted{margin-top:20px;font-size:12px;line-height:1.7}.login-card :deep(.el-button){min-height:44px;font-size:15px}
@media(max-width:760px){.login-page{padding:18px;background:var(--paper)}.login-shell{grid-template-columns:1fr;border-radius:22px;box-shadow:0 22px 50px rgba(16,47,45,.18),0 8px 0 rgba(241,199,75,.56)}.login-visual{min-height:220px}.visual-copy{left:24px;right:24px;bottom:32px}.visual-copy strong{font-size:24px}.login-card{padding:34px 26px 38px}.brand{margin-bottom:28px}}
</style>
