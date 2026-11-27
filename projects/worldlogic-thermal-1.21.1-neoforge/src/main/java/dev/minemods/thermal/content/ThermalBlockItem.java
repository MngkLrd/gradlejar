package dev.minemods.thermal.content;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import java.util.List;

public final class ThermalBlockItem extends BlockItem {
    public ThermalBlockItem(Block block,Properties properties){super(block,properties);}
    private static void line(List<Component> lines,String key,Object... values){
        lines.add(Component.translatable("tooltip.worldlogic_thermal."+key,values).withStyle(ChatFormatting.GRAY));
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
        super.appendHoverText(stack,context,lines,flag);
        lines.add(Component.translatable(getDescriptionId()+".info").withStyle(ChatFormatting.GRAY));
        Block b=getBlock();
        if(b instanceof FixtureBlock f){
            line(lines,"cycle_seconds",f.duration()/20);
            line(lines,"range_blocks",5);
            if(f.cooling())line(lines,"max_cooling",16);else line(lines,"heat_output",65);
        }else if(b instanceof ClimateDeviceBlock d){
            if(d.activeSeconds()>0)line(lines,"cycle_seconds",d.activeSeconds());
            if(d.residualSeconds()>0)line(lines,"residual_seconds",d.residualSeconds());
            double heat=d.heat(d.defaultBlockState().setValue(ClimateDeviceBlock.PHASE,1));
            if(heat>0)line(lines,"heat_output",(int)heat);
            if(d.kind()==ClimateDeviceBlock.Kind.ICE_BOX)line(lines,"max_cooling",13);
            if(d.kind()==ClimateDeviceBlock.Kind.WIND_TOWER)line(lines,"max_cooling",7);
            if(d.kind()==ClimateDeviceBlock.Kind.EVAP_WALL)line(lines,"max_cooling",18);
            line(lines,"range_blocks",5);
        }else if(b instanceof TextileStationBlock t){
            ItemStack input=t.input(),output=t.output();
            line(lines,"station_recipe",input.getCount(),input.getHoverName(),output.getCount(),output.getHoverName());
            line(lines,"cycle_seconds",t.duration()/20);
        }
    }
}
