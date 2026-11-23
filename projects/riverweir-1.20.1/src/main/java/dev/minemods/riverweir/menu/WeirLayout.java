package dev.minemods.riverweir.menu;

/**
 * Pure 1.20.1 vanilla-chest geometry, shared between logical slots and
 * the client screen. Regression-tested for 2/18/27-slot nets.
 */
public final class WeirLayout {
    public static final int INFORMATION_HEIGHT=18;
    private WeirLayout() {}
    public static int rows(int storage) {
        if(storage<1 || storage>27)throw new IllegalArgumentException("Invalid net capacity");
        return (storage+8)/9;
    }
    public static int upperHeight(int storage) { return 17+rows(storage)*18; }
    public static int playerTop(int storage) { return upperHeight(storage)+INFORMATION_HEIGHT; }
    public static int playerInventoryY(int storage) { return playerTop(storage)+14; }
    public static int playerHotbarY(int storage) { return playerTop(storage)+72; }
    public static int inventoryLabelY(int storage) { return playerTop(storage)+4; }
    public static int imageHeight(int storage) { return playerTop(storage)+96; }
}
