package learn.spring_best_practices.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RemoveDestinationRequest(

        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "^[a-zA-Z '\\-]+$")
        String countryName,

        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "^[a-zA-Z '\\-]+$")
        String cityName
) {}
