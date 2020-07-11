package com.alicp.jetcache.support;

import com.unischool.dera.framework.jackson.JacksonUtil;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public class JacksonValueEncoder implements Function<Object, byte[]> {

    @Override
    public byte[] apply(Object o) {

        return JacksonUtil.to(o).getBytes(StandardCharsets.UTF_8);
    }
}
