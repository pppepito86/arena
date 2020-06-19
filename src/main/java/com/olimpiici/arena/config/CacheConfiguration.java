package com.olimpiici.arena.config;

import java.time.Duration;

import com.olimpiici.arena.service.CompetitionService;
import org.ehcache.config.builders.*;
import org.ehcache.jsr107.Eh107Configuration;

import io.github.jhipster.config.jcache.BeanClassLoaderAwareJCacheRegionFactory;
import io.github.jhipster.config.JHipsterProperties;

import org.springframework.boot.autoconfigure.cache.JCacheManagerCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.*;

@Configuration
@EnableCaching
public class CacheConfiguration {

    private final javax.cache.configuration.Configuration<Object, Object> jcacheConfiguration;

    private final javax.cache.configuration.Configuration<Object, Object> slowServiceConfiguration;

    public CacheConfiguration(JHipsterProperties jHipsterProperties) {
        BeanClassLoaderAwareJCacheRegionFactory.setBeanClassLoader(this.getClass().getClassLoader());
        JHipsterProperties.Cache.Ehcache ehcache =
            jHipsterProperties.getCache().getEhcache();

        jcacheConfiguration = Eh107Configuration.fromEhcacheCacheConfiguration(
            CacheConfigurationBuilder.newCacheConfigurationBuilder(Object.class, Object.class,
                ResourcePoolsBuilder.heap(ehcache.getMaxEntries()))
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(ehcache.getTimeToLiveSeconds())))
                .build());

        slowServiceConfiguration = Eh107Configuration.fromEhcacheCacheConfiguration(
            CacheConfigurationBuilder.newCacheConfigurationBuilder(Object.class, Object.class,
                ResourcePoolsBuilder.heap(ehcache.getMaxEntries()))
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMinutes(5)))
                .build());
    }

    @Bean
    public JCacheManagerCustomizer cacheManagerCustomizer() {
        return cm -> {
            cm.createCache(com.olimpiici.arena.repository.UserRepository.USERS_BY_LOGIN_CACHE, jcacheConfiguration);
            cm.createCache(com.olimpiici.arena.repository.UserRepository.USERS_BY_EMAIL_CACHE, jcacheConfiguration);
            cm.createCache(com.olimpiici.arena.domain.User.class.getName(), jcacheConfiguration);
            cm.createCache(com.olimpiici.arena.domain.Authority.class.getName(), jcacheConfiguration);
            cm.createCache(com.olimpiici.arena.domain.User.class.getName() + ".authorities", jcacheConfiguration);
            cm.createCache(com.olimpiici.arena.domain.Competition.class.getName(), jcacheConfiguration);
            cm.createCache(com.olimpiici.arena.domain.Problem.class.getName(), jcacheConfiguration);
            cm.createCache(com.olimpiici.arena.domain.CompetitionProblem.class.getName(), jcacheConfiguration);
            cm.createCache(com.olimpiici.arena.domain.Submission.class.getName(), jcacheConfiguration);
            cm.createCache(com.olimpiici.arena.domain.TagCollection.class.getName(), jcacheConfiguration);
            cm.createCache(com.olimpiici.arena.domain.Tag.class.getName(), jcacheConfiguration);
            cm.createCache(com.olimpiici.arena.domain.TagCollectionTag.class.getName(), jcacheConfiguration);

            cm.createCache(com.olimpiici.arena.service.CompetitionService.USER_POINTS_CACHE, slowServiceConfiguration);
            // jhipster-needle-ehcache-add-entry
        };
    }
}
