package learn.spring_best_practices.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DestinationId implements Serializable {

    @Column(name = "country_name", nullable = false, length = 100)
    private String countryName;

    @Column(name = "city_name", nullable = false, length = 100)
    private String cityName;
}
