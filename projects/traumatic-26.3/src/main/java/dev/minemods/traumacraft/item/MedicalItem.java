package dev.minemods.traumacraft.item;
import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.health.BodyStates;
import dev.minemods.traumacraft.health.BodyState;
import dev.minemods.traumacraft.health.MedicalAction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class MedicalItem extends Item {
    public enum Type {BANDAGE,SPLINT,MEDKIT,STERILE_PATCH,COMPRESSION_WRAP,HEMOSTATIC_GEL,TOURNIQUET,
      FRACTURE_BRACE,BONE_MENDER,COMPACT_MEDKIT,FIELD_MEDKIT,TRAUMA_KIT,SURGICAL_KIT,RECOVERY_SERUM,
      ADRENALINE_INJECTOR,GUARDIAN_INJECTOR,FOCUS_INJECTOR,ENDURANCE_INJECTOR}
    private final Type type;
    public MedicalItem(Properties p,Type type){super(p);this.type=type;}
    public Type medicalType(){return type;}
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        if(level.isClientSide())return InteractionResult.SUCCESS;
        ItemStack stack=player.getItemInHand(hand);
        BodyState state=BodyStates.get(player);
        BodyPart target=MedicalAction.autoTarget(state,type);
        if(target==null||!MedicalAction.apply(player,state,type,target)){
            player.displayClientMessage(Component.translatable("message.traumacraft.no_injury"),true);
            return InteractionResult.FAIL;
        }
        BodyStates.save(player,state);
        if(!player.getAbilities().instabuild)stack.shrink(1);
        player.displayClientMessage(Component.translatable("message.traumacraft.treated",target.label),true);
        return InteractionResult.SUCCESS_SERVER;
    }
}
