from PIL import Image,ImageDraw,ImageFilter,ImageFont
from pathlib import Path
import json,math,shutil,os
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/norsegear'
out=assets/'textures/item'
P={
'X':'#292c30', 'V':'#39454b','D':'#56646b','M':'#89999c','L':'#b8c5c6','H':'#f1f3e7',
'K':'#432d21', 'B':'#754526','R':'#a96937','W':'#d8974f','Y':'#efd0a0',
'Q':'#60331f','F':'#9a542b','T':'#d17b42','E':'#f4af61',
'J':'#6f5a31','G':'#ae8a43','O':'#e4bb68','U':'#fff0aa',
'Z':'#424e38','N':'#517746','n':'#7fa061','i':'#b9cb83',
'A':'#75352e','C':'#af493a','c':'#e57e51',
'q':'#806f56','p':'#b5a17a','w':'#e6d9ae','h':'#fff1cf',
's':'#545963','t':'#777f88','u':'#aab4b5','v':'#dce0d7',
'x':'#423a37','y':'#6e6261','z':'#978a7f',
}
# All art is authored directly on 16 x 16. No resampling of prior icons.
class S:
 def __init__(self):self.im=Image.new('RGBA',(16,16),(0,0,0,0));self.d=ImageDraw.Draw(self.im)
 def c(self,s):return P.get(s,s)
 def pt(self,x,y,c):self.d.point((x,y),fill=self.c(c))
 def l(self,xy,c,w=1):self.d.line(xy,fill=self.c(c),width=w)
 def pol(self,p,c):self.d.polygon(p,fill=self.c(c))
 def rec(self,b,c):self.d.rectangle(b,fill=self.c(c))
 def el(self,b,c):self.d.ellipse(b,fill=self.c(c))
 def contour(self):
  # one pixel contour, not an aggressive dark dilation; omit if visual busy
  a=self.im.getchannel('A');new=a.filter(ImageFilter.MaxFilter(3));edge=Image.new('RGBA',(16,16),P['X']);edge.putalpha(new);edge.alpha_composite(self.im);self.im=edge
 def save(self,n,edge=True):
  if edge:self.contour()
  self.im.save(out/(n+'.png'),optimize=True)
  return self.im

def shaft(s,tail=(2,13),tip=(9,6),width=2):
 s.l([tail,tip],'K',width+1);s.l([(tail[0],tail[1]-1),(tip[0],tip[1]-1)],'R',width)
 s.l([(tail[0]+1,tail[1]-1),(tip[0]+1,tip[1]-1)],'W',1)
 s.pt(tail[0],tail[1],'Y')

def axe(name,kind):
 s=S(); shaft(s,(2,13 if kind=='throw' else 14),(9,6),2 if kind=='throw' else 3)
 if kind=='beard':
  # beard extends below eye, negative cutaway behind bottom blade
  s.pol([(7,4),(9,3),(10,4),(12,4),(14,3),(14,6),(13,7),(12,9),(11,11),(10,11),(10,8),(8,7),(7,6)],'V')
  s.pol([(8,4),(10,5),(12,5),(13,4),(13,7),(11,9),(11,10),(10,9),(10,7),(8,6)],'M')
  s.l([(13,4),(13,7),(11,10)],'H');s.pt(8,4,'L')
 elif kind=='dane':
  # uninterrupted curving long axe edge, NO triangular point at bottom
  s.pol([(7,4),(10,3),(12,2),(13,2),(14,5),(14,8),(13,11),(12,12),(10,10),(9,9),(7,8)],'V')
  s.pol([(8,4),(10,4),(12,3),(13,4),(13,8),(12,10),(11,10),(9,8),(8,7)],'M')
  s.l([(12,3),(13,5),(13,8),(12,10)],'H');s.l([(8,5),(8,7)],'D')
 else:
  s.pol([(7,5),(9,5),(11,4),(13,4),(13,8),(11,9),(9,8),(7,8)],'V')
  s.pol([(8,5),(10,6),(12,5),(12,8),(11,8),(9,7)],'L')
  s.l([(12,5),(12,8)],'H');s.pt(8,6,'M')
 s.l([(7,8),(9,9)],'J',2);s.pt(8,8,'O')
 return s.save(name)

