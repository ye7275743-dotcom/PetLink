import test from 'node:test'
import assert from 'node:assert/strict'
import { usePagination } from '../src/composables/usePagination.js'

function deferred(){
  let resolve
  let reject
  const promise=new Promise((res,rej)=>{resolve=res;reject=rej})
  return {promise,resolve,reject}
}

test('PC pagination keeps the newest response when requests finish out of order', async () => {
  const pager=usePagination({pageSize:20})
  const first=deferred(); const second=deferred()
  const firstLoad=pager.load(()=>first.promise)
  const secondLoad=pager.load(()=>second.promise)
  second.resolve({records:[{id:2}],total:1,pages:1})
  await secondLoad
  first.resolve({records:[{id:1}],total:1,pages:1})
  await firstLoad
  assert.deepEqual(pager.rows.value,[{id:2}])
  assert.equal(pager.loading.value,false)
})
