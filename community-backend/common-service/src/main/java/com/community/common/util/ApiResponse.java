package com.community.common.util;

import java.util.HashMap;
import java.util.Map;

public class ApiResponse extends HashMap<String, Object> {

    public ApiResponse() {
        put("code", 0);
        put("msg", "success");
    }

    /* ---------- 成功 ---------- */
    public static ApiResponse ok() {
        return new ApiResponse();
    }

    public static ApiResponse ok(String msg) {
        ApiResponse r = new ApiResponse();
        r.put("msg", msg);
        return r;
    }

    public static ApiResponse ok(Map<String, ?> data) {
        ApiResponse r = new ApiResponse();
        if (data != null) r.putAll(data);
        return r;
    }

    /* ---------- 失败/错误 ---------- */
    public static ApiResponse fail(int code, String msg) {
        ApiResponse r = new ApiResponse();
        r.put("code", code);
        r.put("msg", msg);
        return r;
    }

    public static ApiResponse fail(String msg) {
        return fail(500, msg);
    }

    public static ApiResponse error(String msg) {
        return fail(500, msg);
    }

    public static ApiResponse error(int code, String msg) {
        return fail(code, msg);
    }

    /* ---------- 辅助 ---------- */
    public ApiResponse data(String key, Object value) {
        this.put(key, value);
        return this;
    }

    public int getCode() {
        Object c = this.get("code");
        if (c instanceof Number) return ((Number) c).intValue();
        try {
            return Integer.parseInt(String.valueOf(c));
        } catch (Exception ignore) {
            return 0;
        }
    }

    public String getMsg() {
        Object m = this.get("msg");
        return m == null ? null : String.valueOf(m);
    }

    /**
     * 兼容习惯：getMessage() == getMsg()
     */
    public String getMessage() {
        return getMsg();
    }

    /**
     * 方便远程调用读取 data 区域
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getData() {
        Object d = this.get("data");
        if (d instanceof Map) return (Map<String, Object>) d;
        return null;
    }

    public boolean isOk() {
        return getCode() == 0;
    }

    public Map<String, Object> toMap() {
        return this;
    }
}
