package dev.minemods.cowardlymobs.ai;

/**
 * Vanilla-speed AI tuning for Cowardly Mobs 0.3.
 * All distance checks are local; no chunk-forcing or global mob scans.
 */
public final class AiBalance {
    private AiBalance() {}

    public static final double THREAT_ARMOR_POINTS = 12.0D;
    public static final double THREAT_ATTACK_DAMAGE = 6.0D;

    public static final double MORALE_HEALTH_LIMIT = 0.65D;
    public static final double MORALE_FEAR_THRESHOLD = 0.36D;
    public static final double MORALE_ALLY_RADIUS = 9.0D;
    public static final double MORALE_ZOMBIE_ALLY_BONUS = 0.17D;
    public static final double MORALE_SKELETON_ALLY_BONUS = 0.09D;
    public static final int MORALE_MAX_ALLIES = 4;

    public static final int FEAR_EVALUATION_INTERVAL_TICKS = 80;
    public static final int FEAR_COOLDOWN_TICKS = 180;
    public static final int FEAR_DURATION_TICKS = 80;
    public static final double FEAR_DETECTION_DISTANCE = 13.0D;

    public static final double RALLY_SEARCH_RADIUS = 15.0D;
    public static final double RALLY_NEIGHBOR_RADIUS = 5.5D;
    public static final double RALLY_PLAYER_SAFE_DISTANCE = 8.0D;
    public static final int RALLY_COURAGE_TICKS = 220;

    public static final double SKELETON_RETREAT_START = 8.0D;
    public static final double SKELETON_RETREAT_END = 11.0D;
    public static final double SKELETON_LINE_RANGE = 11.5D;
    public static final double SKELETON_LINE_SPACING = 3.4D;
    public static final double SKELETON_LINE_SEARCH_RADIUS = 17.0D;
    public static final int SKELETON_LINE_MAX_MEMBERS = 5;
    public static final double SKELETON_LINE_TOLERANCE = 2.4D;
    public static final int SKELETON_REPOSITION_TIMEOUT_TICKS = 50;
    public static final double NORMAL_PATHFINDING_SPEED = 1.0D;
}
