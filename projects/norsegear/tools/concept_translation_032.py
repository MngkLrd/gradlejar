#!/usr/bin/env python3
"""Refine real Minecraft JSON cuboids and pixel atlas. No image-generation inputs.
Runs after old 0.3.1 source checkout, preserves all gameplay code and datapacks.
"""
import json, math, re
from pathlib import Path
from PIL import Image, ImageDraw
ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/norsegear'
ITEM=A/'models/item'
TEX=A/'textures/item/norse_showcase_materials.png'
ATLAS=Image.open(TEX).convert('RGBA')
D=ImageDraw.Draw(ATLAS)
# Cohesive material cells, each 16x16 pixels in a 256 atlas. Derive positions
# from the original 0.3.1 generator so every model UV remains compatible.
material_names={
 'steel':(128,0),'steel_dark':(144,0),'steel_light':(160,0),'steel_edge':(176,0),
 'steel_trim':(192,0),'steel_etch':(208,0),'steel_socket':(224,0),'steel_boss':(240,0),
 'wood':(128,16),'wood_light':(144,16),'wood_end':(160,16),'wood_dark':(176,16),
 'leather':(192,16),'leather_wrap':(208,16),'leather_dark':(224,16),'leather_trim':(240,16),
 'bronze':(128,32),'bronze_light':(144,32),'rivet':(160,32),'steel_ridge':(176,32),
 'bone':(192,32),'fur':(208,32),'mail':(224,32),'shield_wood':(240,32),
 'paint_red':(128,48),'paint_cream':(144,48),'paint_green':(160,48),'steel_flat':(176,48),
 'iron_blue':(192,48),'engraved':(208,48),
}
# Precise sampled palette for the concept: brushed cold steel; structural wood,
# dark recesses, bright beveled corners and warm leather.
C={
 'steel':('#8198a9','#dfebf1','#344d63'),
 'steel_dark':('#516b82','#a3b9c8','#25394b'),
 'steel_light':('#b5ccda','#f5fbfc','#718da4'),
 'steel_edge':('#bfd7e4','#ffffff','#6e92a6'),
 'steel_trim':('#647f95','#ccdce7','#334a61'),
 'steel_etch':('#4b647d','#a4bbc9','#263a50'),
 'steel_socket':('#8497a5','#dfeaf0','#405365'),
 'steel_boss':('#92a9bc','#f0f8fa','#405b74'),
 'wood':('#93603b','#d09b61','#52341f'),
 'wood_light':('#ba7e46','#e6b377','#724020'),
 'wood_end':('#b28858','#efd19a','#6c4b2b'),
 'wood_dark':('#634028','#9e6f48','#3d271b'),
 'leather':('#8c5235','#d08b57','#492d21'),
 'leather_wrap':('#945c3f','#d99a63','#452b23'),
 'leather_dark':('#613b2c','#a36a4c','#2e201c'),
 'leather_trim':('#b77c4c','#f2bc80','#664128'),
 'bronze':('#b48d54','#f5d498','#71502c'),
 'bronze_light':('#dbb575','#ffdfa0','#95653a'),
 'rivet':('#94b0c0','#f4fafc','#506476'),
 'steel_ridge':('#87a8bf','#f7fafb','#456782'),
 'bone':('#e0cea3','#fff4d4','#9e8663'),
 'fur':('#bca78a','#f2e0ba','#77614f'),
 'mail':('#738899','#e3ebf1','#26394a'),
 'shield_wood':('#a5784a','#e8b57d','#563a25'),
 'paint_red':('#bb4d41','#ea836b','#762f29'),
 'paint_cream':('#dcc6a0','#fff1cb','#a18b6c'),
 'paint_green':('#5b7d66','#9bc39c','#314c3c'),
 'steel_flat':('#768da3','#bfdae4','#364e64'),
 'iron_blue':('#799eb7','#e0f1f8','#3b607b'),
 'engraved':('#4c657d','#9bb8c7','#243a51'),
}
from PIL import ImageColor

