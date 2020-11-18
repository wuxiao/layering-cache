package com.github.xiaolyuh.cache.config;

import com.github.xiaolyuh.aspect.LayeringAspect;
import com.github.xiaolyuh.cache.properties.LayeringCacheProperties;
import com.github.xiaolyuh.manager.CacheManager;
import com.github.xiaolyuh.manager.LayeringCacheManager;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * 多级缓存自动配置类
 *
 * @author xiaolyuh
 */
@Configuration
@AutoConfigureAfter({RedisAutoConfiguration.class, RedissonAutoConfiguration.class})
@EnableAspectJAutoProxy
@EnableConfigurationProperties({LayeringCacheProperties.class})
@Import({LayeringCacheServletConfiguration.class})
public class LayeringCacheAutoConfig {

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager layeringCacheManager(StringObjectRedisTemplate stringObjectRedisTemplate, LayeringCacheProperties properties) {
        LayeringCacheManager layeringCacheManager = new LayeringCacheManager(stringObjectRedisTemplate);
        // 默认开启统计功能
        layeringCacheManager.setStats(properties.isStats());
        return layeringCacheManager;
    }

    @Bean
    public LayeringAspect layeringAspect() {
        return new LayeringAspect();
    }

    @Bean
    public StringObjectRedisTemplate stringObjectRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringObjectRedisTemplate(redisConnectionFactory);
    }
}
