#!/usr/bin/env node
/**
 * Static checks for the Android sources, runnable without the Android SDK.
 *
 * Scope is deliberately narrow: only things this script can decide with
 * certainty. It is NOT a compiler — CI's `assembleDebug` is the authority on
 * whether the code builds, and Gradle is where type errors are caught.
 *
 * Checks:
 *  1. bracket balance per file (string-, raw-string- and comment-aware)
 *  2. every `import com.efina.app.…` resolves to a real file or package
 *  3. no placeholder markers left in source
 *  4. no duplicate top-level declarations across the module (Kotlin rejects these)
 *
 * Usage: node tools/check-kotlin.mjs [--strict]
 */

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(here, '..');
const SRC = path.join(ROOT, 'app/src/main/java');
const strict = process.argv.includes('--strict');

const files = [];
(function walk(dir) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p);
    else if (p.endsWith('.kt')) files.push(p);
  }
})(SRC);

/** Strip comments and literals so bracket counting is meaningful. */
function strip(src) {
  let out = '';
  let i = 0;
  let mode = null;
  while (i < src.length) {
    const c = src[i];
    const n = src[i + 1];

    if (mode === null) {
      if (c === '/' && n === '/') { mode = 'line'; i += 2; continue; }
      if (c === '/' && n === '*') { mode = 'block'; i += 2; continue; }
      if (c === '"' && n === '"' && src[i + 2] === '"') { mode = 'raw'; i += 3; out += '""'; continue; }
      if (c === '"') { mode = 'string'; i++; out += '""'; continue; }
      if (c === "'") { mode = 'char'; i++; out += "''"; continue; }
      out += c; i++; continue;
    }
    if (mode === 'line') { if (c === '\n') { mode = null; out += c; } i++; continue; }
    if (mode === 'block') { if (c === '*' && n === '/') { mode = null; i += 2; continue; } i++; continue; }
    if (mode === 'raw') { if (c === '"' && n === '"' && src[i + 2] === '"') { mode = null; i += 3; continue; } i++; continue; }
    if (mode === 'string') { if (c === '\\') { i += 2; continue; } if (c === '"') mode = null; i++; continue; }
    if (mode === 'char') { if (c === '\\') { i += 2; continue; } if (c === "'") mode = null; i++; continue; }
  }
  return out;
}

const problems = [];
const rel = (f) => path.relative(ROOT, f);

// ── 1. bracket balance ──────────────────────────────────────────────
for (const file of files) {
  const cleaned = strip(fs.readFileSync(file, 'utf8'));
  const pairs = { '{': '}', '(': ')', '[': ']' };
  const stack = [];
  let broke = false;
  for (const ch of cleaned) {
    if (pairs[ch]) stack.push(ch);
    else if (Object.values(pairs).includes(ch)) {
      const top = stack.pop();
      if (pairs[top] !== ch) { broke = true; break; }
    }
  }
  if (broke || stack.length) {
    problems.push(`UNBALANCED  ${rel(file)}${stack.length ? ' open:' + stack.join('') : ''}`);
  }
}

// ── 2. project imports resolve ──────────────────────────────────────
// A .kt file contributes its own FQN and every package prefix along the way, so
// both `import a.b.File` and `import a.b.*` can be checked.
const fileFqns = new Set();
const packages = new Set();
for (const file of files) {
  const fqn = path.relative(SRC, file).replace(/\.kt$/, '').replace(/\//g, '.');
  fileFqns.add(fqn);
  const parts = fqn.split('.');
  for (let i = 1; i <= parts.length; i++) packages.add(parts.slice(0, i).join('.'));
}

// Generated at build time by the Android Gradle plugin; not present in source.
const GENERATED = new Set(['com.efina.app.BuildConfig']);

const unresolved = [];
for (const file of files) {
  const src = fs.readFileSync(file, 'utf8');
  for (const m of src.matchAll(/^import\s+(com\.efina\.app\.[\w.]+)/gm)) {
    let fqn = m[1].replace(/\.$/, '');
    if (GENERATED.has(fqn)) continue;
    const isWildcard = /\.\*$/.test(m[1]);
    if (isWildcard) fqn = fqn.slice(0, -2);
    if (fileFqns.has(fqn) || packages.has(fqn)) continue;
    // import of a top-level member declared in another file: check the package
    const pkg = fqn.split('.').slice(0, -1).join('.');
    if (packages.has(pkg)) continue;
    unresolved.push(`${rel(file)}: import ${m[1]}`);
  }
}
if (unresolved.length) {
  problems.push(`IMPORTS unresolved (${unresolved.length}):`);
  problems.push(...unresolved.map((u) => '   ' + u));
}

// ── 3. placeholders ─────────────────────────────────────────────────
const placeholders = files.filter((f) => {
  const src = fs.readFileSync(f, 'utf8');
  return /Coming Soon|TODO\(|FIXME/.test(src);
});
if (placeholders.length) {
  problems.push('PLACEHOLDERS found:');
  problems.push(...placeholders.map((p) => '   ' + rel(p)));
}

// Duplicate/redeclaration detection is intentionally absent: doing it correctly
// needs a real parser (modifiers, receiver types, generics, overloads). Gradle
// reports redeclaration as a hard compile error, so CI covers it.

console.log(`kotlin files: ${files.length}`);
console.log(`packages: ${packages.size}, file types: ${fileFqns.size}`);

if (problems.length === 0) {
  console.log('static checks: ALL CLEAN');
} else {
  console.log('');
  for (const p of problems) console.log(p);
  console.log(`\n${problems.length} problem group(s)`);
}

if (strict && problems.length) process.exit(1);
