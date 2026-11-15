package dev.timberfall.entity;
import dev.timberfall.registry.TimberfallEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
public final class TopplingTreeEntity extends Entity {
    public static final int LIFE_TICKS=30;
    private static final EntityDataAccessor<String> PARTS=
        SynchedEntityData.defineId(TopplingTreeEntity.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DIRECTION=
        SynchedEntityData.defineId(TopplingTreeEntity.class,EntityDataSerializers.INT);
    private String cachedPayload="";
    private List<TreePiece> cachedPieces=List.of();
    public TopplingTreeEntity(EntityType<? extends TopplingTreeEntity> type,Level level){
        super(type,level);
        noPhysics=true;
    }
    public static TopplingTreeEntity create(ServerLevel level,BlockPos root,Direction direction,List<TreePiece> pieces){
        var tree=new TopplingTreeEntity(TimberfallEntities.TOPPLING_TREE.get(),level);
        tree.setPos(root.getX()+0.5,root.getY(),root.getZ()+0.5);
        tree.entityData.set(DIRECTION,direction.get2DDataValue());
        tree.entityData.set(PARTS,encode(pieces));
        return tree;
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){
        builder.define(PARTS,"");
        builder.define(DIRECTION,Direction.NORTH.get2DDataValue());
    }
    public Direction fallDirection(){return Direction.from2DDataValue(entityData.get(DIRECTION));}
    public List<TreePiece> pieces(){
        String payload=entityData.get(PARTS);
        if(!cachedPayload.equals(payload)){
            cachedPayload=payload;
            List<TreePiece> items=new ArrayList<>();
            if(!payload.isEmpty())for(String token:payload.split(";")){
                String[] fields=token.split(",",4);
                if(fields.length!=4)continue;
                try{
                    Block block=BuiltInRegistries.BLOCK.getValue(Identifier.parse(fields[3]));
                    if(block!=null)items.add(new TreePiece(Integer.parseInt(fields[0]),
                        Integer.parseInt(fields[1]),Integer.parseInt(fields[2]),block.defaultBlockState()));
                }catch(RuntimeException ignored){}
            }
            cachedPieces=List.copyOf(items);
        }
        return cachedPieces;
    }
    private static String encode(List<TreePiece> pieces){
        StringBuilder out=new StringBuilder();
        for(TreePiece item:pieces){
            if(!out.isEmpty())out.append(';');
            out.append(item.x()).append(',').append(item.y()).append(',').append(item.z()).append(',')
                .append(BuiltInRegistries.BLOCK.getKey(item.state().getBlock()));
        }
        return out.toString();
    }
    @Override public void tick(){
        super.tick();
        if(!level().isClientSide()&&tickCount>=LIFE_TICKS){
            if(level() instanceof ServerLevel server){
                BlockPos at=blockPosition();
                Direction fall=fallDirection();
                server.playSound(null,at,SoundEvents.WOOD_FALL,SoundSource.BLOCKS,1.3f,0.72f);
                for(TreePiece piece:pieces()){
                    if(!piece.state().is(BlockTags.LEAVES)&&
                       piece.state().getBlock().asItem()!=net.minecraft.world.item.Items.AIR){
                        BlockPos drop=at.offset(fall.getStepX()*piece.y(),1,fall.getStepZ()*piece.y());
                        Block.popResource(server,drop,new ItemStack(piece.state().getBlock().asItem()));
                    }
                }
            }
            discard();
        }
    }
    @Override protected void readAdditionalSaveData(ValueInput input){
        entityData.set(PARTS,input.getStringOr("pieces",""));
        entityData.set(DIRECTION,input.getIntOr("direction",0));
    }
    @Override protected void addAdditionalSaveData(ValueOutput output){
        output.putString("pieces",entityData.get(PARTS));
        output.putInt("direction",entityData.get(DIRECTION));
    }
}