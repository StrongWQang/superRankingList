package com;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

public class SegmentTreeTest {
    private SegmentTree.SegmentTreeNode root;
    private MockRedisClient redisClient;

    @BeforeEach
    void setUp() {
        // 创建一个最大分数为100，分段数为8的线段树
        root = SegmentTree.buildSegmentTree(100, 8);
        redisClient = new MockRedisClient();
    }

    @Test
    void testBuildSegmentTree() {
        assertNotNull(root);
        assertEquals(1, root.lower);
        assertEquals(100, root.upper);
        assertNotNull(root.left);
        assertNotNull(root.right);
    }

    @Test
    void testGetSegmentToUpdate() {
        // 测试获取需要更新的节点
        List<String> segments = SegmentTree.getSegmentToUpdate(root, 50);
        assertFalse(segments.isEmpty());
        // 验证返回的区间格式是否正确
        for (String segment : segments) {
            assertTrue(segment.matches("\\d+-\\d+"));
        }
    }

    @Test
    void testGetSegmentToRead() {
        // 测试获取需要读取的节点
        SegmentTree.SegmentReadResult result = SegmentTree.getSegmentToRead(root, 50);
        assertNotNull(result);
        assertNotNull(result.segment);
        assertFalse(result.segmentFields.isEmpty());
        // 验证返回的区间格式是否正确
        for (String field : result.segmentFields) {
            assertTrue(field.matches("\\d+-\\d+"));
        }
    }

    @Test
    void testUpdateScore() {
        // 测试更新分数
        assertDoesNotThrow(() -> {
            SegmentTree.updateScore(root, 50, 60, redisClient);
        });
        // 验证Redis中的值是否正确更新
        assertTrue(redisClient.getUpdatedFields().contains("50-60"));
    }

    @Test
    void testGetRank() {
        // 设置一些初始数据
        redisClient.setValue("1-100", "100");  // 总用户数
        redisClient.setValue("50-60", "20");   // 50-60分段的用户数
        
        // 测试获取排名
        long rank = SegmentTree.getRank(root, 55, redisClient);
        assertTrue(rank > 0);
    }

    @Test
    void testGetRankWithEmptyResult() {
        // 测试空结果的情况
        long rank = SegmentTree.getRank(root, 1000, redisClient);
        assertEquals(0, rank);
    }

    @Test
    void testUpdateScoreWithInvalidScores() {
        // 测试无效分数的情况
        assertThrows(RuntimeException.class, () -> {
            SegmentTree.updateScore(root, -1, 1000, redisClient);
        });
    }
}

// 模拟Redis客户端
class MockRedisClient implements RedisClient {
    private Map<String, String> data = new HashMap<>();
    private Set<String> updatedFields = new HashSet<>();

    public void setValue(String key, String value) {
        data.put(key, value);
    }

    public Set<String> getUpdatedFields() {
        return updatedFields;
    }

    @Override
    public void eval(String script, List<String> keys, List<String> args) throws Exception {
        // 模拟Redis的HINCRBY操作
        int m = Integer.parseInt(args.get(0));
        int n = Integer.parseInt(args.get(1));
        
        // 处理减1操作
        for (int i = 0; i < m; i++) {
            String field = keys.get(i + 1);
            String value = data.getOrDefault(field, "0");
            data.put(field, String.valueOf(Long.parseLong(value) - 1));
            updatedFields.add(field);
        }
        
        // 处理加1操作
        for (int i = 0; i < n; i++) {
            String field = keys.get(i + m + 1);
            String value = data.getOrDefault(field, "0");
            data.put(field, String.valueOf(Long.parseLong(value) + 1));
            updatedFields.add(field);
        }
    }

    @Override
    public Map<String, String> hmget(String key, List<String> fields) throws Exception {
        Map<String, String> result = new HashMap<>();
        for (String field : fields) {
            result.put(field, data.getOrDefault(field, "0"));
        }
        return result;
    }
} 