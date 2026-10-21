package dev.minemods.openmotion.water;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Pure topology helper, shared by runtime hydraulic networks and headless tests.
 * Blocked valves cannot transmit or receive flow; sprinklers are terminal sinks.
 */
public final class HydraulicGraph {
    private HydraulicGraph() { }

    public static Set<Long> reachable(
            Map<Long, long[]> adjacency, Set<Long> sources,
            Set<Long> blocked, Set<Long> terminals) {
        Set<Long> flowing = new HashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        for (long source : sources) {
            if (!blocked.contains(source) && adjacency.containsKey(source) && flowing.add(source))
                queue.add(source);
        }
        while (!queue.isEmpty()) {
            long node = queue.removeFirst();
            if (terminals.contains(node)) continue;
            for (long neighbor : adjacency.getOrDefault(node, new long[0])) {
                if (!blocked.contains(neighbor) && adjacency.containsKey(neighbor) && flowing.add(neighbor))
                    queue.addLast(neighbor);
            }
        }
        return flowing;
    }
}
