/* App-published artwork uses the existing stage and credit, with no artwork screensaver. */
const wait=ms=>new Promise(r=>setTimeout(r,ms));
const SOFT_W=320,SOFT_H=180,slots=[];
const NATIVE_BACKDROP=!!(window.Android&&typeof Android.nativeBackdrop==='function'&&Android.nativeBackdrop());
const nativeCover=document.createElement('div');nativeCover.id='native-cover';stage.insertBefore(nativeCover,$('#slots'));
function keepImageCover(img){
 nativeCover.style.transition='none';nativeCover.style.opacity='1';
 const copy=img.cloneNode();nativeCover.replaceChildren(copy);void nativeCover.offsetWidth;
 return copy.decode().catch(()=>{});
}
function nativeBackdropReady(key,visible){
 if(currentPreview?.key!==key)return;
 if(pendingPreview?.key===key&&pendingPreview.token===previewEpoch)pendingPreview.commit(visible);
 if(!visible){document.documentElement.classList.remove('native-backdrop');return;}
 warmUpcoming();
 if(document.documentElement.classList.contains('native-backdrop'))return;
 const token=previewEpoch;
 document.documentElement.classList.add('native-backdrop');
 nativeCover.style.transition='opacity 450ms ease-in';
 requestAnimationFrame(()=>{if(token===previewEpoch)nativeCover.style.opacity='0';});
}
if(NATIVE_BACKDROP){
 const svg=document.createElementNS('http://www.w3.org/2000/svg','svg');svg.setAttribute('width','0');svg.setAttribute('height','0');svg.innerHTML='<defs><clipPath id="nativeGlassClip" clipPathUnits="userSpaceOnUse"></clipPath></defs>';stage.appendChild(svg);
 const clip=svg.querySelector('clipPath');let clipShape='';
 setInterval(()=>{
  if(document.hidden)return;
  const scale=vp.clientWidth/1920,origin=stage.getBoundingClientRect(),areas=[];
  for(const el of document.querySelectorAll('.glass,.gear,.xpill')){
   const style=getComputedStyle(el);if(Number(style.opacity)<.01)continue;
   const r=el.getBoundingClientRect();if(!r.width||!r.height)continue;
   areas.push({x:(r.left-origin.left)/scale,y:(r.top-origin.top)/scale,width:r.width/scale,height:r.height/scale,rx:Math.min(parseFloat(style.borderRadius)||0,r.height/scale/2)});
  }
  const shape=JSON.stringify(areas);if(shape===clipShape)return;clipShape=shape;
  clip.replaceChildren(...areas.map(area=>{const rect=document.createElementNS('http://www.w3.org/2000/svg','rect');for(const [k,v] of Object.entries(area))rect.setAttribute(k,v);return rect;}));
 },32);
}
const Art={state:{overlay:false},setOverlay(on){this.state.overlay=on;$('#slots').classList.toggle('overlay',on);$('#dim').style.opacity=on?1:0;},refreshCredit(){},setSpeed(){},next(){}};
function setBlur(on){Art.setOverlay(on);}
let previewEpoch=0,previewTimer=null,previewWarmTimer=null,previewIndex=0,previewApp=null,previewFront=0;
let currentPreview=null,previewVideo=null,pendingPreview=null;
const previews={};
const previewLayers=[0,1].map(()=>{const e=document.createElement('div');e.className='preview-slot';$('#slots').appendChild(e);return e;});
function warmUpcoming(){
 if(!NATIVE_BACKDROP||typeof Android.prefetchBackdrop!=='function')return;
 clearTimeout(previewWarmTimer);
 const token=previewEpoch;
 previewWarmTimer=setTimeout(()=>{
  if(token!==previewEpoch||previewApp!==dockItems[dockIdx]?.id)return;
  const list=previews[previewApp]||[],next=list.length>1?list[(previewIndex+1)%list.length]?.key:null;
  const neighbor=dockItems[dockIdx+1],first=neighbor?.kind==='app'?(previews[neighbor.id]?.[0]?.key||'stock'):'stock';
  for(const key of new Set([first,next]))if(key&&key!==currentPreview?.key)Android.prefetchBackdrop(key);
 },200);
}
function stopPreview(){++previewEpoch;pendingPreview=null;clearTimeout(previewTimer);clearTimeout(previewWarmTimer);previewTimer=null;if(previewVideo){previewVideo.pause();previewVideo.removeAttribute('src');previewVideo.load();previewVideo=null;}for(const el of previewLayers){const v=el.querySelector('video');if(v){v.pause();v.removeAttribute('src');v.load();}}}
function showPreview(id,index=0){
  stopPreview();previewApp=id;previewIndex=index;
  const previousImage=previewLayers[previewFront].querySelector('img');
  if(NATIVE_BACKDROP&&!document.documentElement.classList.contains('native-backdrop')&&previousImage?.naturalWidth)keepImageCover(previousImage);
  const list=previews[id]?.length?previews[id]:[{key:'stock',title:'',art:'https://appassets.androidplatform.net/backgrounds/all-apps.jpg'}],item=list[index%Math.max(list.length,1)];currentPreview=item||null;
  if(item?.key==='stock')$('#credit').replaceChildren();
  const token=previewEpoch;
  if(!item){previewLayers.forEach(e=>{e.style.opacity='0';e.replaceChildren();});$('#credit').replaceChildren();return;}
  const next=1-previewFront,layer=previewLayers[next],img=new Image();
  img.onload=()=>{
    if(token!==previewEpoch)return;
    let committed=false;
    const commit=immediate=>{
      if(committed||token!==previewEpoch)return;committed=true;
      layer.style.transition=immediate?'none':'';previewLayers[previewFront].style.transition=immediate?'none':'';
      layer.replaceChildren(img);layer.dataset.programKey=item.key;layer.style.opacity='1';previewLayers[previewFront].style.opacity='0';previewFront=next;
      const b=document.createElement('b'),s=document.createElement('span');b.textContent=item.title;s.textContent=APPS[id]?.n||'';$('#credit').replaceChildren(b,s);if(item.key==='stock')$('#credit').replaceChildren();
      if(item.video){const v=document.createElement('video');previewVideo=v;v.muted=true;v.playsInline=true;v.autoplay=true;v.src=item.video;v.oncanplay=()=>{if(token===previewEpoch){layer.replaceChildren(v);v.play().catch(()=>{});}};v.load();}
      if(list.length>1&&zone!=='previews')previewTimer=setTimeout(()=>showPreview(id,index+1),12000);
    };
    const nativeVisible=NATIVE_BACKDROP&&document.documentElement.classList.contains('native-backdrop');
    pendingPreview={key:item.key,token,commit};
    if(!nativeVisible)commit(false);
    if(NATIVE_BACKDROP){const coverReady=nativeVisible?Promise.resolve():keepImageCover(img);coverReady.then(()=>requestAnimationFrame(()=>requestAnimationFrame(()=>{if(token===previewEpoch)Android.showBackdrop(item.key);})));}
  };
  img.onerror=()=>{if(token!==previewEpoch)return;if(item.fallbackArt&&img.src!==item.fallbackArt){img.src=item.fallbackArt;return;}if(index<list.length-1)showPreview(id,index+1);else{previewLayers.forEach(e=>e.style.opacity='0');$('#credit').replaceChildren();}};
  img.src=item.art;
}
document.addEventListener('visibilitychange',()=>{if(document.hidden)stopPreview();else showPreview(previewApp,previewIndex);});
