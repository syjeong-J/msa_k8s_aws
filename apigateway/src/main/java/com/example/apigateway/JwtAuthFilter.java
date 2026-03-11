package com.example.apigateway;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthFilter implements GlobalFilter {
    
    @Value("${jwt.secret}") //키(서명) 생성, yaml에서 가져옴
    private String secret ;
    private Key key;

    // token 검증없이 통과하는 endpoint 등록
    private static final List<String> WHITE_LIST_PATHS = List.of(
        "/users/signIn",
        "/health/alive",
        "/product/list"
    );

    @PostConstruct // 클래스 객체 생성 후 컨테이너에 호출되서 초기화
    private void init() {
        System.out.println(">>>> JwtFilter init jwt secret : "+secret);
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        System.out.println(">>>> JwtFilter filter token validation ");

        String bearerToken = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        System.out.println(">>>> JwtFilter filter bearerToken : "+bearerToken);
        
        String endPoint = exchange.getRequest().getURI().getRawPath(); 
        System.out.println(">>> filter endpoint : "+endPoint);
        String method = exchange.getRequest().getMethod().name() ;
        System.out.println(">>>> JwtFilter Request Method : "+method);
        
        if(WHITE_LIST_PATHS.contains(endPoint)) {
            System.out.println(">>>> JwtFilter filter WHITE_LIST_PATH ");
            return chain.filter(exchange); 
        }

        try {

            System.out.println(">>>> JwtFilter Authorization : "+bearerToken);
            if( bearerToken == null || !bearerToken.startsWith("Bearer ")) {
                System.out.println(">>>> JwtFilter not Authorization : ");
                throw new RuntimeException("JwtFilter token exception");
            }
            String token = bearerToken.substring(7);
            System.out.println(">>>> JwtFilter token : "+token);

            Claims claims = Jwts.parserBuilder()
                                .setSigningKey(key)
                                .build()
                                .parseClaimsJws(token)
                                .getBody();

            String email = claims.getSubject() ;
            System.out.println(">>>> JwtFilter claims get email : "+email);

            String role = claims.get("role",String.class);
            System.out.println(">>>> JwtAuthenticationFilter claims get role : "+role);

            // X-User-Id 변수로 email 값과 Role 추가
            // X custom header 라는 것을 의미하는 관례
            ServerWebExchange modifyExchange = exchange.mutate()
                .request(builder -> builder
                            .header("X-User-Email", email)
                            .header("X-User-Role", "Role_"+role)
                        ).build() ;  

            return chain.filter(modifyExchange);

        } catch(Exception e) {
            e.printStackTrace();
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        
        
    }
}
