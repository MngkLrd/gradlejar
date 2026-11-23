package com.kirill.simplesorter.sorting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One-action, immutable plan for spatially consolidating compatible items.
 *
 * IMPORTANT: columns are NOT category-locked. Any vertical column can hold
 * ores in one chest, tools in another and clay in a third. Coordinates are
 * only a soft allocation hint when an already established family needs
 * additional space. The category belongs to the CHEST, not the column.
 *
 * One canonical X/Z grouping per family and connected storage area is
 * elected from actual contents, never from the user's left-to-right order.
 * This prevents one ore-filled chest in a distant sixth column from taking
 * priority over three ore-filled chests in the first column, while still
 * permitting several unrelated families within one vertical stack.
 *
 * Pure Java 17, so 6x4 and unrelated warehouse regression tests require no
 * game runtime.
 */
public final class WarehouseFamilyPlan {
    private static final int CONNECTED_DISTANCE_SQ = 20;

    public record Node(int x, int y, int z, Map<String, Integer> familySlots) {
        public Node {
            familySlots = Map.copyOf(familySlots);
        }
        public int occupied() {
            return familySlots.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    public record Column(int x, int z) {}
    private record Key(int region, String family) {}
    private record Evidence(Column column, int count, int pureSlots) {
        long score() { return 3L * pureSlots + count; }
    }

    private final List<Node> nodes;
    private final int[] region;
    private final Map<Key, Column> anchors;

    private WarehouseFamilyPlan(List<Node> nodes, int[] region,
                                Map<Key, Column> anchors) {
        this.nodes = List.copyOf(nodes);
        this.region = region;
        this.anchors = Map.copyOf(anchors);
    }

    public static WarehouseFamilyPlan analyze(List<Node> nodes) {
        int n = nodes.size();
        int[] region = new int[n];
        java.util.Arrays.fill(region, -1);
        int id = 0;
        for (int start = 0; start < n; start++) {
            if (region[start] >= 0) continue;
            region[start] = id;
            List<Integer> queue = new ArrayList<>();
            queue.add(start);
            for (int current = 0; current < queue.size(); current++) {
                Node a = nodes.get(queue.get(current));
                for (int next = 0; next < n; next++) {
                    if (region[next] >= 0) continue;
                    Node b = nodes.get(next);
                    int dx = a.x() - b.x(), dy = a.y() - b.y(), dz = a.z() - b.z();
                    if (dx * dx + dy * dy + dz * dz <= CONNECTED_DISTANCE_SQ) {
                        region[next] = id;
                        queue.add(next);
                    }
                }
            }
            id++;
        }

        Map<Key, Map<Column, int[]>> votes = new HashMap<>();
        for (int index = 0; index < n; index++) {
            Node node = nodes.get(index);
            Column column = new Column(node.x(), node.z());
            boolean pure = node.familySlots().size() == 1;
            for (Map.Entry<String, Integer> family : node.familySlots().entrySet()) {
                String name = family.getKey();
                if ("locked".equals(name)) continue;
                Key key = new Key(region[index], name);
                int[] weight = votes.computeIfAbsent(key, ignored -> new LinkedHashMap<>())
                        .computeIfAbsent(column, ignored -> new int[2]);
                weight[0] += family.getValue();
                if (pure) weight[1] += family.getValue();
            }
        }

        Map<Key, Column> anchors = new HashMap<>();
        for (var entry : votes.entrySet()) {
            Column bestColumn = null;
            long bestScore = Long.MIN_VALUE;
            for (var evidence : entry.getValue().entrySet()) {
                int[] stats = evidence.getValue();
                long score = 3L * stats[1] + stats[0];
                Column column = evidence.getKey();
                if (bestColumn == null || score > bestScore
                        || (score == bestScore &&
                           (column.x() < bestColumn.x()
                           || (column.x() == bestColumn.x()
                              && column.z() < bestColumn.z())))) {
                    bestColumn = column;
                    bestScore = score;
                }
            }
            if (bestColumn != null) anchors.put(entry.getKey(), bestColumn);
        }
        return new WarehouseFamilyPlan(nodes, region, anchors);
    }

    public int size() { return nodes.size(); }

    public int region(int index) {
        return index < 0 || index >= region.length ? -1 : region[index];
    }

    public boolean sameRegion(int a, int b) {
        return region(a) >= 0 && region(a) == region(b);
    }

    public Column anchor(int sourceIndex, String family) {
        return anchors.get(new Key(region(sourceIndex), family));
    }

    public boolean atHome(int index, String family) {
        Column anchor = anchor(index, family);
        if (anchor == null) return true; // no evidence: keep isolated contents
        Node node = nodes.get(index);
        return node.x() == anchor.x() && node.z() == anchor.z();
    }

    public boolean anchorExists(int index, String family) {
        return anchor(index, family) != null;
    }

    /** No exclusive owner per column: unrelated families can share a column. */
    public int priority(int targetIndex, String family, boolean pureTarget) {
        Column home = anchor(targetIndex, family);
        Node target = nodes.get(targetIndex);
        if (home == null) return pureTarget ? 200 : 0;
        int horizontalDistance = Math.abs(target.x() - home.x())
                + Math.abs(target.z() - home.z());
        return (horizontalDistance == 0 ? 10000 : 0)
                + (pureTarget ? 3000 : 0)
                - 25 * horizontalDistance;
    }

    public List<Integer> indicesInRegion(int sourceIndex) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            if (sameRegion(sourceIndex, i)) indices.add(i);
        }
        return indices;
    }
}
