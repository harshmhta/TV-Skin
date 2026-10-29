const {test}=require('node:test');
const assert=require('node:assert/strict');
const {cleanPins}=require('../web/catalog.js');
test('legacy inputs, duplicate and uninstalled pins do not appear',()=>{
 assert.deepEqual(cleanPins(['youtube','xbox','youtube','missing','netflix'],new Set(['youtube','netflix']),[]),['youtube','netflix']);
});
test('a deliberately empty dock remains empty after restart',()=>{
 assert.deepEqual(cleanPins([],new Set(['youtube']),['youtube']),[]);
});
test('first run uses only available defaults and respects seven slots',()=>{
 const available=new Set(['a','b','c','d','e','f','g','h']);
 assert.deepEqual(cleanPins(null,available,['missing',...available]),['a','b','c','d','e','f','g']);
});
