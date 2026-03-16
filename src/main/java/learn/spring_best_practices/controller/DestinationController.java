package learn.spring_best_practices.controller;

import jakarta.validation.Valid;
import learn.spring_best_practices.dto.request.DestinationRequest;
import learn.spring_best_practices.dto.response.DestinationResponse;
import learn.spring_best_practices.service.DestinationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/destinations")
@RequiredArgsConstructor
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
    @PostMapping
    ResponseEntity<DestinationResponse> addDestination(@Valid @RequestBody DestinationRequest request) {
        log.debug("addDestination called by authenticated principal");
        DestinationResponse response = destinationService.addDestination(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
