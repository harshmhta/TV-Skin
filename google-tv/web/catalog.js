function cleanPins(saved, available, defaults, max=7) {
  const source=Array.isArray(saved)?saved:defaults;
  return [...new Set(source)].filter(id=>available.has(id)).slice(0,max);
}
if(typeof module!=='undefined')module.exports={cleanPins};
