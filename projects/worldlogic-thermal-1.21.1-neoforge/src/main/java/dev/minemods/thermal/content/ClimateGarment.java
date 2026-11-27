package dev.minemods.thermal.content;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.DyedItemColor;
import java.util.List;
/** Ordinary armor slots, not a second equipment inventory. */
public final class ClimateGarment extends ThermalWearable {
    public record Profile(double insulation,double ventilation,double rainShield,double windShield,double sunShield){}
    private final Profile profile;
    public ClimateGarment(Type type,int durability,int rgb,Profile p){
        super(type,new Item.Properties().durability(type.getDurability(durability)).component(DataComponents.DYED_COLOR,new DyedItemColor(rgb,false)));
        profile=p;
    }
    public Profile profile(){return profile;}
    @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> lines,TooltipFlag f){
        super.appendHoverText(s,c,lines,f);
        if(profile.insulation()>0)lines.add(Component.translatable("tooltip.worldlogic_thermal.insulation",(int)(profile.insulation()*100)).withStyle(ChatFormatting.GOLD));
        if(profile.ventilation()>0)lines.add(Component.translatable("tooltip.worldlogic_thermal.ventilation",(int)(profile.ventilation()*100)).withStyle(ChatFormatting.AQUA));
        if(profile.rainShield()>0)lines.add(Component.translatable("tooltip.worldlogic_thermal.rain_shield",(int)(profile.rainShield()*100)).withStyle(ChatFormatting.BLUE));
        if(profile.windShield()>0)lines.add(Component.translatable("tooltip.worldlogic_thermal.wind_shield",(int)(profile.windShield()*100)).withStyle(ChatFormatting.GRAY));
        if(profile.sunShield()>0)lines.add(Component.translatable("tooltip.worldlogic_thermal.sun_shield",(int)(profile.sunShield()*100)).withStyle(ChatFormatting.YELLOW));
    }
}
