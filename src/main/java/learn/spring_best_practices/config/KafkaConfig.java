package learn.spring_best_practices.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${app.kafka.topic.destination-events}")
    private String destinationEventsTopic;

    @Bean
    public NewTopic destinationEventsTopic() {
        return TopicBuilder.name(destinationEventsTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
