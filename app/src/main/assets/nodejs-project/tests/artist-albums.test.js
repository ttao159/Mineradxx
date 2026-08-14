'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');

const {
  normalizeNeteaseArtistAlbums,
  normalizeQQArtistAlbums,
} = require('../artist-albums-api');

test('规范化网易云艺人专辑到统一专辑契约', () => {
  const albums = normalizeNeteaseArtistAlbums({
    body: {
      hotAlbums: [{
        id: 101,
        name: '作品一号',
        artist: { name: '歌手甲' },
        picUrl: 'https://img.example/netease.jpg',
        publishTime: 1704067200000,
        size: 12,
      }],
    },
  }, 12);

  assert.deepEqual(albums, [{
    provider: 'netease',
    id: 101,
    name: '作品一号',
    artist: '歌手甲',
    cover: 'https://img.example/netease.jpg',
    releaseDate: '2024-01-01',
    trackCount: 12,
  }]);
});

test('规范化 QQ 艺人专辑并保留 albumMid 用于详情查询', () => {
  const albums = normalizeQQArtistAlbums({
    album: {
      data: {
        singer_name: '歌手乙',
        list: [{
          albumid: 202,
          album_mid: 'album-mid-202',
          album_name: '作品二号',
          pub_time: '2025-06-06',
          latest_song: { song_count: 9 },
        }],
      },
    },
  }, 12);

  assert.equal(albums.length, 1);
  assert.equal(albums[0].provider, 'qq');
  assert.equal(albums[0].id, 202);
  assert.equal(albums[0].albumMid, 'album-mid-202');
  assert.equal(albums[0].artist, '歌手乙');
  assert.equal(albums[0].releaseDate, '2025-06-06');
  assert.equal(albums[0].trackCount, 9);
  assert.match(albums[0].cover, /album-mid-202/);
});

test('丢弃不完整专辑并遵循响应数量上限', () => {
  const rows = Array.from({ length: 15 }, (_, index) => ({
    id: index + 1,
    name: `专辑 ${index + 1}`,
  }));
  rows.unshift({ id: '', name: '缺少身份' });
  rows.unshift({ id: 99, name: '' });
  const albums = normalizeNeteaseArtistAlbums({ hotAlbums: rows }, 8);
  assert.equal(albums.length, 8);
  assert.equal(albums[0].name, '专辑 1');
});
