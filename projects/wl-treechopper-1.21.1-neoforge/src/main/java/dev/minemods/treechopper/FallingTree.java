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
    private static int mask(CompoundTag f){return f.contains("processedMask")?f.getInt("processedMask"):(1<<Math.min(3,f.getInt("processed")))-1;}
    private static boolean processed(CompoundTag f,int s){return (mask(f)&(1<<s))!=0;}
    private static double offset(CompoundTag f,CompoundTag m){return f.getDouble("dropY")+(m==null?0:m.getDouble("extraDrop"));}
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
        return new Vec3(b.getX()+.5+.5*dir.getStepX()+off.x,b.getY()+.25+off.y+offset(f,member(f,p.asLong())),b.getZ()+.5+.5*dir.getStepZ()+off.z);
    }
    private static boolean allPresent(ServerLevel l,CompoundTag f){
        ListTag list=members(f);
        for(int i=0;i<list.size();i++){
            CompoundTag m=list.getCompound(i);
            if(!m.getBoolean("removed")&&display(l,m)==null)return false;
        }
        return true;
    }
    private static boolean clearAt(ServerLevel l,CompoundTag f,float progress){return clearAt(l,f,progress,0,false);}
    private static boolean clearAt(ServerLevel l,CompoundTag f,float progress,double deltaY,boolean includeRoots){
        Set<BlockPos> roots=new HashSet<>();
        if(!includeRoots)for(long cell:f.getLongArray("roots"))roots.add(BlockPos.of(cell));
        ListTag list=members(f);
        for(int i=0;i<list.size();i++){
            CompoundTag m=list.getCompound(i);
            if(m.getBoolean("log")&&!m.getBoolean("removed")&&!TreeChopper.sweepClear(l,base(f),BlockPos.of(m.getLong("source")),direction(f),progress,roots,offset(f,m)+deltaY))return false;
        }
        return true;
    }
    private static CompoundTag landingAt(ServerLevel l,CompoundTag f){
        if(!f.getBoolean("gridAllowed")||mask(f)!=0)return null;
        double dy=f.getDouble("dropY");int shift=(int)Math.round(dy);
        if(Math.abs(dy-shift)>.015)return null;
        ServerPlayer owner=null;
        if(shift!=0){
            if(!f.hasUUID("owner"))return null;
            owner=l.getServer().getPlayerList().getPlayer(f.getUUID("owner"));
            if(owner==null){var fake=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(l);if(fake.getUUID().equals(f.getUUID("owner")))owner=fake;}
            if(owner==null)return null;
        }
        ListTag adjusted=new ListTag();
        for(int i=0;i<plan(f).size();i++){
            CompoundTag entry=plan(f).getCompound(i).copy();BlockPos p=BlockPos.of(entry.getLong("pos")).offset(0,shift,0);
            if(!FallenTimber.landable(l,p)||(owner!=null&&!l.mayInteract(owner,p)))return null;
            entry.putLong("pos",p.asLong());adjusted.add(entry);
        }
        CompoundTag copy=f.copy();copy.put("landing",adjusted);return copy;
    }
    private static boolean canLower(ServerLevel l,CompoundTag f,List<CompoundTag> group,double amount){
        List<net.minecraft.world.phys.AABB> otherBoxes=new ArrayList<>();
        if(mask(f)!=0){
            Set<Long> own=new HashSet<>();for(CompoundTag m:group)own.add(m.getLong("source"));
            for(int i=0;i<members(f).size();i++){
                CompoundTag m=members(f).getCompound(i);
                if(m.getBoolean("log")&&!m.getBoolean("removed")&&!own.contains(m.getLong("source")))
                    otherBoxes.add(TreeChopper.timberBounds(base(f),BlockPos.of(m.getLong("source")),direction(f),f.getFloat("progress"),false,offset(f,m)).deflate(.001));
            }
        }
        for(CompoundTag m:group){
            if(!TreeChopper.sweepClear(l,base(f),BlockPos.of(m.getLong("source")),direction(f),f.getFloat("progress"),Set.of(),offset(f,m)-amount))return false;
            if(!otherBoxes.isEmpty()){
                var candidate=TreeChopper.timberBounds(base(f),BlockPos.of(m.getLong("source")),direction(f),f.getFloat("progress"),false,offset(f,m)-amount).deflate(.001);
                for(var box:otherBoxes)if(candidate.intersects(box))return false;
            }
        }
        return !group.isEmpty();
    }
    private static double centroidHeight(CompoundTag f,float progress){
        double sum=0;int count=0;
        for(int i=0;i<members(f).size();i++){
            CompoundTag m=members(f).getCompound(i);
            if(m.getBoolean("log")&&!m.getBoolean("removed")){
                sum+=TreeChopper.timberBounds(base(f),BlockPos.of(m.getLong("source")),direction(f),progress,false,offset(f,m)).getCenter().y;count++;
            }
        }
        return count==0?Double.NaN:sum/count;
    }
    /** Once wood is supported, it may rotate about the contact instead of its original stump.
     * Permit only the minimum upward displacement that allows continued rotation while
     * lowering the equal-volume log centroid. No free energy or direction reversal. */
    private static double rotationLift(ServerLevel l,CompoundTag f,float from,float to){
        if(clearAt(l,f,to))return 0;
        List<CompoundTag> whole=new ArrayList<>();components(f).forEach(whole::addAll);
        if(canLower(l,f,whole,.005))return Double.NaN;
        double budget=centroidHeight(f,from)-centroidHeight(f,to);
        if(!Double.isFinite(budget)||budget<=.0001)return Double.NaN;
        // A substep can need less than 1/64 block of lift. The old coarse
        // increment could reject every admissible pose after a support was removed.
        double max=Math.min(.5,budget-.00005);
        double previous=0;
        for(int i=1;i<=16;i++){
            double up=max*i/16;
            if(!clearAt(l,f,from,up,false))break;
            if(clearAt(l,f,to,up,false)){
                double lo=previous,hi=up;
                for(int j=0;j<10;j++){
                    double mid=(lo+hi)*.5;
                    if(clearAt(l,f,to,mid,false)&&clearAt(l,f,from,mid,false))hi=mid;else lo=mid;
                }
                return hi;
            }
            previous=up;
        }
        return Double.NaN;
    }
    private static List<List<CompoundTag>> components(CompoundTag f){
        List<List<CompoundTag>> groups=new ArrayList<>();Map<BlockPos,CompoundTag> unseen=new HashMap<>();
        for(int i=0;i<members(f).size();i++){
            CompoundTag m=members(f).getCompound(i);
            if(m.getBoolean("log")&&!m.getBoolean("removed"))unseen.put(BlockPos.of(m.getLong("source")),m);
        }
        while(!unseen.isEmpty()){
            BlockPos first=unseen.keySet().iterator().next();List<CompoundTag> group=new ArrayList<>();
            Deque<BlockPos> queue=new ArrayDeque<>();queue.add(first);group.add(unseen.remove(first));
            while(!queue.isEmpty()){
                BlockPos p=queue.removeFirst();
                for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){
                    BlockPos next=p.offset(x,y,z);CompoundTag m=unseen.get(next);
                    if(m!=null&&Math.abs(m.getDouble("extraDrop")-group.get(0).getDouble("extraDrop"))<.0001){group.add(unseen.remove(next));queue.add(next);}
                }
            }
            groups.add(group);
        }
        return groups;
    }
    private static double lowerStep(ServerLevel l,CompoundTag f,List<CompoundTag> group){
        // Resolve real contact to approximately 1 mm, instead of leaving a visible 1/8-block gap.
        for(double step=.125;step>=.0009765625;step*=.5)if(canLower(l,f,group,step))return step;
        return 0;
    }
    /** Gravity continues after release of the hinge, including independently supported remnants. */
    private static boolean settle(ServerLevel l,CompoundTag f){
        List<List<CompoundTag>> groups=components(f);boolean moved=false;
        if(mask(f)==0){
            List<CompoundTag> whole=new ArrayList<>();groups.forEach(whole::addAll);
            for(int i=0;i<4;i++){double step=lowerStep(l,f,whole);if(step==0)break;f.putDouble("dropY",f.getDouble("dropY")-step);moved=true;}
        }else for(List<CompoundTag> group:groups){
            for(int i=0;i<4;i++){
                double step=lowerStep(l,f,group);if(step==0)break;
                for(CompoundTag m:group)m.putDouble("extraDrop",m.getDouble("extraDrop")-step);
                moved=true;
            }
        }
        if(mask(f)!=0)for(int i=0;i<members(f).size();i++){
            CompoundTag cap=members(f).getCompound(i);
            if(!cap.getBoolean("cap")||cap.getBoolean("removed"))continue;
            BlockPos p=BlockPos.of(cap.getLong("source"));
            for(int j=0;j<members(f).size();j++){
                CompoundTag m=members(f).getCompound(j);
                if(m.getBoolean("log")&&!m.getBoolean("removed")&&BlockPos.of(m.getLong("source")).distManhattan(p)==1){cap.putDouble("extraDrop",m.getDouble("extraDrop"));break;}
            }
        }
        return moved;
    }
    private static void removeCollider(ServerLevel l,CompoundTag m){
        if(m.hasUUID("collider")&&l.getEntity(m.getUUID("collider")) instanceof TimberCollider collider)collider.discard();
        m.remove("collider");
    }
    private static void colliders(ServerLevel l,Display.BlockDisplay master,CompoundTag f){
        for(int i=0;i<members(f).size();i++){
            CompoundTag m=members(f).getCompound(i);
            if(m.getBoolean("removed")||(!m.getBoolean("log")&&!m.getBoolean("cap"))){removeCollider(l,m);continue;}
            var box=TreeChopper.timberBounds(base(f),BlockPos.of(m.getLong("source")),direction(f),f.getFloat("progress"),m.getBoolean("cap"),offset(f,m));
            if(!l.hasChunkAt(BlockPos.containing(box.getCenter())))continue;
            TimberCollider collider=m.hasUUID("collider")&&l.getEntity(m.getUUID("collider")) instanceof TimberCollider t?t:null;
            if(collider==null){
                collider=TreeChopper.COLLIDER.get().create(l);if(collider==null)continue;
                collider.owner=master.getUUID();collider.setBox(box);
                if(!l.addFreshEntity(collider))continue;
                m.putUUID("collider",collider.getUUID());
            }else collider.setBox(box);
        }
    }
    static int crushLeaves(ServerLevel l,ServerPlayer owner,net.minecraft.world.phys.AABB bounds){
        if(owner==null)return 0;
        var box=bounds.deflate(.001);int removed=0;
        for(BlockPos p:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ))){
            if(!l.hasChunkAt(p))continue;
            var state=l.getBlockState(p);
            if(!TreeChopper.naturalLeaf(state)||!TreeChopper.allowed(l,owner,p,state))continue;
            if(l.destroyBlock(p,false,owner)){removed++;l.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,state),p.getX()+.5,p.getY()+.5,p.getZ()+.5,4,.2,.2,.2,.02);}
        }
        return removed;
    }
    private static void crushPose(ServerLevel l,CompoundTag f){
        if(!f.hasUUID("owner"))return;
        ServerPlayer owner=l.getServer().getPlayerList().getPlayer(f.getUUID("owner"));
        if(owner==null){var fake=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(l);if(fake.getUUID().equals(f.getUUID("owner")))owner=fake;}
        if(owner==null)return;
        for(int i=0;i<members(f).size();i++){
            CompoundTag m=members(f).getCompound(i);
            if(m.getBoolean("log")&&!m.getBoolean("removed"))crushLeaves(l,owner,TreeChopper.timberBounds(base(f),BlockPos.of(m.getLong("source")),direction(f),f.getFloat("progress"),false,offset(f,m)));
        }
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
            CompoundTag transform=TreeChopper.fallTransform(p.getX()-b.getX(),p.getY()-b.getY(),p.getZ()-b.getZ(),dir,progress,m.getBoolean("cap"),true,true);
            ListTag translation=transform.getList("translation",Tag.TAG_FLOAT);
            translation.set(1,FloatTag.valueOf(translation.getFloat(1)+(float)offset(f,m)));
            tag.put("transformation",transform);
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
        for(int s=0;s<sections(f);s++){
            if(processed(f,s))continue;
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
        for(int i=0;i<list.size();i++){CompoundTag m=list.getCompound(i);removeCollider(l,m);Display.BlockDisplay d=display(l,m);if(d!=null&&d!=master)d.discard();}
        master.discard();
    }
    static void tick(ServerLevel l,Display.BlockDisplay master,CompoundTag f){
        if(direction(f)==null||members(f).isEmpty())return;
        if(!allPresent(l,f))return;
        float old=f.getFloat("progress"),next=old;
        boolean wasRest=master.getTags().contains(REST);
        if(wasRest&&l.getGameTime()%5!=0){colliders(l,master,f);return;}
        if(mask(f)==0){
            float goal=Math.min(1f,old+1f/36f);
            for(int i=1;i<=4;i++){
                float candidate=old+(goal-old)*i/4;
                f.putFloat("progress",next);
                double lift=rotationLift(l,f,next,candidate);
                if(!Double.isFinite(lift))break;
                if(lift>0)f.putDouble("dropY",f.getDouble("dropY")+lift);
                next=candidate;f.putFloat("progress",next);crushPose(l,f);
            }
        }
        f.putFloat("progress",next);
        boolean rotating=next>old+.00001f;
        boolean branched=false;
        for(int i=0;i<members(f).size();i++){
            CompoundTag m=members(f).getCompound(i);
            if(!m.getBoolean("log")||m.getBoolean("removed"))continue;
            BlockPos p=BlockPos.of(m.getLong("source"));
            if(p.getX()!=base(f).getX()||p.getZ()!=base(f).getZ()){branched=true;break;}
        }
        // Keep the hinge on a straight, intact tree while an obstacle blocks rotation.
        // Releasing it early made the subsequent angular path intersect the floor.
        // Branching trunks may settle on real branch contacts; cut remnants always release.
        boolean descending=(mask(f)!=0||next>=.99999f||(!rotating&&branched))&&settle(l,f);
        if(rotating||descending){
            if(descending)crushPose(l,f);
            removeMarkers(l,f,true);master.removeTag(REST);pose(l,master,f,next);
        }
        if(!descending&&next>=.99999f&&mask(f)==0){
            CompoundTag landing=landingAt(l,f);
            if(landing!=null&&FallenTimber.land(l,base(f),landing)){discard(l,master,f);return;}
        }
        colliders(l,master,f);
        if(!rotating&&!descending){
            boolean first=master.addTag(REST);
            if(first&&!wasRest)l.playSound(null,base(f),SoundEvents.WOOD_BREAK,SoundSource.BLOCKS,1.3f,.6f);
            markers(l,master,f);
        }
        master.getPersistentData().put(TreeChopper.FALL,f);
    }
    static boolean qaSupported(ServerLevel l,CompoundTag f){
        if(!allPresent(l,f))return false;
        List<List<CompoundTag>> groups=components(f);
        if(groups.isEmpty())return false;
        if(mask(f)==0){List<CompoundTag> whole=new ArrayList<>();groups.forEach(whole::addAll);groups=List.of(whole);}
        for(List<CompoundTag> group:groups)if(canLower(l,f,group,.005)){
            if(mask(f)!=0||f.getFloat("progress")>=.99999f||Math.abs(f.getDouble("dropY"))>.0001)return false;
            // An unprocessed straight trunk is still held by its cut hinge until rotation completes.
            // Verify that the actual original stump exists rather than assume support in air.
            BlockPos root=base(f);
            if(!(l.getBlockState(root).getBlock() instanceof StumpBlock))return false;
            for(CompoundTag m:group){
                BlockPos p=BlockPos.of(m.getLong("source"));
                if(p.getX()!=root.getX()||p.getZ()!=root.getZ())return false;
            }
            CompoundTag cap=member(f,root.asLong());
            if(cap==null||cap.getBoolean("removed")||!cap.getBoolean("cap"))return false;
            var bounds=TreeChopper.timberBounds(root,root,direction(f),f.getFloat("progress"),true,offset(f,cap));
            boolean contact=false;
            for(var shape:l.getBlockState(root).getCollisionShape(l,root).toAabbs()){
                var world=shape.move(root.getX(),root.getY(),root.getZ());
                if(Math.abs(bounds.minY-world.maxY)<.005&&bounds.maxX>=world.minX-.005&&bounds.minX<=world.maxX+.005
                    &&bounds.maxZ>=world.minZ-.005&&bounds.minZ<=world.maxZ+.005){contact=true;break;}
            }
            if(!contact)return false;
        }
        return true;
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
        if(s<0||s>=sections(f)||processed(f,s))return;
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
        for(CompoundTag m:harvest){removeCollider(l,m);Display.BlockDisplay d=display(l,m);if(d!=null)d.discard();m.putBoolean("removed",true);}
        int done=mask(f)|(1<<s);f.putInt("processedMask",done);f.putInt("processed",Integer.bitCount(done));f.putLong("lastBuck",l.getGameTime());
        // Decorative root caps must not hang in space after their supporting logs have been cut.
        for(int i=0;i<members(f).size();i++){
            CompoundTag cap=members(f).getCompound(i);
            if(!cap.getBoolean("cap")||cap.getBoolean("removed"))continue;
            BlockPos p=BlockPos.of(cap.getLong("source"));boolean attached=false;
            for(int j=0;j<members(f).size();j++){
                CompoundTag m=members(f).getCompound(j);
                if(m.getBoolean("log")&&!m.getBoolean("removed")&&BlockPos.of(m.getLong("source")).distManhattan(p)==1){attached=true;break;}
            }
            if(!attached){Display.BlockDisplay d=display(l,cap);if(d==master){model(l,cap,NbtUtils.writeBlockState(net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()));d.removeTag("wl_treechopper_cut_cap");}else if(d!=null)d.discard();cap.putBoolean("removed",true);}
        }
        BlockPos dropAt=BlockPos.containing(marker.position());ItemStack log=new ItemStack(TreeChopper.original(f.getString("species")).getBlock());
        int count=harvest.size();
        while(count>0&&!log.isEmpty()){int n=Math.min(count,log.getMaxStackSize());Block.popResource(l,dropAt,log.copyWithCount(n));count-=n;}
        ((ChopSwing)player).wl$markSwing();if(!(player instanceof FakePlayer))PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,new ChopNetwork.Swing(player.getId()));
        player.swing(InteractionHand.MAIN_HAND,true);
        l.playSound(null,dropAt,SoundEvents.WOOD_BREAK,SoundSource.BLOCKS,1f,1f);
        removeMarkers(l,f,true);
        if(Integer.bitCount(done)>=sections(f))discard(l,master,f);
        else{master.removeTag(REST);master.getPersistentData().put(TreeChopper.FALL,f);}
    }
}
