package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.event.OrderCancelledEvent;
import com.ecommerce.orderservice.event.OrderCreatedEvent;
import com.ecommerce.orderservice.event.OrderStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaProducerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.topics.order-created}")
    private String orderCreatedTopic;

    @Value("${spring.kafka.topics.order-status-changed}")
    private String orderStatusChangedTopic;

    @Value("${spring.kafka.topics.order-cancelled}")
    private String orderCancelledTopic;

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publish OrderCreatedEvent to Kafka
     */
    public void publishOrderCreatedEvent(OrderCreatedEvent event) {
        String key = String.valueOf(event.getOrderId());
        
        logger.info("Publishing OrderCreatedEvent: orderId={}, userId={}, totalAmount={}", 
                event.getOrderId(), event.getUserId(), event.getTotalAmount());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                orderCreatedTopic, 
                key, 
                event
        );

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("OrderCreatedEvent published successfully: topic={}, partition={}, offset={}", 
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                logger.error("Failed to publish OrderCreatedEvent: orderId={}", event.getOrderId(), ex);
            }
        });
    }

    /**
     * Publish OrderStatusChangedEvent to Kafka
     */
    public void publishOrderStatusChangedEvent(OrderStatusChangedEvent event) {
        String key = String.valueOf(event.getOrderId());
        
        logger.info("Publishing OrderStatusChangedEvent: orderId={}, previousStatus={}, newStatus={}", 
                event.getOrderId(), event.getPreviousStatus(), event.getStatus());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                orderStatusChangedTopic, 
                key, 
                event
        );

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("OrderStatusChangedEvent published successfully: topic={}, partition={}, offset={}", 
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                logger.error("Failed to publish OrderStatusChangedEvent: orderId={}", event.getOrderId(), ex);
            }
        });
    }

    /**
     * Publish OrderCancelledEvent to Kafka
     */
    public void publishOrderCancelledEvent(OrderCancelledEvent event) {
        String key = String.valueOf(event.getOrderId());
        
        logger.info("Publishing OrderCancelledEvent: orderId={}, reason={}", 
                event.getOrderId(), event.getCancellationReason());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                orderCancelledTopic, 
                key, 
                event
        );

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                logger.info("OrderCancelledEvent published successfully: topic={}, partition={}, offset={}", 
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                logger.error("Failed to publish OrderCancelledEvent: orderId={}", event.getOrderId(), ex);
            }
        });
    }
}