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

        String productUrl = env.getProperty("productUrl") + "/products/" + productId;

        // 1. checkInventory
        ResponseEntity<String> productEntity = restTemplate.getForEntity(productUrl, String.class);
        System.out.println(productEntity.getStatusCode());
        System.out.println(productEntity.getBody());

        JsonParser parser = new JsonParser();
        JsonObject jsonObject = parser.parse(productEntity.getBody()).getAsJsonObject();

        OrderPlaced orderPlaced = new OrderPlaced();
        try {
            orderPlaced.setOrderId(id);

            this.setPrice(jsonObject.get("price").getAsInt());
            this.setProductName(jsonObject.get("name").getAsString());

            BeanUtils.copyProperties(this, orderPlaced);
            json = objectMapper.writeValueAsString(orderPlaced);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON format exception", e);
        }

        ProducerRecord producerRecord = new ProducerRecord<>("eventTopic", json);
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