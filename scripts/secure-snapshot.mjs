#!/usr/bin/env node

/** Authenticated AES-256-GCM envelope for private PetLink state snapshots. */

import { createCipheriv, createDecipheriv, createHash, randomBytes } from 'node:crypto'
import { createReadStream, createWriteStream } from 'node:fs'
import { appendFile, chmod, link, mkdir, open, readFile, rm, stat, writeFile } from 'node:fs/promises'
import { dirname } from 'node:path'
import { pipeline } from 'node:stream/promises'

const MAGIC = Buffer.from('PETLINK-STATE-SNAPSHOT-V1\n', 'ascii')
const NONCE_BYTES = 12
const TAG_BYTES = 16

function usage() {
  console.error('用法：')
  console.error('  node scripts/secure-snapshot.mjs encrypt <archive.tar.gz|-> <snapshot.enc> <key-file>')
  console.error('  node scripts/secure-snapshot.mjs decrypt <snapshot.enc> <archive.tar.gz> <key-file>')
  process.exit(2)
}

async function readKey(path, create) {
  try {
    const value = (await readFile(path, 'utf8')).trim()
    if (!/^[a-f0-9]{64}$/i.test(value)) throw new Error('密钥文件格式无效（需要 32 字节十六进制密钥）')
    const keyStat = await stat(path)
    if ((keyStat.mode & 0o077) !== 0) throw new Error('密钥文件权限过宽；请设置为仅当前用户可读写（0600）')
    return Buffer.from(value, 'hex')
  } catch (error) {
    if (error.code !== 'ENOENT' || !create) throw error
    const key = randomBytes(32)
    await mkdir(dirname(path), { recursive: true, mode: 0o700 })
    await writeFile(path, `${key.toString('hex')}\n`, { flag: 'wx', mode: 0o600 })
    await chmod(path, 0o600)
    return key
  }
}

async function digest(path) {
  const hash = createHash('sha256')
  for await (const chunk of createReadStream(path)) hash.update(chunk)
  return hash.digest('hex')
}

async function encrypt(input, output, keyPath) {
  if (input !== '-') {
    const inputStat = await stat(input)
    if (!inputStat.isFile()) throw new Error('输入必须是归档文件')
  }
  const key = await readKey(keyPath, true)
  const nonce = randomBytes(NONCE_BYTES)
  const cipher = createCipheriv('aes-256-gcm', key, nonce)
  const temporaryCiphertext = `${output}.ciphertext-${process.pid}.tmp`
  let createdOutput = false
  try {
    const source = input === '-' ? process.stdin : createReadStream(input)
    await pipeline(source, cipher, createWriteStream(temporaryCiphertext, { flags: 'wx', mode: 0o600 }))
    const tag = cipher.getAuthTag()
    await writeFile(output, Buffer.concat([MAGIC, nonce]), { flag: 'wx', mode: 0o600 })
    createdOutput = true
    await pipeline(createReadStream(temporaryCiphertext), createWriteStream(output, { flags: 'a', mode: 0o600 }))
    await appendFile(output, tag)
    await chmod(output, 0o600)
    console.log(`加密完成：${output}`)
    console.log(`SHA-256：${await digest(output)}`)
  } catch (error) {
    if (createdOutput) await rm(output, { force: true })
    throw error
  } finally {
    await rm(temporaryCiphertext, { force: true })
  }
}

async function decrypt(input, output, keyPath) {
  const inputStat = await stat(input)
  const prefixBytes = MAGIC.length + NONCE_BYTES
  if (!inputStat.isFile() || inputStat.size < prefixBytes + TAG_BYTES) throw new Error('加密快照长度无效')
  try {
    await stat(output)
    throw new Error(`输出文件已存在，拒绝覆盖：${output}`)
  } catch (error) {
    if (error.code !== 'ENOENT') throw error
  }

  const file = await open(input, 'r')
  let prefix
  let tag
  try {
    prefix = Buffer.alloc(prefixBytes)
    tag = Buffer.alloc(TAG_BYTES)
    await file.read(prefix, 0, prefix.length, 0)
    await file.read(tag, 0, tag.length, inputStat.size - TAG_BYTES)
  } finally {
    await file.close()
  }
  if (!prefix.subarray(0, MAGIC.length).equals(MAGIC)) throw new Error('不支持的快照格式')

  const key = await readKey(keyPath, false)
  const nonce = prefix.subarray(MAGIC.length)
  const decipher = createDecipheriv('aes-256-gcm', key, nonce)
  decipher.setAuthTag(tag)
  const temporaryOutput = `${output}.partial-${process.pid}.tmp`
  try {
    await pipeline(
      createReadStream(input, { start: prefixBytes, end: inputStat.size - TAG_BYTES - 1 }),
      decipher,
      createWriteStream(temporaryOutput, { flags: 'wx', mode: 0o600 })
    )
    await link(temporaryOutput, output)
    await rm(temporaryOutput, { force: true })
    await chmod(output, 0o600)
    console.log(`解密并通过完整性校验：${output}`)
  } catch (error) {
    await rm(temporaryOutput, { force: true })
    throw new Error(`解密失败或认证标签不匹配：${error.message}`)
  }
}

const [operation, input, output, keyPath] = process.argv.slice(2)
if (!['encrypt', 'decrypt'].includes(operation) || !input || !output || !keyPath) usage()

try {
  if (operation === 'encrypt') await encrypt(input, output, keyPath)
  else await decrypt(input, output, keyPath)
} catch (error) {
  console.error(`快照处理失败：${error.message}`)
  process.exitCode = 1
}
