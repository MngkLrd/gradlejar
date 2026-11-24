package dev.worldfold.fold;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.*;

public final class FoldLink {
    public final UUID id,owner;
    public final Anchor a,b;
    public Anchor c;
    public UUID generation;
    public boolean alternate;
    public int flow,strain,load,capacity;
    public long routeAfter;
    public ItemStack item=ItemStack.EMPTY;
    public FluidStack fluid=FluidStack.EMPTY;
    public FoldLink(UUID id,UUID owner,Anchor a,Anchor b){
        this.id=id;this.owner=owner;this.a=a;this.b=b;this.generation=UUID.randomUUID();
    }
    public Anchor destination(){return alternate&&c!=null?c:b;}
    public List<Anchor> anchors(){return c==null?List.of(a,b):List.of(a,b,c);}
    public boolean buffered(){return !item.isEmpty()||!fluid.isEmpty();}
    public CompoundTag save(HolderLookup.Provider registries){
        CompoundTag t=new CompoundTag();t.putUUID("id",id);t.putUUID("owner",owner);t.putUUID("generation",generation);
        t.put("a",a.save());t.put("b",b.save());if(c!=null)t.put("c",c.save());
        t.putBoolean("alternate",alternate);t.putInt("flow",flow);t.putInt("strain",strain);
        t.put("item",item.saveOptional(registries));t.put("fluid",fluid.saveOptional(registries));return t;
    }
    public static FoldLink read(CompoundTag t,HolderLookup.Provider registries){
        FoldLink f=new FoldLink(t.getUUID("id"),t.getUUID("owner"),Anchor.read(t.getCompound("a")),Anchor.read(t.getCompound("b")));
        f.generation=t.getUUID("generation");if(t.contains("c"))f.c=Anchor.read(t.getCompound("c"));
        f.alternate=t.getBoolean("alternate")&&f.c!=null;f.flow=Math.max(0,Math.min(2,t.getInt("flow")));f.strain=Math.max(0,Math.min(100,t.getInt("strain")));
        f.item=ItemStack.parseOptional(registries,t.getCompound("item"));f.fluid=FluidStack.parseOptional(registries,t.getCompound("fluid"));return f;
    }
}
