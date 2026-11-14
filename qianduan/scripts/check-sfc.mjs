#!/usr/bin/env node
/**
 * 前端 SFC / JS 全量校验脚本（零新增依赖）
 *
 * 用法：cd qianduan && node scripts/check-sfc.mjs
 *
 * 校验范围：
 *   1) src/**\/*.vue → @vue/compiler-sfc 的 parse + compileScript + compileTemplate
 *      + compileStyleAsync（scss 预处理，注入 src/styles/variables.scss）
 *   2) src/**\/*.js  → node --check（语法校验，不执行）
 *
 * 输出：每个文件 PASS/FAIL，结尾 TOTAL/FAIL；任一失败退出码为 1。
 * 注意：本脚本仅做静态编译校验，不替代 `npm run build` 与浏览器交互验收。
 */
import { readFileSync, readdirSync } from 'node:fs'
import { join, relative, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { createHash } from 'node:crypto'
import { spawnSync } from 'node:child_process'
import {
  parse,
  compileScript,
  compileTemplate,
  compileStyleAsync
} from '@vue/compiler-sfc'

const projectRoot = resolve(fileURLToPath(new URL('..', import.meta.url)))
const srcDir = join(projectRoot, 'src')


function walk(dir, acc = []) {
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const full = join(dir, entry.name)
    if (entry.isDirectory()) {
      walk(full, acc)
    } else {
      acc.push(full)
    }
  }
  return acc
}

function toRel(file) {
  return relative(projectRoot, file).replace(/\\/g, '/')
}

// 每个文件生成稳定短 id，供 scoped 样式与模板编译使用
function fileId(relPath) {
  return createHash('sha256').update(relPath).digest('hex').slice(0, 8)
}

function formatErrors(errors) {
  return errors
    .map((err) => (err && err.message ? err.message : String(err)))
    .join(' | ')
}

async function checkVue(file) {
  const raw = readFileSync(file, 'utf-8')
  // 个别历史文件带 BOM，解析前剔除，避免模板首字符异常
  const source = raw.replace(/^\uFEFF/, '')
  const relPath = toRel(file)
  const id = fileId(relPath)

  const { descriptor, errors } = parse(source, { filename: file })
  if (errors.length) {
    throw new Error(`parse 失败：${formatErrors(errors)}`)
  }

  if (descriptor.script || descriptor.scriptSetup) {
    compileScript(descriptor, { id })
  }

  if (descriptor.template) {
    const templateResult = compileTemplate({
      source: descriptor.template.content,
      filename: file,
      id,
      compilerOptions: {
        scoped: descriptor.styles.some((style) => style.scoped)
      }
    })
    if (templateResult.errors.length) {
      throw new Error(`template 编译失败：${formatErrors(templateResult.errors)}`)
    }
  }

  for (const style of descriptor.styles) {
    if (!style.content || !style.content.trim()) continue
    const lang = style.lang || 'css'
    // compiler-sfc 3.x 的节点语言预处理选项入口是 preprocessOptions，
    // 与 vite 的 preprocessorOptions 同名键不同，这里显式注入全局 scss 变量
    const preprocessOptions = {}
    if (lang === 'scss' || lang === 'sass') {
      // 与 vite.config.js 的 additionalData 等价：
      // 通过 loadPaths 把 src 作为 scss 模块根，避免绝对路径/中文路径解析问题
      preprocessOptions.additionalData = '@use "styles/variables.scss" as *;'
      preprocessOptions.loadPaths = [srcDir]
      preprocessOptions.quietDeps = true
    }
    const styleResult = await compileStyleAsync({
      source: style.content,
      filename: file,
      id,
      scoped: style.scoped,
      preprocessLang: style.lang,
      preprocessOptions
    })
    if (styleResult.errors.length) {
      throw new Error(`style 编译失败：${formatErrors(styleResult.errors)}`)
    }
  }
}

function checkJs(file) {
  const result = spawnSync(process.execPath, ['--check', file], { encoding: 'utf-8' })
  if (result.status !== 0) {
    const detail = (result.stderr || result.stdout || '').trim()
    throw new Error(detail || 'node --check 校验失败')
  }
}

async function main() {
  const allFiles = walk(srcDir).sort()
  const vueFiles = allFiles.filter((file) => file.endsWith('.vue'))
  const jsFiles = allFiles.filter((file) => file.endsWith('.js'))
  const total = vueFiles.length + jsFiles.length
  let failed = 0

  for (const file of vueFiles) {
    const relPath = toRel(file)
    try {
      await checkVue(file)
      console.log(`PASS  ${relPath}`)
    } catch (error) {
      failed++
      console.log(`FAIL  ${relPath}`)
      console.log(`      ${error.message}`)
    }
  }

  for (const file of jsFiles) {
    const relPath = toRel(file)
    try {
      checkJs(file)
      console.log(`PASS  ${relPath}`)
    } catch (error) {
      failed++
      console.log(`FAIL  ${relPath}`)
      console.log(`      ${error.message}`)
    }
  }

  console.log('')
  console.log(`TOTAL: ${total}  FAIL: ${failed}  (.vue=${vueFiles.length}, .js=${jsFiles.length})`)
  process.exit(failed === 0 ? 0 : 1)
}

main().catch((error) => {
  console.error(`校验脚本自身异常：${error && error.message ? error.message : error}`)
  process.exit(1)
})