export const MAX_IMAGE_BYTES=5*1024*1024
export const ALLOWED_IMAGE_MIME=new Set(['image/jpeg','image/png'])
export function validateImageMeta({size=0,type=''}={}){
  if(!ALLOWED_IMAGE_MIME.has(type)) return '仅支持 JPG / JPEG / PNG'
  if(Number(size)>MAX_IMAGE_BYTES) return '单张图片不能超过 5MB'
  return ''
}
