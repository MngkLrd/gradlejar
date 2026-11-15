#!/usr/bin/env python3
"""Norse Gear 0.2.0: bespoke 16 x 16 pixel sprites, made on a 16-pixel grid.
No resizing or source image processing is used to draw the released art.
"""
from PIL import Image, ImageDraw, ImageFilter, ImageFont
from pathlib import Path
import json, random, math
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'src/main/resources/assets/norsegear/textures/item'
OUT.mkdir(parents=True,exist_ok=True)
C={
 'ink':'#20252b','deep':'#353e45','darksteel':'#4b5964','steel':'#8a9ba3','lite':'#b9c8c8','shine':'#eef0de','cold':'#697e89',
 'wood0':'#412c25','wood1':'#70442c','wood2':'#a26a40','wood3':'#d39b5f',
 'leather0':'#342825','leather1':'#5d3a2a','leather2':'#97603d','leather3':'#c78f5c',
 'gold0':'#795323','gold1':'#b1873e','gold2':'#f0cf79',
 'bone0':'#897b60','bone1':'#c7b58b','bone2':'#f0dfb6','bone3':'#fdf3d2',
 'green0':'#304938','green1':'#557254','green2':'#79916c',
 'red0':'#6f352d','red1':'#ab5137','red2':'#d4774a',
 'stone0':'#485057','stone1':'#737c83','stone2':'#a4adae','stone3':'#d0d5ce',
 'ore0':'#423c39','ore1':'#6a5543','ore2':'#a16a41','ore3':'#d69a4b',
}
INK=C['ink']
class A:
 def __init__(self):
  self.im=Image.new('RGBA',(16,16),(0,0,0,0));self.d=ImageDraw.Draw(self.im)
 def p(self,x,y,c):self.d.point((x,y),fill=C.get(c,c))
 def line(self,xy,c,w=1):self.d.line(xy,fill=C.get(c,c),width=w)
 def poly(self,xy,c):self.d.polygon(xy,fill=C.get(c,c))
 def rect(self,xy,c):self.d.rectangle(xy,fill=C.get(c,c))
 def ellipse(self,xy,c):self.d.ellipse(xy,fill=C.get(c,c))
 def finish(self,name,outline=True):
  if outline:
   mask=self.im.getchannel('A'); expanded=mask.filter(ImageFilter.MaxFilter(3))
   border=Image.new('RGBA',(16,16),C['ink']);border.putalpha(expanded)
   border.alpha_composite(self.im)
   self.im=border
  self.im.save(OUT/(name+'.png'),optimize=True)
  return self.im

def haft(a,lo=(2,13),hi=(9,5),heavy=False):
 a.line([lo,hi],'wood0',4 if heavy else 3)
 a.line([(lo[0]+1,lo[1]-1),(hi[0]+1,hi[1]-1)],'wood2',2 if heavy else 1)
 a.line([lo,hi],'wood1',1)
 a.p(lo[0],lo[1],'wood3')

def tiny_strap(a,pt,diag=False):
 x,y=pt;a.line([(x-1,y-1),(x+1,y+1)] if diag else [(x-1,y),(x+1,y)],'gold1')
 a.p(x-1,y-1 if diag else y,'gold2')

def seax():
 a=A();haft(a,(2,13),(6,9),False)
 a.poly([(6,9),(8,6),(11,3),(13,1),(13,6),(10,9),(8,10)],'darksteel')
 a.poly([(7,9),(10,5),(12,3),(12,6),(9,9)],'steel')
 a.line([(8,9),(12,5),(12,4)],'shine')
 a.line([(5,10),(7,8)],'gold1',2);a.p(6,9,'gold2')
 a.p(2,13,'wood3');return a.finish('seax')

