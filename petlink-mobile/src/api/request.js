import { API_BASE_URL } from '../config.js'
import { useAuth } from '../store/auth.js'

function redirectToLogin(){
  const pages=getCurrentPages?.()||[]
  const route=pages[pages.length-1]?.route||''
  if(route==='pages/login/index') return
  uni.reLaunch({url:'/pages/login/index'})
}
const statusMessages={400:'请求参数不正确',401:'登录已失效，请重新登录',403:'没有权限执行该操作',404:'请求的资源不存在',409:'业务状态冲突，请刷新后重试',413:'上传文件过大',500:'服务器内部错误',502:'服务暂不可用，请稍后重试',503:'服务暂不可用，请稍后重试',504:'服务响应超时，请稍后重试'}
function createRequestId(){return `h5-${Date.now().toString(36)}-${Math.random().toString(36).slice(2,10)}`}
function safeMessage(message,fallback){return typeof message==='string'&&/[\u4e00-\u9fff]/.test(message)?message:fallback}
function normalizeError(res, fallback='请求失败'){
  const body=res?.data||{},status=res?.statusCode||0
  const message=safeMessage(body?.message,statusMessages[status]||fallback)
  return {status,code:body?.code,message,userMessage:message,requestId:res?.header?.['X-Request-Id']||res?.header?.['x-request-id']}
}
export function request(path,{method='GET',data,headers={},silent=false}={}){
  return new Promise((resolve,reject)=>{
    const token=useAuth().state.token
    uni.request({url:API_BASE_URL+path,method,data,header:{'Content-Type':'application/json','X-Request-Id':createRequestId(),...(token?{Authorization:`Bearer ${token}`}:{}) ,...headers},success(res){
      const body=res.data||{}
      if(res.statusCode>=200&&res.statusCode<300&&(!('code' in body)||body.code===0)){resolve('code' in body?body.data:body);return}
      const err=normalizeError(res)
      if((res.statusCode===401||body.code===40302)){useAuth().logout();redirectToLogin()}
      else if(!silent)uni.showToast({title:err.message,icon:'none'})
      reject(err)
    },fail(raw){const err={status:0,code:null,message:'网络请求失败',userMessage:'网络请求失败',requestId:null,cause:raw};if(!silent)uni.showToast({title:err.message,icon:'none'});reject(err)}})
  })
}
export function uploadTemporary(filePath,{onProgress}={}){
  return new Promise((resolve,reject)=>{
    const token=useAuth().state.token
    const task=uni.uploadFile({url:API_BASE_URL+'/files/temporary',filePath,name:'file',header:{'X-Request-Id':createRequestId(),...(token?{Authorization:`Bearer ${token}`}:{})},success(res){
      let body={};try{body=JSON.parse(res.data)}catch{}
      if(res.statusCode>=200&&res.statusCode<300&&body.code===0){resolve(body.data);return}
      const message=safeMessage(body.message,statusMessages[res.statusCode]||'上传失败')
      const err={status:res.statusCode,code:body.code,message,userMessage:message,requestId:res?.header?.['X-Request-Id']||res?.header?.['x-request-id']}
      if((res.statusCode===401||body.code===40302)){useAuth().logout();redirectToLogin()}else uni.showToast({title:err.message,icon:'none'})
      reject(err)
    },fail(raw){reject({status:0,message:'上传失败',userMessage:'上传失败',requestId:null,cause:raw})}})
    if(task?.onProgressUpdate&&onProgress)task.onProgressUpdate(e=>onProgress(Number(e.progress||0)))
  })
}
export function mediaUrl(path){return API_BASE_URL+path}