def sword(name,short=False):
 s=S()
 if short:
  shaft(s,(2,13),(5,10),2)
  # historical broken-back seax: kink on SPINE, not cutting edge
  s.pol([(6,10),(8,8),(10,5),(13,2),(13,7),(10,10),(8,11)],'V')
  s.pol([(7,10),(10,7),(12,4),(12,7),(9,10)],'M')
  s.l([(8,10),(12,6)],'H');s.pt(12,4,'L')
  s.l([(5,10),(7,8)],'G',2);s.pt(6,9,'U')
 else:
  shaft(s,(2,14),(6,10),2)
  s.pol([(7,10),(8,8),(11,4),(13,1),(14,1),(14,3),(11,7),(9,10)],'D')
  s.pol([(8,9),(11,5),(13,2),(13,4),(10,8)],'M')
  s.l([(10,7),(13,2)],'H');s.l([(9,8),(10,6)],'L')
  s.l([(5,11),(7,9)],'G',2);s.pt(5,11,'U');s.pt(2,14,'O')
 return s.save(name)

def spear(name,is_javelin):
 s=S()
 shaft(s,(2,14),(10,5 if not is_javelin else 6),2 if not is_javelin else 1)
 if not is_javelin:
  s.pol([(9,7),(9,5),(11,2),(14,0),(14,4),(12,7),(11,8)],'D')
  s.pol([(10,6),(11,3),(13,1),(13,4),(11,7)],'M')
  s.l([(11,5),(13,2)],'H')
 else:
  s.pol([(10,7),(10,5),(13,1),(14,1),(13,5),(11,8)],'D')
  s.pol([(11,6),(13,2),(13,4),(11,7)],'M')
  s.l([(12,4),(13,2)],'H')
 s.l([(9,7),(10,8)],'G');s.pt(9,7,'O');return s.save(name)

def shield(name,variant):
 s=S();s.el((1,1,14,14),'V');s.el((2,2,13,13),'L');s.el((3,3,12,12),'B')
 if variant=='round':
  s.pol([(3,3),(7,3),(7,7),(3,7)],'C')
  s.pol([(8,3),(12,3),(12,7),(8,7)],'N')
  s.pol([(3,8),(7,8),(7,12),(4,12)],'N')
  s.pol([(8,8),(12,8),(12,12),(8,12)],'C')
  s.pt(4,5,'c');s.pt(11,10,'c')
 elif variant=='celtic':
  s.pol([(3,3),(7,3),(7,12),(3,11)],'N')
  s.pol([(8,3),(12,4),(12,11),(8,12)],'w')
  s.l([(4,5),(10,11)],'O');s.l([(10,5),(5,10)],'G')
 else:
  s.pol([(3,3),(7,3),(7,12),(4,12)],'R')
  s.pol([(8,3),(12,4),(12,11),(8,12)],'B')
  s.l([(5,4),(5,11)],'K');s.l([(10,4),(10,11)],'K')
  s.l([(4,5),(4,10)],'W')
 # metal rim & dome
 for p in [(5,2),(7,2),(10,3),(12,5),(13,7),(11,12),(6,13),(3,10),(2,7)]:s.pt(*p,'H' if p in [(5,2),(7,2)] else 'M')
 s.el((5,5,10,10),'V');s.el((6,6,9,9),'M');s.pt(7,6,'H');s.pt(8,7,'L');s.pt(8,9,'D')
 return s.save(name,False)

def sling():
 s=S();s.l([(3,13),(5,5)],'K',2);s.l([(5,5),(8,10)],'R',2);s.l([(8,10),(12,4)],'K',2)
 s.l([(5,5),(8,10)],'W');s.l([(8,10),(12,4)],'W');s.pol([(7,8),(10,8),(11,11),(9,12),(7,11)],'B')
 s.pol([(8,9),(10,9),(10,11),(8,11)],'R');s.pt(8,9,'Y');s.pt(3,13,'W');return s.save('sling')

def stone():
 s=S();s.pol([(5,4),(9,3),(12,5),(13,8),(11,11),(8,12),(4,11),(3,8),(3,6)],'s')
 s.pol([(5,5),(9,4),(11,6),(11,9),(9,11),(5,10),(4,8)],'t')
 s.l([(5,6),(7,5),(9,5)],'v');s.l([(5,9),(7,11)],'u');s.pt(11,9,'s');return s.save('stone_shot')

