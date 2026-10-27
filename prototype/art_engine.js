/* ---------- artwork engine ----------
   Rules that keep it robust (same design will ship to the TV):
   1. One state object. render() derives every layer's target opacity from it, so overlapping
      requests (artwork change + All apps opening/closing) can never fight over a layer.
   2. Every async step carries an epoch token; a cancelled or stale step never touches the screen.
   3. Images are decoded before they're shown; ones that fail or time out are skipped.
   4. Only transform and opacity animate (compositor-only; the TV can do these cheaply).
   5. A watchdog repairs any stuck phase or inconsistent layer; timers pause while the page is hidden. */
const ART=[
 {f:'wheatfield.jpg',crop:1.12,t:'Wheat Field with Cypresses',m:'Van Gogh, 1889 · The Met'},
 {f:'greatwave.jpg',crop:1.0,t:'The Great Wave',m:'Hokusai, ca. 1830 · The Met'},
 {f:'irises.jpg',crop:1.12,t:'Irises',m:'Van Gogh, 1890 · The Met'}];
const LONG_TITLE={t:'Under the Wave off Kanagawa (Kanagawa oki nami ura), also known as The Great Wave',m:'Hokusai · The Met'};
const ART_HOLD={fast:8000,real:120000};
const PH={out:1400,swap:700,in:1600},OVERLAY_IN=400,OVERLAY_OUT=500,ZOOM=1.045,LOAD_TIMEOUT=8000;
const REDUCED=matchMedia('(prefers-reduced-motion: reduce)').matches;
// The blurred copy is drawn ONCE per artwork into a tiny canvas and stretched to full screen.
// A full-size image with a live CSS blur costs a lot of GPU memory; when Chrome runs short it silently
// skips drawing it (that's the "sometimes no blur"). The TV build ships these as small pre-blurred files.
const SOFT_W=320,SOFT_H=180;
function drawSoft(canvas,img){const c=canvas.getContext('2d'),W=canvas.width,H=canvas.height;
 const ir=img.naturalWidth/img.naturalHeight,cr=W/H,pad=0.12;               // object-fit: cover, with overscan so blurred edges never show
 let w,h;if(ir>cr){h=H*(1+pad);w=h*ir;}else{w=W*(1+pad);h=w/ir;}
 c.clearRect(0,0,W,H);c.filter='blur(5px) saturate(1.1)';c.drawImage(img,(W-w)/2,(H-h)/2,w,h);c.filter='none';}
const wait=ms=>new Promise(r=>setTimeout(r,ms));
function credit(i){if(i<0)return;const a=(scen.long&&i===1)?LONG_TITLE:ART[i];$('#credit').innerHTML=`<b>${a.t}</b><span>${a.m}</span>`;}

