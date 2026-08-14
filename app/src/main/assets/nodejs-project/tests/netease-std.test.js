'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { handleStandardNetease, SUPPORTED_PATHS } = require('../netease-std');

test('SUPPORTED_PATHS 包含登录、搜索与歌曲关键路径', () => {
  const required = [
    '/login/qr/key',
    '/login/qr/create',
    '/login/qr/check',
    '/login/status',
    '/logout',
    '/search',
    '/cloudsearch',
    '/song/detail',
    '/song/url/v1',
    '/playlist/detail',
    '/lyric/new',
  ];
  for (const p of required) {
    assert.ok(SUPPORTED_PATHS.has(p), `缺少标准路径 ${p}`);
  }
});

test('handleStandardNetease 对非标准路径返回 false（交由后续路由）', async () => {
  const url = new URL('https://example.test/api/search?keywords=x');
  const res = { writeHead() {}, end() {} };
  const req = { method: 'GET' };
  assert.equal(await handleStandardNetease('/api/search', url, req, res), false);
});

test('handleStandardNetease 对标准路径 OPTIONS 请求返回 true 并写 200', async () => {
  const url = new URL('https://example.test/login/qr/key');
  let status = 0;
  const res = {
    writeHead(s) { status = s; },
    end() {},
  };
  const req = { method: 'OPTIONS' };
  assert.equal(await handleStandardNetease('/login/qr/key', url, req, res), true);
  assert.equal(status, 200);
});