def helmet():
 s=S();s.pol([(6,2),(10,2),(12,4),(13,9),(12,12),(10,13),(10,9),(6,9),(6,13),(4,12),(3,9),(4,4)],'D')
 s.pol([(6,3),(10,3),(11,5),(11,7),(5,7),(5,5)],'M');s.l([(6,3),(9,3)],'H')
 s.rec((6,9,7,12),'V');s.rec((10,9,11,12),'V')
 s.l([(8,3),(8,12)],'G',2);s.l([(8,4),(8,10)],'O');s.pt(8,3,'U');return s.save('norse_helmet')

def chest():
 s=S();s.pol([(3,2),(5,3),(6,3),(10,3),(11,3),(13,2),(14,5),(12,8),(12,13),(4,13),(4,8),(2,6)],'K')
 s.pol([(4,3),(6,4),(10,4),(12,3),(13,5),(12,7),(11,6),(11,12),(5,12),(5,6),(4,7),(3,5)],'R')
 s.pol([(5,6),(11,6),(11,11),(5,11)],'V')
 for y in (7,9,11):
  for x in (6,8,10):s.pt(x+(y%2==1 and 1 or 0),y,'L')
 s.l([(5,6),(5,11)],'G');s.l([(11,6),(11,11)],'G')
 s.rec((4,12,12,13),'B');s.pt(8,12,'O');s.pt(4,4,'W');s.pt(12,4,'W');return s.save('norse_chestplate')

def legs():
 s=S();s.pol([(4,2),(11,2),(12,6),(11,13),(8,13),(8,8),(7,8),(7,13),(4,13),(3,6)],'K')
 s.rec((4,3,11,5),'R');s.l([(4,6),(5,12)],'M',2);s.l([(10,6),(10,12)],'M',2)
 s.rec((5,7,6,10),'D');s.rec((9,7,10,10),'D');s.pt(5,7,'H');s.pt(9,7,'H')
 s.l([(4,4),(11,4)],'G');s.pt(8,4,'O');s.rec((4,12,6,13),'B');s.rec((9,12,11,13),'B');return s.save('norse_leggings')

def boots():
 s=S()
 s.pol([(3,3),(6,3),(7,8),(7,12),(5,14),(1,14),(1,12),(2,10)],'K')
 s.pol([(9,3),(12,3),(13,10),(14,12),(14,14),(10,14),(8,12),(8,8)],'K')
 for x in (3,9):
  s.rec((x,4,x+3,7),'R');s.l([(x,5),(x+3,5)],'Y');s.rec((x,9,x+3,11),'M');s.l([(x,9),(x+2,9)],'H')
 s.l([(2,13),(6,13)],'B');s.l([(9,13),(13,13)],'B');s.pt(2,12,'W');s.pt(11,12,'W');return s.save('norse_boots')

def horn(war):
 s=S()
 if war:
  s.pol([(2,12),(4,13),(7,12),(10,9),(12,6),(13,3),(11,2),(10,5),(7,8),(4,9)],'q')
  s.pol([(3,11),(6,11),(9,8),(12,4),(11,3),(10,6),(7,9),(4,10)],'p')
  s.l([(4,10),(6,9),(9,6)],'h');s.rec((3,10,5,12),'G');s.pt(4,10,'U')
  s.pol([(10,2),(12,1),(14,2),(14,4),(12,4)],'J');s.pt(12,2,'O')
 else:
  s.pol([(3,12),(6,13),(8,11),(11,8),(12,4),(11,2),(8,3),(7,6),(5,9)],'q')
  s.pol([(4,11),(6,12),(8,10),(10,7),(11,4),(10,3),(9,4),(8,7),(6,10)],'p')
  s.l([(5,10),(7,10),(9,7)],'w');s.el((9,1,12,4),'K');s.pt(10,2,'h');s.l([(3,11),(5,12)],'R',2)
 return s.save('war_horn' if war else 'carved_horn')

def whet():
 s=S();s.pol([(2,12),(3,10),(10,3),(12,3),(14,5),(13,7),(6,14),(3,14)],'V')
 s.pol([(4,11),(11,4),(13,5),(6,12)],'M');s.l([(5,11),(11,5)],'H');s.l([(6,13),(13,6)],'D')
 s.l([(3,12),(5,14)],'R',2);s.pt(3,12,'O');return s.save('field_whetstone')

