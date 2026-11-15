from pathlib import Path
import base64,zlib,struct,json,sys
NAMES=['bamboo_blowpipe','hunting_blowpipe','reinforced_blowpipe','plain_dart','iron_dart','poison_dart','slowness_dart','fire_dart','sleep_dart']
PIXELS="c-rlj%}Z2a6vq84YGcyJCTmQJY#@hfq(+7@G1P`K`%uCd$`Fldz^oRd<q$%OMjxALhM*9NC=Du=J_yD|hGanzX-O??5$&IN241<f816YYgo*pWv;3X&p7(tY_ZAc^t)?>lSm#*<Ek8Pb4zGq=R5i#ZuGOPH;)6HfvCXp|ZCGvJ*oke>+?VMq<*lrQ<E+-ZlAc+6R}bQKu-=tqUb1xOUaz<>?}>fM8cTR)ku$Tel(p87WHMhhb+s8qg@vj)CletUYih7}>U0%)_IOdex(HIXqoy1?H>}O_k~6cXVKp<8*RZB9&nr1I8DnW*D|>j$dJ%i;Xy6Z4!kb)ZSd$Br4e7a=MfR3v<L?^MOy;8|CZ1y1@)gjsFEg*R*3^jiT*Ap%JnNNfWN%Ei;XpIWw+9@t(s{Kk#e3qcQg*1Vaj~!TuE^5u;c8W1!|to_Wi8EymbGR>!cxp4duy^`t)XS}ML)h|Ku>4xLCce+FDFikYdB%U{?bKO%8&Lop!bMhHS@n<d3MdbOU(`s90RSL*YKM6egAF@UGcbzrO588uKDl3-%hkJ{}%M4hbd@zvUD^S71waWGRuaQ^6viIQ!<?bt)18Kn)jXgG>7wjLn@Xc>kqp7pG$WqT0wUW>F&v}bl_l_xP}v!SvIVcclY0(nxa002P)L_PJeF)YiQp0+5CR&?)Ioyifl<$iM#(f^yKq5&}Z(9LCce+D_c6nHJq@_vSFpXyZ`po+Mh&iUs^ry^M#*a4bA)BpIty@?23w|$ZiTAa`!*iKi0@a(8&uCXnC@9q-KY>h7*=qHmsC)_urmwgf@cK&inQ3<}9yy-(NSkV!W|b#ZqLCmleDFpF?k6d;pzJ+=iAXOE-nu#5J6-%(7vnyu1JQG}w{EVC=Da-j5@5U=7Xt&ZKA2U3Xo@Qe=Z=wRyMyzX4a%iG~"
COLORS={
'bamboo_blowpipe':{'deep':'37462c','shadow':'556a3c','mid':'8b9b55','light':'b8c080','high':'d1cb93','band':'796d3e','bandlight':'c8ad67','grip':'7c5637','griplight':'b58554','bore':'2a2c21'},
'hunting_blowpipe':{'deep':'29221e','shadow':'442e28','mid':'74503b','light':'a4784c','high':'d19a63','band':'875137','bandlight':'cc8b4b','grip':'60362b','griplight':'a76546','bore':'1e1c1c'},
'reinforced_blowpipe':{'deep':'20232b','shadow':'3b3f46','mid':'5b5c63','light':'85878d','high':'c1c1be','band':'6f7985','bandlight':'c6cbd0','grip':'59423c','griplight':'97715d','bore':'16171d'}
}
def px(color):
 return bytes.fromhex(color)+(255).to_bytes(1,'big')
def png(path,data):
 assert len(data)==1024
 def ch(kind,contents):
  return struct.pack('>I',len(contents))+kind+contents+struct.pack('>I',zlib.crc32(kind+contents)&0xffffffff)
 scan=b''.join(b'\x00'+data[i*64:(i+1)*64] for i in range(16))
 path.parent.mkdir(parents=True,exist_ok=True)
 path.write_bytes(b'\x89PNG\r\n\x1a\n'+ch(b'IHDR',struct.pack('>IIBBBBB',16,16,8,6,0,0,0))+ch(b'IDAT',zlib.compress(scan,9))+ch(b'IEND',b''))
