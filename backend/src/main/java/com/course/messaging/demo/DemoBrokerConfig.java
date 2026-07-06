package com.course.messaging.demo;

import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.demo.broker", name = "enabled", havingValue = "true")
public class DemoBrokerConfig {

    public static final String QUEUE_NAME = "demo.ping";

    @Bean
    public Queue demoPingQueue() {
        return new Queue(QUEUE_NAME, true);
    }
}
