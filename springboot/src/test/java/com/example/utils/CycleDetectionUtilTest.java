package com.example.utils;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CycleDetectionUtilTest {

    @Test
    void testNoCycle() {
        List<Integer> nodes = Arrays.asList(1, 2, 3);
        Map<Integer, List<Integer>> edges = new HashMap<>();
        edges.put(1, Arrays.asList(2));
        edges.put(2, Arrays.asList(3));
        assertFalse(CycleDetectionUtil.hasCycle(nodes, edges));
    }

    @Test
    void testDirectCycle() {
        List<Integer> nodes = Arrays.asList(1, 2);
        Map<Integer, List<Integer>> edges = new HashMap<>();
        edges.put(1, Arrays.asList(2));
        edges.put(2, Arrays.asList(1));
        assertTrue(CycleDetectionUtil.hasCycle(nodes, edges));
    }

    @Test
    void testIndirectCycle() {
        List<Integer> nodes = Arrays.asList(1, 2, 3);
        Map<Integer, List<Integer>> edges = new HashMap<>();
        edges.put(1, Arrays.asList(2));
        edges.put(2, Arrays.asList(3));
        edges.put(3, Arrays.asList(1));
        assertTrue(CycleDetectionUtil.hasCycle(nodes, edges));
    }

    @Test
    void testEmptyGraph() {
        assertFalse(CycleDetectionUtil.hasCycle(Collections.emptyList(), Collections.emptyMap()));
    }

    @Test
    void testSelfLoop() {
        List<Integer> nodes = Collections.singletonList(1);
        Map<Integer, List<Integer>> edges = new HashMap<>();
        edges.put(1, Collections.singletonList(1));
        assertTrue(CycleDetectionUtil.hasCycle(nodes, edges));
    }

    @Test
    void testSingleNode() {
        List<Integer> nodes = Collections.singletonList(1);
        assertFalse(CycleDetectionUtil.hasCycle(nodes, Collections.emptyMap()));
    }
}
