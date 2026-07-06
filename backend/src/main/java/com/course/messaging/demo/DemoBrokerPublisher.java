package com.course.messaging.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.demo.broker", name = "enabled", havingValue = "true")
public class DemoBrokerPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishPing(String payload) {
        rabbitTemplate.convertAndSend(DemoBrokerConfig.QUEUE_NAME, payload);
    }
}
