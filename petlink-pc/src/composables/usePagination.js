import {ref} from 'vue'
export function usePagination({pageSize=20}={}){
  const rows=ref([]),page=ref(1),size=ref(pageSize),total=ref(0),pages=ref(0),loading=ref(false),error=ref('')
  let requestSequence=0
  async function load(loader){
    const requestId=++requestSequence
    loading.value=true
    error.value=''
    try{
      const r=await loader({page:page.value,size:size.value})
      // A fast filter/page change can finish out of order. Only the newest
      // response is allowed to update the visible list.
      if(requestId!==requestSequence)return r
      rows.value=r?.records||[]
      total.value=Number(r?.total||0)
      pages.value=Number(r?.pages||0)
      return r
    }catch(e){
      if(requestId===requestSequence)error.value=e?.userMessage||'列表加载失败，请稍后重试'
      throw e
    }finally{
      if(requestId===requestSequence)loading.value=false
    }
  }
  function reset(){requestSequence++;page.value=1;loading.value=false;error.value=''}
  return{rows,page,size,total,pages,loading,error,load,reset}
}
