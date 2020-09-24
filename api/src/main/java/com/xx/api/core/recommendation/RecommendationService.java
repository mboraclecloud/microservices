package com.xx.api.core.recommendation;

import com.xx.api.core.product.Product;
import com.xx.api.core.review.Review;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface RecommendationService {

    @GetMapping(
            value = "/recommendation",
            produces = "application/json"
    )
    Flux<Recommendation> getRecommendations(@RequestParam(
            value = "productId",
            required = true
    ) int productId);

    @PostMapping(
            value = "/recommendation",
            consumes = "application/json",
            produces = "application/json"
    )
    Recommendation createRecommendation(@RequestBody Recommendation body);

    @DeleteMapping(
            value = "/recommendation/{productId}"
    )
    void deleteRecommendation(@PathVariable int productId);
}
