'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');

const {
  sourceKey,
  playlistIdFromInput,
  playlistFingerprint,
} = require('../platform-playlist-link-import');

test('sourceKey 归一化平台别名', () => {
  assert.equal(sourceKey('netease'), 'netease');
  assert.equal(sourceKey('wy'), 'netease');
  assert.equal(sourceKey('QQ'), 'qq');
  assert.equal(sourceKey('tx'), 'qq');
  assert.equal(sourceKey('kugou'), 'kugou');
  assert.equal(sourceKey('kg'), 'kugou');
  assert.equal(sourceKey('unknown'), '');
  assert.equal(sourceKey(''), '');
  assert.equal(sourceKey(null), '');
});

test('playlistIdFromInput 从链接或纯数字提取歌单 ID', () => {
  assert.equal(playlistIdFromInput('https://music.163.com/#/playlist?id=123456789', 'netease'), '123456789');
  assert.equal(playlistIdFromInput('https://music.163.com/playlist/987654321', 'netease'), '987654321');
  assert.equal(playlistIdFromInput('123456789', 'netease'), '123456789');
  assert.equal(playlistIdFromInput('https://c.y.qq.com/base/fcgi-bin/u?__=abc&id=11223344', 'qq'), '11223344');
  assert.equal(playlistIdFromInput('https://www.kugou.com/special/single/55667788.html', 'kugou'), '55667788');
  assert.throws(() => playlistIdFromInput('https://example.com/not-a-playlist', 'netease'), /PLAYLIST_LINK_NOT_RECOGNIZED/);
});

test('playlistFingerprint 基于平台与歌曲身份生成稳定指纹', () => {
  const playlist = {
    id: 'p-1',
    songs: [
      { source: 'netease', id: '1', name: '晴天' },
      { source: 'qq', songmid: 'mid-2', name: '夜曲' },
    ],
  };
  const a = playlistFingerprint(playlist);
  const b = playlistFingerprint(playlist);
  assert.equal(a, b);
  assert.equal(a.length, 16);

  const changed = playlistFingerprint({
    id: 'p-1',
    songs: [{ source: 'netease', id: '2', name: '七里香' }],
  });
  assert.notEqual(a, changed);
});
