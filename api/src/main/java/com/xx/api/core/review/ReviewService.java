package com.xx.api.core.review;

import com.xx.api.core.product.Product;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ReviewService {

    @GetMapping(
            value = "/review",
            produces = "application/json"
    )
    Flux<Review> getReviews(@RequestParam(value = "productId", required = true) int productId);

    @PostMapping(
            value = "/review",
            consumes = "application/json",
            produces = "application/json"
    )
    Review createReview(@RequestBody Review body);

    @DeleteMapping(
            value = "/review/{productId}"
    )
    void deleteReview(@PathVariable int productId);
}
