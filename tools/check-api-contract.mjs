#!/usr/bin/env node
/**
 * Cross-checks every route declared in the Android client against the real
 * Cloudflare Pages Functions backend.
 *
 * The backend uses file-based routing: functions/api/<segment>/index.js serves
 * /api/<segment> and functions/api/<segment>/[id].js serves /api/<segment>/:id.
 * Each handler exports onRequestGet / onRequestPost / onRequestPut /
 * onRequestPatch / onRequestDelete. If a verb is not exported, the router
 * rejects it, so a Kotlin @PATCH against a PUT-only handler fails at runtime.
 *
 * Usage:
 *   node tools/check-api-contract.mjs            report and exit 0
 *   node tools/check-api-contract.mjs --strict   exit 1 on any mismatch
 */

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const ANDROID_ROOT = path.resolve(here, '..');
const WEB_ROOT = process.env.EFINA_WEB_ROOT || '/storage/emulated/0/git/efina';
const API_DIR = path.join(WEB_ROOT, 'functions', 'api');
const KOTLIN_API = path.join(
  ANDROID_ROOT,
  'app/src/main/java/com/efina/app/data/remote/api/EfinaApi.kt'
);

const strict = process.argv.includes('--strict');

/** Build { "GET /api/accounts": true, ... } from the backend source. */
function readBackendRoutes() {
  const routes = new Map();

  const walk = (dir) => {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
      const full = path.join(dir, entry.name);
      if (entry.isDirectory()) walk(full);
      else if (entry.name.endsWith('.js') && !entry.name.endsWith('.test.js')) {
        const rel = path.relative(API_DIR, full).split(path.sep);
        // Param directories like accounts/[id] map to :id in the URL.
        const segs = rel.slice(0, -1).map((seg) => {
          const m = seg.match(/^\[(.+)\]$/);
          return m ? ':' + m[1] : seg;
        });

        let route = null;
        if (entry.name === 'index.js') {
          route = '/api/' + segs.join('/');
        } else if (/^\[.+\]\.js$/.test(entry.name)) {
          route = '/api/' + [...segs, ':' + entry.name.replace(/^\[(.+)\]\.js$/, '$1')].join('/');
        } else if (!entry.name.startsWith('_')) {
          route = '/api/' + [...segs, entry.name.replace(/\.js$/, '')].join('/');
        }

        if (!route) continue;

        const src = fs.readFileSync(full, 'utf8');
        // The backend uses both styles:
        //   export async function onRequestGet(context) { ... }
        //   export const onRequestGet = async (context) => { ... }
        // Missing either one silently hides real routes.
        const verbs = [
          ...src.matchAll(/export\s+async\s+function\s+onRequest(Get|Post|Put|Patch|Delete)/g),
          ...src.matchAll(/export\s+const\s+onRequest(Get|Post|Put|Patch|Delete)\s*=/g),
        ].map((m) => m[1].toUpperCase());

        for (const verb of verbs) {
          routes.set(`${verb} ${route.replace(/\/$/, '')}`, full);
        }
      }
    }
  };

  walk(API_DIR);
  return routes;
}

/** Pull @GET("path") etc. out of the Retrofit interface. */
function readKotlinRoutes() {
  const src = fs.readFileSync(KOTLIN_API, 'utf8');
  const out = [];
  for (const m of src.matchAll(/@(GET|POST|PUT|PATCH|DELETE)\(\s*"([^"]*)"/g)) {
    out.push({ verb: m[1], raw: m[2] });
  }
  return out;
}

/** Normalise {id} (Retrofit) to :id (backend) and strip the base path. */
function normalise(raw) {
  let p = raw.replace(/\{([^}]+)\}/g, ':$1');
  if (!p.startsWith('/')) p = '/' + p;
  if (!p.startsWith('/api/')) p = '/api' + p;
  return p.replace(/\/$/, '');
}

const backend = readBackendRoutes();
const kotlin = readKotlinRoutes();

const ok = [];
const missing = [];
const wrongVerb = [];

for (const { verb, raw } of kotlin) {
  const p = normalise(raw);
  const key = `${verb} ${p}`;
  if (backend.has(key)) {
    ok.push(key);
    continue;
  }
  // Same path, different verb? Report which verbs the backend does accept.
  const alt = [...backend.keys()].filter((k) => k.endsWith(' ' + p)).map((k) => k.split(' ')[0]);
  if (alt.length) wrongVerb.push({ key, alt });
  else missing.push(key);
}

console.log(`backend routes: ${backend.size}`);
console.log(`android routes: ${kotlin.length}`);
console.log(`\nOK (${ok.length})`);
for (const k of ok.sort()) console.log('  ' + k);

if (wrongVerb.length) {
  console.log(`\nWRONG VERB (${wrongVerb.length})`);
  for (const { key, alt } of wrongVerb) {
    console.log(`  ${key}   -> backend accepts: ${alt.join(', ')}`);
  }
}

if (missing.length) {
  console.log(`\nNO SUCH ROUTE (${missing.length})`);
  for (const k of missing.sort()) console.log('  ' + k);
}

const problems = wrongVerb.length + missing.length;
console.log(
  problems === 0
    ? '\ncontract OK: every Android route exists with the right verb'
    : `\n${problems} contract problem(s)`
);

if (strict && problems) process.exit(1);
