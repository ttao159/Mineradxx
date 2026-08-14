'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');

test('各 API 模块可加载并导出接口', () => {
  const mods = {
    'netease-std': require('../netease-std'),
    'kugou-api': require('../kugou-api'),
    'qishui-api': require('../qishui-api'),
    'qq-vip-api': require('../qq-vip-api'),
    'spotify-api': require('../spotify-api'),
    'dj-analyzer': require('../dj-analyzer'),
    'qishui-auth-v6': require('../qishui-auth-v6'),
    'qishui-qr-login': require('../qishui-qr-login'),
  };
  for (const [name, mod] of Object.entries(mods)) {
    assert.equal(typeof mod, 'object', `${name} 应导出对象`);
    assert.ok(Object.keys(mod).length > 0, `${name} 应有导出项`);
  }
});

test('qishuiCookieHasLogin 识别登录态 cookie', () => {
  const { qishuiCookieHasLogin } = require('../qishui-api');
  assert.equal(qishuiCookieHasLogin('sessionid=abc; path=/'), true);
  assert.equal(qishuiCookieHasLogin('sid_guard=xyz'), true);
  assert.equal(qishuiCookieHasLogin('uid_tt=123'), true);
  assert.equal(qishuiCookieHasLogin(''), false);
  assert.equal(qishuiCookieHasLogin('foo=bar; baz=qux'), false);
  assert.equal(qishuiCookieHasLogin(null), false);
});

test('combineQQVipResults 合并多源 VIP 判定', () => {
  const { combineQQVipResults } = require('../qq-vip-api');
  const positive = { decision: 'positive', isVip: true, vipType: 7, vipLevel: 'vip', isSvip: false };
  const negative = { decision: 'negative', isVip: false };
  const merged = combineQQVipResults([negative, positive]);
  assert.equal(merged.isVip, true);
  assert.equal(merged.vipLevel, 'vip');
  const empty = combineQQVipResults([negative]);
  assert.equal(empty.isVip, false);
  const none = combineQQVipResults([]);
  assert.equal(none.isVip, false);
});
