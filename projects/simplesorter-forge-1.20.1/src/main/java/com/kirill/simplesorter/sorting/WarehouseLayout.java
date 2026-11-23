package com.kirill.simplesorter.sorting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One-click spatial hint for a warehouse made of adjacent storage blocks.
 *
 * No signs, saved chest profiles, world ticks or player setup required.
 * The semantic sorter remains the authority; position only breaks ties or
 * guides the selection of previously empty storage blocks.
 *
 * Pure Java17 core to allow geometry tests without starting Minecraft.
 */
public final class WarehouseLayout {
    private static final String[] ORDER = {
            "raw_minerals", "finished_materials", "building", "crafting", "misc", "botanical"
    };
    private static final int MAX_ADJACENT_DISTANCE_SQUARED = 20; // up to four blocks away
    private static final int MIN_CLUSTER_SIZE = 3;

    public record Node(int x, int y, int z, int occupied, Map<String, Integer> labels) {
        public Node {
            labels = Map.copyOf(labels);
        }
    }

    public enum Axis { VERTICAL, HORIZONTAL_X, HORIZONTAL_Z, NONE }

    private record Region(int[] indices, Axis axis, int[] lanes,
                          int laneCount, Map<String, Integer> preferred) {
        int lane(int index) {
            return lanes[index];
        }
    }

    private final Map<Integer, Region> regions;

    private WarehouseLayout(Map<Integer, Region> regions) {
        this.regions = regions;
    }

    public static WarehouseLayout analyze(List<Node> nodes) {
        return analyze(nodes, true);
    }

    /**
     * Non-invasive mode respects strongly seeded shelf roles; aggressive
     * reorganization ignores those roles and enforces a canonical layout.
     */
    public static WarehouseLayout analyze(List<Node> nodes, boolean respectSeeded) {
        Map<Integer, Region> membership = new HashMap<>();
        if (nodes.size() < MIN_CLUSTER_SIZE) return new WarehouseLayout(membership);

        // Only nearby blocks form one "warehouse". A remote chest must never
        // be treated as an extension of the player's storage wall.
        boolean[] seen = new boolean[nodes.size()];
        for (int start = 0; start < nodes.size(); start++) {
            if (seen[start]) continue;
            List<Integer> component = new ArrayList<>();
            List<Integer> pending = new ArrayList<>();
            pending.add(start);
            seen[start] = true;
            for (int p = 0; p < pending.size(); p++) {
                int i = pending.get(p);
                component.add(i);
                Node a = nodes.get(i);
                for (int j = 0; j < nodes.size(); j++) {
                    if (seen[j]) continue;
                    Node b = nodes.get(j);
                    int dx = a.x() - b.x();
                    int dy = a.y() - b.y();
                    int dz = a.z() - b.z();
                    if (dx * dx + dy * dy + dz * dz <= MAX_ADJACENT_DISTANCE_SQUARED) {
                        seen[j] = true;
                        pending.add(j);
                    }
                }
            }
            if (component.size() < MIN_CLUSTER_SIZE) continue;

            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
            int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
            for (int index : component) {
                Node n = nodes.get(index);
                minX = Math.min(minX, n.x()); maxX = Math.max(maxX, n.x());
                minY = Math.min(minY, n.y()); maxY = Math.max(maxY, n.y());
                minZ = Math.min(minZ, n.z()); maxZ = Math.max(maxZ, n.z());
            }

            Axis axis;
            Map<Integer, Integer> countsPerHeight = new HashMap<>();
            for (int index : component) {
                countsPerHeight.merge(nodes.get(index).y(), 1, Integer::sum);
            }
            boolean stackedRows = countsPerHeight.size() >= 2
                    && countsPerHeight.values().stream().filter(count -> count >= 2).count() >= 2;
            if (maxY > minY && (maxY - minY >= 2 || stackedRows)) axis = Axis.VERTICAL;
            else if (maxX - minX >= Math.max(1, maxZ - minZ)) axis = Axis.HORIZONTAL_X;
            else axis = Axis.HORIZONTAL_Z;

            // Avoid guessing row orders for scattered 3D warehouse rooms.
            int[] values = component.stream()
                    .mapToInt(i -> coordinate(nodes.get(i), axis))
                    .distinct().sorted().toArray();
            if (values.length < 2) continue;

            int[] lanes = new int[nodes.size()];
            Arrays.fill(lanes, -1);
            for (int i : component) {
                int coord = coordinate(nodes.get(i), axis);
                int index = Arrays.binarySearch(values, coord);
                lanes[i] = axis == Axis.VERTICAL ? values.length - 1 - index : index;
            }

            Map<String, Integer> preferred = inferredPreferredRows(nodes, component, lanes, values.length, respectSeeded);
            int[] indices = component.stream().mapToInt(Integer::intValue).toArray();
            Region region = new Region(indices, axis, lanes, values.length, preferred);
            for (int index : indices) membership.put(index, region);
        }
        return new WarehouseLayout(membership);
    }

    private static int coordinate(Node node, Axis axis) {
        return switch (axis) {
            case VERTICAL -> node.y();
            case HORIZONTAL_X -> node.x();
            case HORIZONTAL_Z -> node.z();
            default -> 0;
        };
    }