def side(name):
 c=COLORS[name];a=bytearray(1024)
 faces=(('light','mid','mid','shadow'),('mid','shadow','shadow','deep'),('mid','mid','shadow','shadow'),('shadow','shadow','deep','deep'))
 for y in range(16):
  for f in range(4):
   for x in range(4):
    v=faces[f][x]
    if name=='bamboo_blowpipe':
     if y in (4,5,10,11):v='bandlight' if f==0 and x==0 else 'band'
     if y in (12,13):v='griplight' if f==0 and x<2 else 'grip'
     if y in (2,7,9) and f==0 and x==1:v='high'
    elif name=='hunting_blowpipe':
     if y in (2,8,14):v='bandlight' if f==0 and x<2 else 'band'
     if 10<=y<=13:v='griplight' if f==0 and x<2 and y in (10,12) else 'grip'
     if y in (4,5,6) and f==0 and x==0:v='high'
    else:
     if y in (1,2,7,8,13,14):v='bandlight' if f==0 and x<2 else 'band'
     if y in (9,10,11,12):v='griplight' if f==0 and x<2 and y in (9,11) else 'grip'
     if y in (4,5) and f==0 and x==1:v='high'
    index=(y*16+f*4+x)*4;a[index:index+4]=px(c[v])
 return a
def end(name):
 c=COLORS[name];a=bytearray(1024)
 for y in range(4):
  for x in range(4):
   v='bore' if 1<=x<=2 and 1<=y<=2 else 'high' if x==0 and y==0 else 'bandlight' if x==0 or y==0 else 'deep' if x==3 or y==3 else 'band'
   index=(y*16+x)*4;a[index:index+4]=px(c[v])
 return a
def cube(frm,to):
 xa,ya,za=frm;xb,yb,zb=to;w=xb-xa;d=zb-za
 assert all(isinstance(i,int) for i in frm+to) and w in (2,4) and d in (2,4)
 faces={}
 for face,u,width in [('north',0,w),('south',4,w),('east',8,d),('west',12,d)]:
  faces[face]={'uv':[u,ya,u+width,yb],'texture':'#skin'}
 for face in ('up','down'):faces[face]={'uv':[0,0,w,d],'texture':'#end'}
 return {'from':frm,'to':to,'faces':faces}
def model(name):
 elements=[cube([7,1,7],[9,15,9]),cube([6,0,6],[10,2,10]),cube([6,14,6],[10,16,10])]
 if name!='bamboo_blowpipe':elements.append(cube([6,4,6],[10,6,10]))
 return {
 'ambientocclusion':False,'gui_light':'front',
 'textures':{'skin':'blowpipes:item/'+name+'_3d','end':'blowpipes:item/'+name+'_end','particle':'blowpipes:item/'+name+'_3d'},
 'elements':elements,
 'display':{
 'gui':{'rotation':[15,18,-43],'translation':[0,0,0],'scale':[0.91,0.91,0.91]},
 'firstperson_righthand':{'rotation':[5,-80,28],'translation':[1,2,0],'scale':[0.82,0.82,0.82]},
 'firstperson_lefthand':{'rotation':[5,80,-28],'translation':[-1,2,0],'scale':[0.82,0.82,0.82]},
 'thirdperson_righthand':{'rotation':[0,90,35],'translation':[0,1,-0.5],'scale':[0.68,0.68,0.68]},
 'thirdperson_lefthand':{'rotation':[0,-90,-35],'translation':[0,1,-0.5],'scale':[0.68,0.68,0.68]},
 'ground':{'translation':[0,2,0],'scale':[0.52,0.52,0.52]},
 'fixed':{'rotation':[0,0,-45],'scale':[0.8,0.8,0.8]}
 }}
def run(root):
 tex=root/'src/main/resources/assets/blowpipes/textures/item'
 models=root/'src/main/resources/assets/blowpipes/models/item'
 raw=zlib.decompress(base64.b85decode(PIXELS));assert len(raw)==9216
 for i,name in enumerate(NAMES):
  png(tex/(name+'.png'),raw[i*1024:(i+1)*1024])
  if name in COLORS:
   png(tex/(name+'_3d.png'),side(name));png(tex/(name+'_end.png'),end(name))
   m=model(name)
  else:m={'parent':'minecraft:item/handheld','textures':{'layer0':'blowpipes:item/'+name}}
  models.mkdir(parents=True,exist_ok=True)
  (models/(name+'.json')).write_text(json.dumps(m,indent=2)+'\n')
  for e in m.get('elements',[]):
   for f in e['faces'].values():assert all(isinstance(v,int) and 0<=v<=16 for v in f['uv'])
 assert len(list(tex.glob('*.png')))==15
 print('VALIDATED 9 item icons, 6 3D texture planes, 3 native cuboid models: all textures 16x16, integer UV')
if __name__=='__main__':run(Path(sys.argv[1]).resolve())
