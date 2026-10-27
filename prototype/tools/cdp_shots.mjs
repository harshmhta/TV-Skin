// Real-time screenshots of key states. Usage: node cdp_shots.mjs FILE_URL OUT_DIR
import { spawn } from 'node:child_process';
import { writeFileSync } from 'node:fs';
const [, , url, outDir] = process.argv;
const chrome = spawn('/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
  ['--headless=new', '--remote-debugging-port=9334', '--window-size=1280,760', '--hide-scrollbars', '--user-data-dir=/tmp/tvbuild/cdp-profile2', 'about:blank'], { stdio: 'ignore' });
const sleep = ms => new Promise(r => setTimeout(r, ms));
let ver; for (let i = 0; i < 50 && !ver; i++) { try { ver = await (await fetch('http://127.0.0.1:9334/json/version')).json(); } catch { await sleep(200); } }
const ws = new WebSocket(ver.webSocketDebuggerUrl); await new Promise(r => ws.addEventListener('open', r, { once: true }));
let id = 0; const pending = new Map();
ws.addEventListener('message', e => { const m = JSON.parse(e.data); if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); } });
const send = (method, params = {}, sessionId) => new Promise(r => { const i = ++id; pending.set(i, r); ws.send(JSON.stringify({ id: i, method, params, sessionId })); });
const { result: { targetId } } = await send('Target.createTarget', { url });
const { result: { sessionId } } = await send('Target.attachToTarget', { targetId, flatten: true });
const ev = async x => (await send('Runtime.evaluate', { expression: x, awaitPromise: true, returnByValue: true }, sessionId)).result?.result?.value;
const shot = async name => { const r = await send('Page.captureScreenshot', { format: 'png', clip: { x: 24, y: 24, width: 1232, height: 693, scale: 0.6 } }, sessionId); writeFileSync(`${outDir}/${name}.png`, Buffer.from(r.result.data, 'base64')); };
await sleep(2500);
await ev(`openAll()`); await sleep(900); await shot('1-allapps');
await ev(`closeAll();setRender('tv')`); await sleep(900); await ev(`openAll()`); await sleep(900); await shot('2-allapps-tv');
await ev(`closeAll()`); await sleep(900);
await ev(`(async()=>{Art.next();for(let i=0;i<300&&Art.state.phase!=='in';i++)await new Promise(r=>setTimeout(r,20));openAll();})()`); await sleep(2600); await shot('3-allapps-opened-mid-change-tv');
await ev(`closeAll()`); await sleep(1200); await shot('4-home-tv');
const info = await ev(`({phase:Art.state.phase,overlay:Art.state.overlay,repairs:Art.state.repairs.length,softDrawn:Art.layers.map(l=>{const d=l.soft.getContext('2d').getImageData?null:null;return l.ver})})`);
console.log(JSON.stringify(info));
ws.close(); chrome.kill();
