package com.example.template;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired
    private KafkaTemplate kafkaTemplate;

    @Autowired
    private ProductRepository productRepository;

    /**
     * 상품 변경이 발생할때마다, 상품정보를 저장해 놓음
     */
    @KafkaListener(topics = "${eventTopic}")
    public void onDeliveryCompleted(@Payload String message, ConsumerRecord<?, ?> consumerRecord) {
        System.out.println("##### listener : " + message);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        ProductChanged productChanged = null;
        try {
            productChanged = objectMapper.readValue(message, ProductChanged.class);
            if( productChanged.getEventType().equals(ProductChanged.class.getSimpleName())){

            }
            Product product = new Product();
            product.setId(productChanged.getProductId());
            product.setStock(productChanged.getProductStock());
            product.setName(productChanged.getProductName());
            product.setPrice(productChanged.getProductPrice());

            productRepository.save(product);

        }catch (Exception e){

        }

    }
}
