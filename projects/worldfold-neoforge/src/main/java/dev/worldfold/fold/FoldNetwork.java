package dev.worldfold.fold;

import com.mojang.logging.LogUtils;
import dev.worldfold.WorldFold;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;

public final class FoldNetwork {
    private FoldNetwork(){}
    public static void message(Player p,String key,Object...args){p.displayClientMessage(Component.translatable("worldfold."+key,args),true);}
    public static FoldLink create(ServerPlayer player,Anchor a,Anchor b)throws ReflectiveOperationException{
        MinecraftServer server=player.getServer();FoldData data=FoldData.get(server);
        if(data.limit(player.getUUID()))throw new IllegalArgumentException("limit");
        if(a.conflicts(b)||!data.free(a)||!data.free(b))throw new IllegalArgumentException("overlap");
        ServerLevel al=a.level(server),bl=b.level(server);
        if(al==null||bl==null)throw new IllegalArgumentException("blocked");
        a.loadChunks(al);b.loadChunks(bl);
        if(!a.clear(al)||!b.clear(bl))throw new IllegalArgumentException("blocked");
        FoldLink f=new FoldLink(UUID.randomUUID(),player.getUUID(),a,b);
        data.add(f);
        try{PortalBridge.connect(server,a,b,f.generation);}
        catch(ReflectiveOperationException|RuntimeException e){data.remove(f);PortalBridge.removeLoaded(server,a,b,f.generation);throw e;}
        updateCost(server,f);data.setDirty();
        return f;
    }
    public static boolean owner(ServerPlayer p,FoldLink f){return f.owner.equals(p.getUUID())||p.hasPermissions(2);}
    public static void addBranch(ServerPlayer p,FoldLink f,Anchor c){
        FoldData data=FoldData.get(p.getServer());
        if(!owner(p,f))throw new IllegalArgumentException("owner");
        if(f.c!=null)throw new IllegalArgumentException("already_branch");
        if(!data.free(c))throw new IllegalArgumentException("overlap");
        ServerLevel l=c.level(p.getServer());if(l==null)throw new IllegalArgumentException("blocked");
        c.loadChunks(l);if(!c.clear(l))throw new IllegalArgumentException("blocked");
        f.c=c;data.setDirty();
    }
    public static void flow(ServerPlayer p,FoldLink f){
        if(!owner(p,f))throw new IllegalArgumentException("owner");
        if(f.buffered())throw new IllegalArgumentException("buffer");
        f.flow=(f.flow+1)%3;FoldData.get(p.getServer()).setDirty();
        message(p,"flow_set",Component.translatable("worldfold.flow."+f.flow));
    }
    public static void dismantle(ServerPlayer p,FoldLink f){
        if(!owner(p,f))throw new IllegalArgumentException("owner");
        if(f.buffered())throw new IllegalArgumentException("buffer");
        MinecraftServer s=p.getServer();
        if(PortalBridge.occupied(s,f.a,f.destination()))throw new IllegalArgumentException("occupied");
        FoldData data=FoldData.get(s);data.remove(f);
        PortalBridge.removeLoaded(s,f.a,f.destination(),f.generation);
        for(Anchor a:f.anchors())a.output(a.level(s),0);
        message(p,"removed");
    }
    public static void repair(ServerPlayer p,FoldLink f)throws ReflectiveOperationException{
        if(!owner(p,f))throw new IllegalArgumentException("owner");
        MinecraftServer s=p.getServer();
        if(!replace(s,FoldData.get(s),f,f.alternate,true))throw new IllegalArgumentException("occupied_or_blocked");
        message(p,"repaired");
    }
    private static boolean replace(MinecraftServer s,FoldData data,FoldLink f,boolean alternate,boolean force)throws ReflectiveOperationException{
        if(!force&&f.alternate==alternate)return true;
        if(f.buffered()&&!force)return false;
        Anchor previous=f.destination(),next=alternate&&f.c!=null?f.c:f.b;
        ServerLevel al=f.a.level(s),nl=next.level(s);
        if(al==null||nl==null||!f.a.installed(al))return false;
        if(PortalBridge.occupied(s,f.a,previous,next))return false;
        f.a.loadChunks(al);next.loadChunks(nl);
        if(!f.a.clear(al)||!next.clear(nl)||PortalBridge.occupied(s,f.a,previous,next))return false;
        boolean before=f.alternate;UUID old=f.generation,newId=UUID.randomUUID();
        f.alternate=alternate;data.generation(f,newId);
        try{PortalBridge.connect(s,f.a,next,newId);}
        catch(ReflectiveOperationException|RuntimeException e){
            f.alternate=before;data.generation(f,old);PortalBridge.removeLoaded(s,f.a,next,newId);throw e;
        }
        PortalBridge.removeLoaded(s,f.a,previous,old);previous.output(previous.level(s),0);data.setDirty();
        return true;
    }
    private static boolean bothLoaded(MinecraftServer s,FoldLink f){
        Anchor b=f.destination();return f.a.loaded(f.a.level(s))&&b.loaded(b.level(s));
    }
    private static void updateCost(MinecraftServer s,FoldLink f){
        Anchor b=f.destination();boolean cross=!f.a.dimension().equals(b.dimension());
        double dist=cross?2000:Math.sqrt(f.a.center().distanceToSqr(b.center()));
        double area=Math.max(f.a.width()*f.a.height(),b.width()*b.height());
        f.load=SpatialMath.cost(area,dist,cross,b.scale()/f.a.scale(),f.c!=null);
        f.capacity=SpatialMath.capacity(f.a.stabilizers(f.a.level(s))+b.stabilizers(b.level(s)));
    }
    public static void status(ServerPlayer p,FoldLink f){
        MinecraftServer s=p.getServer();updateCost(s,f);
        p.sendSystemMessage(Component.translatable("worldfold.status",f.id.toString().substring(0,8),Component.translatable("worldfold.route."+(f.alternate?"c":"b")),
            Component.translatable("worldfold.flow."+f.flow),f.strain,f.load,f.capacity,bothLoaded(s,f)?"online":"idle"));
        p.sendSystemMessage(Component.literal("A: "+f.a.describe()+" | B: "+f.b.describe()+(f.c==null?"":" | C: "+f.c.describe())));
        p.sendSystemMessage(Component.translatable("worldfold.status_buffer",f.item.isEmpty()?0:f.item.getCount(),f.fluid.isEmpty()?0:f.fluid.getAmount()));
    }
    public static void tick(ServerTickEvent.Post event){
        MinecraftServer s=event.getServer();if(s.overworld()==null)return;
        long tick=s.overworld().getGameTime();if(tick%5!=0)return;
        FoldData data=FoldData.get(s);
        for(FoldLink f:data.links.values()){
            try{tickLink(s,data,f,tick);}
            catch(RuntimeException e){if(tick%100==0)LogUtils.getLogger().error("World Fold link "+f.id+" transport failed",e);}
        }
    }
    private static void tickLink(MinecraftServer s,FoldData data,FoldLink f,long tick){
        ServerLevel al=f.a.level(s);Anchor b=f.destination();ServerLevel bl=b.level(s);
        if(f.c!=null&&f.a.installed(al)&&tick>=f.routeAfter){
            boolean want=f.a.input(al)>=8;
            if(want!=f.alternate){
                f.routeAfter=tick+100;
                try{if(replace(s,data,f,want,false))f.routeAfter=tick+20;}
                catch(ReflectiveOperationException|RuntimeException ex){LogUtils.getLogger().warn("World Fold route change failed for "+f.id,ex);}
            }
        }
        b=f.destination();bl=b.level(s);
        if(f.c!=null){Anchor inactive=f.alternate?f.b:f.c;inactive.output(inactive.level(s),0);}
        if(!bothLoaded(s,f)||!f.a.installed(al)||!b.installed(bl)){
            f.a.output(al,0);b.output(bl,0);return;
        }
        if(tick%20==0){
            updateCost(s,f);
            int strain=SpatialMath.nextStrain(f.strain,f.load,f.capacity);
            if(strain!=f.strain){f.strain=strain;data.setDirty();}
            if(f.strain>=50){
                for(Anchor a:List.of(f.a,b)){
                    ServerLevel l=a.level(s);var pos=a.center();
                    l.sendParticles(ParticleTypes.REVERSE_PORTAL,pos.x,pos.y,pos.z,Math.min(16,3+f.strain/8),.25,a.height()/4,.25,.025);
                }
            }
        }
        int ai=f.a.input(al),bi=b.input(bl);
        f.a.output(al,f.strain>=95?0:bi);b.output(bl,f.strain>=95?0:ai);
        Transport.tick(s,data,f);
    }
    public static void onPortalJoin(EntityJoinLevelEvent event){
        if(!(event.getLevel() instanceof ServerLevel level))return;
        MinecraftServer s=level.getServer();if(s.overworld()==null)return;
        for(String tag:event.getEntity().getTags())if(tag.startsWith(PortalBridge.TAG)){
            try{
                UUID id=UUID.fromString(tag.substring(PortalBridge.TAG.length()));
                if(!FoldData.get(s).liveGenerations.contains(id))event.setCanceled(true);
            }catch(IllegalArgumentException bad){event.setCanceled(true);}return;
        }
    }
    public static void onBreak(BlockEvent.BreakEvent e){
        if(!(e.getLevel() instanceof ServerLevel l)||!e.getState().is(WorldFold.ANCHOR.get()))return;
        if(FoldData.get(l.getServer()).find(l.dimension(),e.getPos())!=null){
            e.setCanceled(true);message(e.getPlayer(),"protected");
        }
    }
    public static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("worldfold")
            .then(Commands.literal("recall").executes(ctx->{
                ServerPlayer p=ctx.getSource().getPlayerOrException();
                int n=PocketSpace.recall(p);if(n==0)message(p,"recall_only");else message(p,"recall_done");return n;
            }))
            .then(Commands.literal("status").executes(ctx->{
                ServerPlayer p=ctx.getSource().getPlayerOrException();
                FoldData data=FoldData.get(p.getServer());int n=0;
                for(FoldLink f:data.links.values())if(owner(p,f)){status(p,f);n++;}
                if(n==0)message(p,"no_links");return n;
            })));
    }
}
