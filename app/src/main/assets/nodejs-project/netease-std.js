'use strict';

// 标准 NeteaseCloudMusicApi 接口适配层。
// Mineradio 桌面版后端使用 /api/* 前缀的封装路由；Folia 歌词舞台直接调用
// NeteaseCloudMusicApi 原生协议（无 /api/ 前缀，cookie 走 query 参数）。
// 本模块在 server.js 静态资源处理之前挂载，把标准路径代理到 NCM 模块函数。

const NCM = require('NeteaseCloudMusicApi');

const SUPPORTED_PATHS = new Set([
  '/album',
  '/album/detail/dynamic',
  '/album/sub',
  '/album/sublist',
  '/artist/album',
  '/artist/detail',
  '/artist/songs',
  '/artist/top/song',
  '/cloud/lyric/get',
  '/cloudsearch',
  '/fm_trash',
  '/history/recommend/songs',
  '/history/recommend/songs/detail',
  '/like',
  '/likelist',
  '/login/qr/check',
  '/login/qr/create',
  '/login/qr/key',
  '/login/status',
  '/logout',
  '/lyric/new',
  '/personal_fm',
  '/personalized',
  '/playlist/detail',
  '/playlist/detail/dynamic',
  '/playlist/subscribe',
  '/playlist/track/all',
  '/playlist/tracks',
  '/recommend/songs',
  '/recommend/songs/dislike',
  '/register/anonimous',
  '/search',
  '/song/chorus',
  '/song/copyright/rcmd',
  '/song/detail',
  '/song/url/v1',
  '/user/account',
  '/user/cloud',
  '/user/cloud/detail',
  '/user/playlist',
]);

function pathToFunctionName(pn) {
  // /song/url/v1 -> song_url_v1 ; /cloudsearch -> cloudsearch
  return String(pn).replace(/^\//, '').split('/').filter(Boolean).join('_');
}

function sendStdJSON(res, data, status) {
  res.writeHead(status || 200, {
    'Content-Type': 'application/json; charset=utf-8',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET,POST,OPTIONS,PUT,DELETE',
    'Access-Control-Allow-Headers': 'Content-Type, Accept, X-Requested-With',
    'Cache-Control': 'no-store, no-cache, must-revalidate',
    'Pragma': 'no-cache',
    'Expires': '0',
  });
  res.end(JSON.stringify(data));
}

/**
 * 处理标准 NCM 接口请求。
 * @returns {Promise<boolean>} 是否已处理（false 表示不是标准接口，交给后续路由）
 */
async function handleStandardNetease(pn, url, req, res) {
  if (!SUPPORTED_PATHS.has(pn)) return false;

  if (req.method === 'OPTIONS') {
    sendStdJSON(res, { code: 200 });
    return true;
  }

  const fnName = pathToFunctionName(pn);
  const fn = NCM[fnName];
  if (typeof fn !== 'function') {
    sendStdJSON(res, { code: 404, msg: 'module not found: ' + fnName }, 404);
    return true;
  }

  const params = {};
  for (const [k, v] of url.searchParams.entries()) {
    if (k === 'cookie') continue;
    params[k] = v;
  }
  const cookie = url.searchParams.get('cookie') || '';

  try {
    const result = await fn({ ...params, cookie, timestamp: Date.now() });
    sendStdJSON(res, result.body && typeof result.body === 'object' ? result.body : { code: 200, ...result });
  } catch (err) {
    console.error('[netease-std]', pn, err && err.message || err);
    sendStdJSON(res, { code: 500, msg: String(err && err.message || err) }, 500);
  }
  return true;
}

module.exports = { handleStandardNetease, SUPPORTED_PATHS };
