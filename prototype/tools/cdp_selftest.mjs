// Real-time self-test: drives headless Chrome over the DevTools protocol (no virtual time),
// so CSS transitions actually run. Usage: node cdp_selftest.mjs FILE_URL RUNS
import { spawn } from 'node:child_process';

const [, , url, runsArg] = process.argv;
const runs = Number(runsArg || 2);
const chrome = spawn('/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
  ['--headless=new', '--remote-debugging-port=9333', '--window-size=1280,900', '--user-data-dir=/tmp/tvbuild/cdp-profile', 'about:blank'],
  { stdio: 'ignore' });
const sleep = ms => new Promise(r => setTimeout(r, ms));

let ver;
for (let i = 0; i < 50 && !ver; i++) {
  try { ver = await (await fetch('http://127.0.0.1:9333/json/version')).json(); } catch { await sleep(200); }
}
const ws = new WebSocket(ver.webSocketDebuggerUrl);
await new Promise(r => ws.addEventListener('open', r, { once: true }));
let id = 0; const pending = new Map();
ws.addEventListener('message', e => { const m = JSON.parse(e.data); if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); } });
const send = (method, params = {}, sessionId) => new Promise(r => { const i = ++id; pending.set(i, r); ws.send(JSON.stringify({ id: i, method, params, sessionId })); });

const { result: { targetId } } = await send('Target.createTarget', { url });
const { result: { sessionId } } = await send('Target.attachToTarget', { targetId, flatten: true });
const evaluate = async expr => (await send('Runtime.evaluate', { expression: expr, awaitPromise: true, returnByValue: true }, sessionId)).result?.result?.value;

await sleep(2500);
for (let r = 1; r <= runs; r++) {
  const res = await evaluate(`(async()=>{Art.state.repairs.length=0;const p=await selfTest(160);return {problems:p,repairs:Art.state.repairs.slice(),loads:Art.state.loadLog.slice(-4)};})()`);
  console.log(`run ${r}:`, JSON.stringify(res));
}
ws.close(); chrome.kill();