const Art=(()=>{
 const layers=[0,1].map(()=>{const el=document.createElement('div');el.className='slot';
  el.innerHTML=`<div class="drift"><img class="sharp" alt=""><canvas class="soft" width="${SOFT_W}" height="${SOFT_H}"></canvas></div>`;$('#slots').appendChild(el);
  return{el,drift:el.querySelector('.drift'),sharp:el.querySelector('.sharp'),soft:el.querySelector('.soft'),anim:null,idx:-1,crop:1,ver:0};});
 const st={front:0,idx:-1,phase:'boot',phaseAt:performance.now(),overlay:false,epoch:0,queued:false,bad:new Set(),
  speed:'fast',timer:null,hidden:document.hidden,lastRender:0,repairs:[],loadLog:[]};
 const EASE_IN='cubic-bezier(.5,0,.75,0)',EASE_OUT='cubic-bezier(.2,.8,.2,1)';
 const num=(el,p='opacity')=>parseFloat(getComputedStyle(el)[p]);
 // set an opacity target; ms=0 snaps. Re-setting the same target is a no-op unless forced.
 function op(el,v,ms,ease='ease',force=false){const t=String(v);
  if(!force&&el.style.opacity===t)return;
  el.style.transition=ms?`opacity ${ms}ms ${ease}`:'none';
  if(force&&el.style.opacity===t){el.style.opacity=String(1-v);void el.offsetWidth;}
  el.style.opacity=t;if(!ms)void el.offsetWidth;}
 const setPhase=p=>{st.phase=p;st.phaseAt=performance.now();};
 const softTarget=()=>(st.phase==='out'||st.phase==='swap'||st.overlay)?1:0;
 function render(ms){const F=layers[st.front],B=layers[1-st.front],s=softTarget();
  op(F.soft,s,ms!=null?ms:(s?OVERLAY_IN:OVERLAY_OUT),s?EASE_IN:EASE_OUT);   // no ?? : TV browser is Chromium 69
  if(st.phase==='idle'||st.phase==='loading'){op(F.el,1,0);op(B.el,0,0);}   // the swap phase owns its own crossfade
  op($('#dim'),st.overlay?1:0,st.overlay?OVERLAY_IN:OVERLAY_OUT,EASE_OUT);
  st.lastRender=performance.now();}
 function showCredit(on){$('#credit').style.opacity=on?1:0;}
 function drift(L,fromCurrent=false){
  let from=L.crop;
  if(fromCurrent){const m=getComputedStyle(L.drift).transform;const v=m&&m!=='none'?parseFloat(m.replace(/^matrix(3d)?\(/,'')):NaN;if(v>0)from=v;}
  if(L.anim){L.anim.cancel();L.anim=null;}
  if(REDUCED){L.drift.style.transform=`translateZ(0) scale(${L.crop})`;return;}
  const to=L.crop*ZOOM,total=ART_HOLD[st.speed]+PH.out+PH.swap+PH.in;
  const frac=Math.min(1,Math.max(0,(to-from)/(to-L.crop||1)));
  L.anim=L.drift.animate([{transform:`translateZ(0) scale(${from})`},{transform:`translateZ(0) scale(${to})`}],
   {duration:Math.max(1000,total*frac),easing:'linear',fill:'forwards'});}
 // Test each file on a throwaway Image first; only a confirmed-good file is handed to a visible layer.
 // (Reusing one <img> after a failed load can leave later decodes hanging, so the layers never see broken files.)
 function loadInto(L,i,token){const a=ART[i],url=ART_BASE+a.f;return new Promise(res=>{let done=false;
  const fin=(ok,why)=>{if(done)return;done=true;clearTimeout(to);
   st.loadLog.push(`${a.f}:${ok?'ok':why}`);if(st.loadLog.length>20)st.loadLog.shift();res(ok&&token===st.epoch);};
  const to=setTimeout(()=>fin(false,'timeout'),LOAD_TIMEOUT);
  const probe=new Image();probe.decoding='async';
  probe.onerror=()=>fin(false,'error');
  probe.onload=()=>{if(done||token!==st.epoch)return fin(false,'cancelled');
   L.crop=a.crop;L.idx=i;L.sharp.src=url;drawSoft(L.soft,probe);L.ver++;   // sharp now served from cache; soft drawn once
   const d=L.sharp.decode?L.sharp.decode().catch(()=>{}):Promise.resolve();
   Promise.race([d,wait(1500)]).then(()=>fin(true));};
  probe.src=url;});}
 async function pickNext(token){const n=ART.length;
  for(let k=1;k<=n;k++){const i=(st.idx+k+n)%n;if(st.bad.has(i)&&st.bad.size<n)continue;
   const ok=await loadInto(layers[1-st.front],i,token);if(token!==st.epoch)return null;if(ok)return i;st.bad.add(i);}
  return null;}
 const sleep=async(ms,token)=>{await wait(ms);return token===st.epoch;};
 function schedule(){clearTimeout(st.timer);st.timer=null;if(st.hidden||st.phase!=='idle')return;
  st.timer=setTimeout(()=>{st.timer=null;advance();},ART_HOLD[st.speed]);}
 async function advance(){
  if(st.phase!=='idle'){st.queued=true;return;}
  clearTimeout(st.timer);st.timer=null;const token=++st.epoch;
  setPhase('loading');const next=await pickNext(token);if(token!==st.epoch)return;
  if(next==null){setPhase('idle');render();schedule();return;}          // nothing loadable: keep the current artwork
  const F=layers[st.front],I=layers[1-st.front];
  setPhase('out');showCredit(false);render(PH.out);
  if(!await sleep(PH.out,token))return;
  setPhase('swap');op(I.soft,1,0);drift(I);op(I.el,1,PH.swap);op(F.el,0,PH.swap);
  if(!await sleep(PH.swap,token))return;
  st.front=1-st.front;st.idx=next;credit(next);showCredit(true);
  setPhase('in');render(PH.in);
  if(!await sleep(PH.in,token))return;
  if(F.anim){F.anim.cancel();F.anim=null;}op(F.soft,0,0);               // tidy the now-hidden layer
  setPhase('idle');render();schedule();
  if(st.queued){st.queued=false;advance();}}
 // force a clean idle state from wherever we are (used by the watchdog)
 function settle(reason){st.epoch++;st.queued=false;
  if(st.phase==='swap'){const I=layers[1-st.front];if(I.idx>=0&&num(I.el)>=num(layers[st.front].el)){st.front=1-st.front;st.idx=I.idx;}}
  const F=layers[st.front],B=layers[1-st.front];
  op(F.el,1,0,'ease',true);op(B.el,0,0,'ease',true);op(B.soft,0,0);if(B.anim){B.anim.cancel();B.anim=null;}
  setPhase('idle');op(F.soft,st.overlay?1:0,300,EASE_OUT,true);render();
  if(!F.anim)drift(F,true);credit(st.idx);showCredit(true);schedule();
  st.repairs.push(`${new Date().toLocaleTimeString()} ${reason}`);console.warn('[art] repaired:',reason);}
 setInterval(()=>{if(st.phase==='boot')return;
  const age=performance.now()-st.phaseAt;
  const lim={loading:LOAD_TIMEOUT*ART.length+2000,out:PH.out+2000,swap:PH.swap+2000,in:PH.in+2000}[st.phase];
  if(lim&&age>lim)return settle('stuck in '+st.phase);
  if(st.phase!=='idle')return;
  const F=layers[st.front],B=layers[1-st.front];
  if(F.el.style.opacity!=='1'||B.el.style.opacity!=='0')return settle('layer targets out of sync');
  if(performance.now()-st.lastRender>1500){
   const want=st.overlay?1:0;if(Math.abs(num(F.soft)-want)>0.02){op(F.soft,want,300,EASE_OUT,true);st.repairs.push('blur level corrected');}
   if(num(F.el)<0.98||num(B.el)>0.02)settle('layer visibility out of sync');}
  if(!st.hidden&&!st.timer)schedule();},1000);
 document.addEventListener('visibilitychange',()=>{st.hidden=document.hidden;
  if(st.hidden){clearTimeout(st.timer);st.timer=null;}else if(st.phase==='idle')schedule();});
 async function boot(){const token=++st.epoch;
  for(let k=0;k<ART.length;k++){const L=layers[0];const ok=await loadInto(L,k,token);
   if(ok){st.front=0;st.idx=k;op(L.el,1,600);drift(L);credit(k);showCredit(true);setPhase('idle');render();schedule();return;}
   st.bad.add(k);}
  setPhase('idle');showCredit(false);}                                    // no artwork at all: the stage's dark ground stays
 boot();
 return{layers,state:st,
  next(){st.phase==='idle'?advance():(st.queued=true);},
  setOverlay(on){on=!!on;if(on===st.overlay)return;st.overlay=on;render();},
  setSpeed(s){st.speed=s;drift(layers[st.front],true);if(st.phase==='idle')schedule();},
  refreshCredit(){credit(st.idx);},settle};
})();
const slots=Art.layers;
function setBlur(on){Art.setOverlay(on);}
$('#next').onclick=()=>{Art.next();vp.focus();};
document.querySelectorAll('[data-speed]').forEach(b=>b.onclick=()=>{
 document.querySelectorAll('[data-speed]').forEach(x=>x.classList.toggle('on',x===b));Art.setSpeed(b.dataset.speed);vp.focus();});

