package dev.minemods.fooddigestion;

public final class Digestion {
    private static final int[] INTERVAL = {40, 70, 100};
    private final int[] hunger = new int[3], timer = new int[3];
    private final float[] saturation = new float[3];

    public int pendingHunger() { return hunger[0] + hunger[1] + hunger[2]; }
    public float pendingSaturation() { return saturation[0] + saturation[1] + saturation[2]; }

    public Intake eat(int level, float sat, int nutrition, float satPoints) {
        if (nutrition < 0 || !Float.isFinite(satPoints) || satPoints < 0) return new Intake(0, 0);
        int actual = Math.min(nutrition, Math.max(0, 20 - level - pendingHunger()));
        int immediate = actual == 0 ? 0 : Math.max(1, (int)Math.floor(actual * 0.4));
        float totalSat = Math.min(satPoints, Math.max(0f, level + actual + pendingHunger() - sat - pendingSaturation()));
        float immediateSat = Math.min(totalSat * 0.4f, Math.max(0f, level + immediate - sat));
        int bucket = nutrition <= 3 ? 0 : nutrition <= 5 ? 1 : 2;
        hunger[bucket] += actual - immediate;
        saturation[bucket] += totalSat - immediateSat;
        if (timer[bucket] <= 0 && (hunger[bucket] > 0 || saturation[bucket] > 0.0001f)) timer[bucket] = INTERVAL[bucket];
        return new Intake(immediate, immediateSat);
    }

    public Intake tick(int level, float sat) {
        int food = 0;
        float fill = 0;
        for (int i = 0; i < 3; i++) {
            if (hunger[i] == 0 && saturation[i] <= 0.0001f) { timer[i] = 0; saturation[i] = 0; continue; }
            if (timer[i] <= 0) timer[i] = INTERVAL[i];
            if (--timer[i] > 0) continue;
            timer[i] = INTERVAL[i];
            float step = hunger[i] > 0 ? saturation[i] / hunger[i] : Math.min(1f, saturation[i]);
            if (hunger[i] > 0) {
                hunger[i]--;
                if (level + food < 20) food++;
            }
            saturation[i] = Math.max(0, saturation[i] - step);
            fill += Math.min(step, Math.max(0f, level + food - sat - fill));
        }
        return new Intake(food, fill);
    }

    public int hunger(int i) { return hunger[i]; }
    public int timer(int i) { return timer[i]; }
    public float saturation(int i) { return saturation[i]; }
    public void restore(int i, int h, float s, int t) {
        hunger[i] = Math.max(0, Math.min(20, h));
        saturation[i] = Float.isFinite(s) ? Math.max(0, Math.min(20, s)) : 0;
        timer[i] = Math.max(0, Math.min(INTERVAL[i], t));
    }
    public record Intake(int food, float saturation) {}
}
