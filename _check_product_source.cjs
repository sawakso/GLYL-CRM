const http = require('http');
const JSEncryptMod = require('E:/CODE/fxiaoke-crm-5174/frontend/node_modules/jsencrypt');
const JSEncrypt = JSEncryptMod.JSEncrypt || JSEncryptMod.default || JSEncryptMod;

const BASE = 'http://127.0.0.1:8081';

function req(method, path, data, headers) {
  return new Promise((resolve, reject) => {
    const body = data ? JSON.stringify(data) : null;
    const u = new URL(BASE + path);
    const h = Object.assign({ 'Content-Type': 'application/json' }, headers || {});
    const r = http.request({ hostname: u.hostname, port: u.port, path: u.pathname, method, headers: h }, (res) => {
      let d = '';
      res.on('data', (c) => (d += c));
      res.on('end', () => resolve({ status: res.statusCode, body: d, cookie: res.headers['set-cookie'] }));
    });
    r.on('error', reject);
    if (body) r.write(body);
    r.end();
  });
}

(async () => {
  const keyRes = await req('GET', '/get-key', null);
  console.log('get-key', keyRes.status);
  const pub = JSON.parse(keyRes.body).data;
  console.log('pub-head', pub.slice(0, 80));
  const enc = new JSEncrypt();
  enc.setPublicKey('-----BEGIN PUBLIC KEY-----\n' + pub + '\n-----END PUBLIC KEY-----');
  const pwd = enc.encrypt('CordysCRM');
  const usr = enc.encrypt('admin');
  console.log('enc ok', !!pwd, !!usr);
  const loginRes = await req('POST', '/login', { username: usr, password: pwd, authenticate: 'LOCAL', platform: 'PC' });
  console.log('login', loginRes.status, loginRes.body.slice(0, 200));
  const lj = JSON.parse(loginRes.body);
  const sessionId = lj.data && lj.data.sessionId;
  const csrfToken = lj.data && lj.data.csrfToken;
  console.log('sessionId', sessionId, 'csrf', csrfToken && csrfToken.slice(0, 30));
  const auth = { 'X-AUTH-TOKEN': sessionId, 'CSRF-TOKEN': csrfToken, 'Organization-Id': '100001' };

  const il = await req('GET', '/is-login', null, auth);
  console.log('is-login', il.status, il.body.slice(0, 160));

  const srcRes = await req('POST', '/field/source/product', { current: 1, pageSize: 10 }, auth);
  console.log('source/product', srcRes.status);
  console.log('source/product resp', srcRes.body.slice(0, 1500));

  const pageRes = await req('POST', '/product/page', { current: 1, pageSize: 10 }, auth);
  console.log('product/page', pageRes.status);
  console.log('product/page resp', pageRes.body.slice(0, 1500));
})().catch((e) => console.error('ERR', e));
