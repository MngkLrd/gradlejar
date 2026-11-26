package dev.minemods.fooddigestion;
public final class DigestionTest {
    public static void main(String[] args) {
        Digestion d = new Digestion();
        Digestion.Intake initial = d.eat(8, 0, 8, 12.8f);
        if (initial.food() != 3 || d.pendingHunger() != 5) throw new AssertionError("steak split");
        int level = 8 + initial.food(); float sat = initial.saturation();
        for (int t = 0; t < 500; t++) { var a = d.tick(level, sat); level += a.food(); sat += a.saturation(); }
        if (level != 16 || Math.abs(sat - 12.8f) > 0.001f || d.pendingHunger() != 0) throw new AssertionError("conservation");
        d = new Digestion(); initial = d.eat(10, 0, 4, 2);
        if (initial.food() != 1) throw new AssertionError("medium");
        for (int t = 0; t < 69; t++) if (d.tick(11,0).food() != 0) throw new AssertionError("too early");
        if (d.tick(11,0).food() != 1) throw new AssertionError("medium timing");
        d = new Digestion(); d.eat(10,0,8,12.8f); d.eat(13,0,8,12.8f);
        if (d.pendingHunger() > 7) throw new AssertionError("unbounded queue");
        System.out.println("Food Digestion core test passed");
    }
}
