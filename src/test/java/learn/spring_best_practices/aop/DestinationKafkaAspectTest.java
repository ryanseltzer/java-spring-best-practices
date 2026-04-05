package learn.spring_best_practices.aop;

import learn.spring_best_practices.dto.event.DestinationEvent;
import learn.spring_best_practices.dto.event.EventType;
import learn.spring_best_practices.dto.response.DestinationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DestinationKafkaAspectTest {

    @Mock
    KafkaTemplate<String, DestinationEvent> kafkaTemplate;

    @InjectMocks
    DestinationKafkaAspect aspect;

    private static final String TOPIC = "VacationEvent";
    private static final LocalDate DATE_FROM = LocalDate.of(2025, 6, 1);
    private static final LocalDate DATE_TO = LocalDate.of(2025, 6, 15);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aspect, "destinationEventsTopic", TOPIC);
    }

    @Test
    void onAddDestination_publishesInsertEventWithCorrectFields() {
        DestinationResponse response = buildResponse("France", "Paris");
        when(kafkaTemplate.send(anyString(), anyString(), any(DestinationEvent.class)))
                .thenReturn(successfulSend());

        aspect.onAddDestination(response);

        ArgumentCaptor<DestinationEvent> captor = ArgumentCaptor.forClass(DestinationEvent.class);
        verify(kafkaTemplate).send(eq(TOPIC), eq("France:Paris"), captor.capture());
        DestinationEvent event = captor.getValue();
        assertThat(event.eventType()).isEqualTo(EventType.INSERT);
        assertThat(event.countryName()).isEqualTo("France");
        assertThat(event.cityName()).isEqualTo("Paris");
        assertThat(event.dateFrom()).isEqualTo(DATE_FROM);
        assertThat(event.dateTo()).isEqualTo(DATE_TO);
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    void onUpdateDestination_publishesUpdateEventWithCorrectFields() {
        DestinationResponse response = buildResponse("Germany", "Berlin");
        when(kafkaTemplate.send(anyString(), anyString(), any(DestinationEvent.class)))
                .thenReturn(successfulSend());

        aspect.onUpdateDestination(response);

        ArgumentCaptor<DestinationEvent> captor = ArgumentCaptor.forClass(DestinationEvent.class);
        verify(kafkaTemplate).send(eq(TOPIC), eq("Germany:Berlin"), captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo(EventType.UPDATE);
    }

    @Test
    void onAddDestination_usesCountryCityAsMessageKey() {
        DestinationResponse response = buildResponse("Japan", "Tokyo");
        when(kafkaTemplate.send(anyString(), anyString(), any(DestinationEvent.class)))
                .thenReturn(successfulSend());

        aspect.onAddDestination(response);

        verify(kafkaTemplate).send(eq(TOPIC), eq("Japan:Tokyo"), any(DestinationEvent.class));
    }

    @Test
    void onAddDestination_kafkaSendFails_doesNotThrow() {
        DestinationResponse response = buildResponse("France", "Paris");
        CompletableFuture<SendResult<String, DestinationEvent>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("broker unavailable"));
        when(kafkaTemplate.send(anyString(), anyString(), any(DestinationEvent.class)))
                .thenReturn(failedFuture);

        // failure is handled asynchronously in whenComplete — must not propagate to caller
        aspect.onAddDestination(response);

        verify(kafkaTemplate).send(eq(TOPIC), eq("France:Paris"), any(DestinationEvent.class));
    }

    @Test
    void onUpdateDestination_kafkaSendFails_doesNotThrow() {
        DestinationResponse response = buildResponse("Germany", "Berlin");
        CompletableFuture<SendResult<String, DestinationEvent>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("broker unavailable"));
        when(kafkaTemplate.send(anyString(), anyString(), any(DestinationEvent.class)))
                .thenReturn(failedFuture);

        aspect.onUpdateDestination(response);

        verify(kafkaTemplate).send(eq(TOPIC), eq("Germany:Berlin"), any(DestinationEvent.class));
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private DestinationResponse buildResponse(String country, String city) {
        return DestinationResponse.builder()
                .countryName(country)
                .cityName(city)
                .dateFrom(DATE_FROM)
                .dateTo(DATE_TO)
                .build();
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<SendResult<String, DestinationEvent>> successfulSend() {
        SendResult<String, DestinationEvent> sendResult = mock(SendResult.class);
        return CompletableFuture.completedFuture(sendResult);
    }
}
