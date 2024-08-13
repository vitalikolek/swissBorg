package me.yukitale.cryptoexchange.exchange.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;
import me.yukitale.cryptoexchange.exchange.security.service.UserDetailsImpl;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {
  private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

  private final Key jwtSecret = Keys.secretKeyFor(SignatureAlgorithm.HS512);

  @Value("${yukitale.app.jwtExpirationMs}")
  private long jwtExpirationMs;

  @Value("${yukitale.app.jwtCookieName}")
  @Getter
  private String jwtCookie;

  public String getJwtFromCookies(HttpServletRequest request) {
    Cookie cookie = WebUtils.getCookie(request, jwtCookie);
    if (cookie != null) {
      return cookie.getValue();
    } else {
      return null;
    }
  }

  public ResponseCookie generateJwtCookie(UserDetailsImpl userPrincipal) {
    String jwt = generateTokenFromEmail(userPrincipal.getEmail());
    return ResponseCookie.from(jwtCookie, jwt).path("/").maxAge(24 * 60 * 60).httpOnly(true).build();
  }

  public ResponseCookie getCleanJwtCookie() {
    return ResponseCookie.from(jwtCookie, null).path("/").build();
  }

  public String getEmailFromJwtToken(String token) {
    return extractClaims(token).getBody().getSubject();
  }

  public boolean validateJwtToken(String authToken) {
    try {
      extractClaims(authToken);
      return true;
    } catch (Exception ignored) {}
    /*} catch (SignatureException e) {
      logger.error("Invalid JWT signature: {}", e.getMessage());
    } catch (MalformedJwtException e) {
      logger.error("Invalid JWT token: {}", e.getMessage());
    } catch (ExpiredJwtException e) {
      logger.error("JWT token is expired: {}", e.getMessage());
    } catch (UnsupportedJwtException e) {
      logger.error("JWT token is unsupported: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
      logger.error("JWT claims string is empty: {}", e.getMessage());
    }*/

    return false;
  }

  private Jws<Claims> extractClaims(String token) {
    return Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
  }

  public String generateTokenFromEmail(String email) {
    return Jwts.builder()
            .setSubject(email)
            .setIssuedAt(new Date())
            .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
            .signWith(jwtSecret)
            .compact();
  }


  public String generateTokenFromEmailAndPassword(String email, String password) {
    return Jwts.builder()
            .setSubject(email + ";" + password)
            .setIssuedAt(new Date())
            .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
            .signWith(jwtSecret)
            .compact();
  }

  public Pair<String, String> getEmailAndPasswordFromJwtToken(String token) {
    String subject = extractClaims(token).getBody().getSubject();
    return Pair.of(subject.split(";")[0], subject.split(";")[1]);
  }
}
