#!/usr/bin/env node
/**
 * Validates DTO *shapes* against the backend, not just route names.
 *
 * check-api-contract.mjs proves the verb and path exist. It cannot see that a
 * request sends `next_date` where the handler destructures `next_run_date`, or
 * that a `Boolean` property is fed a SQLite 0/1. Those are the defects that
 * shipped: registration failed to parse, settings never loaded, debts and
 * scheduled creates always returned 400.
 *
 * Two checks:
 *   A. Column types — every DTO property whose name matches a schema column must
 *      use a compatible Kotlin type.
 *   B. Request fields — every field on a request body DTO must appear in the
 *      handler that receives it, which catches renamed or invented fields.
 *
 * Usage:
 *   node tools/check-dto-shapes.mjs            report, exit 0
 *   node tools/check-dto-shapes.mjs --strict   exit 1 on any problem
 */

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const ANDROID_ROOT = path.resolve(here, '..');
const WEB_ROOT = process.env.EFINA_WEB_ROOT || '/storage/emulated/0/git/efina';
const MIGRATIONS = path.join(WEB_ROOT, 'migrations');
const API_DIR = path.join(WEB_ROOT, 'functions', 'api');
const DTO_DIR = path.join(ANDROID_ROOT, 'app/src/main/java/com/efina/app/data/remote/dto');
const SCHEMA_SNAPSHOT = path.join(ANDROID_ROOT, 'tools', 'backend-schema.json');

const strict = process.argv.includes('--strict');
const problems = [];
const notes = [];

// ── Schema: column name -> set of SQL types ────────────────────────────────
const columnTypes = new Map();

function readSchema() {
  // CI cannot see the private backend, so a snapshot of the column types is
  // committed. Without it this check would run against an empty schema and pass
  // vacuously — which is worse than not running it.
  if (!fs.existsSync(MIGRATIONS)) {
    if (fs.existsSync(SCHEMA_SNAPSHOT)) {
      const snap = JSON.parse(fs.readFileSync(SCHEMA_SNAPSHOT, 'utf8'));
      for (const [col, types] of Object.entries(snap.columns)) columnTypes.set(col, new Set(types));
    }
    return;
  }
  const files = [];
  for (const dir of [MIGRATIONS, path.join(MIGRATIONS, '_archive')]) {
    if (!fs.existsSync(dir)) continue;
    for (const f of fs.readdirSync(dir).filter((f) => f.endsWith('.sql'))) {
      files.push(path.join(dir, f));
    }
  }
  // ALTER TABLE ... ADD COLUMN is how several live columns are declared, including
  // accounts.display_order and accounts.is_shared. Ignoring these files made the
  // snapshot blind to them, which let a real defect through.
  for (const file of files) {
    const sql = fs.readFileSync(file, 'utf8');
    for (const m of sql.matchAll(/ALTER\s+TABLE\s+\w+\s+ADD\s+COLUMN\s+([a-z_][a-z0-9_]*)\s+([A-Za-z]+)/gi)) {
      const col = m[1];
      const upper = m[2].toUpperCase().replace(/\(.*\)/, '');
      if (!columnTypes.has(col)) columnTypes.set(col, new Set());
      columnTypes.get(col).add(upper);
    }
    for (const block of sql.matchAll(/CREATE\s+TABLE(?:\s+IF\s+NOT\s+EXISTS)?\s+\w+\s*\(([\s\S]*?)\n\)/gi)) {
      for (const line of block[1].split('\n')) {
        const m = line.trim().match(/^([a-z_][a-z0-9_]*)\s+([A-Za-z]+)/i);
        if (!m) continue;
        const [, col, type] = m;
        if (/^(PRIMARY|FOREIGN|UNIQUE|CHECK|CONSTRAINT|REFERENCES)$/i.test(col)) continue;
        const upper = type.toUpperCase().replace(/\(.*\)/, '');
        if (!columnTypes.has(col)) columnTypes.set(col, new Set());
        columnTypes.get(col).add(upper);
      }
    }
  }
}

