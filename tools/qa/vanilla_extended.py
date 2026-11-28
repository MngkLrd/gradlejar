"""R6: analyze deferred vanilla metadata and large images without treating atlases as sprites."""
import hashlib
import json
from collections import Counter, defaultdict
from .visual_lint import decode, stats

PREFIX="assets/minecraft/textures/"
MAX_FRAMES=4096
MAX_FRAME_PIXELS=65536

def positive_int(n,label):
    if type(n) is not int or n<1:
        raise ValueError(label+" must be a positive integer")
    return n

def role_hint(path):
    parts=path.split("/")
    root=parts[0]
    if root=="gui":
        return "gui_sprite" if len(parts)>1 and parts[1]=="sprites" else "gui_sheet_or_background"
    return {"entity":"entity_skin_or_layer","models":"equipment_uv_or_layer",
            "armor":"equipment_uv_or_layer","font":"font_sheet","colormap":"color_lookup",
            "environment":"environment_image","particle":"particle_sprite",
            "block":"block_texture_or_layer","item":"item_texture_or_layer",
            "map":"map_texture_or_layer","painting":"painting","effect":"effect_icon",
            "misc":"misc_image_or_mask"}.get(root,"other/"+root)

def alpha_summary(im):
    alpha=im.getchannel("A")
    h=alpha.histogram()
    bbox=alpha.getbbox()
    return {"transparent":h[0],"opaque":h[255],"partial":sum(h[1:255]),
            "visible_bbox":list(bbox) if bbox else None,
            "visible_fraction":round((im.width*im.height-h[0])/(im.width*im.height),6)}

