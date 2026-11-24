package dev.worldfold.fold;

import dev.worldfold.WorldFold;
import dev.worldfold.block.SeamAnchorBlock;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

public record Anchor(ResourceKey<Level> dimension,BlockPos floor,Direction normal,int w,int h,double scale) {
    public double width(){return w*scale;}
    public double height(){return h*scale;}
    public Vec3 center(){return new Vec3(floor.getX()+.5,floor.getY()+1+height()/2,floor.getZ()+.5);}
    public Vec3 axisW(){return new Vec3(0,1,0).cross(Vec3.atLowerCornerOf(normal.getNormal()));}
    public AABB clearance(){
        Vec3 c=center();
        double x=normal.getAxis()==Direction.Axis.X?.55:width()/2-.01;
        double z=normal.getAxis()==Direction.Axis.Z?.55:width()/2-.01;
        return new AABB(c.x-x,floor.getY()+1.01,c.z-z,c.x+x,floor.getY()+1+height()-.01,c.z+z);
    }
    public ServerLevel level(MinecraftServer s){return s.getLevel(dimension);}
    public boolean loaded(ServerLevel l){
        if(l==null)return false;AABB b=clearance().inflate(1);
        for(int x=(int)Math.floor(b.minX)>>4;x<=((int)Math.floor(b.maxX)>>4);x++)
            for(int z=(int)Math.floor(b.minZ)>>4;z<=((int)Math.floor(b.maxZ)>>4);z++)
                if(!l.hasChunkAt(new BlockPos(x<<4,floor.getY(),z<<4)))return false;
        return true;
    }
    public void loadChunks(ServerLevel l){
        AABB b=clearance().inflate(1);
        for(int x=(int)Math.floor(b.minX)>>4;x<=((int)Math.floor(b.maxX)>>4);x++)
            for(int z=(int)Math.floor(b.minZ)>>4;z<=((int)Math.floor(b.maxZ)>>4);z++)
                l.getChunkAt(new BlockPos(x<<4,floor.getY(),z<<4));
    }
    public boolean installed(ServerLevel l){
        return l!=null&&l.hasChunkAt(floor)&&l.getBlockState(floor).is(WorldFold.ANCHOR.get())
            &&l.getBlockState(floor).getValue(SeamAnchorBlock.FACING)==normal;
    }
    public boolean clear(ServerLevel l){
        if(!installed(l)||!loaded(l)||!l.getWorldBorder().isWithinBounds(floor))return false;
        AABB b=clearance();
        return b.minY>=l.getMinBuildHeight()&&b.maxY<l.getMaxBuildHeight()
            &&l.getWorldBorder().isWithinBounds(BlockPos.containing(b.minX,b.minY,b.minZ))
            &&l.getWorldBorder().isWithinBounds(BlockPos.containing(b.maxX,b.maxY,b.maxZ))
            &&!l.getBlockCollisions(null,b).iterator().hasNext();
    }
    public int input(ServerLevel l){
        if(!installed(l))return 0;
        Direction back=normal.getOpposite();BlockPos p=floor.relative(back);
        if(!l.hasChunkAt(p))return 0;
        return l.getSignal(p,back);
    }
    public void output(ServerLevel l,int value){
        if(!installed(l))return;
        BlockState state=l.getBlockState(floor);
        int v=Math.max(0,Math.min(15,value));
        if(state.getValue(SeamAnchorBlock.POWER)!=v)l.setBlock(floor,state.setValue(SeamAnchorBlock.POWER,v),3);
    }
    public int stabilizers(ServerLevel l){
        if(!installed(l))return 0;int n=0;
        for(Direction d:Direction.Plane.HORIZONTAL){
            BlockPos p=floor.relative(d);
            if(l.hasChunkAt(p)&&l.getBlockState(p).is(WorldFold.STABILIZER.get()))n++;
        }
        return n;
    }
    public boolean conflicts(Anchor other){
        return dimension.equals(other.dimension)&&(floor.equals(other.floor)||clearance().inflate(.2).intersects(other.clearance()));
    }
    public CompoundTag save(){
        CompoundTag t=new CompoundTag();t.putString("dim",dimension.location().toString());t.putLong("floor",floor.asLong());
        t.putString("normal",normal.getName());t.putInt("w",w);t.putInt("h",h);t.putDouble("scale",scale);return t;
    }
    public static Anchor read(CompoundTag t){
        Direction d=Direction.byName(t.getString("normal"));int w=t.getInt("w"),h=t.getInt("h");double s=t.getDouble("scale");
        if(d==null||d.getAxis()==Direction.Axis.Y||w<3||w>15||h<3||h>9||(s!=.5&&s!=1&&s!=2))throw new IllegalArgumentException("Bad spatial anchor");
        return new Anchor(ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(t.getString("dim"))),BlockPos.of(t.getLong("floor")),d,w,h,s);
    }
    public static Anchor at(ServerLevel l,BlockPos p,int w,int h,double scale){
        BlockState state=l.getBlockState(p);
        if(!state.is(WorldFold.ANCHOR.get()))throw new IllegalArgumentException("Not a seam anchor");
        return new Anchor(l.dimension(),p,state.getValue(SeamAnchorBlock.FACING),w,h,scale);
    }
    public String describe(){return dimension.location()+" "+floor.getX()+","+floor.getY()+","+floor.getZ();}
}
