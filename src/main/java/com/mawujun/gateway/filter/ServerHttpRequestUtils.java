package com.mawujun.gateway.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;

public class ServerHttpRequestUtils {
    private static final ThreadLocal<ServerHttpRequest> threadLocal = new ThreadLocal();

//    public static ServerHttpRequest getThreadLocal() {
//        return threadLocal.get();
//    }

    public static ServerHttpRequest get() {
        ServerHttpRequest map = threadLocal.get();
        return map;
    }


    public static void set(ServerHttpRequest value) {
        threadLocal.set(value);
    }
//
//    public static void set(ServerHttpRequest keyValueMap) {
//        ServerHttpRequest map = threadLocal.get();
//        map.putAll(keyValueMap);
//    }

    public static void remove() {
        threadLocal.remove();
    }

//    @SuppressWarnings("unchecked")
//    public static <T> T remove(String key) {
//        ServerHttpRequest map = threadLocal.get();
//        return (T) map.remove(key);
//    }

}
