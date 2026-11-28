package dev.minemods.treechopper;
/** Applied only when the server accepts a chopping strike. */
public interface ChopSwing {
    void wl$markSwing();
    int wl$swingDuration();
}
