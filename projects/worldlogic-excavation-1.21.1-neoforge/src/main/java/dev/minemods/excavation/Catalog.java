package dev.minemods.excavation;
import com.google.gson.Gson;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

final class Catalog {
    record Spec(String source,String texture,String host,String mineral,boolean coreMineral,String family,String en,String ru,String drop){
        boolean ore(){return !family.isEmpty();}
        String key(){return source.replace(':','_');}
        BlockState original(){return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(source)).defaultBlockState();}
    }
    record Family(String id,String en,String ru,String texture,String product,String nugget,String processing){}
    record Data(List<Spec> specs,List<Family> families){}
    static final Data DATA=read();
    static final Map<ResourceLocation,Spec> ORIGINALS=new HashMap<>();
    static final Map<String,DeferredBlock<ChipBlock>> CHIPS=new LinkedHashMap<>();
    static final Map<String,Map<Integer,DeferredItem<Item>>> FRAGMENTS=new LinkedHashMap<>();
    static {for(Spec s:DATA.specs())ORIGINALS.put(ResourceLocation.parse(s.source()),s);}
    private static Data read(){
        try(var stream=Catalog.class.getResourceAsStream("/worldlogic_excavation/catalog.json")){
            if(stream==null)throw new IllegalStateException("Missing excavation catalog");
            try(var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){
                Data d=new Gson().fromJson(reader,Data.class);
                if(d==null||d.specs().isEmpty()||d.families().isEmpty())throw new IllegalStateException("Empty catalog");
                return d;
            }
        }catch(IOException e){throw new UncheckedIOException(e);}
    }
    static Spec spec(BlockState state){
        return state.getBlock() instanceof ChipBlock b?b.spec:ORIGINALS.get(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }
    static ItemStack fragment(String family,int units,int count){return new ItemStack(FRAGMENTS.get(family).get(units).get(),count);}
    static int units(ItemStack stack,String family){
        var m=FRAGMENTS.get(family);if(m==null)return 0;
        for(var e:m.entrySet())if(stack.is(e.getValue().get()))return e.getKey()*stack.getCount();
        return 0;
    }
    static String size(int units){return switch(units){case 1->"small";case 4->"medium";case 9->"large";default->throw new IllegalArgumentException();};}
}
