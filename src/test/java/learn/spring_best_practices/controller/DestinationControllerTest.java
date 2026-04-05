package learn.spring_best_practices.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import learn.spring_best_practices.dto.response.DestinationListResponse;
import learn.spring_best_practices.dto.response.DestinationResponse;
import learn.spring_best_practices.exception.AppErrorCode;
import learn.spring_best_practices.exception.AppException;
import learn.spring_best_practices.service.DestinationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    private static final String BASE_URL   = "/api/destinations";
    private static final String ADD_URL    = BASE_URL + "/add";
    private static final String VERIFY_URL = BASE_URL + "/verify";
    private static final String LIST_URL   = BASE_URL + "/list";
    private static final String UPDATE_URL = BASE_URL + "/update";

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

    // ── addDestination: 201 Created ───────────────────────────────────────

    @Test
    void addDestination_validRequest_returns201WithBody() throws Exception {
        DestinationResponse stub = DestinationResponse.builder()
                .countryName("United Kingdom").cityName("London")
                .dateFrom(LocalDate.of(2026, 6, 1)).dateTo(LocalDate.of(2026, 12, 1))
                .build();
        when(destinationService.addDestination(any())).thenReturn(stub);

        mockMvc.perform(post(ADD_URL)
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

        mockMvc.perform(post(ADD_URL)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SAME_DAY_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dateFrom").value("2026-06-01"))
                .andExpect(jsonPath("$.dateTo").value("2026-06-01"));
    }

    // ── addDestination: 400 Validation ────────────────────────────────────

    @Test
    void addDestination_blankCountry_returns400() throws Exception {
        String body = """
                {"countryName":"","cityName":"London","dateFrom":"2026-06-01","dateTo":"2026-12-01"}
                """;

        mockMvc.perform(post(ADD_URL)
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

        mockMvc.perform(post(ADD_URL)
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

        mockMvc.perform(post(ADD_URL)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.appErrorCode").value(AppErrorCode.VALIDATION_FAILED.getAppCode()));
    }

    // ── addDestination: 401 Unauthenticated ───────────────────────────────

    @Test
    void addDestination_noToken_returns401() throws Exception {
        mockMvc.perform(post(ADD_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    // ── addDestination: 409 Conflict ──────────────────────────────────────

    @Test
    void addDestination_duplicate_returns409WithAppCode418() throws Exception {
        when(destinationService.addDestination(any()))
                .thenThrow(new AppException(AppErrorCode.DUPLICATE_DESTINATION));

        mockMvc.perform(post(ADD_URL)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.appErrorCode").value(418));
    }

    // ── addDestination: 422 Domain errors ─────────────────────────────────

    @Test
    void addDestination_invalidCountry_returns422WithAppCode415() throws Exception {
        when(destinationService.addDestination(any()))
                .thenThrow(new AppException(AppErrorCode.INVALID_COUNTRY));

        mockMvc.perform(post(ADD_URL)
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

        mockMvc.perform(post(ADD_URL)
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

        mockMvc.perform(post(ADD_URL)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.appErrorCode").value(417));
    }

    // ── verifyDestination: 200 OK ─────────────────────────────────────────

    @Test
    void verifyDestination_validRequest_returns200WithBody() throws Exception {
        DestinationResponse stub = DestinationResponse.builder()
                .countryName("United Kingdom").cityName("London")
                .dateFrom(LocalDate.of(2026, 6, 1)).dateTo(LocalDate.of(2026, 12, 1))
                .build();
        when(destinationService.verifyDestination(any())).thenReturn(stub);

        mockMvc.perform(post(VERIFY_URL)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryName").value("United Kingdom"))
                .andExpect(jsonPath("$.cityName").value("London"));
    }

    // ── verifyDestination: 400 Validation ─────────────────────────────────

    @Test
    void verifyDestination_blankCity_returns400() throws Exception {
        String body = """
                {"countryName":"United Kingdom","cityName":"","dateFrom":"2026-06-01","dateTo":"2026-12-01"}
                """;

        mockMvc.perform(post(VERIFY_URL)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.appErrorCode").value(AppErrorCode.VALIDATION_FAILED.getAppCode()));
    }

    @Test
    void verifyDestination_injectionInCityName_returns400() throws Exception {
        String body = """
                {"countryName":"United Kingdom","cityName":"<img src=x onerror=alert(1)>",
                 "dateFrom":"2026-06-01","dateTo":"2026-12-01"}
                """;

        mockMvc.perform(post(VERIFY_URL)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.appErrorCode").value(AppErrorCode.VALIDATION_FAILED.getAppCode()));
    }

    // ── verifyDestination: 401 Unauthenticated ────────────────────────────

    @Test
    void verifyDestination_noToken_returns401() throws Exception {
        mockMvc.perform(post(VERIFY_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    // ── verifyDestination: 422 Domain errors ──────────────────────────────

    @Test
    void verifyDestination_invalidCountry_returns422() throws Exception {
        when(destinationService.verifyDestination(any()))
                .thenThrow(new AppException(AppErrorCode.INVALID_COUNTRY));

        mockMvc.perform(post(VERIFY_URL)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.appErrorCode").value(415));
    }

    @Test
    void verifyDestination_invalidCity_returns422() throws Exception {
        when(destinationService.verifyDestination(any()))
                .thenThrow(new AppException(AppErrorCode.INVALID_CITY));

        mockMvc.perform(post(VERIFY_URL)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.appErrorCode").value(416));
    }

    // ── removeDestination: 200 OK ─────────────────────────────────────────

    @Test
    void removeDestination_found_returns200WithBody() throws Exception {
        DestinationResponse stub = DestinationResponse.builder()
                .countryName("United Kingdom").cityName("London")
                .dateFrom(LocalDate.of(2026, 6, 1)).dateTo(LocalDate.of(2026, 12, 1))
                .build();
        when(destinationService.removeDestination(any())).thenReturn(stub);

        mockMvc.perform(delete(BASE_URL + "/United Kingdom/London")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryName").value("United Kingdom"))
                .andExpect(jsonPath("$.cityName").value("London"));
    }

    // ── removeDestination: 400 Path variable injection ────────────────────

    @Test
    void removeDestination_injectionInPath_returns400() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/<script>alert(1)<script>/London")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isBadRequest());
    }

    // ── removeDestination: 401 Unauthenticated ────────────────────────────

    @Test
    void removeDestination_noToken_returns401() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/United Kingdom/London"))
                .andExpect(status().isUnauthorized());
    }

    // ── removeDestination: 404 Not Found ──────────────────────────────────

    @Test
    void removeDestination_notFound_returns404() throws Exception {
        when(destinationService.removeDestination(any()))
                .thenThrow(new AppException(AppErrorCode.DESTINATION_NOT_FOUND));

        mockMvc.perform(delete(BASE_URL + "/United Kingdom/London")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.appErrorCode").value(419));
    }

    // ── listDestinations: 200 OK ──────────────────────────────────────────

    @Test
    void listDestinations_validRange_returns200WithBody() throws Exception {
        DestinationResponse item = DestinationResponse.builder()
                .countryName("United Kingdom").cityName("London")
                .dateFrom(LocalDate.of(2026, 6, 1)).dateTo(LocalDate.of(2026, 12, 1))
                .build();
        when(destinationService.listDestinations(any()))
                .thenReturn(DestinationListResponse.of(List.of(item)));

        mockMvc.perform(get(LIST_URL)
                        .header("Authorization", bearerToken())
                        .param("dateFrom", "2026-06-01")
                        .param("dateTo",   "2026-12-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.destinations[0].countryName").value("United Kingdom"))
                .andExpect(jsonPath("$.destinations[0].cityName").value("London"));
    }

    @Test
    void listDestinations_noResults_returns200WithEmptyList() throws Exception {
        when(destinationService.listDestinations(any()))
                .thenReturn(DestinationListResponse.of(List.of()));

        mockMvc.perform(get(LIST_URL)
                        .header("Authorization", bearerToken())
                        .param("dateFrom", "2026-06-01")
                        .param("dateTo",   "2026-12-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.destinations").isEmpty());
    }

    // ── listDestinations: 401 Unauthenticated ─────────────────────────────

    @Test
    void listDestinations_noToken_returns401() throws Exception {
        mockMvc.perform(get(LIST_URL)
                        .param("dateFrom", "2026-06-01")
                        .param("dateTo",   "2026-12-01"))
                .andExpect(status().isUnauthorized());
    }

    // ── listDestinations: 400 Missing params ──────────────────────────────

    @Test
    void listDestinations_missingDateFrom_returns400() throws Exception {
        mockMvc.perform(get(LIST_URL)
                        .header("Authorization", bearerToken())
                        .param("dateTo", "2026-12-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listDestinations_missingDateTo_returns400() throws Exception {
        mockMvc.perform(get(LIST_URL)
                        .header("Authorization", bearerToken())
                        .param("dateFrom", "2026-06-01"))
                .andExpect(status().isBadRequest());
    }

    // ── listDestinations: 422 Invalid date range ───────────────────────────

    @Test
    void listDestinations_dateFromAfterDateTo_returns422() throws Exception {
        when(destinationService.listDestinations(any()))
                .thenThrow(new AppException(AppErrorCode.INVALID_DATE_RANGE));

        mockMvc.perform(get(LIST_URL)
                        .header("Authorization", bearerToken())
                        .param("dateFrom", "2026-12-01")
                        .param("dateTo",   "2026-06-01"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.appErrorCode").value(417));
    }

    // ── updateDestination: 200 OK ─────────────────────────────────────────

    @Test
    void updateDestination_validRequest_returns200WithBody() throws Exception {
        DestinationResponse stub = DestinationResponse.builder()
                .countryName("United Kingdom").cityName("London")
                .dateFrom(LocalDate.of(2027, 1, 1)).dateTo(LocalDate.of(2027, 6, 1))
                .build();
        when(destinationService.updateDestination(any())).thenReturn(stub);

        mockMvc.perform(put(UPDATE_URL)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryName").value("United Kingdom"))
                .andExpect(jsonPath("$.cityName").value("London"))
                .andExpect(jsonPath("$.dateFrom").value("2027-01-01"))
                .andExpect(jsonPath("$.dateTo").value("2027-06-01"));
    }

    // ── updateDestination: 400 Validation ────────────────────────────────

    static Stream<String> invalidUpdateBodies() {
        return Stream.of(
                """
                {"countryName":"","cityName":"London","dateFrom":"2026-06-01","dateTo":"2026-12-01"}
                """,
                """
                {"countryName":"<script>xss</script>","cityName":"London","dateFrom":"2026-06-01","dateTo":"2026-12-01"}
                """,
                """
                {"countryName":"United Kingdom","cityName":"London"}
                """
        );
    }

    @ParameterizedTest
    @MethodSource("invalidUpdateBodies")
    void updateDestination_invalidBody_returns400(String body) throws Exception {
        mockMvc.perform(put(UPDATE_URL)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.appErrorCode").value(AppErrorCode.VALIDATION_FAILED.getAppCode()));
    }

    // ── updateDestination: 401 Unauthenticated ────────────────────────────

    @Test
    void updateDestination_noToken_returns401() throws Exception {
        mockMvc.perform(put(UPDATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    // ── updateDestination: 404 Not Found ──────────────────────────────────

    @Test
    void updateDestination_notFound_returns404WithAppCode419() throws Exception {
        when(destinationService.updateDestination(any()))
                .thenThrow(new AppException(AppErrorCode.DESTINATION_NOT_FOUND));

        mockMvc.perform(put(UPDATE_URL)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.appErrorCode").value(419));
    }

    // ── updateDestination: 422 Domain errors ──────────────────────────────

    @Test
    void updateDestination_invalidCountry_returns422WithAppCode415() throws Exception {
        when(destinationService.updateDestination(any()))
                .thenThrow(new AppException(AppErrorCode.INVALID_COUNTRY));

        mockMvc.perform(put(UPDATE_URL)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.appErrorCode").value(415));
    }

    @Test
    void updateDestination_invalidCity_returns422WithAppCode416() throws Exception {
        when(destinationService.updateDestination(any()))
                .thenThrow(new AppException(AppErrorCode.INVALID_CITY));

        mockMvc.perform(put(UPDATE_URL)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.appErrorCode").value(416));
    }

    @Test
    void updateDestination_invalidDateRange_returns422WithAppCode417() throws Exception {
        when(destinationService.updateDestination(any()))
                .thenThrow(new AppException(AppErrorCode.INVALID_DATE_RANGE));

        mockMvc.perform(put(UPDATE_URL)
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.appErrorCode").value(417));
    }
}
