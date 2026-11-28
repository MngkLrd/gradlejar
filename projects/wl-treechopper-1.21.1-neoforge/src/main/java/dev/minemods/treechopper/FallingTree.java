package dev.minemods.treechopper;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

/** One persisted coordinator advances the entire rigid trunk, including after collision. */
final class FallingTree {
    static final String REST="wl_treechopper_resting", MARKER="wl_treechopper_buck_marker";
    private FallingTree(){}
    private static BlockPos base(CompoundTag f){return BlockPos.of(f.getLong("base"));}
    private static Direction direction(CompoundTag f){return Direction.byName(f.getString("direction"));}
    private static ListTag members(CompoundTag f){return f.getList("members",Tag.TAG_COMPOUND);}
    private static ListTag plan(CompoundTag f){return f.getList("landing",Tag.TAG_COMPOUND);}
    private static int sections(CompoundTag f){return Math.min(3,plan(f).size());}
    private static int from(CompoundTag f,int s){return s*plan(f).size()/sections(f);}
    private static int to(CompoundTag f,int s){return (s+1)*plan(f).size()/sections(f);}
    private static CompoundTag member(CompoundTag f,long source){
        ListTag list=members(f);
        for(int i=0;i<list.size();i++){CompoundTag m=list.getCompound(i);if(m.getLong("source")==source)return m;}
        return null;
    }
    private static Display.BlockDisplay display(ServerLevel l,CompoundTag m){
        return m!=null&&m.hasUUID("entity")&&l.getEntity(m.getUUID("entity")) instanceof Display.BlockDisplay d?d:null;
    }
    private static Vec3 center(CompoundTag f,BlockPos p,float progress){
        BlockPos b=base(f);Direction dir=direction(f);
        Vector3f off=TreeChopper.fallOffset(p.getX()-b.getX(),p.getY()-b.getY(),p.getZ()-b.getZ(),dir,progress,false,true,true);
        off.add(TreeChopper.rotation(dir,(float)(Math.PI*.5*progress*progress)).transform(new Vector3f(.5f,.5f,.5f)));
        return new Vec3(b.getX()+.5+.5*dir.getStepX()+off.x,b.getY()+.25+off.y,b.getZ()+.5+.5*dir.getStepZ()+off.z);
    }
    private static boolean allPresent(ServerLevel l,CompoundTag f){
        ListTag list=members(f);
        for(int i=0;i<list.size();i++){
            CompoundTag m=list.getCompound(i);
            if(!m.getBoolean("removed")&&display(l,m)==null)return false;
        }
        return true;
    }
    private static boolean clearAt(ServerLevel l,CompoundTag f,float progress){
        Set<BlockPos> roots=new HashSet<>();
        for(long cell:f.getLongArray("roots"))roots.add(BlockPos.of(cell));
        ListTag list=members(f);
        for(int i=0;i<list.size();i++){
            CompoundTag m=list.getCompound(i);
            if(m.getBoolean("log")&&!m.getBoolean("removed")&&!TreeChopper.sweepClear(l,base(f),BlockPos.of(m.getLong("source")),direction(f),progress,roots))return false;
        }
        return true;
    }
    private static boolean gridClear(ServerLevel l,CompoundTag f){
        if(!f.getBoolean("gridAllowed"))return false;
        ListTag list=plan(f);
        for(int i=0;i<list.size();i++){
            BlockPos p=BlockPos.of(list.getCompound(i).getLong("pos"));
            if(!FallenTimber.landable(l,p))return false;
        }
        return true;
    }
    private static void pose(ServerLevel l,Display.BlockDisplay master,CompoundTag f,float progress){
        ListTag list=members(f);
        BlockPos b=base(f);Direction dir=direction(f);
        for(int i=0;i<list.size();i++){
            CompoundTag m=list.getCompound(i);
            if(m.getBoolean("removed"))continue;
            Display.BlockDisplay d=display(l,m);if(d==null)continue;
            BlockPos p=BlockPos.of(m.getLong("source"));
            CompoundTag tag=d.saveWithoutId(new CompoundTag());
            tag.putInt("interpolation_duration",2);tag.putInt("start_interpolation",0);
            tag.put("transformation",TreeChopper.fallTransform(p.getX()-b.getX(),p.getY()-b.getY(),p.getZ()-b.getZ(),dir,progress,m.getBoolean("cap"),true,true));
            d.load(tag);
        }
        // load() replaces the persistent tag; retain the authoritative coordinator state.
        master.getPersistentData().put(TreeChopper.FALL,f);
    }
    private static void model(ServerLevel l,CompoundTag m,CompoundTag state){
        Display.BlockDisplay d=display(l,m);if(d==null)return;
        CompoundTag tag=d.saveWithoutId(new CompoundTag());tag.put("block_state",state);d.load(tag);
    }
    private static void removeMarkers(ServerLevel l,CompoundTag f,boolean restore){
        ListTag marks=f.getList("markers",Tag.TAG_COMPOUND);
        for(int i=0;i<marks.size();i++){
            CompoundTag mark=marks.getCompound(i);
            if(mark.hasUUID("entity")&&l.getEntity(mark.getUUID("entity")) instanceof Interaction e)e.discard();
            CompoundTag m=member(f,mark.getLong("source"));
            if(restore&&m!=null&&!m.getBoolean("removed"))model(l,m,m.getCompound("state"));
        }
        f.remove("markers");
    }
    private static void markers(ServerLevel l,Display.BlockDisplay master,CompoundTag f){
        if(f.contains("markers")||sections(f)==0)return;
        ListTag marks=new ListTag();ListTag landing=plan(f);
        for(int s=f.getInt("processed");s<sections(f);s++){
            int i=from(f,s)+(to(f,s)-from(f,s)-1)/2;
            CompoundTag entry=landing.getCompound(i);long source=entry.getLong("source");
            CompoundTag m=member(f,source);if(m==null)continue;
            Vec3 v=center(f,BlockPos.of(source),f.getFloat("progress"));
            BlockPos cell=BlockPos.containing(v);
            if(!l.hasChunkAt(cell))continue;
            Interaction marker=EntityType.INTERACTION.create(l);if(marker==null)continue;
            CompoundTag config=new CompoundTag();config.putFloat("width",1.2f);config.putFloat("height",1.2f);config.putBoolean("response",true);
            marker.load(config);marker.setPos(v.x,v.y-.6,v.z);marker.setNoGravity(true);marker.addTag(MARKER);
            CompoundTag data=new CompoundTag();data.putUUID("master",master.getUUID());data.putInt("section",s);marker.getPersistentData().put(MARKER,data);
            if(!l.addFreshEntity(marker))continue;
            CompoundTag mark=new CompoundTag();mark.putUUID("entity",marker.getUUID());mark.putInt("section",s);mark.putLong("source",source);marks.add(mark);
            // The same native wood model with a recessed notch, rotated by the shared pose.
            var original=NbtUtils.readBlockState(l.holderLookup(net.minecraft.core.registries.Registries.BLOCK),m.getCompound("state"));
            Direction.Axis axis=original.hasProperty(BlockStateProperties.AXIS)?original.getValue(BlockStateProperties.AXIS):Direction.Axis.Y;
            model(l,m,NbtUtils.writeBlockState(TreeChopper.fallenState(f.getString("species"),axis,true)));
        }
        if(!marks.isEmpty())f.put("markers",marks);
    }
    private static void discard(ServerLevel l,Display.BlockDisplay master,CompoundTag f){
        removeMarkers(l,f,false);
        ListTag list=members(f);
        for(int i=0;i<list.size();i++){Display.BlockDisplay d=display(l,list.getCompound(i));if(d!=null&&d!=master)d.discard();}
        master.discard();
    }
    static void tick(ServerLevel l,Display.BlockDisplay master,CompoundTag f){
        if(direction(f)==null||members(f).isEmpty())return;
        if(!allPresent(l,f))return; // preserve the group until its entities are loaded.
        float old=f.getFloat("progress");
        if(f.getInt("processed")>0){markers(l,master,f);return;}
        if(master.getTags().contains(REST)&&l.getGameTime()%5!=0)return;
        float next=old;
        float goal=Math.min(1f,old+1f/36f);
        // Small bounded steps; the high end of a tall tree moves much farther than its base.
        int subdivisions=4;
        for(int i=1;i<=subdivisions;i++){
            float candidate=old+(goal-old)*i/subdivisions;
            if(!clearAt(l,f,candidate))break;
            next=candidate;
        }
        if(next>old+.00001f){
            removeMarkers(l,f,true);master.removeTag(REST);
            f.putFloat("progress",next);pose(l,master,f,next);
        }
        if(next>=.99999f&&gridClear(l,f)){
            if(FallenTimber.land(l,base(f),f))discard(l,master,f);
            return;
        }
        if(next<=old+.00001f||next>=.99999f){
            boolean first=master.addTag(REST);
            if(first)l.playSound(null,base(f),SoundEvents.WOOD_BREAK,SoundSource.BLOCKS,1.3f,.6f);
            markers(l,master,f);
        }
        master.getPersistentData().put(TreeChopper.FALL,f);
    }
    static void attack(AttackEntityEvent event){
        if(!(event.getTarget() instanceof Interaction marker)||!marker.getPersistentData().contains(MARKER))return;
        event.setCanceled(true);
        if(!(event.getEntity() instanceof ServerPlayer player)||!(player.level() instanceof ServerLevel l)||player.isSpectator()||!player.mayBuild()||!(player.getMainHandItem().getItem() instanceof AxeItem))return;
        if(player.getEyePosition().distanceToSqr(marker.position().add(0,.6,0))>Math.pow(player.entityInteractionRange()+.7,2))return;
        CompoundTag data=marker.getPersistentData().getCompound(MARKER);
        if(!data.hasUUID("master")||!(l.getEntity(data.getUUID("master")) instanceof Display.BlockDisplay master))return;
        CompoundTag f=master.getPersistentData().getCompound(TreeChopper.FALL);
        if(!master.getTags().contains(REST)||!allPresent(l,f))return;
        int s=data.getInt("section");
        if(s<0||s>=sections(f)||s!=f.getInt("processed")){
            player.displayClientMessage(Component.translatable("message.wl_treechopper.notch_order"),true);return;
        }
        ListTag marks=f.getList("markers",Tag.TAG_COMPOUND);
        boolean valid=false;
        for(int i=0;i<marks.size();i++){CompoundTag m=marks.getCompound(i);if(m.hasUUID("entity")&&marker.getUUID().equals(m.getUUID("entity")))valid=true;}
        if(!valid)return;
        if(WorldLogicIntegration.effort(player).power()<=0)return;
        long last=f.getLong("lastBuck");
        if(f.contains("lastBuck")&&l.getGameTime()-last<ChopEffort.cooldown(WorldLogicIntegration.effort(player).power()))return;
        ListTag landing=plan(f);List<CompoundTag> harvest=new ArrayList<>();
        for(int i=from(f,s);i<to(f,s);i++){
            long source=landing.getCompound(i).getLong("source");
            CompoundTag m=member(f,source);
            if(m==null||m.getBoolean("removed"))continue;
            BlockPos p=BlockPos.containing(center(f,BlockPos.of(source),f.getFloat("progress")));
            if(!l.hasChunkAt(p)||!l.mayInteract(player,p))return;
            harvest.add(m);
        }
        if(harvest.isEmpty())return;
        for(CompoundTag m:harvest){Display.BlockDisplay d=display(l,m);if(d!=null)d.discard();m.putBoolean("removed",true);}
        f.putInt("processed",s+1);f.putLong("lastBuck",l.getGameTime());
        BlockPos dropAt=BlockPos.containing(marker.position());ItemStack log=new ItemStack(TreeChopper.original(f.getString("species")).getBlock());
        int count=harvest.size();
        while(count>0&&!log.isEmpty()){int n=Math.min(count,log.getMaxStackSize());Block.popResource(l,dropAt,log.copyWithCount(n));count-=n;}
        ((ChopSwing)player).wl$markSwing();if(!(player instanceof FakePlayer))PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,new ChopNetwork.Swing(player.getId()));
        player.swing(InteractionHand.MAIN_HAND,true);
        l.playSound(null,dropAt,SoundEvents.WOOD_BREAK,SoundSource.BLOCKS,1f,1f);
        marker.discard();
        if(s+1>=sections(f))discard(l,master,f);
        else master.getPersistentData().put(TreeChopper.FALL,f);
    }
}
