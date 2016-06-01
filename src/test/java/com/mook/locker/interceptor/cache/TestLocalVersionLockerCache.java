package com.mook.locker.interceptor.cache;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.mook.locker.annotation.VersionLocker;
import com.mook.locker.domain.User;
import com.mook.locker.interceptor.cache.exception.UncachedMapperException;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
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

    private static Configuration configuration;

    @BeforeClass
    public static void buildSessionFactory() throws IOException {
        Reader reader = Resources.getResourceAsReader("Configuration.xml");
        factory = new SqlSessionFactoryBuilder().build(reader, "test");
        configuration = factory.getConfiguration();
    }

    @Before
    public void initCache() {
        cache = new LocalVersionLockerCache();
    }

    private void cachedAllUpdateMethods() {
        cache.cacheMappers(configuration, UPDATE_METHOD_ID, new Class<?>[]{User.class});
        cache.cacheMappers(configuration, UPDATE_METHOD_ID, new Class<?>[]{String.class, String.class});
        cache.cacheMappers(configuration, UPDATE_METHOD_ID, new Class<?>[]{Map.class});
        cache.cacheMappers(configuration, UPDATE_METHOD_ID, new Class<?>[]{});
    }

    @Test
    public void testCacheMappersWithMapperRegistry() throws UncachedMapperException {
        cachedAllUpdateMethods();

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
        cachedAllUpdateMethods();
        Assert.assertTrue(cache.hasCachedMappers());
    }

    @Test(expected = UncachedMapperException.class)
    public void testClearMapperRegistry() throws UncachedMapperException {
        cachedAllUpdateMethods();
        cache.clear();
        cache.getAnnotation(UPDATE_METHOD_ID, new Class<?>[]{User.class});
    }

    @Test(expected = UncachedMapperException.class)
    public void testClear() throws UncachedMapperException {
        cachedAllUpdateMethods();
        cache.clear();
        cache.getAnnotation(UPDATE_METHOD_ID, new Class<?>[]{User.class});
    }

    @Test(expected = RuntimeException.class)
    public void testCacheWithInvalidMapper() {
        cache.cacheMappers(configuration, "com.mook.locker.mapper.InvalidUserMapper.updateUser", new Class<?>[]{User.class});
    }

    @Test
    public void testCachedManyTimes() {
        Assert.assertTrue(cache.cacheMappers(configuration, UPDATE_METHOD_ID, new Class<?>[]{User.class}));
        Assert.assertFalse(cache.cacheMappers(configuration, UPDATE_METHOD_ID, new Class<?>[]{User.class}));
    }

    @Test
    public void testCachedManyTimesOnMultiThread() throws InterruptedException {
        final AtomicInteger trueCount = new AtomicInteger(0);
        final AtomicInteger falseCount = new AtomicInteger(0);
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
                    if (cache.cacheMappers(configuration, UPDATE_METHOD_ID, new Class<?>[]{User.class})) {
                        trueCount.getAndIncrement();
                    } else {
                        falseCount.getAndIncrement();
                    }
                    latch2.countDown();
                }
            });
        }
        latch.countDown();
        latch2.await();
        Assert.assertTrue(trueCount.get() == 1);
        Assert.assertTrue(falseCount.get() == threadCount - 1);
    }

    @Test
    public void testCacheMappersWithMapperRegistryOnMultiThread() throws
            UncachedMapperException, InterruptedException {
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
                    cachedAllUpdateMethods();
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
