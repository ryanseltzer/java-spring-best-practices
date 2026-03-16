package learn.spring_best_practices.service;

import learn.spring_best_practices.dto.request.DestinationRequest;
import learn.spring_best_practices.dto.response.DestinationResponse;

public interface DestinationService {

    DestinationResponse addDestination(DestinationRequest request);
}
