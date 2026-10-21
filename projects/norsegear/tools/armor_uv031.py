#!/usr/bin/env python3
"""Native 256px per-part UV atlas for the shipped humanoid Armor ModelPart mesh."""
from pathlib import Path
from PIL import Image,ImageDraw
import re,math,random,json
ROOT=Path(__file__).resolve().parents[1]
p=ROOT/'src/main/java/dev/norsegear/client/model/NorseArmorModel.java';txt=p.read_text()
pattern=re.compile(r'((?:head|body|root\.getChild\("(?:left_arm|right_arm|left_leg|right_leg)"\))\.addOrReplaceChild\("([^"]+)", CubeListBuilder\.create\(\)\.texOffs\()\d+,\s*\d+(\)\s*\.addBox\(([^)]*)\))')
parts=[];curx=cury=rowh=0
for m in pattern.finditer(txt):
 arg=m.group(4)
 v=[float(s[:-1] if s.endswith('F') else s) for s in re.findall(r'-?\d+(?:\.\d+)?F?',arg)]
 if len(v)!=6:continue
 _,_,_,sx,sy,sz=v
 ww=math.ceil((2*sx+2*sz))+2;hh=math.ceil(sy+sz)+2
 if curx+ww>250:curx=0;cury+=rowh;rowh=0
 assert cury+hh<256,(m.group(2),cury+hh)
 parts.append({'name':m.group(2),'u':curx+1,'v':cury+1,'size':[sx,sy,sz]})
 curx+=ww;rowh=max(rowh,hh)
print('Packed',len(parts),'UV parts, max y:',cury+rowh)
i=iter(parts)
replaced=pattern.sub(lambda m:m.group(1)+f'{(q:=next(i))["u"]}, {q["v"]}'+m.group(3),txt)
replaced=replaced.replace('LayerDefinition.create(mesh, 128, 64)','LayerDefinition.create(mesh, 256, 256)')
p.write_text(replaced)
COL={
 'steel':[(104,132,153),(182,204,218),(45,69,91)],
 'light':[(145,170,184),(229,241,247),(83,113,132)],
 'dark':[(75,100,127),(153,177,194),(39,56,74)],
 'leather':[(134,89,54),(209,147,91),(77,48,31)],
 'mail':[(78,97,114),(195,211,223),(35,51,66)],
 'fur':[(170,155,131),(236,217,178),(106,91,75)],
 'bronze':[(171,122,61),(237,194,106),(105,68,34)]}

def material(name):
 if name in ('mail_front',):return 'mail'
 if name in ('fur_collar',):return 'fur'
 if 'buckle' in name or name=='brow_emblem':return 'bronze'
 if any(x in name for x in ('leather','strap','belt','back_strap','boot_toe')):return 'leather'
 if name in ('nasal_guard','helm_ridge','pauldron_rim','brow'):return 'light'
 if name in ('back_plate','left_temple','right_temple'):return 'dark'
 return 'steel'

img=Image.new('RGBA',(256,256),(0,0,0,0));d=ImageDraw.Draw(img)
for part in parts:
 u,v=part['u'],part['v'];sx,sy,sz=part['size']
 w,h,dep=max(1,round(sx)),max(1,round(sy)),max(1,round(sz))
 zones=[('up',(u+dep,v,u+dep+w-1,v+dep-1)),('down',(u+dep+w,v,u+dep+2*w-1,v+dep-1)),
  ('west',(u,v+dep,u+dep-1,v+dep+h-1)),('north',(u+dep,v+dep,u+dep+w-1,v+dep+h-1)),
  ('east',(u+dep+w,v+dep,u+2*dep+w-1,v+dep+h-1)),('south',(u+2*dep+w,v+dep,u+2*dep+2*w-1,v+dep+h-1))]
 mat=material(part['name']);base,hi,lo=COL[mat]
 for face,(x0,y0,x1,y1) in zones:
  if x1<x0 or y1<y0:continue
  d.rectangle((x0,y0,x1,y1),fill=base)
  if mat=='mail':
   for py in range(y0,y1+1):
    for px in range(x0,x1+1):
     row=(py-y0)%4;col=(px-x0+(py//4)%2*2)%4
     if row==3 and col==3:clr=(0,0,0,0)
     elif row in(0,1) and col in(0,1):clr=(*hi,255)
     elif row==2 or col==2:clr=(*lo,255)
     else:clr=(*base,255)
     img.putpixel((px,py),clr)
  elif mat=='fur':
   for xx in range(x0,x1+1,3):
    if xx+1<=x1:
     d.line((xx,y0,xx,y1),fill=hi if xx%6<3 else lo)
  elif mat=='leather':
   for py in range(y0+3,y1,5):d.line((x0,py,x1,py),fill=lo)
   for px in range(x0+2,x1,5):d.point((px,y0+1),fill=hi)
  else:
   d.line((x0,y0,x1,y0),fill=hi)
   d.line((x0,y0,x0,y1),fill=hi)
   d.line((x0,y1,x1,y1),fill=lo)
   if x1-x0>3 and y1-y0>3:
    d.rectangle((x0+1,y0+2,x1-1,y1-2),fill=base)
    d.line((x0+2,y0+3,min(x0+5,x1-2),y0+3),fill=hi)
   if mat in ('steel','dark') and x1-x0>6 and y1-y0>6:
    for xx,yy in [(x0+2,y0+2),(x1-2,y0+2),(x0+2,y1-2),(x1-2,y1-2)]:
     d.point((xx,yy),fill=hi)
     d.point((xx+1,yy+1),fill=lo)
for filename in ('ironweave_layer_1.png','ironweave_layer_2.png'):
 out=ROOT/'src/main/resources/assets/norsegear/textures/models/armor'/filename
 img.save(out)
(ROOT/'tools/armor_uv_manifest_031.json').write_text(json.dumps(parts,indent=2)+'\n')
print('Painted real ModelPart UV atlas: 256x256')
