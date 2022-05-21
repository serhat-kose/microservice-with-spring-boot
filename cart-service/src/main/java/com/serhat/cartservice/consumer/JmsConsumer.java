package com.serhat.cartservice.consumer;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.serhat.cartservice.entity.*;
import com.serhat.cartservice.repository.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.jms.annotation.*;
import org.springframework.stereotype.*;

@Component
public class JmsConsumer {

    @Autowired
    ProductRepository productRepository;

    @JmsListener(destination = "${product.jms.destination}")
    public void consumeMessage(String data)  {

        try {

            ObjectMapper mapper = new ObjectMapper();
            //Json data to Product object
            Product product = mapper.readValue(data,Product.class);
            productRepository.save(product);

        } catch (JsonProcessingException e){
            e.getStackTrace();
        }
    }
}
