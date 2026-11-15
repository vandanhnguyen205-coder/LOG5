package com.logistics.coldchain.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${coldchain.rabbitmq.queue}")
    private String queueName;

    @Value("${coldchain.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${coldchain.rabbitmq.routingkey}")
    private String routingKey;

    @Bean
    public Queue telemetryQueue() {
        return new Queue(queueName, true);
    }

    @Bean
    public DirectExchange coldChainExchange() {
        return new DirectExchange(exchangeName);
    }

    @Bean
    public Binding binding(Queue telemetryQueue, DirectExchange coldChainExchange) {
        return BindingBuilder.bind(telemetryQueue).to(coldChainExchange).with(routingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
