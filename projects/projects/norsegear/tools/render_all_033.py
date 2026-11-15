#!/usr/bin/env python3
"""Source-driven showcase for all 24 actual Norse Gear OBJ game meshes."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
from render_obj_032 import render

ROOT = Path(__file__).resolve().parents[1]
ITEMS = [
    ('seax','SEAX'), ('bearded_axe','BEARDED AXE'), ('dane_axe','DANE AXE'),
    ('viking_sword','VIKING SWORD'), ('spear','SPEAR'), ('javelin','JAVELIN'),
    ('throwing_axe','THROWING AXE'), ('round_shield','ROUND SHIELD'),
    ('celtic_shield','CELTIC SHIELD'), ('bossed_shield','BOSSED SHIELD'),
    ('norse_helmet','NORSE HELMET'), ('norse_chestplate','CHESTPLATE'),
    ('norse_leggings','LEGGINGS'), ('norse_boots','BOOTS'),
    ('sling','SLING'), ('stone_shot','STONE SHOT'),
    ('war_horn','WAR HORN'), ('field_whetstone','WHETSTONE'),
    ('travel_ration','TRAVEL RATION'), ('pitch_flask','PITCH FLASK'),
    ('bog_iron','BOG IRON'), ('forged_iron','FORGED IRON'),
    ('leather_binding','LEATHER BINDING'), ('carved_horn','CARVED HORN')
]
W,COLUMNS,CW,CH = 1500,4,355,335
HEIGHT = 110 + ((len(ITEMS)+COLUMNS-1)//COLUMNS)*CH + 40
atlas = Image.new('RGB', (W, HEIGHT), '#111b25')
draw = ImageDraw.Draw(atlas)
try:
    font = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 25)
    small = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 16)
except OSError:
    font = ImageFont.load_default()
    small = font
draw.text((34,22),'NORSE GEAR 0.3.3  |  ALL 24 IN-GAME OBJ MODELS',font=font,fill='#f1f5f9')
draw.text((34,64),'Rendered from the shipped OBJ meshes and 256px texture atlas. Not Minecraft client screenshots.',font=small,fill='#93b1c3')
for i,(name,label) in enumerate(ITEMS):
    x=32+(i % COLUMNS)*365
    y=108+(i//COLUMNS)*CH
    draw.rounded_rectangle((x,y,x+CW-10,y+CH-10),radius=12,fill='#1a2b3a',outline='#536b82',width=2)
    angle=45 if 'shield' in name else 32
    if name in ('norse_helmet','norse_chestplate','norse_leggings','norse_boots'): angle=18
    picture=render(name,az=angle,elev=14,w=CW-20,h=CH-82,zoom=.86)
    atlas.paste(picture,(x+6,y+6),picture)
    draw.text((x+14,y+CH-54),label,font=small,fill='#f1f5fa')
    draw.text((x+14,y+CH-29),f'{i+1:02d} / 24  -  textured OBJ',font=small,fill='#9eb5c5')
out=ROOT/'PREVIEW_ALL_24_ACTUAL_033.png'
atlas.save(out,optimize=True)
print('Rendered 24 real OBJ meshes:',out)
