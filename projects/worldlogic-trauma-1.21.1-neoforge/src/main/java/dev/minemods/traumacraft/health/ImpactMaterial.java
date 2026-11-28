package dev.minemods.traumacraft.health;

/** Nominal properties of observed contact object, never effective impact mass or a damage multiplier. */
public record ImpactMaterial(Kind kind,String objectId,String evidence,double massKg,double volumeM3,double densityKgM3,java.util.Optional<dev.minemods.traumacraft.integration.ContactPhysics.Surface> surface) {
    public ImpactMaterial(Kind kind,String objectId,String evidence,double massKg,double volumeM3,double densityKgM3){this(kind,objectId,evidence,massKg,volumeM3,densityKgM3,java.util.Optional.empty());}
    public ImpactMaterial withSurface(java.util.Optional<dev.minemods.traumacraft.integration.ContactPhysics.Surface> response){return new ImpactMaterial(kind,objectId,evidence,massKg,volumeM3,densityKgM3,response);}
    public enum Kind { LANDING_SURFACE, HELD_ITEM }
    public ImpactMaterial {
        java.util.Objects.requireNonNull(surface);
        if(surface.isPresent()&&kind!=Kind.LANDING_SURFACE)throw new IllegalArgumentException("Surface response belongs to a landing surface");
        if(kind==null||objectId==null||objectId.isEmpty()||objectId.length()>128||evidence==null||evidence.isEmpty()||evidence.length()>32
            ||!Double.isFinite(massKg)||massKg<=0||!Double.isFinite(volumeM3)||volumeM3<=0||!Double.isFinite(densityKgM3)||densityKgM3<=0)
            throw new IllegalArgumentException("Invalid material observation");
    }
}
