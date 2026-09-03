import { ref, computed } from 'vue'
import { mergePageRecords } from './paginationPolicy.js'

export function usePagedList(loader,{pageSize=20,initialQuery={}}={}){
  const records = ref([])
  const page = ref(1)
  const size = ref(pageSize)
  const total = ref(0)
  const pages = ref(0)
  const loading = ref(false)
  const loadingMore = ref(false)
  const error = ref('')
  const query = ref({...initialQuery})
  let requestSequence = 0
  const hasMore = computed(() => pages.value ? page.value < pages.value : records.value.length < total.value)

  async function fetchPage(targetPage,{append=false}={}){
    if((loading.value || loadingMore.value) && append) return
    const requestId = ++requestSequence
    append ? loadingMore.value=true : loading.value=true
    error.value=''
    try{
      const result = await loader({page:targetPage,size:size.value,...query.value})
      // Ignore responses from an obsolete filter/refresh request. This keeps
      // slow networks from putting an older page back on screen.
      if(requestId !== requestSequence) return result
      const incoming = result?.records || []
      records.value = append ? mergePageRecords(records.value,incoming) : incoming
      page.value = Number(result?.page || targetPage)
      total.value = Number(result?.total || records.value.length)
      pages.value = Number(result?.pages || Math.ceil(total.value / size.value) || 0)
      return result
    }catch(e){
      if(requestId !== requestSequence) return
      error.value = e?.userMessage || '列表加载失败，请稍后重试'
      throw e
    }finally{
      if(requestId === requestSequence) append ? loadingMore.value=false : loading.value=false
    }
  }
  async function refresh(nextQuery){
    if(nextQuery) query.value={...nextQuery}
    page.value=1
    records.value=[]
    return fetchPage(1,{append:false})
  }
  async function loadMore(){
    if(!hasMore.value || loading.value || loadingMore.value) return
    return fetchPage(page.value+1,{append:true})
  }
  async function retry(){ return records.value.length ? loadMore() : refresh() }
  return {records,page,size,total,pages,loading,loadingMore,error,query,hasMore,refresh,loadMore,retry}
}
