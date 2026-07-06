package com.course.messaging.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.demo.broker", name = "enabled", havingValue = "true")
public class DemoBrokerListener {

    @RabbitListener(queues = DemoBrokerConfig.QUEUE_NAME)
    public void onMessage(String payload) {
        log.info("[demo-broker] received message: {}", payload);
    }
}
