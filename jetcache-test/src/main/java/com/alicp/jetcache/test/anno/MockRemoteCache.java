/**
 * Created on  13-09-22 16:54
 */
package com.alicp.jetcache.test.anno;

import com.alicp.jetcache.*;
import com.alicp.jetcache.embedded.LinkedHashMapCacheBuilder;
import com.alicp.jetcache.external.ExternalCacheConfig;
import com.alicp.jetcache.external.ExternalKeyUtil;
import com.alicp.jetcache.support.FastjsonKeyConvertor;
import com.alicp.jetcache.support.JavaValueDecoder;
import com.alicp.jetcache.support.JavaValueEncoder;
import com.alicp.jetcache.test.AbstractCacheTest;
import org.junit.Test;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:yeli.hl@taobao.com">huangli</a>
 */
public class MockRemoteCache<K, V> implements Cache<K, V> {
    private Cache<Bytes, byte[]> cache;
    private ExternalCacheConfig config;

    public static class MockRemoteCacheTest extends AbstractCacheTest {
        @Test
        public void Test() throws Exception {
            MockRemoteCacheBuilder b = new MockRemoteCacheBuilder();
            b.setKeyConvertor(FastjsonKeyConvertor.INSTANCE);
            b.setValueDecoder(JavaValueDecoder.INSTANCE);
            b.setValueEncoder(JavaValueEncoder.INSTANCE);
            b.setKeyPrefix("PREFIX");
            cache = b.buildCache();
            baseTest();
        }
    }


    public MockRemoteCache(ExternalCacheConfig config) {
        this.config = config;
        cache = LinkedHashMapCacheBuilder.createLinkedHashMapCacheBuilder()
                .expireAfterWrite(config.getDefaultExpireInMillis(), TimeUnit.MILLISECONDS)
                .buildCache();
    }

    @Override
    public CacheConfig config() {
        return config;
    }

    static class Bytes {
        byte[] bs;

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Bytes) {
                return Arrays.equals(bs, ((Bytes) obj).bs);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int x = 0;
            for (byte b : bs) {
                x += b;
            }
            return x;
        }
    }

    private Bytes buildKey(K key) {
        try {
            Object newKey = key;
            if (config().getKeyConvertor() != null) {
                newKey = config.getKeyConvertor().apply(key);
            }
            byte[] keyBytes = ExternalKeyUtil.buildKeyAfterConvert(newKey, config.getKeyPrefix());
            Bytes bs = new Bytes();
            bs.bs = keyBytes;
            return bs;
        } catch (IOException e) {
            throw new CacheException(e);
        }
    }


    //-------------------------------


    @Override
    public <T> T unwrap(Class<T> clazz) {
        return null;
    }

    @Override
    public AutoReleaseLock tryLock(K key, long expire, TimeUnit timeUnit) {
        return cache.tryLock(buildKey(key), expire, timeUnit);
    }

    @Override
    public CacheGetResult<V> GET(K key) {
        CacheGetResult r = cache.GET(buildKey(key));
        if (r.isSuccess()) {
            V v = (V) config.getValueDecoder().apply((byte[]) r.getValue());
            r.setValue(v);
        }
        return r;
    }

    @Override
    public MultiGetResult<K, V> GET_ALL(Set<? extends K> keys) {
        ArrayList<K> keyList = new ArrayList<>(keys.size());
        ArrayList<Bytes> newKeyList = new ArrayList<>(keys.size());
        keys.stream().forEach((k) -> {
            Bytes newKey = buildKey(k);
            keyList.add(k);
            newKeyList.add(newKey);
        });
        MultiGetResult<Bytes, byte[]> result = cache.GET_ALL(new HashSet(keys));
        Map<Bytes, CacheGetResult<byte[]>> resultMap = result.getValues();
        if (resultMap != null) {
            Map<K, CacheGetResult<V>> returnMap = new HashMap<>();
            for (int i = 0; i < keyList.size(); i++) {
                K key = keyList.get(i);
                Bytes newKey = newKeyList.get(i);
                CacheGetResult r = resultMap.get(newKey);
                r.setValue(config.getValueDecoder().apply((byte[]) r.getValue()));
                returnMap.put(key, r);
            }
            result.setValues((Map) returnMap);
        }
        return (MultiGetResult) result;
    }

    @Override
    public CacheResult PUT(K key, V value, long expire, TimeUnit timeUnit) {
        return cache.PUT(buildKey(key), config.getValueEncoder().apply(value), expire, timeUnit);
    }

    @Override
    public CacheResult PUT_ALL(Map<? extends K, ? extends V> map, long expire, TimeUnit timeUnit) {
        Map<Bytes, byte[]> newMap = new HashMap<>();
        map.entrySet().forEach((e) -> newMap.put(buildKey(e.getKey()), config.getValueEncoder().apply(e.getValue())));
        return cache.PUT_ALL(newMap, expire, timeUnit);
    }

    @Override
    public CacheResult REMOVE(K key) {
        return cache.REMOVE(buildKey(key));
    }

    @Override
    public CacheResult REMOVE_ALL(Set<? extends K> keys) {
        return cache.REMOVE_ALL(keys.stream().map((k) -> buildKey(k)).collect(Collectors.toSet()));
    }

    @Override
    public CacheResult PUT_IF_ABSENT(K key, V value, long expire, TimeUnit timeUnit) {
        return cache.PUT_IF_ABSENT(buildKey(key), config.getValueEncoder().apply(value), expire, timeUnit);
    }
}
