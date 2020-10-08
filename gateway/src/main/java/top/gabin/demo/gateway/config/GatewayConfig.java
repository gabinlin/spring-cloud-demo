package top.gabin.demo.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class GatewayConfig {
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder, RateLimiter redisRateLimiter) {
        return builder.routes()
                .route("path_route", r -> r.path("/baidu")
                        // 映射请求到别的服务端口
//                        .filters(f -> f.circuitBreaker(config -> {
//                            config.setFallbackUri("http://localhost:9070");
//                        }))
                        // 靠，看源码这里要登录才有效果，也就是说这个限流过滤器是依赖授权的，可能可以开启匿名登录
                        .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(new RedisRateLimiter(1, 2))))
                        .uri("http://localhost:9070"))
//                .route("limit_route", r -> r
//                        .path("/**")
//                        // 限流
//                        .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter)))
//                        .uri("http://httpbin.org"))
                .build();
    }

//    @Bean
//    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
//        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
//                .circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
//                .timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(4)).build()).build());
//    }
}
