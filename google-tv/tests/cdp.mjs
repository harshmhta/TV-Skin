export async function connectPage(port){
 const targets=await (await fetch(`http://127.0.0.1:${port}/json/list`)).json();
 const target=targets.find(t=>t.url==='https://appassets.androidplatform.net/index.html');
 if(!target)throw Error('CleanFrame WebView target not found');
 const ws=new WebSocket(target.webSocketDebuggerUrl),pending=new Map();let nextId=0;
 await new Promise((resolve,reject)=>{ws.onopen=resolve;ws.onerror=reject;});
 ws.onmessage=event=>{const m=JSON.parse(event.data);if(m.id){const p=pending.get(m.id);pending.delete(m.id);if(m.error)p.reject(Error(m.error.message));else p.resolve(m.result);}};
 return {async evaluate(expression){const id=++nextId;const p=new Promise((resolve,reject)=>pending.set(id,{resolve,reject}));ws.send(JSON.stringify({id,method:'Runtime.evaluate',params:{expression,returnByValue:true,awaitPromise:true}}));const r=await p;if(r.exceptionDetails)throw Error(JSON.stringify(r.exceptionDetails));return r.result.value;},close(){ws.close();}};
}
