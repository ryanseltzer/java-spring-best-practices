package learn.spring_best_practices.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import learn.spring_best_practices.dto.response.DestinationResponse;
import learn.spring_best_practices.exception.AppErrorCode;
import learn.spring_best_practices.exception.AppException;
import learn.spring_best_practices.service.DestinationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.crypto.SecretKey;
import java.time.LocalDate;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack controller test — loads the complete Spring context so the real
 * JWT filter chain is exercised.  Authenticated tests supply a properly signed
 * Bearer token; the 401 test omits it entirely.
 *
 * {@code springSecurity()} is applied to MockMvc so the full filter chain runs.
 * Real JWT tokens are used instead of {@code @WithMockUser} to validate the
 * security integration end-to-end.
 *
 * Tests run sequentially ({@code SAME_THREAD}) to prevent concurrent Mockito stub
 * overwrites on the shared {@code @MockitoBean}.
 */
@SpringBootTest
@Execution(ExecutionMode.SAME_THREAD)
class DestinationControllerTest {

    @Autowired WebApplicationContext context;
    @MockitoBean DestinationService destinationService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // ── JWT helpers ───────────────────────────────────────────────────────

    /** Must match application.yaml app.jwt.secret */
    private static final String SECRET =
            "dGhpcy1pcy1hLXZlcnktbG9uZy1zZWNyZXQta2V5LWZvci1qd3Qtc2lnbmluZw==";

    private static final String URL = "/api/destinations";

    private String bearerToken() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        String token = Jwts.builder()
                .subject("testuser")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key)
                .compact();
        return "Bearer " + token;
    }

    private static final String VALID_BODY = """
            {
              "countryName": "United Kingdom",
              "cityName":    "London",
              "dateFrom":    "2026-06-01",
              "dateTo":      "2026-12-01"
            }
            """;

    private static final String SAME_DAY_BODY = """
            {
              "countryName": "United Kingdom",
              "cityName":    "London",
              "dateFrom":    "2026-06-01",
              "dateTo":      "2026-06-01"
            }
            """;

    // ── 201 Created ───────────────────────────────────────────────────────

    @Test
    void addDestination_validRequest_returns201WithBody() throws Exception {
        DestinationResponse stub = DestinationResponse.builder()
                .countryName("United Kingdom").cityName("London")
                .dateFrom(LocalDate.of(2026, 6, 1)).dateTo(LocalDate.of(2026, 12, 1))
                .build();
        when(destinationService.addDestination(any())).thenReturn(stub);

        mockMvc.perform(post(URL)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.countryName").value("United Kingdom"))
                .andExpect(jsonPath("$.cityName").value("London"))
                .andExpect(jsonPath("$.dateFrom").value("2026-06-01"))
                .andExpect(jsonPath("$.dateTo").value("2026-12-01"));
    }

    @Test
    void addDestination_sameDayTrip_returns201() throws Exception {
        DestinationResponse stub = DestinationResponse.builder()
                .countryName("United Kingdom").cityName("London")
                .dateFrom(LocalDate.of(2026, 6, 1)).dateTo(LocalDate.of(2026, 6, 1))
                .build();
        when(destinationService.addDestination(any())).thenReturn(stub);

        mockMvc.perform(post(URL)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SAME_DAY_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dateFrom").value("2026-06-01"))
                .andExpect(jsonPath("$.dateTo").value("2026-06-01"));
    }

    // ── 400 Validation ────────────────────────────────────────────────────

    @Test
    void addDestination_blankCountry_returns400() throws Exception {
        String body = """
                {"countryName":"","cityName":"London","dateFrom":"2026-06-01","dateTo":"2026-12-01"}
                """;

        mockMvc.perform(post(URL)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.appErrorCode").value(AppErrorCode.VALIDATION_FAILED.getAppCode()))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void addDestination_injectionInCountryName_returns400() throws Exception {
        String body = """
                {"countryName":"<script>xss</script>","cityName":"London",
                 "dateFrom":"2026-06-01","dateTo":"2026-12-01"}
                """;

        mockMvc.perform(post(URL)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.appErrorCode").value(AppErrorCode.VALIDATION_FAILED.getAppCode()));
    }

    @Test
    void addDestination_missingDates_returns400() throws Exception {
        String body = """
                {"countryName":"United Kingdom","cityName":"London"}
                """;

        mockMvc.perform(post(URL)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.appErrorCode").value(AppErrorCode.VALIDATION_FAILED.getAppCode()));
    }

    // ── 401 Unauthenticated ───────────────────────────────────────────────

    @Test
    void addDestination_noToken_returns401() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    // ── 409 Conflict ──────────────────────────────────────────────────────

    @Test
    void addDestination_duplicate_returns409WithAppCode418() throws Exception {
        when(destinationService.addDestination(any()))
                .thenThrow(new AppException(AppErrorCode.DUPLICATE_DESTINATION));

        mockMvc.perform(post(URL)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.appErrorCode").value(418));
    }

    // ── 422 Domain errors ─────────────────────────────────────────────────

    @Test
    void addDestination_invalidCountry_returns422WithAppCode415() throws Exception {
        when(destinationService.addDestination(any()))
                .thenThrow(new AppException(AppErrorCode.INVALID_COUNTRY));

        mockMvc.perform(post(URL)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.appErrorCode").value(415))
                .andExpect(jsonPath("$.httpStatus").value(422));
    }

    @Test
    void addDestination_invalidCity_returns422WithAppCode416() throws Exception {
        when(destinationService.addDestination(any()))
                .thenThrow(new AppException(AppErrorCode.INVALID_CITY));

        mockMvc.perform(post(URL)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.appErrorCode").value(416));
    }

    @Test
    void addDestination_invalidDateRange_returns422WithAppCode417() throws Exception {
        when(destinationService.addDestination(any()))
                .thenThrow(new AppException(AppErrorCode.INVALID_DATE_RANGE));

        mockMvc.perform(post(URL)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.appErrorCode").value(417));
    }
}
