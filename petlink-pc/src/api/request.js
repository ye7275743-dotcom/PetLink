import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuth } from '../store/auth.js'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000
})

function createRequestId(){
  if(typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') return crypto.randomUUID()
  return `web-${Date.now().toString(36)}-${Math.random().toString(36).slice(2,10)}`
}

const statusMessages = {
  400: '请求参数不正确',
  401: '登录已失效，请重新登录',
  403: '没有权限执行该操作',
  404: '请求的资源不存在',
  409: '业务状态冲突，请刷新后重试',
  413: '上传文件过大',
  500: '服务器内部错误',
  502: '服务暂不可用，请稍后重试',
  503: '服务暂不可用，请稍后重试',
  504: '服务响应超时，请稍后重试'
}

function fallbackMessage(error, status) {
  if (error?.code === 'ECONNABORTED' || error?.code === 'ETIMEDOUT') return '请求超时，请稍后重试'
  if (!error?.response) return '无法连接服务器，请检查网络'
  return statusMessages[status] || '请求失败，请稍后重试'
}

function safeServerMessage(message, fallback) {
  return typeof message === 'string' && /[\u4e00-\u9fff]/.test(message) ? message : fallback
}

client.interceptors.request.use(config => {
  const { state } = useAuth()
  if(state.token) config.headers.Authorization = `Bearer ${state.token}`
  config.headers['X-Request-Id'] = config.headers['X-Request-Id'] || createRequestId()
  return config
})

client.interceptors.response.use(
  response => {
    const body = response.data
    if(body && typeof body === 'object' && 'code' in body){
      if(body.code === 0) return body.data
      return Promise.reject(Object.assign(new Error(safeServerMessage(body.message, '请求失败')), { code: body.code, response }))
    }
    return body
  },
  error => {
    const status = error.response?.status
    const body = error.response?.data
    if(status === 401){
      useAuth().logout()
      if(location.pathname !== '/login') location.href = '/login'
    }
    // Never expose Axios/browser English such as "Request failed with status code 404".
    const message = safeServerMessage(body?.message, fallbackMessage(error, status))
    if(status !== 401 && !error.config?.silent) ElMessage.error(message)
    const requestId = error.response?.headers?.['x-request-id'] || error.config?.headers?.['X-Request-Id']
    return Promise.reject(Object.assign(error, { code: body?.code, userMessage: message, requestId }))
  }
)

export default client
