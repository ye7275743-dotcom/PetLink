<template>
  <PageHeader title="个人资料" subtitle="账户信息与可编辑资料">
    <el-button :loading="loading" @click="load">刷新</el-button>
  </PageHeader>

  <ListError :message="error" @retry="load" />

  <div class="card profile-card" v-loading="loading">
    <template v-if="profile">
      <div class="profile-heading">
        <div class="avatar">{{ (profile.nickname || profile.account || '?').slice(0, 1) }}</div>
        <div>
          <h2>{{ profile.nickname || profile.account }}</h2>
          <div class="muted">{{ profile.account }} · {{ displayText(profile.roleCode) }}</div>
        </div>
      </div>

      <el-descriptions :column="2" border class="account-details">
        <el-descriptions-item label="账号">{{ profile.account }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ displayText(profile.roleCode) }}</el-descriptions-item>
        <el-descriptions-item label="账号状态">{{ displayText(profile.status) }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ profile.createdAt || '—' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider />
      <h3 class="section-title">编辑资料</h3>
      <p class="muted help">仅可修改昵称和手机号；账号、角色、状态等受保护字段不可在此页面修改。</p>
      <el-form label-position="top" @submit.prevent="save">
        <div class="form-grid">
          <el-form-item label="昵称" :error="errors.nickname">
            <el-input v-model="edit.nickname" maxlength="50" show-word-limit />
          </el-form-item>
          <el-form-item label="手机号" :error="errors.phone">
            <el-input v-model="edit.phone" maxlength="20" placeholder="可选" />
          </el-form-item>
        </div>
        <el-button type="primary" :loading="saving" @click="save">保存资料</el-button>
      </el-form>
    </template>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '../components/PageHeader.vue'
import ListError from '../components/ListError.vue'
import { authApi } from '../api/index.js'
import { useAuth } from '../store/auth.js'
import { displayText } from '../utils/displayText.js'

const auth = useAuth()
const profile = ref(null)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const edit = reactive({ nickname: '', phone: '' })
const errors = reactive({ nickname: '', phone: '' })
const mainlandPhone = /^1[3-9]\d{9}$/

function fill(value) {
  profile.value = value
  edit.nickname = value?.nickname || ''
  edit.phone = value?.phone || ''
}

function validate() {
  const nickname = edit.nickname.trim()
  const phone = edit.phone.trim()
  errors.nickname = nickname.length >= 1 && nickname.length <= 50 ? '' : '昵称必填且长度为 1～50 个字符'
  errors.phone = !phone || mainlandPhone.test(phone) ? '' : '请输入正确的中国大陆手机号'
  return !errors.nickname && !errors.phone
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    fill(await authApi.me())
  } catch (e) {
    error.value = e?.userMessage || '个人资料加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!validate()) return
  saving.value = true
  try {
    const value = await authApi.patchMe({
      nickname: edit.nickname.trim(),
      phone: edit.phone.trim() || null
    })
    fill(value)
    auth.setUser(value)
    ElMessage.success('资料已保存')
  } catch (e) {
    ElMessage.error(e?.userMessage || '资料保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.profile-card{max-width:820px}.profile-heading{display:flex;align-items:center;gap:15px;margin-bottom:22px}.profile-heading h2{font-family:var(--display);font-size:28px;margin:0 0 5px}.avatar{width:58px;height:58px;border-radius:50%;display:grid;place-items:center;background:var(--ink);color:var(--yellow);font:700 26px var(--display)}.account-details{margin-bottom:22px}.help{margin:-4px 0 16px;font-size:13px}
</style>
