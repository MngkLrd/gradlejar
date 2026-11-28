package dev.minemods.traumacraft.health;

/** Nominal properties of observed contact object, never effective impact mass or a damage multiplier. */
public record ImpactMaterial(Kind kind,String objectId,String evidence,double massKg,double volumeM3,double densityKgM3) {
    public enum Kind { LANDING_SURFACE, HELD_ITEM }
    public ImpactMaterial {
        if(kind==null||objectId==null||objectId.isEmpty()||objectId.length()>128||evidence==null||evidence.isEmpty()||evidence.length()>32
            ||!Double.isFinite(massKg)||massKg<=0||!Double.isFinite(volumeM3)||volumeM3<=0||!Double.isFinite(densityKgM3)||densityKgM3<=0)
            throw new IllegalArgumentException("Invalid material observation");
    }
}
