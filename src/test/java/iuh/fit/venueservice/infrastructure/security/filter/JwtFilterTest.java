package iuh.fit.venueservice.infrastructure.security.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtFilterTest {

    @Test
    void gatewayRoleHeaderBecomesSpringRoleAuthorityAndContextIsCleared() throws Exception {
        JwtFilter filter = new JwtFilter();
        ReflectionTestUtils.setField(filter, "gatewaySecret", "test-secret");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/owner/venues");
        request.addHeader("X-Gateway-Secret", "test-secret");
        request.addHeader("X-User-Id", "43ea6795-55ca-4ea8-8c43-150c418cff24");
        request.addHeader("X-User-Email", "owner@goatsports.vn");
        request.addHeader("X-User-Roles", "VENUE_OWNER");
        request.addHeader("X-User-Authorities", "GET_OWN_VENUES,UPDATE_OWN_VENUE");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Authentication> captured = new AtomicReference<>();
        FilterChain chain = (servletRequest, servletResponse) ->
                captured.set(SecurityContextHolder.getContext().getAuthentication());

        filter.doFilter(request, response, chain);

        assertNotNull(captured.get());
        assertTrue(captured.get().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_VENUE_OWNER")));
        assertTrue(captured.get().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("UPDATE_OWN_VENUE")));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
