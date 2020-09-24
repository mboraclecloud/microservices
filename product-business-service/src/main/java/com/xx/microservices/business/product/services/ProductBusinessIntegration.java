package com.xx.microservices.business.product.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xx.api.core.product.Product;
import com.xx.api.core.product.ProductService;
import com.xx.api.core.recommendation.Recommendation;
import com.xx.api.core.recommendation.RecommendationService;
import com.xx.api.core.review.Review;
import com.xx.api.core.review.ReviewService;
import com.xx.util.exceptions.InvalidInputException;
import com.xx.util.exceptions.NotFoundException;
import com.xx.util.http.HttpErrorInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpMethod.GET;
import static reactor.core.publisher.Flux.empty;

@Component
public class ProductBusinessIntegration implements ProductService, RecommendationService, ReviewService {

    private static final Logger LOG = LoggerFactory.getLogger(ProductBusinessIntegration.class);
    private final WebClient webClient;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    private final String productServiceUrl;
    private final String recommendationServiceUrl;
    private final String reviewServiceUrl;

    @Autowired
    public ProductBusinessIntegration(
            WebClient.Builder webClient,
            RestTemplate restTemplate,
            ObjectMapper mapper,
            @Value("${app.product-service.host}") String productServiceHost,
            @Value("${app.product-service.port}") int productServicePort,

            @Value("${app.recommendation-service.host}") String recommendationServiceHost,
            @Value("${app.recommendation-service.port}") int recommendationServicePort,

            @Value("${app.review-service.host}") String reviewServiceHost,
            @Value("${app.review-service.port}") int reviewServicePort
    ) {

        this.webClient = webClient.build();
        this.mapper = mapper;
        this.restTemplate = restTemplate;
        productServiceUrl = "http://" + productServiceHost + ":" + productServicePort + "/product/";
        recommendationServiceUrl = "http://" + recommendationServiceHost + ":" + recommendationServicePort + "/recommendation?productId=";
        reviewServiceUrl = "http://" + reviewServiceHost + ":" + reviewServicePort + "/review?productId=";
    }


    @Override
    public Mono<Product> getProduct(int productId) {

            String url = productServiceUrl + "/" + productId;
            LOG.debug("Will call the getProduct API on URL: {}", url);

            return webClient.get().uri(url)
                    .retrieve()
                    .bodyToMono(Product.class)
                    .log()
                    .onErrorMap(WebClientResponseException.class, ex -> handleException(ex));
    }

    private String getErrorMessage(HttpClientErrorException ex) {
        try {
            return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (IOException ioex) {
            return ex.getMessage();
        }
    }

    private String getErrorMessage(WebClientResponseException ex) {
        try {
            return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (IOException ioex) {
            return ex.getMessage();
        }
    }


    @Override
    public Flux<Recommendation> getRecommendations(int productId) {

            String url = recommendationServiceUrl + "?productId=" + productId;

            LOG.debug("Will call the getRecommendations API on URL: {}", url);

            return webClient.get().uri(url)
                    .retrieve()
                    .bodyToFlux(Recommendation.class)
                    .log()
                    .onErrorResume(error -> empty());

    }

    @Override
    public Flux<Review> getReviews(int productId) {
            String url = reviewServiceUrl + "?productId=" + productId;

            LOG.debug("Will call the getReviews API on URL: {}", url);
            return webClient.get().uri(url)
                    .retrieve()
                    .bodyToFlux(Review.class)
                    .onErrorResume(error -> empty());
    }

    @Override
    public Product createProduct(Product body) {

        try {
            String url = productServiceUrl;
            LOG.debug("Will post a new product to URL: {}", url);

            Product product = restTemplate.postForObject(url, body, Product.class);
            LOG.debug("Created a product with id: {}", product.getProductId());

            return product;

        } catch (HttpClientErrorException ex) {
            throw handleHttpClientException(ex);
        }
    }

    @Override
    public void deleteProduct(int productId) {
        try {
            String url = productServiceUrl + "/" + productId;
            LOG.debug("Will call the deleteProduct API on URL: {}", url);

            restTemplate.delete(url);

        } catch (HttpClientErrorException ex) {
            throw handleHttpClientException(ex);
        }
    }

    @Override
    public Recommendation createRecommendation(Recommendation body) {

        try {
            String url = recommendationServiceUrl;
            LOG.debug("Will post a new recommendation to URL: {}", url);

            Recommendation recommendation = restTemplate.postForObject(url, body, Recommendation.class);
            LOG.debug("Created a recommendation with id: {}", recommendation.getProductId());

            return recommendation;

        } catch (HttpClientErrorException ex) {
            throw handleHttpClientException(ex);
        }
    }

    @Override
    public void deleteRecommendation(int productId) {
        try {
            String url = recommendationServiceUrl + "?productId=" + productId;
            LOG.debug("Will call the deleteRecommendations API on URL: {}", url);

            restTemplate.delete(url);

        } catch (HttpClientErrorException ex) {
            throw handleHttpClientException(ex);
        }
    }


    @Override
    public Review createReview(Review body) {

        try {
            String url = reviewServiceUrl;
            LOG.debug("Will post a new review to URL: {}", url);

            Review review = restTemplate.postForObject(url, body, Review.class);
            LOG.debug("Created a review with id: {}", review.getProductId());

            return review;

        } catch (HttpClientErrorException ex) {
            throw handleHttpClientException(ex);
        }
    }

    @Override
    public void deleteReview(int productId) {
        try {
            String url = reviewServiceUrl + "?productId=" + productId;
            LOG.debug("Will call the deleteReviews API on URL: {}", url);

            restTemplate.delete(url);

        } catch (HttpClientErrorException ex) {
            throw handleHttpClientException(ex);
        }
    }

    private RuntimeException handleHttpClientException(HttpClientErrorException ex) {
        switch (ex.getStatusCode()) {

            case NOT_FOUND:
                return new NotFoundException(getErrorMessage(ex));

            case UNPROCESSABLE_ENTITY :
                return new InvalidInputException(getErrorMessage(ex));

            default:
                LOG.warn("Got a unexpected HTTP error: {}, will rethrow it", ex.getStatusCode());
                LOG.warn("Error body: {}", ex.getResponseBodyAsString());
                return ex;
        }
    }

    private Throwable handleException(Throwable ex) {

        if (!(ex instanceof WebClientResponseException)) {
            LOG.warn("Got a unexpected error: {}, will rethrow it", ex.toString());
            return ex;
        }

        WebClientResponseException wcre = (WebClientResponseException)ex;

        switch (wcre.getStatusCode()) {

            case NOT_FOUND:
                return new NotFoundException(getErrorMessage(wcre));

            case UNPROCESSABLE_ENTITY :
                return new InvalidInputException(getErrorMessage(wcre));

            default:
                LOG.warn("Got a unexpected HTTP error: {}, will rethrow it", wcre.getStatusCode());
                LOG.warn("Error body: {}", wcre.getResponseBodyAsString());
                return ex;
        }
    }

}
