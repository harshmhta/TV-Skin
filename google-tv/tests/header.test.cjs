const {test}=require('node:test');
const assert=require('node:assert/strict');
const {clockParts,weatherCondition,parseWeather}=require('../web/header.js');
test('12-hour clock distinguishes midnight, noon and late evening',()=>{
 assert.deepEqual(clockParts(0,5),{time:'12:05',period:'AM'});
 assert.deepEqual(clockParts(12,0),{time:'12:00',period:'PM'});
 assert.deepEqual(clockParts(23,59),{time:'11:59',period:'PM'});
});
test('weather uses actual Celsius data and rejects missing or differently scaled temperatures',()=>{
 assert.deepEqual(parseWeather({current:{temperature_2m:14.2,weather_code:3,is_day:0},current_units:{temperature_2m:'°C'}},123),{temperature:14.2,code:3,isDay:false,updatedAt:123});
 assert.throws(()=>parseWeather({current:{temperature_2m:null,weather_code:0,is_day:1},current_units:{temperature_2m:'°C'}}));
 assert.throws(()=>parseWeather({current:{temperature_2m:70,weather_code:0,is_day:1},current_units:{temperature_2m:'°F'}}));
});
test('night, fog, snow and thunderstorms use the matching condition',()=>{
 assert.equal(weatherCondition(0,false).icon,'moon');
 assert.equal(weatherCondition(45,true).icon,'fog');
 assert.equal(weatherCondition(73,true).icon,'snow');
 assert.equal(weatherCondition(95,true).icon,'storm');
});