def ax(name,kind):
 a=A()
 if kind=='dane':
  haft(a,(2,14),(9,6),True)
  # broad curved arc, no jutting lower triangle
  a.poly([(7,4),(10,3),(12,2),(13,1),(13,4),(14,5),(14,8),(13,11),(12,12),(11,10),(10,9),(8,9),(7,7)],'darksteel')
  a.poly([(8,4),(11,3),(12,2),(12,5),(13,6),(13,8),(12,10),(11,9),(9,8),(8,7)],'steel')
  a.line([(13,2),(13,9),(12,11)],'shine')
  a.line([(7,4),(7,7)],'deep',2)
 elif kind=='beard':
  haft(a,(2,14),(9,6))
  a.poly([(7,3),(9,3),(11,4),(13,3),(14,3),(14,7),(12,9),(12,11),(11,12),(10,10),(10,8),(8,7),(7,6)],'darksteel')
  a.poly([(8,4),(11,5),(13,4),(13,7),(11,9),(11,11),(10,9),(10,7),(8,6)],'steel')
  a.line([(13,4),(13,7),(11,10)],'shine')
  a.p(8,4,'lite')
 else: # throwing
  haft(a,(3,13),(8,7))
  a.poly([(6,3),(8,4),(10,4),(12,2),(13,3),(13,8),(11,9),(10,8),(8,8),(6,7)],'darksteel')
  a.poly([(7,4),(9,5),(11,4),(12,3),(12,7),(11,8),(9,7),(7,6)],'steel')
  a.line([(12,3),(12,7)],'shine');a.p(8,5,'lite')
 tiny_strap(a,(7,8),True)
 return a.finish(name)

def sword():
 a=A();haft(a,(2,14),(6,10))
 a.poly([(7,9),(10,5),(12,2),(14,1),(14,3),(12,6),(8,10)],'darksteel')
 a.poly([(8,9),(11,5),(13,2),(13,4),(11,7),(9,9)],'steel')
 a.line([(9,9),(13,3)],'shine')
 a.line([(4,11),(7,8)],'gold1',2);a.p(4,11,'gold2')
 a.p(2,14,'gold1');return a.finish('viking_sword')

def long_spear(name,is_javelin):
 a=A(); a.line([(2,14),(10,5 if not is_javelin else 6)],'wood0',3)
 a.line([(3,13),(10,5 if not is_javelin else 6)],'wood2')
 a.line([(4,11),(5,10)],'wood3')
 if is_javelin:
  a.poly([(9,7),(9,5),(13,1),(14,1),(13,5),(11,8)],'darksteel')
  a.poly([(10,6),(12,3),(13,2),(12,6),(11,7)],'steel')
  a.p(13,2,'shine')
 else:
  a.poly([(8,6),(9,4),(11,2),(14,0),(14,4),(12,7),(10,8)],'darksteel')
  a.poly([(10,5),(11,3),(13,1),(13,4),(11,6)],'steel')
  a.line([(11,5),(13,2)],'shine')
 a.line([(8,7),(10,9)],'gold0');a.p(9,7,'gold2')
 return a.finish(name)

def stone():
 a=A();a.poly([(4,5),(9,4),(12,6),(13,9),(10,12),(6,12),(3,10),(3,7)],'stone0')
 a.poly([(5,5),(9,5),(11,7),(11,10),(8,11),(5,10),(4,8)],'stone1')
 a.line([(5,6),(8,5),(10,7)],'stone3')
 a.line([(5,8),(5,10),(8,11)],'stone2')
 a.p(10,9,'stone0');a.p(8,7,'stone2');return a.finish('stone_shot')

def sling():
 a=A();a.line([(3,13),(5,5),(6,3)],'leather0',2)
 a.line([(5,4),(8,8),(11,12)],'leather2',2)
 a.line([(8,8),(11,4)],'leather1',2)
 a.poly([(7,8),(9,8),(10,10),(9,12),(7,12),(6,10)],'leather1')
 a.poly([(7,9),(9,9),(9,11),(7,11)],'leather2')
 a.p(7,9,'leather3');a.p(3,13,'leather3');return a.finish('sling')

