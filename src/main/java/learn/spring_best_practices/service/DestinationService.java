package learn.spring_best_practices.service;


import learn.spring_best_practices.dto.request.DestinationListRequest;
import learn.spring_best_practices.dto.request.DestinationRequest;
import learn.spring_best_practices.dto.request.RemoveDestinationRequest;
import learn.spring_best_practices.dto.response.DestinationListResponse;
import learn.spring_best_practices.dto.response.DestinationResponse;

public interface DestinationService {

    DestinationResponse addDestination(DestinationRequest request);

    DestinationResponse removeDestination(RemoveDestinationRequest request);

    DestinationResponse verifyDestination(DestinationRequest request);

    DestinationListResponse listDestinations(DestinationListRequest request);

    DestinationResponse updateDestination(DestinationRequest request);
}
