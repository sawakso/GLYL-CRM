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
      res.setEncoding('utf8');
      res.on('data', (c) => (d += c));
      res.on('end', () => resolve({ status: res.statusCode, body: d }));
    });
    r.on('error', reject);
    if (body) r.write(body);
    r.end();
  });
}

(async () => {
  const keyRes = await req('GET', '/get-key', null);
  const pub = JSON.parse(keyRes.body).data;
  const enc = new JSEncrypt();
  enc.setPublicKey('-----BEGIN PUBLIC KEY-----\n' + pub + '\n-----END PUBLIC KEY-----');
  const loginRes = await req('POST', '/login', {
    username: enc.encrypt('admin'),
    password: enc.encrypt('CordysCRM'),
    authenticate: 'LOCAL',
    platform: 'PC',
  });
  const lj = JSON.parse(loginRes.body);
  const auth = {
    'X-AUTH-TOKEN': lj.data.sessionId,
    'CSRF-TOKEN': lj.data.csrfToken,
    'Organization-Id': '100001',
  };

  const cfg = await req('GET', '/field/source/config/PRODUCT', null, auth);
  console.log('config status', cfg.status);
  const cj = JSON.parse(cfg.body);
  const fields = (cj.data && cj.data.fields) || [];
  console.log('fields count', fields.length);
  fields.forEach((f) => {
    console.log(
      JSON.stringify({
        id: f.id,
        name: f.name,
        type: f.type,
        businessKey: f.businessKey,
        internalKey: f.internalKey,
        resourceFieldId: f.resourceFieldId,
      })
    );
  });
})().catch((e) => console.error('ERR', e));
