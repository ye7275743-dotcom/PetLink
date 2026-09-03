import { ref } from 'vue'

export function useAsyncState(){
  const loading = ref(false)
  const error = ref('')
  let lastRunner = null
  async function run(runner){
    lastRunner = runner
    loading.value = true
    error.value = ''
    try { return await runner() }
    catch (e) { error.value = e?.userMessage || '加载失败，请稍后重试'; throw e }
    finally { loading.value = false }
  }
  async function retry(){ if(lastRunner) return run(lastRunner) }
  return { loading, error, run, retry }
}

export function useSubmit(){
  const submitting = ref(false)
  async function submit(runner){
    if(submitting.value) return
    submitting.value = true
    try { return await runner() }
    finally { submitting.value = false }
  }
  return { submitting, submit }
}