def shield(name,kind):
 a=A();a.ellipse((1,1,14,14),'darksteel');a.ellipse((2,2,13,13),'gold1')
 a.ellipse((3,3,12,12),'wood1')
 # flat painted timber sectors, strong color blocks, not gradients
 if kind=='round':
  a.poly([(3,3),(7,3),(7,7),(3,7)],'red1');a.poly([(8,3),(12,4),(12,7),(8,7)],'green0')
  a.poly([(3,8),(7,8),(7,12),(4,12)],'green0');a.poly([(8,8),(12,8),(12,12),(8,12)],'red1')
  a.line([(4,4),(6,6)],'red2');a.line([(9,11),(11,9)],'red2')
 elif kind=='celtic':
  a.poly([(3,3),(8,3),(8,12),(3,10)],'green1');a.poly([(9,3),(12,4),(12,12),(9,12)],'bone1')
  a.line([(5,5),(10,10)],'gold2');a.line([(10,5),(5,10)],'gold1')
 elif kind=='bossed':
  a.poly([(3,3),(7,3),(7,12),(3,11)],'wood2');a.poly([(8,3),(12,4),(12,11),(8,12)],'wood1')
  a.line([(5,3),(5,11)],'wood0');a.line([(10,4),(10,11)],'wood0')
  a.p(4,5,'wood3');a.p(9,6,'wood2')
 # thin exposed rim & boss
 a.ellipse((1,1,14,14),outline='steel' if False else 'darksteel') if False else None
 a.p(7,2,'lite');a.p(2,7,'lite');a.p(12,5,'lite');a.p(7,13,'darksteel')
 a.ellipse((6,6,9,9),'darksteel');a.p(7,6,'shine');a.p(8,7,'steel');a.p(7,8,'stone1')
 return a.finish(name)

def helm():
 a=A();a.poly([(4,3),(6,2),(10,2),(12,4),(13,9),(11,13),(9,14),(9,10),(7,10),(7,14),(4,12),(3,9)],'darksteel')
 a.poly([(5,4),(7,3),(10,3),(11,5),(11,8),(5,8)],'steel');a.line([(6,4),(9,3),(11,5)],'lite')
 a.line([(8,3),(8,12)],'gold1',2);a.p(8,4,'gold2')
 a.rect((5,9,6,11),'deep');a.rect((10,9,11,11),'deep')
 a.p(5,7,'shine');a.p(11,7,'steel');return a.finish('norse_helmet')

def chest():
 a=A();a.poly([(4,2),(6,3),(10,3),(12,2),(14,5),(12,8),(12,13),(4,13),(4,8),(2,7),(2,5)],'wood0')
 a.poly([(5,3),(7,4),(10,4),(11,3),(13,5),(12,7),(11,6),(11,12),(5,12),(5,6),(4,7),(3,5)],'leather2')
 a.poly([(6,6),(10,6),(11,11),(5,11)],'darksteel')
 for y in (7,9,11):
  for x in (6,8,10):
   if y==11 and x==10:continue
   a.p(x,y,'lite' if (x+y)%3 else 'steel')
 a.line([(5,6),(5,11)],'gold0');a.line([(11,6),(11,11)],'gold0')
 a.rect((5,12,11,13),'leather0');a.p(8,12,'gold2');a.p(4,4,'leather3');a.p(12,4,'leather3')
 return a.finish('norse_chestplate')

def leggings():
 a=A();a.poly([(4,2),(11,2),(12,7),(10,13),(8,13),(8,8),(7,8),(7,13),(4,13),(3,7)],'leather0')
 a.rect((4,3,11,5),'leather2');a.line([(4,6),(5,12)],'steel',2);a.line([(10,6),(10,12)],'steel',2)
 a.rect((5,7,6,10),'darksteel');a.rect((9,7,10,10),'darksteel')
 a.p(6,7,'lite');a.p(10,7,'lite');a.line([(4,4),(11,4)],'gold0');a.p(8,4,'gold2')
 a.rect((4,12,6,13),'wood2');a.rect((9,12,11,13),'wood2');return a.finish('norse_leggings')

