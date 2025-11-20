    package com.bmcho.apigateway.filter;

    import org.springframework.cloud.client.loadbalancer.LoadBalanced;
    import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
    import org.springframework.cloud.gateway.filter.GatewayFilter;
    import org.springframework.cloud.gateway.filter.GatewayFilterChain;
    import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
    import org.springframework.http.HttpStatus;
    import org.springframework.stereotype.Component;
    import org.springframework.web.reactive.function.client.WebClient;
    import org.springframework.web.server.ServerWebExchange;
    import reactor.core.publisher.Mono;

    import java.util.Map;

    @Component
    public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

        @LoadBalanced
        private final WebClient webClient;

        public JwtAuthenticationFilter(ReactorLoadBalancerExchangeFilterFunction lbFunction) {
            super(Config.class);
            this.webClient = WebClient.builder()
                    .filter(lbFunction)
                    .baseUrl("http://user-service")
                    .build();
        }

        @Override
        public GatewayFilter apply(Config config) {
            return (exchange, chain) -> {
                String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

                // 1. Authorization 헤더 없거나 Bearer 형식 아니면 바로 401
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    return handleAuthenticationError(exchange, new IllegalStateException("Missing or invalid Authorization header"));
                }

                String token = authHeader.substring(7);

                // 2. 토큰이 있으면 반드시 유효해야 함
                return validateToken(token)
                        .flatMap(userId -> proceedWithUserId(userId, exchange, chain))
                        .onErrorResume(e -> handleAuthenticationError(exchange, e)); // 모든 에러 → 401
            };
        }

        private Mono<Void> handleAuthenticationError(ServerWebExchange exchange, Throwable e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        private Mono<Long> validateToken(String token) {
            return webClient.post()
                    .uri("/auth/validate")
                    .bodyValue(Map.of("token", token))
                    .header("Content-Type", "application/json")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .map(response -> Long.valueOf(response.get("id").toString()));
        }

        private Mono<Void> proceedWithUserId(Long userId, ServerWebExchange exchange, GatewayFilterChain chain) {
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(builder -> builder.header("X-USER-ID", String.valueOf(userId)))
                    .build();
            return chain.filter(mutatedExchange);
        }

        public static class Config {
            // 필터 구성을 위한 설정 클래스
        }
    }