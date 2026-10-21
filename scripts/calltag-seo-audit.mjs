#!/usr/bin/env node
/**
 * Inspect deployed Cloudflare Pages output, NOT just root source files.
 * Static check: 17 sitemap entries, canonical/indexability, root/web mirrors,
 * and dynamic SEO values injected by the active Pages Worker.
 * --live: inspect the public response (including Worker transformations).
 */
import { readFileSync, existsSync } from 'node:fs';
import { join } from 'node:path';

const origin = 'https://calltag.pagero.kr';
const live = process.argv.includes('--live');
const root = process.cwd();
let failures = 0;
const log = (ok, msg) => {
  console.log((ok ? 'PASS ' : 'FAIL ') + msg);
  if (!ok) failures++;
};
const read = path => readFileSync(join(root, path), 'utf8');
const canonical = html => {
  const tags = [...html.matchAll(/<link\b[^>]*>/gi)].map(m => m[0])
    .filter(tag => /\brel=["']canonical["']/i.test(tag));
  return { count: tags.length, href: tags[0]?.match(/\bhref=["']([^"']+)["']/i)?.[1] ?? null };
};
const title = html => html.match(/<title>([\s\S]*?)<\/title>/i)?.[1]?.trim() ?? '';
const description = html => html.match(/<meta\b[^>]*name=["']description["'][^>]*>/i)?.[0]?.match(/content=["']([^"']+)["']/i)?.[1] ?? '';
const h1 = html => html.match(/<h1\b[^>]*>([\s\S]*?)<\/h1>/i)?.[1]?.replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim() ?? '';
const hasNoindex = html => [...html.matchAll(/<meta\b[^>]*>/gi)].some(m => {
  const tag = m[0];
  return /\bname=["'](?:robots|googlebot)["']/i.test(tag) && /\bcontent=["'][^"']*noindex/i.test(tag);
});
const sitemapUrls = xml => [...xml.matchAll(/<loc>(https:\/\/calltag\.pagero\.kr\/[^<]*)<\/loc>/g)].map(m => m[1]);
const pathOf = url => new URL(url).pathname;
const sourcePath = (url, base = '') => {
  const path = pathOf(url);
  return base + (path === '/' ? 'index.html' : path.replace(/^\/|\/$/g, '') + '/index.html');
};

const sitemap = read('web/sitemap.xml');
const urls = sitemapUrls(sitemap);
log(urls.length === 17, 'Deployed sitemap lists 17 intended public URLs');
log(urls.length === new Set(urls).size, 'Deployed sitemap URLs are unique');
log(read('sitemap.xml') === sitemap, 'Root and deployed sitemap are identical');
log(read('robots.txt') === read('web/robots.txt'), 'Root and deployed robots are identical');
const robots = read('web/robots.txt');
log(robots.includes('Sitemap: ' + origin + '/sitemap.xml'), 'Deployed robots advertises sitemap');
log(!/^Disallow:\s*\/\s*$/m.test(robots), 'Deployed robots does not disallow site');
log(!/^Disallow:\s*\/guide\/?\s*$/m.test(robots), 'Deployed robots does not block guides');

const deployHome = read('web/index.html');
const sourceHome = read('index.html');
const worker = read('web/_worker.js');
const sourceWorker = read('_worker-core.js');
log(title(deployHome) === title(sourceHome), 'Root and deployed homepage title match');
log(description(deployHome) === description(sourceHome), 'Root and deployed homepage description match');
log(h1(deployHome) === h1(sourceHome), 'Root and deployed homepage H1 match');
log(worker.includes('const SEO_TITLE=' + JSON.stringify(title(deployHome)).replace(/"/g, "'") + ';'), 'Active Worker SEO_TITLE matches HTML title');
log(worker.includes('const SEO_DESCRIPTION=' + JSON.stringify(description(deployHome)).replace(/"/g, "'") + ';'), 'Active Worker SEO_DESCRIPTION matches HTML description');
log(sourceWorker.includes('const SEO_TITLE=' + JSON.stringify(title(deployHome)).replace(/"/g, "'") + ';'), 'Fallback Worker SEO_TITLE matches deployed HTML');
log(!worker.includes('visibility:hidden!important'), 'Active Worker does not conceal entire content until JS runs');
log(!sourceWorker.includes('visibility:hidden!important'), 'Fallback Worker does not conceal main content until JS runs');
const guideAnchorLinks = [...deployHome.matchAll(/<a\\b[^>]*href=["'](\\/guide\\/[^"']*)["']/gi)].map(m => m[1]);
log(guideAnchorLinks.includes('/guide/'), 'Deployed homepage links directly to guide hub');
log(guideAnchorLinks.length >= 10, 'Deployed homepage exposes crawlable guide links');
log(!deployHome.includes('3일 무료체험') && deployHome.includes('7일 무료'), 'Deployed trial terms match current 7-day policy');
log(!deployHome.includes('베타 테스트 참여자 후기를 준비'), 'No placeholder testimonials on deployed landing');
log(existsSync(join(root, 'web/account-deletion/index.html')), 'Deployed account-deletion policy exists');
log(deployHome.includes('href="/account-deletion/"'), 'Deployed account-deletion policy is linked');
log(worker.includes('stripSeo(body)'), 'Active Worker strips old homepage metadata before injection');
log(worker.includes('SEO_HEAD'), 'Active Worker includes generated homepage SEO metadata');

for (const url of urls) {
  const path = pathOf(url);
  const deployed = sourcePath(url, 'web/');
  const original = sourcePath(url);
  if (!existsSync(join(root, deployed))) {
    log(false, 'Missing deployed document: ' + deployed);
    continue;
  }
  const html = read(deployed);
  if (path !== '/' && existsSync(join(root, original))) {
    log(read(original) === html, 'Root/deploy guide mirror: ' + path);
  }
  const can = canonical(html);
  log(can.count === 1 && can.href === url, 'Deployed canonical: ' + path);
  log(!hasNoindex(html), 'Deployed index permitted: ' + path);
  log(Boolean(title(html)) && Boolean(h1(html)) && Boolean(description(html)), 'Deployed title, H1, description: ' + path);
}

async function get(url) {
  const response = await fetch(url, {
    redirect: 'follow',
    headers: { 'User-Agent': 'CallTag-SEO-Audit/2.0' },
    signal: AbortSignal.timeout(15000)
  });
  return { response, body: await response.text() };
}
if (live) {
  try {
    const { response, body } = await get(origin + '/robots.txt');
    log(response.status === 200 && body.includes('Sitemap: ' + origin + '/sitemap.xml'), 'LIVE robots 200 and sitemap reference');
  } catch (error) { log(false, 'LIVE robots: ' + error.message); }
  try {
    const { response, body } = await get(origin + '/sitemap.xml');
    const actual = sitemapUrls(body);
    log(response.status === 200 && urls.length === actual.length && urls.every(url => actual.includes(url)), 'LIVE sitemap exactly matches deployed source');
  } catch (error) { log(false, 'LIVE sitemap: ' + error.message); }

  for (let i = 0; i < urls.length; i += 4) {
    await Promise.all(urls.slice(i, i + 4).map(async url => {
      const path = pathOf(url);
      try {
        const { response, body } = await get(url);
        const can = canonical(body);
        const robotsHeader = response.headers.get('x-robots-tag') ?? '';
        const servedType = response.headers.get('content-type') ?? '';
        log(response.status === 200 && servedType.includes('text/html'), 'LIVE HTML 200: ' + path + ' (' + response.status + ')');
        log(pathOf(response.url) === path, 'LIVE no unexpected redirect: ' + path);
        log(can.count === 1 && can.href === url, 'LIVE single canonical: ' + path);
        log(!hasNoindex(body) && !/\bnoindex\b/i.test(robotsHeader), 'LIVE index permitted: ' + path);
        log(Boolean(title(body)) && Boolean(h1(body)) && Boolean(description(body)), 'LIVE SEO content present: ' + path);
        if (path === '/') {
          log(title(body) === title(deployHome), 'LIVE homepage title is current deployed title');
          log(description(body) === description(deployHome), 'LIVE homepage description is current deployed description');
        }
      } catch (error) { log(false, 'LIVE request ' + path + ': ' + error.message); }
    }));
  }
}

console.log('\nChecked ' + urls.length + ' URLs from web/, mode=' + (live ? 'live+static' : 'static') + ', failures=' + failures);
if (failures) process.exitCode = 1;
