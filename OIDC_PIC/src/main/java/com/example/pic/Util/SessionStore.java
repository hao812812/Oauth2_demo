package com.example.pic.Util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionStore {
	
    // 用 ConcurrentHashMap 模擬暫存區
    private static final Map<String, Map<String, String>> store = new ConcurrentHashMap<>();
    
    /**
     * 儲存授權請求
     */
    public static void put(String sessionId, Map<String, String> data) {
        store.put(sessionId, data);
    }

    /**
     * 取得授權請求
     */
    public static Map<String, String> get(String sessionId) {
        return store.get(sessionId);
    }

    /**
     * 移除授權請求
     */
    public static void remove(String sessionId) {
        store.remove(sessionId);
    }
    
}
