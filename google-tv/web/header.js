/* Clock and weather are part of the installed launcher, including their reset defaults. */
function clockParts(hours,minutes){return {time:`${hours%12||12}:${String(minutes).padStart(2,'0')}`,period:hours>=12?'PM':'AM'};}
function weatherCondition(code,isDay){
 if(code===0)return {icon:isDay?'sun':'moon',label:'Clear'};
 if(code<=2)return {icon:isDay?'partly':'moon',label:code===1?'Mostly clear':'Partly cloudy'};
 if(code===3)return {icon:'cloud',label:'Cloudy'};
 if(code===45||code===48)return {icon:'fog',label:'Fog'};
 if([71,73,75,77,85,86].includes(code))return {icon:'snow',label:'Snow'};
 if(code>=95)return {icon:'storm',label:'Thunderstorms'};
 return {icon:'rain',label:code<60?'Drizzle':'Rain'};
}
function parseWeather(response,now=Date.now()){
 const c=response?.current;
 if(!c||!Number.isFinite(c.temperature_2m)||c.temperature_2m< -100||c.temperature_2m>70||!Number.isInteger(c.weather_code)||![0,1].includes(c.is_day)||response.current_units?.temperature_2m!=='°C')throw Error('Weather unavailable');
 return {temperature:c.temperature_2m,code:c.weather_code,isDay:c.is_day===1,updatedAt:now};
}
if(typeof module!=='undefined')module.exports={clockParts,weatherCondition,parseWeather};
if(typeof document!=='undefined'){
 const clock=document.querySelector('#clock');
 clock.innerHTML='<span id="clock-time"></span><span id="clock-period"></span>';
 window.tick=function(){const p=clockParts(new Date().getHours(),new Date().getMinutes());if(document.querySelector('#clock-time').textContent!==p.time)document.querySelector('#clock-time').textContent=p.time;if(document.querySelector('#clock-period').textContent!==p.period)document.querySelector('#clock-period').textContent=p.period;};
 const weather=document.querySelector('#weather'),config=DEFAULT_PROFILE.weather;
 const locationKey=`${config.latitude},${config.longitude},${config.temperatureUnit}`;
 const paths={
  sun:'<circle cx="12" cy="12" r="4"/><path d="M12 2v2m0 16v2M2 12h2m16 0h2M5 5l1.5 1.5m11 11L19 19M5 19l1.5-1.5m11-11L19 5"/>',
  moon:'<path d="M20.8 13.5A9 9 0 0 1 10.5 3.2a9 9 0 1 0 10.3 10.3Z"/>',
  cloud:'<path d="M6 18a4 4 0 1 1 1-7.9A6 6 0 0 1 18.7 10 4 4 0 0 1 18 18Z"/>',
  partly:'<path d="M8 2v2M2 8h2m.5-3.5L6 6m7-1.5L11.5 6"/><circle cx="8" cy="8" r="3"/><path d="M9 20a4 4 0 0 1-.6-8A5 5 0 0 1 18 12a4 4 0 1 1 0 8Z"/>',
  fog:'<path d="M5 12a4 4 0 0 1 2-7.9A6 6 0 0 1 19 7a3 3 0 0 1 1 5M3 16h18M5 20h14"/>',
  rain:'<path d="M6 14a4 4 0 1 1 1-7.9A6 6 0 0 1 18.7 6 4 4 0 0 1 18 14M7 18l-1 3m7-3-1 3m7-3-1 3"/>',
  snow:'<path d="M6 14a4 4 0 1 1 1-7.9A6 6 0 0 1 18.7 6 4 4 0 0 1 18 14M8 18v4m-2-2h4m6-2v4m-2-2h4"/>',
  storm:'<path d="M6 14a4 4 0 1 1 1-7.9A6 6 0 0 1 18.7 6 4 4 0 0 1 18 14M13 13l-4 6h4l-2 4"/>'
 };
 let currentWeather=null,weatherRequest=null,weatherTimer=null,headerActive=true;
 function renderWeather(offline=false){
  weather.hidden=false;
  if(!currentWeather){weather.replaceChildren();const label=document.createElement('span');label.className='weather-unavailable';label.textContent='Weather unavailable';weather.appendChild(label);return;}
  const condition=weatherCondition(currentWeather.code,currentWeather.isDay);
  weather.innerHTML=`<svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">${paths[condition.icon]}</svg><span class="weather-temperature"></span><span class="weather-city"></span>`;
  weather.querySelector('.weather-temperature').textContent=`${Math.round(currentWeather.temperature)}°C`;
  weather.querySelector('.weather-city').textContent=config.city+(offline?' · Offline':'');
  weather.setAttribute('aria-label',`${config.city}, ${condition.label}, ${Math.round(currentWeather.temperature)} degrees Celsius${offline?', last known weather':''}`);
  weather.title='Weather data: Open-Meteo';
 }
 try{const cached=JSON.parse(window.Android?.loadWeather?.()||'null');if(cached?.locationKey===locationKey&&Number.isFinite(cached.temperature)&&Date.now()-cached.updatedAt<6*3600000){currentWeather=cached;renderWeather(Date.now()-cached.updatedAt>3600000);}}catch{}
 async function refreshWeather(){
  clearTimeout(weatherTimer);if(!headerActive||weatherRequest)return;
  if(currentWeather&&Date.now()-currentWeather.updatedAt<15*60000){weatherTimer=setTimeout(refreshWeather,15*60000-(Date.now()-currentWeather.updatedAt));return;}
  const request=new AbortController();weatherRequest=request;const timeout=setTimeout(()=>request.abort(),6000);
  try{
   const url=new URL('https://api.open-meteo.com/v1/forecast');
   for(const [k,v]of Object.entries({latitude:config.latitude,longitude:config.longitude,current:'temperature_2m,weather_code,is_day',temperature_unit:config.temperatureUnit,timezone:config.timezone,forecast_days:1}))url.searchParams.set(k,v);
   const result=await fetch(url,{signal:request.signal});if(!result.ok)throw Error('Weather unavailable');
   currentWeather={...parseWeather(await result.json()),locationKey};renderWeather();window.Android?.saveWeather?.(JSON.stringify(currentWeather));
  }catch(e){if(headerActive)renderWeather(true);}finally{clearTimeout(timeout);if(weatherRequest===request)weatherRequest=null;if(headerActive)weatherTimer=setTimeout(refreshWeather,currentWeather?15*60000:5*60000);}
 }
 window.headerPause=function(){headerActive=false;clearTimeout(weatherTimer);weatherRequest?.abort();weatherRequest=null;};
 window.headerResume=function(){headerActive=true;tick();refreshWeather();};
 tick();setInterval(tick,10000);headerResume();
}
