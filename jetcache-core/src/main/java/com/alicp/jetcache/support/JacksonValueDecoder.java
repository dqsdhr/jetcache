package com.alicp.jetcache.support;

import com.unischool.dera.framework.jackson.JacksonUtil;

import java.util.function.Function;

public class JacksonValueDecoder implements Function<byte[], Object> {

    @Override
    public Object apply(byte[] bytes) {
        return JacksonUtil.from(new String(bytes), Object.class);
    }
}
