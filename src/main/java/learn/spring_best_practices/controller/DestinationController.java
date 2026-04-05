package learn.spring_best_practices.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import learn.spring_best_practices.dto.request.DestinationListRequest;
import learn.spring_best_practices.dto.request.DestinationRequest;
import learn.spring_best_practices.dto.request.RemoveDestinationRequest;
import learn.spring_best_practices.dto.response.DestinationListResponse;
import learn.spring_best_practices.dto.response.DestinationResponse;
import learn.spring_best_practices.service.DestinationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;



@RestController
@RequestMapping("/api/destinations")
@RequiredArgsConstructor
@Validated
@Slf4j
public class DestinationController {

    private final DestinationService destinationService;

    /**
     * POST /api/destinations
     *
     * Adds a new travel destination. Requires a valid Bearer JWT in the Authorization header.
     *
     * OWASP mitigations applied at this layer:
     *  A03 – @Valid triggers Bean Validation before the method body executes
     *  A08 – @RequestBody deserialises only the declared DTO fields (no mass-assignment)
     */
    @PostMapping("/add")
    ResponseEntity<DestinationResponse> addDestination(@Valid @RequestBody DestinationRequest request) {
        log.debug("addDestination called by authenticated principal");
        DestinationResponse response = destinationService.addDestination(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @DeleteMapping("/{countryName}/{cityName}")
    ResponseEntity<DestinationResponse> removeDestination(
            @PathVariable @Size(max = 100) @Pattern(regexp = "^[a-zA-Z '\\-]+$") String countryName,
            @PathVariable @Size(max = 100) @Pattern(regexp = "^[a-zA-Z '\\-]+$") String cityName) {
        log.debug("removeDestination called by authenticated principal");
        RemoveDestinationRequest request = new RemoveDestinationRequest(countryName, cityName);
        return ResponseEntity.ok(destinationService.removeDestination(request));
    }

    @PostMapping("/verify")
    public ResponseEntity<DestinationResponse> verifyDestination(@Valid @RequestBody DestinationRequest request) {
        log.debug("verifyDestination called by authenticated principal");
        DestinationResponse response = destinationService.verifyDestination(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * GET /api/destinations?dateFrom=YYYY-MM-DD&dateTo=YYYY-MM-DD
     *
     * Returns all destinations whose date range overlaps with the requested range.
     */
    @GetMapping("/list")
    public ResponseEntity<DestinationListResponse> listDestinations(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        log.debug("listDestinations called by authenticated principal");
        DestinationListRequest request = new DestinationListRequest();
        request.setDateFrom(dateFrom);
        request.setDateTo(dateTo);
        return ResponseEntity.ok(destinationService.listDestinations(request));
    }

    @PutMapping("/update")
    public ResponseEntity<DestinationResponse> updateDestination(@Valid @RequestBody DestinationRequest request) {
        log.debug("updateDestination called by authenticated principal");
        DestinationResponse response = destinationService.updateDestination(request);
        return ResponseEntity.ok(response);
    }
    
}
