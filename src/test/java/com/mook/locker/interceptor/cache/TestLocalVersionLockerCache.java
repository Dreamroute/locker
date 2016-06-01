package com.mook.locker.interceptor.cache;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.mook.locker.annotation.VersionLocker;
import com.mook.locker.domain.User;
import com.mook.locker.interceptor.cache.exception.UncachedMapperException;
import org.apache.ibatis.binding.MapperRegistry;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by wyx on 2016/6/1.
 */
public class TestLocalVersionLockerCache {

    private static final String UPDATE_METHOD_ID = "com.mook.locker.mapper.UserMapper.updateUser";

    private VersionLockerCache cache;

    private static SqlSessionFactory factory;

    private static MapperRegistry mapperRegistry;

    @BeforeClass
    public static void buildSessionFactory() throws IOException {
        Reader reader = Resources.getResourceAsReader("Configuration.xml");
        factory = new SqlSessionFactoryBuilder().build(reader, "test");
        mapperRegistry = factory.getConfiguration().getMapperRegistry();
    }

    @Before
    public void initCache() {
        cache = new LocalVersionLockerCache();
    }

    @Test
    public void testCacheMappersWithMapperRegistry() throws UncachedMapperException {
        cache.cacheMappers(mapperRegistry);

        VersionLocker versionLocker1 = cache.getAnnotation(UPDATE_METHOD_ID, new Class<?>[]{User.class});
        VersionLocker versionLocker2 = cache.getAnnotation(UPDATE_METHOD_ID, new Class<?>[]{String.class, String.class});
        VersionLocker versionLocker3 = cache.getAnnotation(UPDATE_METHOD_ID, new Class<?>[]{Map.class});
        VersionLocker versionLocker4 = cache.getAnnotation(UPDATE_METHOD_ID, new Class<?>[]{});
        Assert.assertTrue(versionLocker1 != null && versionLocker1.value());
        Assert.assertTrue(versionLocker2 != null && versionLocker2.value());
        Assert.assertTrue(versionLocker3 != null && !versionLocker3.value());
        Assert.assertTrue(versionLocker4 == null);
    }

    @Test(expected = UncachedMapperException.class)
    public void testCacheWithException() throws UncachedMapperException {
        cache.getAnnotation(UPDATE_METHOD_ID, new Class<?>[]{User.class});
    }

    @Test
    public void testHasCachedMappers() {
        cache.cacheMappers(mapperRegistry);
        Assert.assertTrue(cache.hasCachedMappers());
    }

    @Test(expected = UncachedMapperException.class)
    public void testClearMapperRegistry() throws UncachedMapperException {
        cache.cacheMappers(mapperRegistry);
        cache.clear();
        cache.getAnnotation(UPDATE_METHOD_ID, new Class<?>[]{User.class});
    }

    @Test(expected = UncachedMapperException.class)
    public void testClear() throws UncachedMapperException {
        cache.cacheMappers(mapperRegistry);
        cache.clear();
        cache.getAnnotation(UPDATE_METHOD_ID, new Class<?>[]{User.class});
    }

    @Test
    public void testCacheMappersWithMapperRegistryOnMultiThread() throws UncachedMapperException, InterruptedException {
        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("Start post cache data");
                    cache.cacheMappers(mapperRegistry);
                    latch2.countDown();
                    VersionLocker versionLocker1 = null;
                    try {
                        versionLocker1 = cache.getAnnotation(UPDATE_METHOD_ID, new Class<?>[]{User.class});
                    } catch (UncachedMapperException e) {
                        e.printStackTrace();
                    }
                    Assert.assertTrue(versionLocker1 != null && versionLocker1.value());

                }
            });
        }

        try {
            cache.getAnnotation(UPDATE_METHOD_ID, new Class<?>[]{User.class});
            Assert.assertTrue(false);
        } catch (UncachedMapperException ignored) {

        }
        latch.countDown();
        latch2.await();
        VersionLocker versionLocker1 = cache.getAnnotation(UPDATE_METHOD_ID, new Class<?>[]{User.class});
        VersionLocker versionLocker2 = cache.getAnnotation(UPDATE_METHOD_ID, new Class<?>[]{String.class, String.class});
        VersionLocker versionLocker3 = cache.getAnnotation(UPDATE_METHOD_ID, new Class<?>[]{Map.class});
        VersionLocker versionLocker4 = cache.getAnnotation(UPDATE_METHOD_ID, new Class<?>[]{});
        Assert.assertTrue(versionLocker1 != null && versionLocker1.value());
        Assert.assertTrue(versionLocker2 != null && versionLocker2.value());
        Assert.assertTrue(versionLocker3 != null && !versionLocker3.value());
        Assert.assertTrue(versionLocker4 == null);

        final CountDownLatch latch3 = new CountDownLatch(1);

        pool.execute(new Runnable() {
            @Override
            public void run() {
                cache.clear();
                latch3.countDown();
            }
        });
        latch3.await();
        try {
            cache.getAnnotation(UPDATE_METHOD_ID, new Class<?>[]{User.class});
            Assert.assertTrue(false);
        } catch (UncachedMapperException ignored) {

        }
    }
}
