import test from 'node:test'
import assert from 'node:assert/strict'
import { usePagedList } from '../src/composables/pagedList.js'

test('Mobile refresh ignores a slower response from the previous query', async () => {
  const pending=[]
  const list=usePagedList(() => new Promise(resolve => pending.push(resolve)), {pageSize:20})
  const first=list.refresh()
  const second=list.refresh({status:'AVAILABLE'})
  assert.equal(pending.length,2)
  pending[1]({records:[{id:2}],page:1,size:20,total:1,pages:1})
  await second
  pending[0]({records:[{id:1}],page:1,size:20,total:1,pages:1})
  await first
  assert.deepEqual(list.records.value,[{id:2}])
  assert.equal(list.loading.value,false)
})
