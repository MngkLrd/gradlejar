package dev.fractal.cell;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

public final class CellData extends SavedData {
 public static final int LIMIT=256,MAX_DEPTH=3;
 public record Address(ResourceKey<Level> dimension,BlockPos position){}
 public static final class Cell {
  public final int id,parent,depth; public final Address address; public final Material material;
  public boolean destroyed;
  public int reinforcement,conductivity,light,input,output,localR,localC,localL;
  public String preview="";
  Cell(int id,int parent,int depth,Address address,Material material){this.id=id;this.parent=parent;this.depth=depth;this.address=address;this.material=material;int[] initial=new int[6];for(int i=0;i<6;i++)initial[i]=material.initial(i);var p=LatticeMath.evaluate(initial);reinforcement=localR=p.reinforcement();conductivity=localC=p.conductivity();light=localL=p.light();}
 }
 public final Map<Integer,Cell> cells=new LinkedHashMap<>();
 private final Map<Address,Integer> hosts=new HashMap<>();
 private int next;
 public static CellData get(MinecraftServer s){return s.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(CellData::new,CellData::read,null),"fractal_cells_v1");}
 public Cell host(ResourceKey<Level> dim,BlockPos pos){return cells.get(hosts.get(new Address(dim,pos)));}
 public Cell at(BlockPos pos){int id=CellSpace.index(pos);return id<0?null:cells.get(id);}
 public boolean available(){return next<LIMIT;}
 public boolean live(Cell c){for(int n=0;c!=null&&n<=MAX_DEPTH;n++){if(c.destroyed)return false;if(c.parent<0)return true;c=cells.get(c.parent);}return false;}
 public Cell allocate(ResourceKey<Level> dim,BlockPos p,Cell parent,Material m){
  if(!available()||(parent!=null&&(!live(parent)||parent.depth>=MAX_DEPTH)))throw new IllegalStateException("cell limit");
  Cell c=new Cell(next++,parent==null?-1:parent.id,parent==null?1:parent.depth+1,new Address(dim,p.immutable()),m);
  cells.put(c.id,c);hosts.put(c.address,c.id);setDirty();return c;
 }
 public void destroyAt(ResourceKey<Level> dim,BlockPos p){Cell c=host(dim,p);if(c!=null)destroy(c);}
 public void destroy(Cell c){if(!c.destroyed){c.destroyed=true;hosts.remove(c.address,c.id);setDirty();}}
 @Override public CompoundTag save(CompoundTag t,HolderLookup.Provider regs){
  t.putInt("next",next);ListTag list=new ListTag();
  for(Cell c:cells.values()){CompoundTag n=new CompoundTag();n.putInt("id",c.id);n.putInt("parent",c.parent);n.putInt("depth",c.depth);n.putString("dim",c.address.dimension().location().toString());n.putLong("pos",c.address.position().asLong());n.putString("material",c.material.name());n.putBoolean("destroyed",c.destroyed);n.putInt("reinforcement",c.reinforcement);n.putInt("conductivity",c.conductivity);n.putInt("light",c.light);n.putInt("input",c.input);n.putInt("output",c.output);n.putInt("localR",c.localR);n.putInt("localC",c.localC);n.putInt("localL",c.localL);n.putString("preview",c.preview);list.add(n);}
  t.put("cells",list);return t;
 }
 private static CellData read(CompoundTag t,HolderLookup.Provider regs){
  CellData d=new CellData();d.next=Math.max(0,t.getInt("next"));ListTag list=t.getList("cells",Tag.TAG_COMPOUND);
  for(int i=0;i<list.size();i++){CompoundTag n=list.getCompound(i);int id=n.getInt("id"),depth=n.getInt("depth");if(id<0||id>=LIMIT||depth<1||depth>MAX_DEPTH)continue;
   try{Cell c=new Cell(id,n.getInt("parent"),depth,new Address(ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(n.getString("dim"))),BlockPos.of(n.getLong("pos"))),Material.valueOf(n.getString("material")));c.destroyed=n.getBoolean("destroyed");c.reinforcement=Math.clamp(n.getInt("reinforcement"),0,3);c.conductivity=Math.clamp(n.getInt("conductivity"),0,3);c.light=Math.clamp(n.getInt("light"),0,3);c.input=Math.clamp(n.getInt("input"),0,15);c.output=Math.clamp(n.getInt("output"),0,15);c.localR=n.contains("localR")?Math.clamp(n.getInt("localR"),0,3):c.reinforcement;c.localC=n.contains("localC")?Math.clamp(n.getInt("localC"),0,3):c.conductivity;c.localL=n.contains("localL")?Math.clamp(n.getInt("localL"),0,3):c.light;c.preview=n.getString("preview");if(c.preview.length()>Preview.MAX_ENCODED||!c.preview.startsWith("v3:"))c.preview="";d.cells.put(id,c);if(!c.destroyed)d.hosts.put(c.address,id);d.next=Math.max(d.next,id+1);}catch(RuntimeException ignored){}
  }return d;
 }
}