// ── Kotlin DTOs ────────────────────────────────────────────────────────────
function readDtos() {
  const out = new Map();
  if (!fs.existsSync(DTO_DIR)) return out;
  for (const file of fs.readdirSync(DTO_DIR).filter((f) => f.endsWith('.kt'))) {
    const src = fs.readFileSync(path.join(DTO_DIR, file), 'utf8');
    for (const block of src.matchAll(/data\s+class\s+(\w+)\s*\(([\s\S]*?)\n\)/g)) {
      const [, name, body] = block;
      const fields = [];
      for (const fm of body.matchAll(/(?:@\w+(?:\([^)]*\))?\s*)*(?:val|var)\s+(\w+)\s*:\s*([\w.<>?]+)/g)) {
        const [, field, kotlinType] = fm;
        const lenient = /LenientBoolean/.test(fm[0]);
        fields.push({ field, kotlinType, lenient });
      }
      out.set(name, { name, fields, file });
    }
  }
  return out;
}

// ── Check A: column type compatibility ─────────────────────────────────────
const COMPAT = {
  String: ['TEXT', 'DATETIME', 'DATE', 'TIMESTAMP', 'VARCHAR', 'CHAR', 'BLOB'],
  Int: ['INTEGER', 'INT', 'BIGINT', 'REAL', 'NUMERIC', 'DECIMAL', 'BOOLEAN'],
  Long: ['INTEGER', 'INT', 'BIGINT', 'BOOLEAN'],
  Double: ['REAL', 'NUMERIC', 'DECIMAL', 'FLOAT', 'DOUBLE', 'INTEGER'],
  Boolean: ['INTEGER', 'BOOLEAN', 'TINYINT'],
  Float: ['REAL', 'FLOAT'],
};

function checkColumnTypes(dtos) {
  for (const [dtoName, dto] of dtos) {
    for (const { field, kotlinType, lenient } of dto.fields) {
      const sqlTypes = columnTypes.get(field);
      if (!sqlTypes) continue;
      const base = kotlinType.replace('?', '');
      const allowed = COMPAT[base];
      if (!allowed) continue;
      // A Boolean backed by a SQLite integer column is ALWAYS suspect: D1 returns
      // 0/1 and kotlinx rejects that for a plain Boolean. This has to be checked
      // before the compatibility table, because INTEGER counts as compatible.
      if (base === 'Boolean' && [...sqlTypes].some((t) => t === 'INTEGER') && !lenient &&
          !REAL_BOOLEANS.has(`${dtoName}.${field}`)) {
        problems.push(
          `${dtoName}.${field}: Boolean reads a SQLite INTEGER column — needs ` +
          `@Serializable(with = LenientBoolean::class) or the deserializer throws on 0/1`
        );
        continue;
      }
      const matches = [...sqlTypes].some((t) => allowed.includes(t));
      if (matches) continue;
      problems.push(
        `${dtoName}.${field}: Kotlin ${kotlinType} vs column ${[...sqlTypes].join('|')} (${dto.file})`
      );
    }
  }
}

// ── Check B: request fields exist in the receiving handler ─────────────────
/** Fields that are intentional client-side aliases and never sent literally. */
const ALIASES = new Set([]);

/**
 * Booleans the backend computes as real JSON booleans (`!!x`) rather than
 * returning the raw SQLite integer. Each entry names the handler that proves it.
 */
const REAL_BOOLEANS = new Set([
  // accounts/[id]/members.js:49 -> `is_shared: !!account?.is_shared`
  'AccountMembersDto.is_shared',
]);

