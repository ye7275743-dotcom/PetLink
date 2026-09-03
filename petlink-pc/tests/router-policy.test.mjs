import test from 'node:test'
import assert from 'node:assert/strict'
import { routeDecision, isWorkbenchRole } from '../src/router/policy.js'

test('unauthenticated protected route redirects to login',()=>{
  assert.equal(routeDecision({hasToken:false,allowedRoles:['ADMIN']}),'/login')
})

test('login route with a valid workbench session returns to dashboard',()=>{
  assert.equal(routeDecision({isPublic:true,isLogin:true,hasToken:true,role:'ADMIN'}),'/dashboard')
  assert.equal(routeDecision({isPublic:true,isLogin:true,hasToken:true,role:'RESCUER'}),'/dashboard')
})

test('USER is not a PC workbench role and protected routes redirect to login',()=>{
  assert.equal(isWorkbenchRole('USER'),false)
  assert.equal(routeDecision({hasToken:true,role:'USER',allowedRoles:['ADMIN','RESCUER']}),'/login')
})

test('valid workbench role mismatch returns to dashboard while valid role is allowed',()=>{
  assert.equal(routeDecision({hasToken:true,role:'RESCUER',allowedRoles:['ADMIN']}),'/dashboard')
  assert.equal(routeDecision({hasToken:true,role:'ADMIN',allowedRoles:['ADMIN']}),true)
  assert.equal(routeDecision({hasToken:true,role:'RESCUER',allowedRoles:['ADMIN','RESCUER']}),true)
})
