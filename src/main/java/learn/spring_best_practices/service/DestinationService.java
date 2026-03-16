package learn.spring_best_practices.service;


import learn.spring_best_practices.dto.request.DestinationRequest;
import learn.spring_best_practices.dto.response.DestinationResponse;
import learn.spring_best_practices.entity.DestinationId;

public interface DestinationService {

    DestinationResponse addDestination(DestinationRequest request);

    DestinationResponse removeDestination(DestinationId id);

    DestinationResponse verifyDestination(DestinationRequest request);
}
