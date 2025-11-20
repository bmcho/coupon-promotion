package com.bmcho.userservice.service;

import com.bmcho.userservice.config.properties.JwtProperties;
import com.bmcho.userservice.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class JWTService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;

    public String generateToken(User user) {
        long currentTimeMillis = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", "USER")
                .issuedAt(new Date(currentTimeMillis))
                .expiration(new Date(currentTimeMillis + 3600000)) // Token expires in 1 hour
                .signWith(getPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }

    public Claims validateToken(String token) {
        try {
            return parseJwtClaims(token);
        } catch (Exception e) {
            log.error("Token validation error: ", e);
            throw new IllegalArgumentException("Invalid token");
        }
    }

    public String refreshToken(String token) {
        Claims claims = parseJwtClaims(token);
        long currentTimeMillis = System.currentTimeMillis();
        return Jwts.builder()
                .subject(claims.getSubject())
                .claims(claims)
                .issuedAt(new Date(currentTimeMillis))
                .expiration(new Date(currentTimeMillis + 3600000))
                .signWith(getPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }

    private Claims parseJwtClaims(String token) {
        return Jwts.parser()
                .verifyWith(getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private PrivateKey getPrivateKey() {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.getPrivateKey());
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(spec);
        } catch (Exception e) {
            throw new RuntimeException("Invalid private key", e);
        }
    }

    private PublicKey getPublicKey() {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.getPublicKey());
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(spec);
        } catch (Exception e) {
            throw new RuntimeException("Invalid public key", e);
        }
    }
}
