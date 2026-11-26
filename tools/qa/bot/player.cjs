// Only connects to the local isolated QA server. Never joins public servers.
const mineflayer = require('mineflayer');
const readline = require('node:readline');
const port = Number(process.argv[2]);
const version = process.argv[3];
if (!Number.isInteger(port) || port < 1 || port > 65535 || !['1.20.1','1.21.1'].includes(version)) process.exit(2);
const bot = mineflayer.createBot({host:'127.0.0.1', port, username:'MineModsQA', auth:'offline', version, hideErrors:false});
const emit = (obj) => process.stdout.write(JSON.stringify(obj)+'\n');
const sleep = (ms) => new Promise(r=>setTimeout(r,ms));
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
      await bot.activateBlock(b);
      result.packet='use_item_on via mineflayer';
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
    }else if(c.action==='quit'){
      emit({id:c.id,ok:true});bot.quit();return;
    }else throw Error('Unknown action '+c.action);
    emit({id:c.id,ok:true,...result});
  }catch(e){emit({id:c&&c.id,ok:false,error:String(e)});}
  finally{busy=false;}
});
setTimeout(()=>{emit({event:'error',message:'Bot lifetime limit'});process.exit(3)},120000).unref();