def boots():
 a=A()
 a.poly([(3,3),(6,3),(7,9),(7,12),(5,14),(1,14),(1,12),(2,10)],'leather1')
 a.poly([(9,3),(12,3),(13,10),(14,12),(14,14),(10,14),(8,12),(8,9)],'leather1')
 for x in (3,9):
  a.rect((x,4,x+3,7),'leather2');a.line([(x,5),(x+3,5)],'bone1');a.rect((x,9,x+3,11),'steel')
 a.line([(1,13),(6,13)],'wood0');a.line([(9,13),(14,13)],'wood0')
 a.p(2,12,'leather3');a.p(10,12,'leather3')
 return a.finish('norse_boots')

def horn(name,war):
 a=A()
 if war:
  a.poly([(2,12),(4,13),(7,12),(10,9),(12,6),(13,3),(11,2),(10,5),(7,8),(4,9)],'bone0')
  a.poly([(3,11),(6,11),(9,8),(12,4),(11,3),(10,6),(7,9),(4,10)],'bone1')
  a.line([(4,10),(6,9),(9,6)],'bone2',2)
  a.rect((3,10,5,12),'gold1');a.p(4,10,'gold2')
  a.poly([(10,2),(12,1),(14,2),(14,4),(12,4)],'gold0')
  a.p(13,2,'gold2')
 else:
  a.poly([(3,12),(6,13),(8,11),(11,8),(12,4),(11,2),(8,3),(7,6),(5,9)],'bone0')
  a.poly([(4,11),(6,12),(8,10),(10,7),(11,4),(10,3),(9,4),(8,7),(6,10)],'bone1')
  a.line([(5,10),(7,10),(9,7)],'bone2')
  a.ellipse((9,1,12,4),'leather0');a.p(10,2,'bone3')
  a.line([(3,11),(5,12)],'leather2',2)
 return a.finish(name)

def whet():
 a=A();a.poly([(2,12),(3,10),(10,3),(12,3),(14,5),(13,7),(6,14),(3,14)],'darksteel')
 a.poly([(4,11),(11,4),(13,5),(6,12)],'stone1')
 a.line([(5,11),(11,5)],'stone3');a.line([(6,13),(13,6)],'deep')
 a.line([(3,12),(5,14)],'leather2',2);a.p(3,12,'gold1')
 return a.finish('field_whetstone')

def ration():
 a=A();a.poly([(2,8),(5,4),(11,4),(14,8),(13,12),(10,14),(5,13),(2,11)],'leather0')
 a.poly([(3,8),(5,5),(11,5),(13,8),(12,11),(9,13),(5,12)],'red0')
 a.poly([(4,7),(6,5),(11,6),(12,8),(10,11),(6,11)],'red1')
 a.line([(5,7),(10,11)],'bone1',2);a.line([(8,5),(12,9)],'bone1',2)
 a.p(4,9,'red2');a.p(9,7,'red2');return a.finish('travel_ration')

def pitch():
 a=A();a.poly([(6,2),(10,2),(10,5),(12,6),(13,12),(11,14),(5,14),(3,12),(4,7),(6,5)],'darksteel')
 a.poly([(6,7),(10,7),(12,11),(10,13),(5,13),(4,11)],'stone0')
 a.poly([(5,8),(8,7),(10,8),(11,11),(9,12),(5,12)],'stone1')
 a.p(6,9,'lite');a.line([(5,3),(11,3)],'leather2',2)
 a.line([(9,4),(13,8)],'gold1',2);a.p(9,4,'gold2');return a.finish('pitch_flask')

def leather_binding():
 a=A();a.ellipse((3,4,12,12),'leather0');a.ellipse((4,5,11,11),'leather2')
 a.ellipse((6,6,9,9),'leather0');a.ellipse((7,7,8,8),(0,0,0,0))
 a.line([(4,11),(9,14)],'leather1',2);a.line([(9,14),(12,13)],'leather3')
 a.p(4,6,'leather3');a.p(5,5,'leather3')
 return a.finish('leather_binding')

