/* Android replaces platform actions, not the prototype's dock, grid or motion. */
let catalog=[],previewTiles=[],previewTileIndex=0,previewAccess=true;
const originalGlyph=glyph,originalKey=key,originalFocusDock=focusDock;
const escapeText=s=>String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
glyph=function(id,size){const a=APPS[id];if(a.ic||a.word||a.mono)return originalGlyph(id,size);const vector=a.pkg==='com.android.tv.settings'?ICONS.settings:WHITE_ICONS[a.pkg];if(vector)return `<span class="ic" style="width:${size}px;height:${size}px">${vector}</span>`;return `<div class="mono">${escapeText(a.n.split(/\s+/).map(w=>w[0]).slice(0,2).join(''))}</div>`;};
function savePins(){if(window.Android)Android.savePins(JSON.stringify(pinned));}
function setCatalog(data){
  cancelHold();if(zone==='edit')exitEdit();if(zone==='all')closeAll();if(zone==='previews')closePreviews();
  catalog=data.apps||[];
  for(const id of Object.keys(APPS))delete APPS[id];
  for(const app of catalog){APPS[app.id]={...BRANDS[app.id],n:app.name,icon:app.icon,pkg:app.pkg};}
  const available=new Set(catalog.map(a=>a.id));
  const defaults=DEFAULT_PROFILE.defaultPins.filter(id=>available.has(id));
  if(!defaults.length)defaults.push(...catalog.slice(0,5).map(a=>a.id));
  pinned=cleanPins(data.pins,available,defaults);
  for(const id of Object.keys(previews))delete previews[id];
  Object.assign(previews,data.previews||{});
  previewAccess=data.previewAccess!==false;
  buildDock(false);savePins();
  showPreview(dockItems[dockIdx]?.id);
  vp.focus();document.documentElement.dataset.ready='true';
}
focusDock=function(i,anim=true){originalFocusDock(i,anim);if(zone==='dock'&&previewApp!==dockItems[i].id)showPreview(dockItems[i].id);};
buildAll=function(){const ga=$('#gridApps');ga.replaceChildren();$('#gridSrc').replaceChildren();tiles=[];
  for(const app of catalog){const el=tileEl(app.id,escapeText(app.name),glyph(app.id,52));el.setAttribute('role','button');el.setAttribute('aria-label',app.name);ga.appendChild(el);tiles.push({id:app.id,el,name:app.name});}
};
const originalFocusTile=focusTile;
focusTile=function(i){if(!tiles.length)return;originalFocusTile(i);tiles[i].el.scrollIntoView({block:'nearest'});};
launch=function(id){if(!APPS[id]?.pkg)return;hideLabel();if(zone==='all')closeAll();stopPreview();if(window.Android)Android.launchApp(id);};
openPanel=function(){if(window.Android)Android.openSetting('inputs');};
for(const name of ['moveEdit','pinTile']){const fn=window[name];window[name]=function(...args){const result=fn(...args);savePins();return result;};}
const originalUnpin=unpinEdit;
unpinEdit=async function(){await originalUnpin();savePins();};
function closePreviews(){$('#programs')?.remove();$('#continue-scrim')?.remove();stage.classList.remove('preview-active');zone='dock';setBlur(false);focusDock(dockIdx);showPreview(previewApp,previewIndex);}
function focusProgram(i){
 previewTileIndex=(i+previewTiles.length)%previewTiles.length;
 const t=previewTiles[previewTileIndex],item=t.item;
 showPreview(t.id,previewTileIndex);
 const info=$('#programs');info.querySelector('.continue-caption').textContent=item.watchNext?'Continue watching':appName(t.id);
 info.querySelector('.continue-title').textContent=item.title;
 info.querySelector('.continue-play span').textContent=item.watchNext?'Play':'Open';
 info.querySelectorAll('.continue-dot').forEach((dot,j)=>dot.classList.toggle('selected',j===previewTileIndex));
}
function openPreviews(){const id=dockItems[dockIdx]?.id,list=previews[id]||[];
  if(!previewAccess){window.Android?.requestPreviewAccess?.();return;}
  if(!list.length){toast('This app has not shared any previews');return;}
  const selected=previewIndex%list.length;stopPreview();zone='previews';setBlur(false);hideLabel();stage.classList.add('preview-active');
  const shade=document.createElement('div');shade.id='continue-scrim';stage.insertBefore(shade,$('#top'));
  const panel=document.createElement('div');panel.id='programs';panel.className='continue-info';
  panel.innerHTML='<div class="continue-caption"></div><div class="continue-title"></div><div class="continue-actions"><div class="continue-play"><svg viewBox="0 0 24 24" width="28" height="28"><path fill="currentColor" d="m6 3 16 9-16 9z"/></svg><span>Play</span></div><div class="continue-dots"></div></div>';
  previewTiles=list.map(item=>({item,id}));
  list.forEach(()=>{const dot=document.createElement('span');dot.className='continue-dot';panel.querySelector('.continue-dots').appendChild(dot);});
  stage.appendChild(panel);focusDock(dockIdx,false);focusProgram(selected);
}
key=function(k){
  if(document.documentElement.dataset.ready!=='true')return;
  if(zone==='previews'){
    if(k==='back'||k==='down'){closePreviews();return;}
    if(k==='ok'){const t=previewTiles[previewTileIndex];if(window.Android)Android.openProgram(t.item.key);return;}
    if(k==='left'||k==='right')focusProgram(previewTileIndex+(k==='left'?-1:1));return;
  }
  if(zone==='gear'&&k==='ok'){if(window.Android)Android.openSetting('tv');return;}
  if(zone==='dock'&&k==='ok'&&dockItems[dockIdx].kind==='inputs'){if(window.Android)Android.openSetting('inputs');return;}
  if(zone==='dock'&&(k==='up'||k==='down')&&dockItems[dockIdx].kind==='app'){openPreviews();return;}
  originalKey(k);
};
function androidKey(k,action,repeat){
  if(document.documentElement.dataset.ready!=='true')return;
  if(k==='ok'){if(action==='down'&&!repeat)okDown();else if(action==='up')okUp();}
  else if(action==='down'){cancelHold();key(k);}
}
function androidPause(){cancelHold();stopPreview();headerPause();}
function androidResume(){cancelHold();vp.focus();showPreview(previewApp,previewIndex);headerResume();}
function androidHome(){cancelHold();if(document.documentElement.dataset.ready!=='true'){vp.focus();return;}if(zone==='previews')closePreviews();else if(zone==='all')closeAll();else if(zone==='edit')exitEdit();else if(zone==='gear'){zone='dock';$('#gear').classList.remove('f');}focusDock(dockIdx,false);vp.focus();}
if(window.Android)Android.refresh();
