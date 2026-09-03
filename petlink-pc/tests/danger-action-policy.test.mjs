import test from 'node:test'
import assert from 'node:assert/strict'
import { failureResolutionFromDialog } from '../src/domain/rescueResolutionPolicy.js'

test('FAILED resolution distinguishes reopen close and dismiss',()=>{
  assert.equal(failureResolutionFromDialog('confirm'),'REOPEN')
  assert.equal(failureResolutionFromDialog('cancel'),'CLOSE')
  assert.equal(failureResolutionFromDialog('close'),null)
  assert.equal(failureResolutionFromDialog('esc'),null)
})
