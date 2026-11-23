package com.kirill.simplesorter.sorting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Learns *vertical column* ownership from existing inventory contents.
 *
 * A warehouse wall's X/Z columns are NOT the same thing as its Y shelves.
 * The earlier layout used Y alone, causing saplings and ore to move across
 * already-established botanical/mineral columns.
 *
 * Column ownership is inferred only from confident, non-misc evidence.
 * It never allocates a column solely because it is physically nearby.
 * Pure Java 17 so the regression suite can cover adversarial layouts.
 */
public final class WarehouseColumns {
    private static final int ADJACENT_DISTANCE_SQUARED = 20;
    private static final int MIN_OCCUPIED_FOR_ROLE = 4;
    private static final double ROLE_PURITY = 0.67D;

    public record Node(int x, int y, int z, int occupied, Map<String, Integer> labels) {
        public Node {
            labels = Map.copyOf(labels);
        }
    }

    private record Column(int component, int coordinate) {}

    private final String[] roles;
    private final int[] components;
    private final boolean active;

    private WarehouseColumns(String[] roles, int[] components, boolean active) {
        this.roles = roles;
        this.components = components;
        this.active = active;
    }

    public static WarehouseColumns analyze(List<Node> nodes) {
        int n = nodes.size();
        String[] roles = new String[n];
        int[] components = new int[n];
        java.util.Arrays.fill(roles, "");
        java.util.Arrays.fill(components, -1);
        if (n < 4) return new WarehouseColumns(roles, components, false);

        int componentId = 0;
        boolean found = false;
        for (int first = 0; first < n; first++) {
            if (components[first] >= 0) continue;
            List<Integer> connected = new ArrayList<>();
            connected.add(first);
            components[first] = componentId;
            for (int i = 0; i < connected.size(); i++) {
                Node a = nodes.get(connected.get(i));
                for (int candidate = 0; candidate < n; candidate++) {
                    if (components[candidate] >= 0) continue;
                    Node b = nodes.get(candidate);
                    int dx = a.x() - b.x();
                    int dy = a.y() - b.y();
                    int dz = a.z() - b.z();
                    if (dx * dx + dy * dy + dz * dz <= ADJACENT_DISTANCE_SQUARED) {
                        components[candidate] = componentId;
                        connected.add(candidate);
                    }
                }
            }
            componentId++;
            if (connected.size() < 4) continue;

            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
            int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
            for (int index : connected) {
                Node node = nodes.get(index);
                minX = Math.min(minX, node.x());
                maxX = Math.max(maxX, node.x());
                minY = Math.min(minY, node.y());
                maxY = Math.max(maxY, node.y());
                minZ = Math.min(minZ, node.z());
                maxZ = Math.max(maxZ, node.z());
            }
            if (minY == maxY || (minX == maxX && minZ == maxZ)) continue;

            // X wall: different X coordinates are different vertical columns.
            // Z wall: different Z coordinates are different vertical columns.
            boolean xWall = (maxX - minX) >= (maxZ - minZ);
            Map<Integer, List<Integer>> buckets = new LinkedHashMap<>();
            for (int index : connected) {
                Node node = nodes.get(index);
                int coord = xWall ? node.x() : node.z();
                buckets.computeIfAbsent(coord, ignored -> new ArrayList<>()).add(index);
            }
            if (buckets.size() < 2) continue;

            int seededColumns = 0;
            for (List<Integer> indices : buckets.values()) {
                if (indices.size() < 2) continue;
                int minHeight = Integer.MAX_VALUE, maxHeight = Integer.MIN_VALUE;
                Map<String, Integer> votes = new HashMap<>();
                int occupied = 0;
                for (int index : indices) {
                    Node node = nodes.get(index);
                    minHeight = Math.min(minHeight, node.y());
                    maxHeight = Math.max(maxHeight, node.y());
                    occupied += node.occupied();
                    for (var vote : node.labels().entrySet()) {
                        String family = WarehouseLayout.family(vote.getKey());
                        if (!family.equals("misc")) {
                            votes.merge(family, vote.getValue(), Integer::sum);
                        }
                    }
                }
                if (minHeight == maxHeight || occupied < MIN_OCCUPIED_FOR_ROLE) continue;
                String dominant = "";
                int maximum = 0;
                for (var vote : votes.entrySet()) {
                    if (vote.getValue() > maximum) {
                        maximum = vote.getValue();
                        dominant = vote.getKey();
                    }
                }
                if (maximum < MIN_OCCUPIED_FOR_ROLE ||
                        (double) maximum / occupied < ROLE_PURITY) continue;
                for (int index : indices) roles[index] = dominant;
                seededColumns++;
            }
            if (seededColumns > 0) found = true;
        }
        return new WarehouseColumns(roles, components, found);
    }

    public boolean active() { return active; }

    public String role(int index) {
        return index < 0 || index >= roles.length ? "" : roles[index];
    }

    public boolean sameRegion(int a, int b) {
        return a >= 0 && a < components.length && b >= 0 && b < components.length
                && components[a] >= 0 && components[a] == components[b];
    }

    public boolean matches(int index, String category) {
        String role = role(index);
        return !role.isEmpty() && role.equals(WarehouseLayout.family(category));
    }

    public boolean conflicts(int index, String category) {
        String role = role(index);
        return !role.isEmpty() && !role.equals(WarehouseLayout.family(category));
    }

    public boolean hasEstablishedColumn(int sourceIndex, String category) {
        for (int i = 0; i < roles.length; i++) {
            if (sameRegion(sourceIndex, i) && matches(i, category)) return true;
        }
        return false;
    }

    /**
     * If a seeded column exists, a different/unknown column is not an
     * acceptable destination for this family. Unknown items are not forced.
     */
    public boolean allowsDestination(int sourceIndex, int destinationIndex, String category) {
        if (!active || !sameRegion(sourceIndex, destinationIndex)) return true;
        if (conflicts(destinationIndex, category)) return false;
        return !hasEstablishedColumn(sourceIndex, category)
                || matches(destinationIndex, category);
    }

    /** Prefer empty space below an already seeded category, not an
     * unrelated chest on the same horizontal Y shelf. */
    public double preference(int sourceIndex, int destinationIndex, String category) {
        if (!active || !sameRegion(sourceIndex, destinationIndex)) return 0D;
        if (conflicts(destinationIndex, category)) return -1D;
        return matches(destinationIndex, category) ? 1D : 0D;
    }
}
