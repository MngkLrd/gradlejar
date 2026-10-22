package dev.minemods.cowardlymobs.ai;

/** Fixed 0.1.0 gameplay tuning. No item-based buffs or artificial speed multipliers. */
public final class AiBalance {
    private AiBalance() {}

    public static final double WOUNDED_FRACTION = 0.40D;
    public static final double THREAT_ARMOR_POINTS = 12.0D;
    public static final double THREAT_ATTACK_DAMAGE = 6.0D;
    public static final float FEAR_ROLL_CHANCE = 0.40F;
    public static final int FEAR_EVALUATION_INTERVAL_TICKS = 90;
    public static final int FEAR_COOLDOWN_TICKS = 180;
    public static final int FEAR_DURATION_TICKS = 80;
    public static final double FEAR_DETECTION_DISTANCE = 13.0D;

    public static final double SKELETON_RETREAT_START = 8.0D;
    public static final double SKELETON_RETREAT_END = 11.0D;
    public static final double SKELETON_SPREAD_START = 3.0D;
    public static final double SKELETON_SPREAD_END = 4.0D;
    public static final double SKELETON_JOIN_START = 8.0D;
    public static final double SKELETON_JOIN_END = 5.0D;
    public static final int SKELETON_REPOSITION_TIMEOUT_TICKS = 65;
    public static final double NORMAL_PATHFINDING_SPEED = 1.0D;
}
