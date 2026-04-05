package learn.spring_best_practices.aop;

import learn.spring_best_practices.dto.event.DestinationEvent;
import learn.spring_best_practices.dto.event.EventType;
import learn.spring_best_practices.dto.response.DestinationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class DestinationKafkaAspect {

    private final KafkaTemplate<String, DestinationEvent> kafkaTemplate;

    @Value("${app.kafka.topic.destination-events}")
    private String destinationEventsTopic;

    @AfterReturning(
            pointcut = "execution(* learn.spring_best_practices.service.impl.DestinationServiceImpl.addDestination(..))",
            returning = "result"
    )
    public void onAddDestination(Object result) {
        publishEvent((DestinationResponse) result, EventType.INSERT);
    }

    @AfterReturning(
            pointcut = "execution(* learn.spring_best_practices.service.impl.DestinationServiceImpl.updateDestination(..))",
            returning = "result"
    )
    public void onUpdateDestination(Object result) {
        publishEvent((DestinationResponse) result, EventType.UPDATE);
    }

    private void publishEvent(DestinationResponse response, EventType eventType) {
        DestinationEvent event = DestinationEvent.from(response, eventType);
        String key = response.countryName() + ":" + response.cityName();
        kafkaTemplate.send(destinationEventsTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("[Kafka] Failed to publish {} event for {}/{}: {}",
                                eventType, response.countryName(), response.cityName(), ex.getMessage());
                    } else {
                        log.debug("[Kafka] Published {} event for {}/{} → topic={} partition={} offset={}",
                                eventType, response.countryName(), response.cityName(),
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
