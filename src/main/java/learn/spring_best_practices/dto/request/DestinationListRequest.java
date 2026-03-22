package learn.spring_best_practices.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class DestinationListRequest {

    @NotNull(message = "dateFrom is required")
    private LocalDate dateFrom;

    @NotNull(message = "dateTo is required")
    private LocalDate dateTo;
}
