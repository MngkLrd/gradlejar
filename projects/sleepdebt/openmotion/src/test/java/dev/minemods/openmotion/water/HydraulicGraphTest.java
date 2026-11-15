package dev.minemods.openmotion.water;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

public class HydraulicGraphTest {
    private static Map<Long, long[]> branchingNetwork() {
        // 1 pump -> 2 tee -> 3 upper sprinkler, 4 valve -> 5 lower sprinkler
        return Map.of(1L, new long[]{2}, 2L, new long[]{1, 3, 4},
                3L, new long[]{2}, 4L, new long[]{2, 5}, 5L, new long[]{4});
    }

    @Test
    void openingValveFeedsBothBranches() {
        var wet = HydraulicGraph.reachable(branchingNetwork(), Set.of(1L), Set.of(), Set.of(3L, 5L));
        assertEquals(Set.of(1L,2L,3L,4L,5L), wet);
    }

    @Test
    void closingValveIsolatesOnlyItsBranch() {
        var wet = HydraulicGraph.reachable(branchingNetwork(), Set.of(1L), Set.of(4L), Set.of(3L,5L));
        assertEquals(Set.of(1L,2L,3L), wet);
    }

    @Test
    void disconnectedSubgraphStaysDry() {
        var graph = new HashMap<Long, long[]>(branchingNetwork());
        graph.put(6L, new long[]{7});
        graph.put(7L, new long[]{6});
        var wet = HydraulicGraph.reachable(graph, Set.of(1L), Set.of(), Set.of(3L,5L));
        assertFalse(wet.contains(6L));
        assertFalse(wet.contains(7L));
    }

    @Test
    void sprinklerCannotBecomeAThroughPipe() {
        var graph = Map.of(1L, new long[]{2},2L,new long[]{1,3},3L,new long[]{2});
        var wet = HydraulicGraph.reachable(graph, Set.of(1L), Set.of(), Set.of(2L));
        assertEquals(Set.of(1L,2L), wet);
    }

    @Test
    void poweredOffPumpLeavesNetworkDry() {
        assertTrue(HydraulicGraph.reachable(branchingNetwork(), Set.of(), Set.of(), Set.of()).isEmpty());
    }

    @Test
    void multiPumpCanSupplyIsolatedBranch() {
        var wet = HydraulicGraph.reachable(branchingNetwork(), Set.of(1L,5L), Set.of(4L), Set.of(3L,5L));
        assertEquals(Set.of(1L,2L,3L,5L), wet);
    }

    @Test
    void largeNetworkBeyondOld384NodeLimit() {
        Map<Long, long[]> graph = new HashMap<>();
        for(long i=0;i<1000;i++) graph.put(i,new long[]{i>0?i-1:0,i<999?i+1:999});
        var wet=HydraulicGraph.reachable(graph,Set.of(0L),Set.of(),Set.of(999L));
        assertEquals(1000,wet.size());
    }
}
