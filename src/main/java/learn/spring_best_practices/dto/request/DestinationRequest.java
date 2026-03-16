package learn.spring_best_practices.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class DestinationRequest {

    // A03: Pattern restricts to safe characters — prevents injection via country/city fields
    @NotBlank(message = "Country name is required")
    @Size(max = 100, message = "Country name must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z '\\-]+$", message = "Country name contains invalid characters")
    private String countryName;

    @NotBlank(message = "City name is required")
    @Size(max = 100, message = "City name must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z '\\-]+$", message = "City name contains invalid characters")
    private String cityName;

    @NotNull(message = "Date from is required")
    private LocalDate dateFrom;

    @NotNull(message = "Date to is required")
    private LocalDate dateTo;
}
