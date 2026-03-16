package learn.spring_best_practices.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock JwtUtil jwtUtil;
    @Mock FilterChain filterChain;
    @InjectMocks JwtAuthenticationFilter filter;

    MockHttpServletRequest request;
    MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request  = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── Missing / malformed header ────────────────────────────────────────

    @Test
    void noAuthorizationHeader_passesThrough_noAuthSet() throws Exception {
        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void nonBearerHeader_passesThrough_noAuthSet() throws Exception {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtil);
    }

    // ── Invalid token ─────────────────────────────────────────────────────

    @Test
    void invalidToken_sends401_filterChainNotCalled() throws Exception {
        request.addHeader("Authorization", "Bearer bad.token.here");
        when(jwtUtil.isTokenValid("bad.token.here")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(any(), any());
    }

    // ── Valid token paths ─────────────────────────────────────────────────

    @Test
    void validToken_withSubject_setsAuthenticationAndContinues() throws Exception {
        request.addHeader("Authorization", "Bearer good.token");
        when(jwtUtil.isTokenValid("good.token")).thenReturn(true);
        when(jwtUtil.extractSubject("good.token")).thenReturn("alice");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("alice");
    }

    @Test
    void validToken_nullSubject_doesNotSetAuthentication_butContinues() throws Exception {
        request.addHeader("Authorization", "Bearer null.subject.token");
        when(jwtUtil.isTokenValid("null.subject.token")).thenReturn(true);
        when(jwtUtil.extractSubject("null.subject.token")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void validToken_alreadyAuthenticated_doesNotOverwriteExistingAuth() throws Exception {
        UsernamePasswordAuthenticationToken existing =
                new UsernamePasswordAuthenticationToken("existing", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(existing);

        request.addHeader("Authorization", "Bearer good.token");
        when(jwtUtil.isTokenValid("good.token")).thenReturn(true);
        when(jwtUtil.extractSubject("good.token")).thenReturn("newcomer");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("existing");
    }
}