def bog():
 a=A();a.poly([(3,5),(5,3),(11,4),(13,7),(12,11),(9,13),(4,12),(2,9)],'ore0')
 a.poly([(4,5),(6,4),(10,5),(12,7),(11,10),(7,11),(4,10)],'ore1')
 for x,y in [(5,5),(8,6),(11,8),(6,9),(9,11)]:a.p(x,y,'ore2')
 for x,y in [(6,6),(9,8),(5,9)]:a.p(x,y,'ore3')
 a.p(4,7,'stone1');a.p(10,6,'stone0');return a.finish('bog_iron')

def forged():
 a=A();a.poly([(2,7),(4,4),(12,4),(14,7),(13,11),(10,13),(3,12)],'darksteel')
 a.poly([(3,7),(5,5),(12,5),(13,7),(10,8),(4,8)],'lite')
 a.poly([(4,9),(10,9),(13,7),(12,10),(10,12),(4,11)],'steel')
 a.line([(4,6),(11,6)],'shine');a.line([(3,10),(10,11)],'deep')
 return a.finish('forged_iron')

ART={
 'seax':seax,'bearded_axe':lambda:ax('bearded_axe','beard'),'dane_axe':lambda:ax('dane_axe','dane'),
 'viking_sword':sword,'spear':lambda:long_spear('spear',False),'javelin':lambda:long_spear('javelin',True),
 'throwing_axe':lambda:ax('throwing_axe','throw'),'sling':sling,'stone_shot':stone,
 'round_shield':lambda:shield('round_shield','round'),'celtic_shield':lambda:shield('celtic_shield','celtic'),
 'bossed_shield':lambda:shield('bossed_shield','bossed'),
 'norse_helmet':helm,'norse_chestplate':chest,'norse_leggings':leggings,'norse_boots':boots,
 'war_horn':lambda:horn('war_horn',True),'carved_horn':lambda:horn('carved_horn',False),
 'field_whetstone':whet,'travel_ration':ration,'pitch_flask':pitch,
 'leather_binding':leather_binding,'bog_iron':bog,'forged_iron':forged
}

if __name__=='__main__':
 for name,func in ART.items():
  im=func()
  assert im.size==(16,16),name
  assert im.getchannel('A').getbbox(),name
 # Sheet with deliberately simple checker instead of AI-generated frames
 from PIL import ImageFont
 tiles=[]
 for name in ART:
  im=Image.open(OUT/(name+'.png'))
  tile=Image.new('RGBA',(188,210),'#191e23')
  d=ImageDraw.Draw(tile)
  for y in range(16):
   for x in range(16):
    d.rectangle((x*10+14,y*10+10,x*10+23,y*10+19),fill=('#353b43' if (x+y)%2 else '#404751'))
  up=im.resize((160,160),Image.Resampling.NEAREST)
  tile.alpha_composite(up,(14,10))
  label=name.replace('_',' ').title()
  d.text((11,179),label,fill='#f0e8d4')
  d.text((11,194),'16 x 16  /  RGBA',fill='#9caeb4')
  tiles.append(tile)
 sheet=Image.new('RGB',(188*6,210*4),'#141920')
 for i,tile in enumerate(tiles):sheet.paste(tile,(i%6*188,i//6*210),tile)
 sheet.save(str(ROOT/'ART_PREVIEW.png'))
 # Native atlas 96x64 (6 per row), no frames
 atlas=Image.new('RGBA',(96,64),(0,0,0,0))
 for i,n in enumerate(ART): atlas.alpha_composite(Image.open(OUT/(n+'.png')),(i%6*16,i//6*16))
 atlas.save(str(ROOT/'NATIVE_ATLAS.png'))
 print('AUTHORED',len(ART),'native sprites, preview saved')