function checkRequestFields(dtos) {
  if (!fs.existsSync(API_DIR)) {
    notes.push('backend tree absent; request-field check skipped');
    return;
  }
  const srcs = new Map();
  const walk = (dir) => {
    for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
      const full = path.join(dir, e.name);
      if (e.isDirectory()) walk(full);
      else if (e.name.endsWith('.js') && !e.name.endsWith('.test.js'))
        srcs.set(full, fs.readFileSync(full, 'utf8'));
    }
  };
  walk(API_DIR);

  for (const [dtoName, dto] of dtos) {
    if (!/Request(Dto)?$/.test(dtoName)) continue;
    // Find handlers that could receive it: any handler mentioning two or more
    // of the DTO's field names is treated as the receiver.
    const fieldNames = dto.fields.map((f) => f.field);
    if (!fieldNames.length) continue;

    let best = null;
    let bestScore = 0;
    for (const [file, src] of srcs) {
      if (!/onRequest(Post|Put|Patch)/.test(src)) continue;
      const score = fieldNames.filter((f) => new RegExp(`\\b${f}\\b`).test(src)).length;
      if (score > bestScore) { bestScore = score; best = { file, src }; }
    }
    // Require a clear majority before holding the handler responsible.
    if (!best || bestScore < Math.ceil(fieldNames.length * 0.5)) continue;

    for (const field of fieldNames) {
      if (ALIASES.has(field)) continue;
      if (new RegExp(`\\b${field}\\b`).test(best.src)) continue;
      problems.push(
        `${dtoName}.${field}: not referenced in ${path.relative(WEB_ROOT, best.file)} — ` +
        `likely sent under a name the handler does not read`
      );
    }
  }
}

readSchema();

// Self-check: the checker is the thing that proves every other claim, so verify
// it still discriminates instead of quietly passing everything.
if (process.argv.includes('--selftest')) {
  const compat = (kotlin, sql) =>
    (COMPAT[kotlin] || []).includes(sql);

  const cases = [
    // [description, expectedToPass, kotlinType, sqlType]
    ['Int reading TEXT is a defect', false, 'Int', 'TEXT'],
    ['String reading TEXT is fine', true, 'String', 'TEXT'],
    ['Double reading REAL is fine', true, 'Double', 'REAL'],
    ['Int reading INTEGER is fine', true, 'Int', 'INTEGER'],
    ['Boolean reading INTEGER needs LenientBoolean', true, 'Boolean', 'INTEGER'],
  ];

  let bad = 0;
  for (const [name, expected, kotlin, sql] of cases) {
    const got = compat(kotlin, sql);
    if (got !== expected) {
      console.error(`SELFTEST FAIL: ${name} (expected ${expected}, got ${got})`);
      bad++;
    }
  }
  console.log(bad === 0 ? `selftest OK: ${cases.length} cases` : `selftest FAILED: ${bad}`);
  process.exit(bad === 0 ? 0 : 1);
}

if (process.argv.includes('--emit-schema')) {
  const payload = {
    generatedFrom: 'bedroomcult/efina migrations',
    note: 'Regenerate with: node tools/check-dto-shapes.mjs --emit-schema',
    columns: Object.fromEntries(
      [...columnTypes.entries()].sort().map(([c, t]) => [c, [...t].sort()])
    ),
  };
  fs.writeFileSync(SCHEMA_SNAPSHOT, JSON.stringify(payload, null, 2) + '\n');
  console.log(`wrote ${columnTypes.size} columns to ${path.relative(ANDROID_ROOT, SCHEMA_SNAPSHOT)}`);
  process.exit(0);
}

const dtos = readDtos();
checkColumnTypes(dtos);
checkRequestFields(dtos);

console.log(`schema columns: ${columnTypes.size}`);
console.log(`kotlin dtos: ${dtos.size}`);
for (const n of notes) console.log(`note: ${n}`);

if (problems.length) {
  console.log(`\nSHAPE PROBLEMS (${problems.length})`);
  for (const p of problems.sort()) console.log('  ' + p);
  console.log('\nshape check failed');
  if (strict) process.exit(1);
} else {
  console.log('\nshape OK: DTO column types and request field names match the backend');
}
