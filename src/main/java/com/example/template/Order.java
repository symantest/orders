package com.example.template;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.BeanUtils;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;

import javax.persistence.*;
import java.util.Optional;

@Entity
@Table(name = "order_table")
public class Order {

    @Id
    @GeneratedValue
    private Long id;
    private Long productId;
    private String productName;
    private int quantity;
    private int price;
    private String customerId;
    private String customerName;
    private String customerAddr;

    /**
     * 주문이 들어옴
     */
    @PostPersist
    private void publishOrderPlaced() {
        KafkaTemplate kafkaTemplate = Application.applicationContext.getBean(KafkaTemplate.class);
        RestTemplate restTemplate = Application.applicationContext.getBean(RestTemplate.class);

        Environment env = Application.applicationContext.getEnvironment();
        ObjectMapper objectMapper = new ObjectMapper();
        String json = null;

        if( productId == null ){
            throw new RuntimeException();
        }

        if("true".equalsIgnoreCase(env.getProperty("checkStock"))){
            // 1. 주문에 대한 상품 조회 - API
            String productUrl = env.getProperty("productUrl") + "/products/" + productId;

            ResponseEntity<String> productEntity = restTemplate.getForEntity(productUrl, String.class);
            JsonParser parser = new JsonParser();
            JsonObject jsonObject = parser.parse(productEntity.getBody()).getAsJsonObject();

            this.setPrice(jsonObject.get("price").getAsInt());
            this.setProductName(jsonObject.get("name").getAsString());

            if( jsonObject.get("stock").getAsInt() < getQuantity()){
                throw new RuntimeException("No Available stock!");
            }
        }

        OrderPlaced orderPlaced = new OrderPlaced();
        try {
            orderPlaced.setOrderId(id);
            BeanUtils.copyProperties(this, orderPlaced);
            json = objectMapper.writeValueAsString(orderPlaced);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON format exception", e);
        }

        // 2. 주문이 발생함 이벤트 발송
        String topicName = env.getProperty("eventTopic");
        ProducerRecord producerRecord = new ProducerRecord<>(topicName, json);
        kafkaTemplate.send(producerRecord);
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerAddr() {
        return customerAddr;
    }

    public void setCustomerAddr(String customerAddr) {
        this.customerAddr = customerAddr;
    }
}