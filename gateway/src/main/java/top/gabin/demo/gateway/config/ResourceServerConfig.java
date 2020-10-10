package top.gabin.demo.gateway.config;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;
import top.gabin.demo.gateway.authorization.AuthorizationManager;
import top.gabin.demo.gateway.component.RestAuthenticationEntryPoint;
import top.gabin.demo.gateway.component.RestfulAccessDeniedHandler;
import top.gabin.demo.gateway.constant.AuthConstant;

import java.net.HttpURLConnection;
import java.util.List;

@AllArgsConstructor
@Configuration
@EnableWebFluxSecurity
public class ResourceServerConfig {
    private final AuthorizationManager authorizationManager;
    private final IgnoreUrlsConfig ignoreUrlsConfig;
    private final RestfulAccessDeniedHandler restfulAccessDeniedHandler;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http.oauth2ResourceServer().jwt()
                .jwtAuthenticationConverter(jwtAuthenticationConverter());
        List<String> urls = ignoreUrlsConfig.getUrls();
        http.authorizeExchange()
                .pathMatchers(urls.toArray(new String[urls.size()])).permitAll()//白名单配置
                .anyExchange().access(authorizationManager)//鉴权管理器配置
                .and().exceptionHandling()
                .accessDeniedHandler(restfulAccessDeniedHandler)//处理未授权
                .authenticationEntryPoint(restAuthenticationEntryPoint)//处理未认证
                .and().csrf().disable();
        return http.build();
    }

    @Bean
    public Converter<Jwt, ? extends Mono<? extends AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix(AuthConstant.AUTHORITY_PREFIX);
        jwtGrantedAuthoritiesConverter.setAuthoritiesClaimName(AuthConstant.AUTHORITY_CLAIM_NAME);
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
    }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        // 比较尴尬的是，这里要先知道授权服务器地址
//        return new NimbusReactiveJwtDecoder("http://localhost:9400/rsa/publicKey");

        // 做延迟加载，缺点是服务中心和网关都要先起来，这期间如果先调用就完蛋蛋了，不过怎么说呢，这个其实也有改进的空间，总之是个思路
        // 注意，我这边用的地址是网关转发的地址，理论上是有负载均衡的东西在里面，和上面那个9400不一样，接口都有了，自己不懂得扩展，那没办法，
        // 总不可能别人什么都帮你写好了
        return new ReactiveJwtDecoder() {
            private volatile NimbusReactiveJwtDecoder nimbusReactiveJwtDecoder;
            @Override
            public Mono<Jwt> decode(String token) throws JwtException {
                NimbusReactiveJwtDecoder decoder = getNimbusReactiveJwtDecoder();
                return decoder.decode(token);
            }

            private NimbusReactiveJwtDecoder getNimbusReactiveJwtDecoder() {
                if (nimbusReactiveJwtDecoder == null) {
                    synchronized (this) {
                        if (nimbusReactiveJwtDecoder == null) {
                            // 这里做延迟加载
                            nimbusReactiveJwtDecoder = new NimbusReactiveJwtDecoder("http://localhost:9080/auth/rsa/publicKey");
                        }
                    }
                }
                return nimbusReactiveJwtDecoder;
            }
        };
    }

}

