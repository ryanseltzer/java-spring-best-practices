package learn.spring_best_practices.service.helpers;

import learn.spring_best_practices.dto.request.DestinationRequest;
import learn.spring_best_practices.entity.DestinationId;
import learn.spring_best_practices.exception.AppErrorCode;
import learn.spring_best_practices.exception.AppException;

import java.time.LocalDate;

public final class DestinationServiceHelper {

    private DestinationServiceHelper() {}

    /** Same-day trips (dateFrom == dateTo) are valid; only rejects dateFrom > dateTo. */
    public static void validateDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom.isAfter(dateTo)) {
            throw new AppException(AppErrorCode.INVALID_DATE_RANGE);
        }
    }

    public static DestinationId buildId(DestinationRequest request) {
        return new DestinationId(request.getCountryName().trim(), request.getCityName().trim());
    }
}
