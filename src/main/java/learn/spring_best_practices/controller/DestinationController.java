package learn.spring_best_practices.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import learn.spring_best_practices.dto.request.DestinationRequest;
import learn.spring_best_practices.dto.response.DestinationResponse;
import learn.spring_best_practices.entity.DestinationId;
import learn.spring_best_practices.service.DestinationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


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
        DestinationResponse response = destinationService.removeDestination(new DestinationId(countryName, cityName));
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<DestinationResponse> verifyDestination(@Valid @RequestBody DestinationRequest request) {
        log.debug("verifyDestination called by authenticated principal");
        DestinationResponse response = destinationService.verifyDestination(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
}