def patch(name):
 x,y=material_names[name];b,h,s=[ImageColor.getrgb(c) for c in C[name]]
 D.rectangle((x,y,x+15,y+15),fill=(*b,255))
 D.polygon([(x,y),(x+15,y),(x+13,y+2),(x+1,y+2)], fill=(*h,255))
 D.line((x+0,y+2,x+0,y+13),fill=(*h,255),width=1)
 D.rectangle((x,y+13,x+15,y+15),fill=(*s,255))
 D.line((x+14,y+4,x+14,y+12),fill=(*s,255))
 if name.startswith(('steel','iron')) or name in ('bronze','bronze_light','rivet','engraved'):
  D.rectangle((x+2,y+4,x+12,y+10),fill=(*b,255))
  D.line((x+3,y+4,x+11,y+4),fill=(*h,255))
  D.line((x+4,y+10,x+11,y+10),fill=(*s,255))
  if 'edge' in name or 'ridge' in name:
   D.line((x+5,y+3,x+5,y+12),fill=(*h,255))
  if name in ('engraved','steel_etch'):
   D.line((x+4,y+8,x+6,y+6,x+9,y+9,x+11,y+6),fill=(*s,255),width=1)
 elif name.startswith(('wood','leather')) or name=='shield_wood':
  for dx in (3,8,11):
   D.line((x+dx,y+3,x+dx-(1 if dx%2 else 0),y+12),fill=(*(h if dx==8 else s),255))
  if 'leather' in name:
   D.line((x+2,y+4,x+11,y+11),fill=(*h,255),width=1)
   D.line((x+3,y+5,x+12,y+12),fill=(*s,255),width=1)
 elif name=='mail':
  D.rectangle((x,y,x+15,y+15),fill=(*s,255))
  for py in range(1,15,4):
   for px in range(1+(py//4)%2*2,15,4):
    D.arc((x+px-1,y+py,x+px+2,y+py+2),0,260,fill=(*h,255))
    D.point((x+px+1,y+py+2),fill=(*b,255))
for name in C:patch(name)
# 64px front: plank joins and a bold red/ivory swirl; make painting intentional,
# add 1px wood fibers sparingly and avoid black pixel noise.
for xx in range(4,63,8):
 for yy in range(7,57):
  pix=ATLAS.getpixel((xx,yy))
  if pix[3] and pix[0]>0:
   a=(max(0,pix[0]-27),max(0,pix[1]-27),max(0,pix[2]-23),255)
   ATLAS.putpixel((xx,yy),a)
for xx in range(3,63,9):
 for yy in range(8,56,13):
  if ATLAS.getpixel((xx+1,yy))[3]:
   pix=ATLAS.getpixel((xx+1,yy));ATLAS.putpixel((xx+1,yy),tuple(min(255,p+20) for p in pix[:3])+(255,))
# rear shield grips and wood already painted into first 64x64 (x=64..127).
ATLAS.save(TEX)

def uv(mat):
 x,y=material_names[mat];return [x/16,y/16,x/16+1,y/16+1]

def cube(name,xyz,upper,material,face=None):
 fr=[round(float(v),4) for v in xyz];to=[round(float(v),4) for v in upper]
 assert all(0<=fr[k]<to[k]<=16 for k in range(3)),(name,fr,to)
 mats={f:material for f in ['north','south','east','west','up','down']}
 if face:mats.update(face)
 return {'name':name,'from':fr,'to':to,'faces':{f:{'uv':uv(v),'texture':'#atlas'} for f,v in mats.items()}}

# SPEAR: leaf-shaped stepped head, pronounced central steel fuller and raised rib,
# true conical reduction of cross-section instead of a cuboid stick ending.
for kind in ('spear','javelin'):
 path=ITEM/(kind+'.json');data=json.loads(path.read_text())
 # Retain original long shaft and leather; replace tiny existing blade.
 els=[e for e in data['elements'] if e.get('name') in ('ash-wood-grain','pommel','leather-helix-band','handle-ferrule')]
 start=11.05 if kind=='spear' else 11.6
 half=1.0 if kind=='spear' else 0.81
 els += [cube('forged-socket',(7.45,start-.62,7.45),(8.55,start+.52,8.55),'steel_socket'),
         cube('socket-taper',(7.59,start+.45,7.59),(8.41,start+1.05,8.41),'steel_trim'),
         cube('socket-bright-collar',(7.35,start-.46,7.35),(8.65,start-.23,8.65),'steel_edge'),
         cube('socket-bottom-band',(7.37,start-.79,7.37),(8.63,start-.65,8.63),'steel_trim')]
 # Cross-section varies along the blade; layered steel front/back and a narrow
 # central ridge create a multi-faceted, raised tip in Minecraft's cuboid system.
 sections=[(.7,1.05,.67),(.95,1.65,.85),(1.65,2.25,1.48),(2.25,2.65,1.55),(2.65,3.10,1.24),(3.10,3.53,.88),(3.53,3.92,.46),(3.92,4.13,.18)]
 factor=.88 if kind=='javelin' else 1.0
 for i,(bottom,top,w) in enumerate(sections):
  y0,y1=start+bottom*factor,start+top*factor
  if y1>15.96:y1=15.96
  if y0>=y1:continue
  ww=w*factor
  els.append(cube(f'leaf-profile-{i}',(8-ww,y0,7.70),(8+ww,y1,8.31),'steel_light' if i in (0,6,7) else 'steel_flat',face={'south':'steel','north':'steel_dark'}))
  if i<7:
   els.append(cube(f'leaf-edge-rim-left-{i}',(8-ww-.08,y0,7.81),(8-ww+.16,y1,8.23),'steel_edge'))
   els.append(cube(f'leaf-edge-rim-right-{i}',(8+ww-.16,y0,7.81),(8+ww+.08,y1,8.23),'steel_edge'))
  rib=.29 if i<5 else .19
  els.append(cube(f'raised-spine-{i}',(8-rib,y0,8.32),(8+rib,y1,8.68 if i<6 else 8.5),'steel_ridge',face={'south':'steel_edge'}))
 # Two small iron pins holding tang in shaft
 for side in [-1,1]:
  els.append(cube('socket-rivet',(7.5+side*.46,start-.2,7.5),(7.72+side*.46,start+.03,7.72),'rivet')) if side==1 else None
 data['elements']=els
 data['display']['gui']={'rotation':[18,30,-36],'translation':[0,0,0],'scale':[.92,.92,.92]}
 path.write_text(json.dumps(data,indent=2)+'\n')

# Shield: model still uses plank strips and a circular silhouette, but now add
# real layered embossing, iron rim clamp plates and faceted umbon rather than flat plate.
path=ITEM/'round_shield.json';data=json.loads(path.read_text());els=data['elements']
for i in range(12):
 ang=2*math.pi*i/12;cx,cy=8+7.1*math.cos(ang),8+7.1*math.sin(ang)
 # Keep decorative bolts aligned around the perimeter of the disk.
 ex=.22
 if ex<cx-ex<cx+ex<16 and ex<cy-ex<cy+ex<16:
  els.append(cube(f'rim-rivet-{i}',(cx-ex,cy-ex,8.44),(cx+ex,cy+ex,8.92),'rivet'))
  els.append(cube(f'rim-rivet-highlight-{i}',(cx-.09,cy-.08,8.94),(cx+.09,cy+.1,9.05),'steel_edge'))
# staggered octagonal-looking umbon; small steps prevent entire disk from being box.
for i,(rad,z0,z1,mat) in enumerate([(2.85,8.8,9.12,'steel_dark'),(2.55,9.12,9.4,'steel_trim'),(2.17,9.4,9.72,'steel_boss'),(1.67,9.72,10.03,'steel_light'),(.98,10.03,10.25,'steel_ridge'),(.4,10.25,10.33,'steel_edge')]):
 els.append(cube(f'raised-umbon-tier-{i}',(8-rad,8-rad,z0),(8+rad,8+rad,z1),mat))
# Four clamps around disk hold iron rim in place, with dark inset and rivets.
for name,x0,y0,x1,y1 in [('north',7.3,14.28,8.7,15.36),('south',7.3,.64,8.7,1.72),('west',.64,7.3,1.72,8.7),('east',14.28,7.3,15.36,8.7)]:
 els.append(cube(f'{name}-strap-clamp',(x0,y0,8.42),(x1,y1,8.71),'steel_trim'))
 # manually small rivets for each clamp
 xc=(x0+x1)/2;yc=(y0+y1)/2
 els.append(cube(f'{name}-strap-rivet',(xc-.19,yc-.19,8.73),(xc+.19,yc+.19,9.01),'rivet'))
# back hand-grip with buckled iron anchoring
els.append(cube('back-oak-grip',(6.63,6.8,5.95),(9.37,9.2,6.65),'wood_dark'))
for y in (6.88,8.67):
 els.append(cube('back-grip-clamp',(6.57,y,5.81),(9.44,y+.43,6.05),'steel_trim'))
 for x in (6.95,8.91):els.append(cube('grip-strap-bolt',(x,y+.11,5.68),(x+.18,y+.29,5.81),'rivet'))
data['elements']=els
data['display']['thirdperson_lefthand']['translation'][2]=4.1
data['display']['thirdperson_righthand']['translation'][2]=4.1
path.write_text(json.dumps(data,indent=2)+'\n')

# armor class is game-facing geometry, not just a preview model.
jpath=ROOT/'src/main/java/dev/norsegear/client/model/NorseArmorModel.java'
src=jpath.read_text()
# Each new detail is a real child on an animated humanoid bone.
parts=[
 ('head','crown_upper_band',(-4.34,-8.6,-4.35,8.68,.48,8.70)),
 ('head','crown_lower_band',(-4.35,-6.45,-4.52,8.7,.48,8.9)),
 ('head','nasal_crest',( -.36,-6.65,-5.35,.72,4.35,.42)),
 ('head','left_cheek_rim',(3.72,-4.1,-4.24,.62,2.35,1.5)),
 ('head','right_cheek_rim',(-4.34,-4.1,-4.24,.62,2.35,1.5)),
 ('head','back_brow_stud',(-.55,-6.56,4.25,1.1,.7,.42)),
 ('head','headband_stud_left',(-3.15,-6.78,-5.16,.65,.63,.46)),
 ('head','headband_stud_right',(2.55,-6.78,-5.16,.65,.63,.46)),
 ('body','left_chest_strap',(-3.58,1.6,-3.95,.92,6.18,.58)),
 ('body','right_chest_strap',(2.65,1.6,-3.95,.92,6.18,.58)),
 ('body','left_armor_rail',(-4.15,3.0,-3.66,.66,4.6,.64)),
 ('body','right_armor_rail',(3.49,3.0,-3.66,.66,4.6,.64)),
 ('body','chest_heart_plate',(-1.35,3.35,-4.33,2.7,1.65,.58)),
 ('body','belt_lower_trim',(-4.33,9.75,-3.16,8.66,.5,6.32)),
 ('body','left_mail_tab',(-3.8,10.1,-3.01,2.0,2.8,.85)),
 ('body','right_mail_tab',(1.8,10.1,-3.01,2.0,2.8,.85)),
 ('body','back_horizontal_strap',(-3.9,4.55,3.34,7.8,.9,.75)),
 ('right_arm','pauldron_top_plate',(-4.23,-2.48,-2.75,5.38,1.05,5.5)),
 ('left_arm','pauldron_top_plate',(-1.15,-2.48,-2.75,5.38,1.05,5.5)),
 ('right_arm','upper_arm_plate',(-3.7,1.0,-2.55,4.85,2.3,5.07)),
 ('left_arm','upper_arm_plate',(-1.14,1.0,-2.55,4.85,2.3,5.07)),
 ('right_arm','arm_bracer',(-3.52,6.05,-2.37,4.44,2.0,4.74)),
 ('left_arm','arm_bracer',(-.92,6.05,-2.37,4.44,2.0,4.74)),
 ('right_leg','greave_front',(-2.28,7.05,-3.04,4.56,2.3,.82)),
 ('left_leg','greave_front',(-2.28,7.05,-3.04,4.56,2.3,.82)),
 ('right_leg','shin_band',(-2.35,8.85,-3.09,4.7,.56,1.13)),
 ('left_leg','shin_band',(-2.35,8.85,-3.09,4.7,.56,1.13)),
]
# texOffs initial seed later adjusted by UV repacker (our own, not old source tool).
new_lines=[];firstx=0
for parent,name,v in parts:
 x,y,z,dx,dy,dz=v
 obj={'head':'head','body':'body','right_arm':'root.getChild("right_arm")','left_arm':'root.getChild("left_arm")','right_leg':'root.getChild("right_leg")','left_leg':'root.getChild("left_leg")'}[parent]
 new_lines.append(f'        {obj}.addOrReplaceChild("{name}", CubeListBuilder.create().texOffs(0, 0)\n            .addBox({x:.2f}F, {y:.2f}F, {z:.2f}F, {dx:.2f}F, {dy:.2f}F, {dz:.2f}F),\n            net.minecraft.client.model.geom.PartPose.ZERO);')
assert src.count('        return LayerDefinition.create(mesh, 256, 256);')==1
src=src.replace('        return LayerDefinition.create(mesh, 256, 256);','\n'.join(new_lines)+'\n        return LayerDefinition.create(mesh, 256, 256);')
# Reversible slot masking must explicitly include new child names
for slot,arr in [('head',[n for p,n,_ in parts if p=='head']),('body',[n for p,n,_ in parts if p=='body'])]:
 old=f'        {slot}.visible = '+('helmet;' if slot=='head' else 'chest;')
 assert old in src
 indent=f'        for (String detail : new String[]{{{", ".join(chr(34)+n+chr(34) for n in arr)}}}) {slot}.getChild(detail).visible = '+('helmet;' if slot=='head' else 'chest;')
 src=src.replace(old,old+'\n'+indent)
for parent in ('rightArm','leftArm'):
 old=f'        {parent}.getChild("pauldron_rim").visible = chest;'
 assert old in src
 src=src.replace(old,old+'\n'+''.join(f'        {parent}.getChild("{n}").visible = chest;\n' for n in ('pauldron_top_plate','upper_arm_plate','arm_bracer')).rstrip())
old='            leg.getChild("boot_toe").visible = boots;'
assert old in src
src=src.replace(old,old+'\n            leg.getChild("greave_front").visible = legs;\n            leg.getChild("shin_band").visible = legs;')
jpath.write_text(src)
# Pack updated Java UVs, with no overlapping cube faces. Reuses the project's
# field-tested UV packer after removing its initial texture generated elsewhere.
import subprocess,sys
subprocess.run([sys.executable,str(ROOT/'tools/armor_uv031.py')],cwd=ROOT,check=True)
# Repaint the revised 256px atlas with material-specific rich pixel clusters
# around steel edge / warm leather / mail. Never modify alpha holes in mail.
uv_manifest=json.loads((ROOT/'tools/armor_uv_manifest_031.json').read_text())
for layer in ('ironweave_layer_1.png','ironweave_layer_2.png'):
 path=A/'textures/models/armor'/layer
 im=Image.open(path).convert('RGBA');q=ImageDraw.Draw(im)
 for part in uv_manifest:
  name=part['name'];u,v=part['u'],part['v'];sx,sy,sz=part['size'];ww=int(round(sx));hh=int(round(sy));dd=int(round(sz))
  if name in ('crown_upper_band','crown_lower_band','pauldron_top_plate','upper_arm_plate','greave_front','shin_band'):
   for bx,by,bw,bh in [(u+dd,v+dd,ww,hh),(u+dd+ww+dd,v+dd,ww,hh)]:
    if bw<2 or bh<2:continue
    q.line((bx+1,by+1,bx+bw-2,by+1),fill=(231,239,242,255))
    if bh>3:q.line((bx+1,by+bh-2,bx+bw-2,by+bh-2),fill=(50,72,90,255))
    if name in ('upper_arm_plate','pauldron_top_plate') and bw>=4 and bh>=4:
     q.rectangle((bx+int(bw/2)-1,by+int(bh/2)-1,bx+int(bw/2),by+int(bh/2)),fill=(222,231,230,255))
  if name in ('left_chest_strap','right_chest_strap'):
   # separate leather thickness, brighter edge, dark stitching
   x0=u+dd; y0=v+dd
   for yy in range(y0+1,y0+hh,3):
    if ww>0:q.point((x0,yy),fill=(225,173,114,255))
 im.save(path)
# Update release metadata, README
pr=ROOT/'gradle.properties';pr.write_text(pr.read_text().replace('mod_version=0.3.1','mod_version=0.3.2'))
bp=ROOT/'build-profile.json';j=json.loads(bp.read_text());j['version']='0.3.2';bp.write_text(json.dumps(j,indent=2)+'\n')
readme=ROOT/'README.md';readme.write_text(readme.read_text()+'''\n\n## 0.3.2 Concept-to-Game Geometry\n- Rebuilt spear/javelin leaf with faceted, stepped steel sections and raised spine.\n- Round shield gains 12 iron rim rivets, inset clamps, layered umbon and reverse grip.\n- Worn armor adds animated helmet bands, cheek rims, armor straps, chest plate, pauldrons and greaves.\n- Distinct 256x256 UV pixels for each worn-armor structural detail.\n- Inspect PREVIEW_3D_ACTUAL_032.png and PREVIEW_ARMOR_ACTUAL_032.png, both source-rendered.\n- Optional Better Combat support, all original gameplay/recipes/worldgen preserved.\n''')
print('Updated JSON models, armor Java, textured UVs and version 0.3.2')
