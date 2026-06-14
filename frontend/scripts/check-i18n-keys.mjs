#!/usr/bin/env node
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";

const __dirname = dirname(fileURLToPath(import.meta.url));
const messagesDir = resolve(__dirname, "../src/i18n/messages");
const baseLocale = "en";
const targetLocales = ["zh-TW"];

function load(locale) {
  const path = resolve(messagesDir, `${locale}.json`);
  return JSON.parse(readFileSync(path, "utf8"));
}

function walk(node, prefix, out) {
  if (node === null || typeof node !== "object" || Array.isArray(node)) {
    out.set(prefix, typeof node);
    return;
  }
  for (const [key, value] of Object.entries(node)) {
    const next = prefix ? `${prefix}.${key}` : key;
    walk(value, next, out);
  }
}

function collect(tree) {
  const map = new Map();
  walk(tree, "", map);
  return map;
}

function diff(baseMap, targetMap) {
  const missingInTarget = [];
  const extraInTarget = [];
  const typeConflicts = [];

  for (const [key, type] of baseMap) {
    if (!targetMap.has(key)) {
      missingInTarget.push(key);
    } else if (targetMap.get(key) !== type) {
      typeConflicts.push({ key, base: type, target: targetMap.get(key) });
    }
  }
  for (const key of targetMap.keys()) {
    if (!baseMap.has(key)) extraInTarget.push(key);
  }
  return { missingInTarget, extraInTarget, typeConflicts };
}

function printSection(title, items, formatItem) {
  if (items.length === 0) return;
  console.log(`\n${title} (${items.length}):`);
  for (const item of items) console.log(`  - ${formatItem(item)}`);
}

const baseMap = collect(load(baseLocale));
let hasError = false;

for (const locale of targetLocales) {
  const targetMap = collect(load(locale));
  const { missingInTarget, extraInTarget, typeConflicts } = diff(baseMap, targetMap);

  console.log(`=== ${baseLocale} vs ${locale} ===`);
  console.log(`base keys: ${baseMap.size}, target keys: ${targetMap.size}`);

  printSection(
    `Missing in ${locale} (present in ${baseLocale})`,
    missingInTarget,
    (k) => k,
  );
  printSection(
    `Extra in ${locale} (absent in ${baseLocale})`,
    extraInTarget,
    (k) => k,
  );
  printSection(
    `Type conflicts`,
    typeConflicts,
    ({ key, base, target }) => `${key}: ${baseLocale}=${base}, ${locale}=${target}`,
  );

  if (
    missingInTarget.length > 0 ||
    extraInTarget.length > 0 ||
    typeConflicts.length > 0
  ) {
    hasError = true;
  } else {
    console.log(`OK — ${locale} matches ${baseLocale}`);
  }
}

process.exit(hasError ? 1 : 0);
