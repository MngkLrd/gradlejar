package dev.minemods.thermal.content;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
public final class TextileToolItem extends Item {
    public enum Kind { SPINDLE, NEEDLE, COMB }
    private final Kind kind;
    public TextileToolItem(Kind k){super(new Properties().durability(128));kind=k;}
    public int processingCount(){return kind==Kind.SPINDLE?2:1;}
    public ItemStack processingOutput(){return switch(kind){
        case SPINDLE -> new ItemStack(ThermalContent.THREAD.get(),4);
        case COMB -> new ItemStack(ThermalContent.COMBED_WOOL.get(),2);
        case NEEDLE -> ItemStack.EMPTY;
    };}
    public static int repairedDamage(ItemStack armor){return Math.max(0,armor.getDamageValue()-Math.max(1,armor.getMaxDamage()/4));}
    public boolean operate(Player p,ItemStack tool){
        ItemStack material=p.getOffhandItem(),result;
        int required=1;
        switch(kind){
            case SPINDLE -> {if(!material.is(ThermalContent.FIBER.get())||material.getCount()<processingCount())return false;required=processingCount();result=processingOutput();}
            case COMB -> {if(!material.is(ItemTags.WOOL))return false;result=processingOutput();}
            case NEEDLE -> {
                if(!(material.getItem() instanceof ArmorItem)||!ThermalContent.creativeItems().contains(material.getItem())||material.getDamageValue()==0)return false;
                ItemStack thread=ItemStack.EMPTY;
                for(int i=0;i<36;i++){var candidate=p.getInventory().getItem(i);if(candidate.is(ThermalContent.THREAD.get())){thread=candidate;break;}}
                if(thread.isEmpty())return false;
                material.setDamageValue(repairedDamage(material));
                if(!p.getAbilities().instabuild){thread.shrink(1);tool.hurtAndBreak(1,p,EquipmentSlot.MAINHAND);}
                return true;
            }
            default -> {return false;}
        }
        if(!p.getAbilities().instabuild){material.shrink(required);tool.hurtAndBreak(1,p,EquipmentSlot.MAINHAND);}
        if(!p.getInventory().add(result))p.drop(result,false);
        return true;
    }
    @Override public void appendHoverText(ItemStack s,TooltipContext c,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag f){
        lines.add(net.minecraft.network.chat.Component.translatable(getDescriptionId()+".info").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
    @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand hand){
        ItemStack s=p.getItemInHand(hand);if(hand!=InteractionHand.MAIN_HAND)return InteractionResultHolder.pass(s);
        if(!l.isClientSide&&!operate(p,s))return InteractionResultHolder.pass(s);
        return InteractionResultHolder.sidedSuccess(s,l.isClientSide);
    }
}
