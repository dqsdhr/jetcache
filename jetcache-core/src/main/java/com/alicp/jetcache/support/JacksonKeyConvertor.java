/**
 * Created on  13-09-10 15:45
 */
package com.alicp.jetcache.support;

import com.unischool.dera.framework.jackson.JacksonUtil;

import java.util.function.Function;

/**
 * @author <a href="mailto:areyouok@gmail.com">huangli</a>
 */
public class JacksonKeyConvertor implements Function<Object, Object> {

    public static final JacksonKeyConvertor INSTANCE = new JacksonKeyConvertor();

    @Override
    public Object apply(Object originalKey) {
        if (originalKey == null) {
            return null;
        }
        if (originalKey instanceof String) {
            return originalKey;
        }
        return JacksonUtil.to(originalKey);
    }

}

