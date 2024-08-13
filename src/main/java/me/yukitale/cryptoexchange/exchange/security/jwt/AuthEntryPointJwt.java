package me.yukitale.cryptoexchange.exchange.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthEntryPointJwt.class);

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
    //LOGGER.error("Unauthorized error: {}", request.getRequestURI() + ", " + authException.getMessage());

    if (authException instanceof BadCredentialsException) {
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

      Map<String, Object> body = new HashMap<>();
      body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
      body.put("error", "Unauthorized");
      body.put("message", authException.getMessage());
      body.put("path", request.getServletPath());

      ObjectMapper mapper = new ObjectMapper();
      mapper.writeValue(response.getOutputStream(), body);
      return;
    }

    response.sendRedirect("/signin");
  }
}
