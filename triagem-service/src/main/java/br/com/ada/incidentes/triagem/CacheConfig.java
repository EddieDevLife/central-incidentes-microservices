package br.com.ada.incidentes.triagem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * As três perguntas do cache:
 *   O QUE guarda?        -> a regra de SLA de cada prioridade (chave "regras-sla::P1" etc.)
 *   POR QUANTO TEMPO?    -> 10 minutos (TTL), configurável no application.yml
 *   QUANDO APAGA?        -> quando a regra é alterada (PUT /regras-sla/{prioridade})
 *                           ou quando o TTL vence, o que vier primeiro.
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    public static final String CACHE_REGRAS_SLA = "regras-sla";

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration(
            @Value("${app.cache.regras-sla-ttl-minutos:10}") long ttlMinutos) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(ttlMinutos))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new Jackson2JsonRedisSerializer<>(RegraSlaDto.class)));
    }

    /**
     * Cache é atalho, não é a fonte da verdade. Se o Redis cair, o serviço
     * NÃO deve parar: registra o aviso e segue buscando direto no banco
     * (mais lento, mas funcionando).
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache fora do ar ao LER {}: {}. Buscando no banco.", key, e.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("Cache fora do ar ao GRAVAR {}: {}", key, e.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache fora do ar ao APAGAR {}: {}", key, e.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("Cache fora do ar ao LIMPAR: {}", e.getMessage());
            }
        };
    }
}
