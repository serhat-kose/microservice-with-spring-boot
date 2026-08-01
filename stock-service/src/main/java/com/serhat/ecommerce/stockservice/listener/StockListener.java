package com.serhat.ecommerce.stockservice.listener;

import com.serhat.ecommerce.stockservice.model.Stock;
import com.serhat.ecommerce.stockservice.repository.StockRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class StockListener {
    private final StockRepository repo;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper = new ObjectMapper();

    public StockListener(StockRepository repo, KafkaTemplate<String, String> kafka) {
        this.repo = repo;
        this.kafka = kafka;
    }

    @KafkaListener(topics = "stock-reserve-request", groupId = "stock-group")
    @Transactional
    public void onReserveRequest(String message) throws Exception {
        Map<?,?> m = mapper.readValue(message, Map.class);
        String orderId = (String) m.get("orderId");
        Object itemsObj = m.get("items");
        List<Map<String,Object>> items = mapper.convertValue(itemsObj, List.class);

        // check availability
        for (Map<String,Object> it : items) {
            String productId = (String) it.get("productId");
            int qty = (int) ( (Number) it.get("quantity") ).intValue();
            Optional<Stock> sOpt = repo.findByProductId(productId);
            if (sOpt.isEmpty() || sOpt.get().getQuantity() < qty) {
                String evt = mapper.writeValueAsString(Map.of("orderId", orderId, "reason", "insufficient_stock", "userId", m.get("userId")));
                kafka.send("stock-reservation-failed", orderId, evt);
                return;
            }
        }

        // reserve (decrement)
        for (Map<String,Object> it : items) {
            String productId = (String) it.get("productId");
            int qty = (int) ( (Number) it.get("quantity") ).intValue();
            Stock s = repo.findByProductId(productId).get();
            s.setQuantity(s.getQuantity() - qty);
            repo.save(s);
        }
        String evt = mapper.writeValueAsString(Map.of("orderId", orderId, "userId", m.get("userId"), "items", items, "amount", m.get("amount")));
        kafka.send("stock-reserved", orderId, evt);
    }

    @KafkaListener(topics = "stock-release", groupId = "stock-group")
    @Transactional
    public void onRelease(String message) throws Exception {
        Map<?,?> m = mapper.readValue(message, Map.class);
        Object itemsObj = m.get("items");
        List<Map<String,Object>> items = mapper.convertValue(itemsObj, List.class);

        for (Map<String,Object> it : items) {
            String productId = (String) it.get("productId");
            int qty = (int) ( (Number) it.get("quantity") ).intValue();
            Optional<Stock> sOpt = repo.findByProductId(productId);
            if (sOpt.isPresent()) {
                Stock s = sOpt.get();
                s.setQuantity(s.getQuantity() + qty);
                repo.save(s);
            } else {
                // ignore or create back
                repo.save(Stock.builder().productId(productId).quantity(qty).build());
            }
        }
    }
}
