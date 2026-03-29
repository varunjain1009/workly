package com.workly.modules.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@Slf4j
public class JwtUtils {

    @Value("${auth.token.secret}")
    private String secret;

    @Value("${auth.token.ttl-hours}")
    private int ttlHours;

    private Key key;

    @PostConstruct
    public void init() {
        log.debug("JwtUtils: Initializing HMAC key from secret (ttl={}h)", ttlHours);
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        log.debug("JwtUtils: Key initialized successfully");
    }

    public String extractMobileNumber(String token) {
        log.debug("JwtUtils: Extracting mobile number from token");
        String mobile = extractClaim(token, Claims::getSubject);
        log.debug("JwtUtils: Extracted mobile: {}", mobile);
        return mobile;
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        log.debug("JwtUtils: Parsing JWT claims");
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    public String generateToken(String mobileNumber) {
        log.debug("JwtUtils: [ENTER] generateToken - mobile: {}", mobileNumber);
        Map<String, Object> claims = new HashMap<>();
        String token = createToken(claims, mobileNumber);
        log.debug("JwtUtils: [EXIT] generateToken - token generated for {}", mobileNumber);
        return token;
    }

    private String createToken(Map<String, Object> claims, String subject) {
        long issuedAt = System.currentTimeMillis();
        long expiry = issuedAt + 1000L * 60 * 60 * ttlHours;
        log.debug("JwtUtils: createToken - subject: {}, expiresAt: {}", subject, new Date(expiry));
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(issuedAt))
                .setExpiration(new Date(expiry))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token, String mobileNumber) {
        log.debug("JwtUtils: [ENTER] validateToken - mobile: {}", mobileNumber);
        final String extractedMobile = extractMobileNumber(token);
        boolean expired = isTokenExpired(token);
        boolean valid = extractedMobile.equals(mobileNumber) && !expired;
        log.debug("JwtUtils: [EXIT] validateToken - mobile: {}, expired: {}, valid: {}", mobileNumber, expired, valid);
        return valid;
    }

    private Boolean isTokenExpired(String token) {
        Date expiry = extractClaim(token, Claims::getExpiration);
        boolean expired = expiry.before(new Date());
        log.debug("JwtUtils: isTokenExpired - expiry: {}, expired: {}", expiry, expired);
        return expired;
    }
}
