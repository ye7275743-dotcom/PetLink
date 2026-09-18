#!/usr/bin/env node

/**
 * Build the reproducible course fixture from a fresh PetLink database.
 *
 * The fixture is intentionally generated through the public REST API instead
 * of shipping a production dump. This keeps the repository free of server
 * credentials, uploaded private files and real contact information while
 * preserving the same business states and source-linked case flows.
 *
 * Required environment:
 *   PETLINK_REAL_CASE_PASSWORD=<the password assigned to the local demo users>
 * Optional:
 *   PETLINK_REAL_CASE_ORIGIN=http://127.0.0.1:8080/api
 */

import { spawn } from 'node:child_process'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = dirname(dirname(fileURLToPath(import.meta.url)))
const scripts = ['seed-real-cases.mjs', 'seed-real-cases-batch2.mjs']

if (!process.env.PETLINK_REAL_CASE_PASSWORD) {
  console.error('缺少 PETLINK_REAL_CASE_PASSWORD。请只在本地或受控演示环境中设置。')
  process.exit(2)
}

function run(script) {
  return new Promise((resolve, reject) => {
    const child = spawn(process.execPath, [join(ROOT, 'scripts', script)], {
      cwd: ROOT,
      env: process.env,
      stdio: 'inherit'
    })
    child.once('error', reject)
    child.once('exit', code => code === 0 ? resolve() : reject(new Error(`${script} exited with ${code}`)))
  })
}

try {
  for (const script of scripts) await run(script)
  console.log('PetLink 可复现课程案例已写入：第一批流程案例 + 第二批领养档案。')
} catch (error) {
  console.error(`课程案例生成失败：${error.message}`)
  process.exitCode = 1
}