    public static String family(String category) {
        if (category == null || category.isBlank()) return "misc";
        return switch (category) {
            case "raw_minerals", "ores", "raw_ores", "tiny_dust", "small_dust", "dusts",
                    "gems", "salts" -> "raw_minerals";
            case "finished_materials", "ingots", "nuggets", "metal_sheets", "plates",
                    "rods", "wires", "gears" -> "finished_materials";
            case "blocks", "building", "stone", "loose_stones", "wood", "logs" -> "building";
            case "crafting", "adhesives", "molds", "clays", "sticks",
                    "tool_heads", "smithing_templates", "vessels", "redstone", "tools",
                    "weapons", "armor" -> "crafting";
            case "botanical", "plants", "fruit_tree_parts", "food", "seeds" -> "botanical";
            default -> "misc";
        };
    }

    public static int defaultRank(String category) {
        String family = family(category);
        for (int i = 0; i < ORDER.length; i++) {
            if (ORDER[i].equals(family)) return i;
        }
        return 4;
    }

    private static Map<String, Integer> inferredPreferredRows(
            List<Node> nodes, List<Integer> component, int[] lane, int rowCount,
            boolean respectSeeded) {
        Map<String, Integer> preferred = new LinkedHashMap<>();
        // A clear, already seeded row is stronger evidence than our canonical
        // order, but a random mixed chest must never seed an entire lane.
        int[][] votes = new int[ORDER.length][rowCount];
        int[] totalInLane = new int[rowCount];
        for (int index : component) {
            Node node = nodes.get(index);
            int row = lane[index];
            totalInLane[row] += node.occupied();
            for (var entry : node.labels().entrySet()) {
                int family = defaultRank(entry.getKey());
                votes[family][row] += entry.getValue();
            }
        }
        boolean[] occupiedLane = new boolean[rowCount];
        // First claim high-confidence anchored rows in order of evidence.
        record Evidence(int family, int row, int count, double purity) {}
        List<Evidence> anchors = new ArrayList<>();
        for (int family = 0; family < ORDER.length; family++) {
            for (int row = 0; row < rowCount; row++) {
                int count = votes[family][row];
                int all = totalInLane[row];
                if (count >= 3 && all > 0 && (double) count / all >= 0.72D) {
                    anchors.add(new Evidence(family, row, count, (double) count / all));
                }
            }
        }
        if (!respectSeeded) anchors.clear();
        anchors.sort(Comparator.comparingInt(Evidence::count).reversed()
                .thenComparing(Comparator.comparingDouble(Evidence::purity).reversed()));
        for (Evidence e : anchors) {
            String key = ORDER[e.family()];
            if (preferred.containsKey(key) || occupiedLane[e.row()]) continue;
            preferred.put(key, e.row());
            occupiedLane[e.row()] = true;
        }

        // Fill the remaining rows automatically. With fewer than six rows
        // multiple related classes share a row; with more, extra rows serve
        // as additional storage capacity.
        for (int family = 0; family < ORDER.length; family++) {
            String key = ORDER[family];
            if (preferred.containsKey(key)) continue;
            double ideal = (double) family * (rowCount - 1) / (ORDER.length - 1);
            int closest = -1;
            double best = Double.POSITIVE_INFINITY;
            for (int row = 0; row < rowCount; row++) {
                if (rowCount >= ORDER.length && occupiedLane[row]) continue;
                double diff = Math.abs(row - ideal);
                if (diff < best) { best = diff; closest = row; }
            }
            if (closest == -1) closest = (int) Math.round(ideal);
            preferred.put(key, closest);
            occupiedLane[closest] = true;
        }
        return Map.copyOf(preferred);
    }

    /** Relative preference [0,1]; zero means no confident shelf layout. */
    public double preference(int index, String category) {
        Region region = regions.get(index);
        if (region == null) return 0D;
        int preferred = region.preferred().getOrDefault(family(category), -1);
        if (preferred < 0) return 0D;
        return 1D - (double) Math.abs(preferred - region.lane(index))
                / Math.max(1, region.laneCount() - 1);
    }

    public boolean sameRegion(int a, int b) {
        return regions.containsKey(a) && regions.get(a) == regions.get(b);
    }

    public Axis axis(int index) {
        Region region = regions.get(index);
        return region == null ? Axis.NONE : region.axis();
    }

    public int lane(int index) {
        Region region = regions.get(index);
        return region == null ? -1 : region.lane(index);
    }

    /** Prefer an empty chest in the correct row, but never at the expense
     * of a better semantic match or inventory insertion constraints. */
    public double emptyPlacementScore(int sourceIndex, int destIndex, String category) {
        if (!sameRegion(sourceIndex, destIndex)) return -1D;
        return preference(destIndex, category);
    }

    /**
     * New, conservatively placed shelf organization: an isolated or homogeneous
     * stack can move into an empty chest in its clearly better shelf. This is
     * still one-click and does not require pre-labelling storage. If the layout
     * is ambiguous, no automatic relocation is attempted.
     */
    public boolean shouldUsePreferredEmpty(int sourceIndex, String category,
                                           List<Integer> empties) {
        if (axis(sourceIndex) == Axis.NONE || preference(sourceIndex, category) > 0.35D) {
            return false;
        }
        for (int empty : empties) {
            if (sameRegion(sourceIndex, empty) && preference(empty, category) >= 0.90D) {
                return true;
            }
        }
        return false;
    }

    public int analyzedBlocks() { return regions.size(); }
}
