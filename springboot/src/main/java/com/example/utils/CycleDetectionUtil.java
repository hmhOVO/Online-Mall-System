package com.example.utils;

import java.util.*;

public final class CycleDetectionUtil {

    private CycleDetectionUtil() {}

    /**
     * 使用 Kahn 算法检测有向图是否存在环。
     *
     * @param nodeIds 所有节点 ID
     * @param edges   有向边，key 依赖 value 列表（即 key 必须在 value 完成后才能开始）
     * @return true 表示存在环
     */
    public static boolean hasCycle(Collection<Integer> nodeIds, Map<Integer, List<Integer>> edges) {
        Map<Integer, Integer> inDegree = new HashMap<>();
        for (Integer nodeId : nodeIds) {
            inDegree.put(nodeId, 0);
        }
        for (Map.Entry<Integer, List<Integer>> entry : edges.entrySet()) {
            Integer from = entry.getKey();
            List<Integer> toList = entry.getValue();
            if (toList == null) continue;
            for (Integer to : toList) {
                if (!inDegree.containsKey(to)) {
                    inDegree.put(to, 0);
                }
                inDegree.put(to, inDegree.get(to) + 1);
            }
            if (!inDegree.containsKey(from)) {
                inDegree.put(from, 0);
            }
        }

        Queue<Integer> queue = new LinkedList<>();
        for (Map.Entry<Integer, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        int visited = 0;
        while (!queue.isEmpty()) {
            Integer current = queue.poll();
            visited++;
            List<Integer> neighbors = edges.getOrDefault(current, Collections.emptyList());
            for (Integer neighbor : neighbors) {
                int degree = inDegree.get(neighbor) - 1;
                inDegree.put(neighbor, degree);
                if (degree == 0) {
                    queue.offer(neighbor);
                }
            }
        }
        return visited != inDegree.size();
    }
}