def ration():
 s=S();s.pol([(2,8),(5,4),(11,4),(14,8),(13,12),(10,14),(5,13),(2,11)],'K')
 s.pol([(3,8),(5,5),(11,5),(13,8),(12,11),(9,13),(5,12)],'A')
 s.pol([(4,7),(6,5),(11,6),(12,8),(10,11),(6,11)],'C')
 s.l([(5,7),(10,11)],'p',2);s.l([(8,5),(12,9)],'w');s.pt(4,9,'c');s.pt(9,7,'c');return s.save('travel_ration')

def flask():
 s=S();s.pol([(6,2),(10,2),(10,5),(12,6),(13,12),(11,14),(5,14),(3,12),(4,7),(6,5)],'V')
 s.pol([(6,7),(10,7),(12,11),(10,13),(5,13),(4,11)],'s')
 s.pol([(5,8),(8,7),(10,8),(11,11),(9,12),(5,12)],'t')
 s.pt(6,9,'v');s.l([(5,3),(11,3)],'R',2);s.l([(9,4),(13,8)],'G',2);s.pt(9,4,'O');return s.save('pitch_flask')

def binding():
 s=S();s.el((3,4,12,12),'K');s.el((4,5,11,11),'R');s.el((6,6,9,9),'K')
 s.l([(4,11),(9,14)],'B',2);s.l([(9,14),(12,13)],'W');s.pt(4,6,'W');s.pt(10,6,'W');return s.save('leather_binding')

def iron(ore):
 s=S()
 if ore:
  s.pol([(3,5),(6,3),(11,4),(13,7),(12,11),(9,13),(4,12),(2,9)],'x')
  s.pol([(4,5),(8,4),(11,6),(10,9),(5,10),(3,8)],'y')
  s.pol([(5,6),(7,5),(8,6),(7,8),(5,8)],'F');s.pol([(10,8),(12,8),(11,11),(9,10)],'T')
  s.pt(6,6,'E');s.pt(10,10,'W')
 else:
  s.pol([(2,7),(4,5),(11,5),(14,7),(14,11),(12,13),(4,13),(2,11)],'D')
  s.pol([(3,7),(5,6),(11,6),(13,7),(12,10),(4,10)],'L')
  s.l([(5,7),(11,7)],'H');s.l([(4,11),(12,11)],'V');s.pt(3,8,'M')
 return s.save('bog_iron' if ore else 'forged_iron')

names=[]
for func in [lambda:sword('seax',True),lambda:axe('bearded_axe','beard'),lambda:axe('dane_axe','dane'),lambda:sword('viking_sword'),lambda:spear('spear',False),lambda:spear('javelin',True),lambda:axe('throwing_axe','throw'),sling,stone,lambda:shield('round_shield','round'),lambda:shield('celtic_shield','celtic'),lambda:shield('bossed_shield','bossed'),helmet,chest,legs,boots,lambda:horn(True),lambda:horn(False),whet,ration,flask,binding,lambda:iron(True),lambda:iron(False)]:func()
print('Redrawn',len(list(out.glob('*.png'))),'item textures')
# Generate real 16 x 16 showcase, nearest neighbor, neutral gray inventory-style.
names=['seax','bearded_axe','dane_axe','viking_sword','spear','javelin','throwing_axe','sling','stone_shot','round_shield','celtic_shield','bossed_shield','norse_helmet','norse_chestplate','norse_leggings','norse_boots','war_horn','carved_horn','field_whetstone','travel_ration','pitch_flask','leather_binding','bog_iron','forged_iron']
font=ImageFont.load_default()
preview=Image.new('RGB',(6*164+28,4*177+44),'#20252b');dr=ImageDraw.Draw(preview)
for i,n in enumerate(names):
 x=16+(i%6)*164;y=12+(i//6)*177
 for iy in range(16):
  for ix in range(16):
   clr='#48505b' if (ix//2+iy//2)%2 else '#3d454f'
   dr.rectangle((x+ix*8,y+iy*8,x+ix*8+7,y+iy*8+7),fill=clr)
 im=Image.open(out/(n+'.png')).resize((128,128),Image.Resampling.NEAREST)
 preview.paste(im,(x,y),im)
 dr.text((x,y+131),n,fill='#f0f0e7',font=font)
preview.save(root/'ART_PREVIEW.png')
print('Preview created')
