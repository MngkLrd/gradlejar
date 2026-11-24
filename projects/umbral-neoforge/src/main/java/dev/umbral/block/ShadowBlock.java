package dev.umbral.block;

import dev.umbral.shadow.ShadowData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.*;

public final class ShadowBlock extends Block {
    // Each horizontal quarter of a block carries an independently quantized surface.
    // 0=absent; 1..4=surface height in quarters. No whole-block collision.
    public static final IntegerProperty NW=IntegerProperty.create("nw",0,4);
    public static final IntegerProperty NE=IntegerProperty.create("ne",0,4);
    public static final IntegerProperty SW=IntegerProperty.create("sw",0,4);
    public static final IntegerProperty SE=IntegerProperty.create("se",0,4);
    private static final IntegerProperty[] PARTS={NW,NE,SW,SE};
    private static final VoxelShape[] SHAPES=buildShapes();
    public ShadowBlock(Properties p){
        super(p);registerDefaultState(stateDefinition.any().setValue(NW,0).setValue(NE,0).setValue(SW,0).setValue(SE,0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(NW,NE,SW,SE);}
    private static VoxelShape[] buildShapes(){
        VoxelShape[] arr=new VoxelShape[625];
        for(int code=0;code<arr.length;code++){
            VoxelShape shape=Shapes.empty();int n=code;
            for(int i=0;i<4;i++){
                int h=n%5;n/=5;if(h==0)continue;
                int x=(i&1)*8,z=(i>>1)*8;
                shape=Shapes.or(shape,Block.box(x,h*4-3,z,x+8,h*4,z+8));
            }
            arr[code]=shape.optimize();
        }
        return arr;
    }
    public static int code(BlockState s){return s.getValue(NW)+5*s.getValue(NE)+25*s.getValue(SW)+125*s.getValue(SE);}
    public static BlockState fromCode(BlockState s,int code){
        for(IntegerProperty p:PARTS){s=s.setValue(p,code%5);code/=5;}
        return s;
    }
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPES[code(s)];}
    @Override public VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPES[code(s)];}
    @Override public void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moved){
        super.onPlace(s,l,p,old,moved);
        if(l instanceof ServerLevel server&&!old.is(this))server.scheduleTick(p,this,40);
    }
    @Override public void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource random){
        if(!ShadowData.get(l).owns(p)||code(s)==0)l.removeBlock(p,false);
        else l.scheduleTick(p,this,40);
    }
}
