// Only connects to the local isolated QA server. Never joins public servers.
const mineflayer = require('mineflayer');
const readline = require('node:readline');
const port = Number(process.argv[2]);
const version = process.argv[3];
if (!Number.isInteger(port) || port < 1 || port > 65535 || !['1.20.1','1.21.1','1.21.4'].includes(version)) process.exit(2);
const bot = mineflayer.createBot({host:'127.0.0.1', port, username:'MineModsQA', auth:'offline', version, hideErrors:false});
const emit = (obj) => process.stdout.write(JSON.stringify(obj)+'\n');
const sleep = (ms) => new Promise(r=>setTimeout(r,ms));
function direction(c) {
  const d=c.face??1;
  if(!Number.isInteger(d)||d<0||d>5) throw Error('Invalid face');
  return d;
}
function itemNamed(name) {
  const i=bot.inventory.items().find(x=>x.name===name);
  if(!i) throw Error('Missing inventory item '+name);
  return i;
}
async function useBlock(b,c) {
  await bot.lookAt(b.position.offset(.5,.5,.5),true);
  // All supported protocols accept these fields; older serializers ignore worldBorderHit.
  // Keep the explicit sequence monotonic with the raw start/abort digging packets.
  bot._client.write('block_place',{hand:0,location:b.position,direction:direction(c),
    cursorX:.5,cursorY:direction(c)===1?.99:.5,cursorZ:.5,insideBlock:false,worldBorderHit:false,sequence:sequence++});
  bot.swingArm('right');
}
function nearEntity(c) {
  const e=bot.nearestEntity(e=>e.name===c.entity && e.position.distanceTo(bot.entity.position)<4);
  if(!e)throw Error('Missing nearby entity '+c.entity);
  return e;
}
async function control(c) {
  const ms=c.ms??800;
  if(!Number.isInteger(ms)||ms<100||ms>4000)throw Error('Invalid movement duration');
  const start=bot.entity.position.clone();
  let peak=start.y;
  if(typeof c.yaw==='number')await bot.look(c.yaw*Math.PI/180,0,true);
  try {
    if(c.action==='move'||c.forward)bot.setControlState(c.direction??'forward',true);
    if(c.action==='jump')bot.setControlState('jump',true);
    if(c.sprint)bot.setControlState('sprint',true);
    if(c.sneak)bot.setControlState('sneak',true);
    const end=Date.now()+ms;
    while(Date.now()<end){await sleep(50);peak=Math.max(peak,bot.entity.position.y);}
  } finally {bot.clearControlStates();}
  const delta=bot.entity.position.minus(start);
  if(c.action==='jump'&&peak-start.y<.35)throw Error('No observed jump');
  if(c.action==='move'&&Math.hypot(delta.x,delta.z)<.2)throw Error('No observed movement');
  return {delta:{x:delta.x,y:delta.y,z:delta.z},peakY:peak,startY:start.y};
}
let sequence=10, ready=false, busy=false;
const rl=readline.createInterface({input:process.stdin,terminal:false});
bot.once('spawn',()=>{ready=true; emit({event:'ready',version:bot.version});});
bot.on('error',e=>emit({event:'error',message:String(e)}));
bot.on('kicked',e=>emit({event:'error',message:'kicked: '+JSON.stringify(e)}));
bot.on('end',()=>{emit({event:'end'});process.exit(0)});
function pos(x,y,z){const p=bot.entity.position;return p.offset(x-p.x,y-p.y,z-p.z);}
async function blockAt(c){
  const p=pos(c.x,c.y,c.z);
  for(let i=0;i<50;i++){const b=bot.blockAt(p);if(b&&b.name===c.block)return b;await sleep(100);}
  throw Error('Expected client block '+c.block+' at '+p);
}
rl.on('line',async line=>{
  let c;
  try{
    c=JSON.parse(line);
    if(!ready||busy)throw Error('Client not ready or busy');
    busy=true;
    let result={};
    if(c.action==='position'){
      for(let i=0;i<50;i++){if(bot.entity.position.distanceTo(pos(c.x,c.y,c.z))<1)break;await sleep(100);}
      if(bot.entity.position.distanceTo(pos(c.x,c.y,c.z))>=1)throw Error('Teleport not received');
      result.position=bot.entity.position.toString();
    }else if(c.action==='startDig'||c.action==='abortDig'){
      const b=await blockAt(c);
      await bot.lookAt(b.position.offset(.5,.5,.5),true);
      const status=c.action==='startDig'?0:1;
      bot._client.write('block_dig',{status,location:b.position,face:2,sequence:sequence++});
      // Deliberately NO finished-digging packet: server must instamine on initial action.
      result.packet=status===0?'START_DESTROY_BLOCK':'ABORT_DESTROY_BLOCK';
    }else if(c.action==='activate'){
      const b=await blockAt(c);
      await useBlock(b,c);
      result.packet='block_place / use_item_on';
    }else if(c.action==='inventory'){
      let item;
      for(let i=0;i<50;i++){item=bot.inventory.items().find(x=>x.name===c.item);if(item)break;await sleep(100);}
      if(!item||item.count<c.count)throw Error('Inventory item missing: '+c.item);
      result.count=item.count;
    }else if(c.action==='drop'){
      const item=bot.inventory.items().find(x=>x.name===c.item);
      if(!item)throw Error('Cannot drop missing item');
      await bot.equip(item,'hand');
      bot._client.write('block_dig',{status:4,location:{x:0,y:0,z:0},face:0,sequence:sequence++});
      result.packet='DROP_ITEM';
    }else if(c.action==='equip'){
      await bot.equip(itemNamed(c.item),c.destination??'hand');
      result.item=c.item;
    }else if(c.action==='move'||c.action==='jump'){
      result=await control(c);
    }else if(c.action==='craft'){
      const id=bot.registry.itemsByName[c.item]?.id;
      if(id===undefined)throw Error('Unknown recipe output '+c.item);
      const table=c.table?await blockAt(c.table):null;
      const recipes=bot.recipesFor(id,null,c.count??1,table);
      if(!recipes.length)throw Error('No craftable recipe for '+c.item);
      await bot.craft(recipes[0],c.count??1,table);
      result.output=c.item;
    }else if(c.action==='place'){
      await bot.equip(itemNamed(c.item),'hand');
      const b=await blockAt(c);
      await useBlock(b,c);
      result.packet='block_place';
    }else if(c.action==='dig'){
      const b=await blockAt(c);
      await bot.dig(b,true);
      result.block=c.block;
    }else if(c.action==='attack'||c.action==='interact'){
      const e=nearEntity(c);
      await bot.lookAt(e.position.offset(0,Math.min(e.height??1,.9),0),true);
      if(c.action==='attack')bot.attack(e);
      else await bot.activateEntity(e);
      result.entity=e.name;
    }else if(c.action==='use'){
      await bot.equip(itemNamed(c.item),'hand');
      try{bot.activateItem();await sleep(c.ms??500);}finally{bot.deactivateItem();}
      result.item=c.item;
    }else if(c.action==='quit'){
      emit({id:c.id,ok:true});bot.quit();return;
    }else throw Error('Unknown action '+c.action);
    emit({id:c.id,ok:true,...result});
  }catch(e){emit({id:c&&c.id,ok:false,error:String(e)});}
  finally{busy=false;}
});
setTimeout(()=>{emit({event:'error',message:'Bot lifetime limit'});process.exit(3)},120000).unref();
