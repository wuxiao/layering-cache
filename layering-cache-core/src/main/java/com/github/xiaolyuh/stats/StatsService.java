package com.github.xiaolyuh.stats;

import com.github.xiaolyuh.cache.Cache;
import com.github.xiaolyuh.cache.LayeringCache;
import com.github.xiaolyuh.manager.AbstractCacheManager;
import com.github.xiaolyuh.manager.CacheManager;
import com.github.xiaolyuh.setting.LayeringCacheSetting;
import com.github.xiaolyuh.support.Lock;
import com.github.xiaolyuh.util.JsonUtils;
import com.github.xiaolyuh.util.RedisHelper;
import com.github.xiaolyuh.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 统计服务
 *
 * @author yuhao.wang3
 */
public class StatsService {
    private static Logger logger = LoggerFactory.getLogger(StatsService.class);

    /**
     * 缓存统计数据前缀
     */
    public static final String CACHE_STATS_KEY_PREFIX = "layering-cache:cache_stats_info:xiaolyuh:";

    private static final AtomicInteger SEQUENCE = new AtomicInteger(1);
    private static final String PREFIX = "layering-cache-pool";
    /**
     * 定时任务线程池
     */
    private static ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r);
        int seq = SEQUENCE.getAndIncrement();
        thread.setName(PREFIX + (seq > 1 ? "-" + seq : ""));
        if (!thread.isDaemon())
            thread.setDaemon(true);
        return thread;
    });

    /**
     * {@link AbstractCacheManager }
     */
    private AbstractCacheManager cacheManager;

    /**
     * 获取缓存统计list
     *
     * @param cacheNameParam 缓存名称
     * @return List&lt;CacheStatsInfo&gt;
     */
    public List<CacheStatsInfo> listCacheStats(String cacheNameParam) {
        logger.debug("获取缓存统计数据");

        Set<String> layeringCacheKeys = RedisHelper.scan(cacheManager.getRedisTemplate(), CACHE_STATS_KEY_PREFIX + "*");
        if (CollectionUtils.isEmpty(layeringCacheKeys)) {
            return Collections.emptyList();
        }
        // 遍历找出对应统计数据
        List<CacheStatsInfo> statsList = new ArrayList<>();
        for (String key : layeringCacheKeys) {
            if (key.endsWith("_lock") || (StringUtils.isNotBlank(cacheNameParam) && !key.startsWith(CACHE_STATS_KEY_PREFIX + cacheNameParam))) {
                continue;
            }

            CacheStatsInfo cacheStats = (CacheStatsInfo) cacheManager.getRedisTemplate().opsForValue().get(key);
            if (!Objects.isNull(cacheStats)) {
                statsList.add(cacheStats);
            }
        }

        return statsList.stream().sorted(Comparator.comparing(CacheStatsInfo::getHitRate)).collect(Collectors.toList());
    }

    /**
     * 同步缓存统计list
     */
    public void syncCacheStats() {
        RedisTemplate<String, Object> redisTemplate = cacheManager.getRedisTemplate();
        // 清空统计数据
        resetCacheStat();
        executorService.scheduleWithFixedDelay(() -> {
            logger.debug("执行缓存统计数据采集定时任务");
            Set<AbstractCacheManager> cacheManagers = AbstractCacheManager.getCacheManager();
            for (AbstractCacheManager abstractCacheManager : cacheManagers) {
                // 获取CacheManager
                Collection<String> cacheNames = ((CacheManager) abstractCacheManager).getCacheNames();
                for (String cacheName : cacheNames) {
                    // 获取Cache
                    Collection<Cache> caches = ((CacheManager) abstractCacheManager).getCache(cacheName);
                    for (Cache cache : caches) {
                        LayeringCache layeringCache = (LayeringCache) cache;
                        LayeringCacheSetting layeringCacheSetting = layeringCache.getLayeringCacheSetting();
                        // 加锁并增量缓存统计数据，缓存key=固定前缀 +缓存名称加 + 内部缓存名
                        String redisKey = CACHE_STATS_KEY_PREFIX + cacheName + layeringCacheSetting.getInternalKey();
                        Lock lock = new Lock(redisTemplate, redisKey, 60, 5000);
                        try {
                            if (lock.tryLock()) {
                                CacheStatsInfo cacheStats = (CacheStatsInfo) redisTemplate.opsForValue().get(redisKey);
                                if (Objects.isNull(cacheStats)) {
                                    cacheStats = new CacheStatsInfo();
                                }

                                // 设置缓存唯一标示
                                cacheStats.setCacheName(cacheName);
                                cacheStats.setInternalKey(layeringCacheSetting.getInternalKey());

                                cacheStats.setDepict(layeringCacheSetting.getDepict());
                                // 设置缓存配置信息
                                cacheStats.setLayeringCacheSetting(layeringCacheSetting);

                                // 设置缓存统计数据
                                CacheStats layeringCacheStats = layeringCache.getCacheStats();
                                CacheStats firstCacheStats = layeringCache.getFirstCache().getCacheStats();
                                CacheStats secondCacheStats = layeringCache.getSecondCache().getCacheStats();

                                // 清空加载缓存时间
                                firstCacheStats.getAndResetCachedMethodRequestTime();
                                secondCacheStats.getAndResetCachedMethodRequestTime();

                                cacheStats.setRequestCount(cacheStats.getRequestCount() + layeringCacheStats.getAndResetCacheRequestCount());
                                cacheStats.setMissCount(cacheStats.getMissCount() + layeringCacheStats.getAndResetCachedMethodRequestCount());
                                cacheStats.setTotalLoadTime(cacheStats.getTotalLoadTime() + layeringCacheStats.getAndResetCachedMethodRequestTime());
                                cacheStats.setHitRate((cacheStats.getRequestCount() - cacheStats.getMissCount()) / (double) cacheStats.getRequestCount() * 100);

                                cacheStats.setFirstCacheRequestCount(cacheStats.getFirstCacheRequestCount() + firstCacheStats.getAndResetCacheRequestCount());
                                cacheStats.setFirstCacheMissCount(cacheStats.getFirstCacheMissCount() + firstCacheStats.getAndResetCachedMethodRequestCount());

                                cacheStats.setSecondCacheRequestCount(cacheStats.getSecondCacheRequestCount() + secondCacheStats.getAndResetCacheRequestCount());
                                cacheStats.setSecondCacheMissCount(cacheStats.getSecondCacheMissCount() + secondCacheStats.getAndResetCachedMethodRequestCount());

                                // 将缓存统计数据写到redis
                                redisTemplate.opsForValue().set(redisKey, cacheStats, 24, TimeUnit.HOURS);

                                logger.info("Layering Cache 统计信息：{}", JsonUtils.toJson(cacheStats));
                            }
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        } finally {
                            lock.unlock();
                        }
                    }
                }
            }

            //  初始时间间隔是1分
        }, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * 关闭线程池
     */
    public void shutdownExecutor() {
        executorService.shutdown();
    }

    /**
     * 重置缓存统计数据
     */
    public void resetCacheStat() {
        RedisTemplate<String, Object> redisTemplate = cacheManager.getRedisTemplate();
        Set<String> layeringCacheKeys = RedisHelper.scan(redisTemplate, CACHE_STATS_KEY_PREFIX + "*");

        for (String key : layeringCacheKeys) {
            if(key.endsWith("_lock")) continue;
            resetCacheStat(key);
        }
    }

    /**
     * 重置缓存统计数据
     *
     * @param redisKey redisKey
     */
    public void resetCacheStat(String redisKey) {
        RedisTemplate<String, Object> redisTemplate = cacheManager.getRedisTemplate();
        CacheStatsInfo cacheStats = (CacheStatsInfo) redisTemplate.opsForValue().get(redisKey);
        if (Objects.nonNull(cacheStats)) {
            cacheStats.clearStatsInfo();
            // 将缓存统计数据写到redis
            redisTemplate.opsForValue().set(redisKey, cacheStats, 24, TimeUnit.HOURS);
        }
    }

    public void setCacheManager(AbstractCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
}