def animation_plan(im,meta):
    if not isinstance(meta,dict):raise ValueError("animation must be an object")
    w,h=im.size
    fw,fh=meta.get("width"),meta.get("height")
    # Standard vertical strip default. Support both explicit axes. Other
    # layouts with omitted axes are left unresolved rather than guessed.
    if fw is None and fh is None:
        if w>h:raise NotImplementedError("horizontal default frame layout not audited")
        fw=fh=w
    elif fw is None or fh is None:
        raise NotImplementedError("single explicit frame dimension not audited")
    fw=positive_int(fw,"width");fh=positive_int(fh,"height")
    if w%fw or h%fh:raise ValueError("frame dimensions do not divide image")
    count=(w//fw)*(h//fh)
    if count>MAX_FRAMES or fw*fh>MAX_FRAME_PIXELS:
        raise NotImplementedError("frame analysis budget exceeded")
    default=positive_int(meta.get("frametime",1),"frametime")
    interpolation=meta.get("interpolate",False)
    if type(interpolation) is not bool:raise ValueError("interpolate must be boolean")
    frames=meta.get("frames",[])
    if not isinstance(frames,list):raise ValueError("frames must be a list")
    if not frames:frames=list(range(count))
    if len(frames)>MAX_FRAMES:raise NotImplementedError("sequence analysis budget exceeded")
    sequence=[]
    for f in frames:
        index=f.get("index") if isinstance(f,dict) else f
        duration=positive_int(f.get("time",default),"frame time") if isinstance(f,dict) else default
        if type(index) is not int or not 0<=index<count:
            raise ValueError("frame index outside atlas")
        sequence.append((index,duration))
    return fw,fh,count,sequence,interpolation

def transition(a,b):
    # Compare visible RGBA, ignoring hidden RGB in fully transparent pixels.
    changed=mask=both=0;distance=0
    for p,q in zip(a.getdata(),b.getdata()):
        changed+=((p!=q) and (p[3] or q[3]))!=0
        mask+=(bool(p[3])!=bool(q[3]))
        if p[3] and q[3]:
            both+=1
            distance+=sum(abs(p[k]-q[k]) for k in range(3))
    return {"changed_fraction":round(changed/(a.width*a.height),6),
            "mask_changed_fraction":round(mask/(a.width*a.height),6),
            "mean_rgb_delta_both_visible":round(distance/(both*3*255),6) if both else None}

def analyze_animation(im,meta):
    fw,fh,count,sequence,interpolation=animation_plan(im,meta)
    cols=im.width//fw
    frames={}
    rows=[]
    for index in sorted({i for i,t in sequence}):
        x=(index%cols)*fw;y=(index//cols)*fh
        frame=im.crop((x,y,x+fw,y+fh));frames[index]=frame
        rows.append({"index":index,"rgba_sha256":hashlib.sha256(frame.tobytes()).hexdigest(),
                     "metrics":stats(frame)})
    # Analyze each directed transition once, even if a long sequence repeats it.
    edges=Counter((sequence[i][0],sequence[(i+1)%len(sequence)][0]) for i in range(len(sequence)))
    transitions=[{"from":i,"to":j,"occurrences":n,**transition(frames[i],frames[j])}
                 for (i,j),n in sorted(edges.items())]
    return {"frame_width":fw,"frame_height":fh,"available_frames":count,"used_frames":len(frames),
            "distinct_used_rgba_frames":len({r["rgba_sha256"] for r in rows}),
            "sequence_length":len(sequence),"cycle_ticks":sum(t for i,t in sequence),
            "interpolate":interpolation,"sequence":[{"index":i,"ticks":t} for i,t in sequence],
            "frames":rows,"transitions":transitions,"visual_review":"NOT_RUN"}

def deferred_row(z,row):
    path=PREFIX+row["path"]
    im=decode(z.read(path))
    out={"path":row["path"],"role_hint":role_hint(row["path"]),"sha256":row["sha256"],
         "width":im.width,"height":im.height,"original_status":row["status"],
         "alpha":alpha_summary(im),"visual_review":"NOT_RUN","renderer_review":"NOT_RUN"}
    if row["metadata_present"]:
        metadata=json.loads(z.read(path+".mcmeta"))
        if not isinstance(metadata,dict):raise ValueError("metadata must be an object")
        out["metadata_sections"]=sorted(metadata)
        out["metadata_sha256"]=hashlib.sha256(z.read(path+".mcmeta")).hexdigest()
        if "gui" in metadata:
            gui=metadata["gui"]
            if not isinstance(gui,dict):raise ValueError("gui must be an object")
            scaling=gui.get("scaling",{})
            if not isinstance(scaling,dict):raise ValueError("scaling must be an object")
            out["gui_scaling"]={k:scaling[k] for k in ("type","width","height","border") if k in scaling}
        if "animation" in metadata:
            try:
                out["animation"]=analyze_animation(im,metadata["animation"])
                out["status"]="ANIMATION_FRAMES_MEASURED"
            except NotImplementedError as e:
                out.update(status="ANIMATION_LAYOUT_UNSUPPORTED",reason=str(e))
        else:
            out["status"]="NON_ANIMATION_METADATA_INSPECTED"
    else:
        out["status"]="LARGE_IMAGE_STRUCTURE_MEASURED"
    return out

def extended_inventory(z,rows):
    result=[];roles=defaultdict(Counter)
    for row in rows:
        role=role_hint(row["path"])
        roles[role][row["status"]]+=1
        if row["status"] not in ("METADATA_REVIEW_REQUIRED","LARGE_TEXTURE_INVENTORIED"):continue
        try:result.append(deferred_row(z,row))
        except (ValueError,OSError) as exc:
            result.append({"path":row["path"],"status":"ANALYSIS_ERROR","reason":str(exc),"visual_review":"NOT_RUN"})
    totals=dict(Counter(r["status"] for r in result))
    return {"schema":1,"scope":"Deferred PNGs: metadata/frame and large-image structure metrics, role hints only. No UV segmentation or rendered animation/GUI certification.",
            "deferred_count":len(result),"statuses":totals,"role_coverage":{k:dict(v) for k,v in sorted(roles.items())},
            "animation_used_frames":sum(r.get("animation",{}).get("used_frames",0) for r in result),
            "animation_distinct_frames":sum(r.get("animation",{}).get("distinct_used_rgba_frames",0) for r in result),
            "assets":result}
