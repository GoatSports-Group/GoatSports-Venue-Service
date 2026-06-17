package iuh.fit.venueservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class SecurityHeaderFilter extends OncePerRequestFilter {

    private final String sharedSecret;

    public SecurityHeaderFilter(String sharedSecret) {
        this.sharedSecret = sharedSecret;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String actuatorPath = contextPath + "/actuator";

        return requestUri.equals(actuatorPath) || requestUri.startsWith(actuatorPath + "/");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain
    ) throws ServletException, IOException
    {
        String gatewaySecret = request.getHeader("X-Gateway-Secret");
        if (gatewaySecret == null || !gatewaySecret.equals(sharedSecret)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"Truy cập trực tiếp bị cấm. Phải đi qua API Gateway.\"}");
            return;
        }

        String userId = request.getHeader("X-User-Id");
        String username = request.getHeader("X-User-Username");
        String rolesHeader = request.getHeader("X-User-Roles");

        if (userId != null && rolesHeader != null && !userId.isEmpty() && !rolesHeader.isEmpty()) {
            List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesHeader.split(","))
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.trim()))
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    username != null ? username : userId, null, authorities);

            authentication.setDetails(userId);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
