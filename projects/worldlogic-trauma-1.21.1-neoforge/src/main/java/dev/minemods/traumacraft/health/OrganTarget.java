package dev.minemods.traumacraft.health;

/** Approximate local anatomy inside the vanilla player bounds, not extra entity hitboxes.
 * Lateral coordinate positive toward the player's left, height in 0..1, rear contact from attack source.
 */
public final class OrganTarget {
    private OrganTarget(){}
    public static OrganState.Organ select(BodyPart region,double lateral,double height,boolean rear){
        if(!Double.isFinite(lateral)||!Double.isFinite(height))return null;
        return switch(region){
            case HEAD -> OrganState.Organ.BRAIN;
            case CHEST -> {
                if(Math.abs(lateral)<.27&&height<.73&&!rear)yield OrganState.Organ.HEART;
                yield lateral>=0?OrganState.Organ.LEFT_LUNG:OrganState.Organ.RIGHT_LUNG;
            }
            case STOMACH -> {
                if(rear&&Math.abs(lateral)>.18)yield lateral>0?OrganState.Organ.LEFT_KIDNEY:OrganState.Organ.RIGHT_KIDNEY;
                if(height<.475)yield OrganState.Organ.INTESTINES;
                if(lateral<-.12)yield OrganState.Organ.LIVER;
                yield OrganState.Organ.STOMACH;
            }
            default -> null;
        };
    }
}
