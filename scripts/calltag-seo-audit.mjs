#!/usr/bin/env node
/**
 * CallTag indexability audit.
 * Local: validates every sitemap entry against the checked-out static HTML.
 * --live: also requests the public site and checks HTTP status, canonical and robots.
 * It does not attempt to claim or force Google indexing.
 */
import { readFileSync, existsSync } from 'node:fs';
import { join } from 'node:path';

const root = process.cwd();
const origin = 'https://calltag.pagero.kr';
const live = process.argv.includes('--live');
let failed = 0;

function log(ok, message) {
  console.log((ok ? 'PASS' : 'FAIL') + ' ' + message);
  if (!ok) failed++;
}
function read(path) {
  return readFileSync(join(root, path), 'utf8');
}
function canonical(html) {
  const tag = html.match(/<link\b[^>]*\brel=["']canonical["'][^>]*>/i)?.[0];
  return tag?.match(/\bhref=["']([^"']+)["']/i)?.[1] ?? null;
}
function title(html) {
  return html.match(/<title>([\s\S]*?)<\/title>/i)?.[1]?.trim() ?? null;
}
function isNoindex(html) {
  return [...html.matchAll(/<meta\b[^>]*>/gi)].some(m => {
    const tag = m[0];
    return /\bname=["'](?:robots|googlebot)["']/i.test(tag) && /\bcontent=["'][^"']*noindex/i.test(tag);
  });
}
const sitemap = read('sitemap.xml');
const entries = [...sitemap.matchAll(/<loc>(https:\/\/calltag\.pagero\.kr\/[^<]*)<\/loc>/g)].map(m => m[1]);
log(entries.length > 0, 'Sitemap contains at least one URL');
log(entries.length === new Set(entries).size, 'Sitemap URLs are unique');
log(!sitemap.includes('http://calltag.pagero.kr'), 'Sitemap uses HTTPS');
const robots = read('robots.txt');
log(robots.includes('Sitemap: ' + origin + '/sitemap.xml'), 'robots.txt advertises the correct sitemap');
log(!/^Disallow:\s*\/\s*$/m.test(robots), 'robots.txt does not block the site');

for (const url of entries) {
  const path = new URL(url).pathname;
  const file = path === '/' ? 'index.html' : path.replace(/^\/|\/$/g, '') + '/index.html';
  if (!existsSync(join(root, file))) {
    log(false, 'Missing indexed HTML source: ' + file);
    continue;
  }
  const html = read(file);
  log(canonical(html) === url, 'Canonical: ' + path);
  log(!isNoindex(html), 'Index allowed: ' + path);
  log(Boolean(title(html)), 'Title exists: ' + path);
  log(/<h1(?:\s|>)/i.test(html), 'H1 exists: ' + path);
}

async function get(url) {
  const response = await fetch(url, {
    headers: { 'User-Agent': 'CallTag-SEO-Audit/1.0 (public-indexability-check)' },
    redirect: 'follow',
    signal: AbortSignal.timeout(15000)
  });
  const body = await response.text();
  return { response, body };
}
if (live) {
  try {
    const remoteRobots = await get(origin + '/robots.txt');
    log(remoteRobots.response.ok && remoteRobots.body.includes('Sitemap: ' + origin + '/sitemap.xml'), 'LIVE robots.txt available');
  } catch (error) { log(false, 'LIVE robots.txt: ' + error.message); }
  try {
    const remoteSitemap = await get(origin + '/sitemap.xml');
    const remoteEntries = [...remoteSitemap.body.matchAll(/<loc>(https:\/\/calltag\.pagero\.kr\/[^<]*)<\/loc>/g)].map(m => m[1]);
    log(remoteSitemap.response.ok && entries.every(url => remoteEntries.includes(url)), 'LIVE sitemap contains all source URLs');
  } catch (error) { log(false, 'LIVE sitemap: ' + error.message); }

  for (let i = 0; i < entries.length; i += 4) {
    await Promise.all(entries.slice(i, i + 4).map(async url => {
      const path = new URL(url).pathname;
      try {
        const { response, body } = await get(url);
        const xRobots = response.headers.get('x-robots-tag') ?? '';
        const contentType = response.headers.get('content-type') ?? '';
        log(response.status === 200, 'LIVE HTTP 200: ' + path + ' (' + response.status + ')');
        log(contentType.includes('text/html'), 'LIVE HTML content type: ' + path);
        log(canonical(body) === url, 'LIVE canonical matches URL: ' + path);
        log(!/\bnoindex\b/i.test(xRobots) && !isNoindex(body), 'LIVE no noindex: ' + path);
        log(Boolean(title(body)) && /<h1(?:\s|>)/i.test(body), 'LIVE title + H1 rendered: ' + path);
      } catch (error) {
        log(false, 'LIVE request ' + path + ': ' + error.message);
      }
    }));
  }
}
console.log('\nURLs: ' + entries.length + ', mode: ' + (live ? 'local+live' : 'local') + ', failures: ' + failed);
if (failed) process.exitCode = 1;
