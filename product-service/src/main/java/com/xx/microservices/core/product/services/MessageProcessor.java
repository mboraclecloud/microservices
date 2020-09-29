package com.xx.microservices.core.product.services;

import com.xx.api.core.product.Product;
import com.xx.api.core.product.ProductService;
import com.xx.api.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;

@EnableBinding(Sink.class)
public class MessageProcessor {
    private final static Logger LOG = LoggerFactory.getLogger(MessageProcessor.class);
    private final ProductService productService;

    @Autowired
    public MessageProcessor(ProductService productService) {
        this.productService = productService;
    }

    @StreamListener(target = Sink.INPUT)
    public void process (Event<Integer, Product> event){
        LOG.info("Process message created at {}...",event.getEventCreatedAt());
        switch (event.getEventType()){
            case CREATE:
                Product product = event.getData();
                LOG.info("Create product with ID: {}", product.getProductId());
                productService.createProduct(product);
                break;
            case DELETE:
                int productId = event.getKey();
                LOG.info("Delete product with ID: {}", productId);
                productService.deleteProduct(productId);
                break;
            default:
                LOG.warn("Invalid eventType: {}, expected a CREATE or DELETE event", event.getEventType());
              break;
        }
        LOG.info("Message processing done!");
    }
}
