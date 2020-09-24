package com.xx.microservices.business.product;

import com.xx.api.business.product.ProductAggregate;
import com.xx.api.business.product.RecommendationSummary;
import com.xx.api.business.product.ReviewSummary;
import com.xx.api.core.product.Product;
import com.xx.api.core.recommendation.Recommendation;
import com.xx.api.core.review.Review;
import com.xx.microservices.business.product.services.ProductBusinessIntegration;
import com.xx.util.exceptions.InvalidInputException;
import com.xx.util.exceptions.NotFoundException;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.just;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
class ProductBusinessServiceApplicationTests {
	private static final int PRODUCT_ID_OK = 1;
	private static final int PRODUCT_ID_NOT_FOUND = 2;
	private static final int PRODUCT_ID_INVALID = 3;

	@Autowired
	private WebTestClient client;

	@MockBean
	private ProductBusinessIntegration integration;

	@Before
	public void setUp() {

		when(integration.getProduct(PRODUCT_ID_OK)).
				thenReturn(just(new Product(PRODUCT_ID_OK, "name", 1, "mock-address")));
		when(integration.getRecommendations(PRODUCT_ID_OK)).
				thenReturn(Flux.fromIterable(singletonList(new Recommendation(PRODUCT_ID_OK, 1, "author", "1", "content", "mock address"))));
		when(integration.getReviews(PRODUCT_ID_OK)).
				thenReturn(Flux.fromIterable(singletonList(new Review(PRODUCT_ID_OK, 1, "author", "subject", "content", "mock address"))));

		when(integration.getProduct(PRODUCT_ID_NOT_FOUND)).thenThrow(new NotFoundException("No product found for productId: " + PRODUCT_ID_NOT_FOUND));

		when(integration.getProduct(PRODUCT_ID_INVALID)).thenThrow(new InvalidInputException("INVALID: " + PRODUCT_ID_INVALID));
	}

	@Test
	void contextLoads() {
	}

	@Test
	public void createCompositeProduct1() {

		ProductAggregate compositeProduct = new ProductAggregate(1, "name", 1, null, null, null);

		postAndVerifyProduct(compositeProduct, OK);
	}

	@Test
	public void createCompositeProduct2() {
		ProductAggregate compositeProduct = new ProductAggregate(1, "name", 1,
				singletonList(new RecommendationSummary(1, "a", 1+"", "c")),
				singletonList(new ReviewSummary(1, "a", "s", "c")), null);

		postAndVerifyProduct(compositeProduct, OK);
	}

	@Test
	public void deleteCompositeProduct() {
		ProductAggregate compositeProduct = new ProductAggregate(1, "name", 1,
				singletonList(new RecommendationSummary(1, "a", 1+"", "c")),
				singletonList(new ReviewSummary(1, "a", "s", "c")), null);

		postAndVerifyProduct(compositeProduct, OK);

		deleteAndVerifyProduct(compositeProduct.getProductId(), OK);
		deleteAndVerifyProduct(compositeProduct.getProductId(), OK);
	}


	@Test
	public void getProductById() {
		setUp();
		getAndVerifyProduct(PRODUCT_ID_OK, OK)
				.jsonPath("$.productId").isEqualTo(PRODUCT_ID_OK)
				.jsonPath("$.recommendations.length()").isEqualTo(1)
				.jsonPath("$.reviews.length()").isEqualTo(1);
	}

	@Test
	public void getProductNotFound() {
		setUp();
		getAndVerifyProduct(PRODUCT_ID_NOT_FOUND, NOT_FOUND)
				.jsonPath("$.path").isEqualTo("/product-business/" + PRODUCT_ID_NOT_FOUND)
				.jsonPath("$.message").isEqualTo("No product found for productId: " + PRODUCT_ID_NOT_FOUND);
	}


	@Test
	public void getProductInvalidInput() {
		setUp();
		getAndVerifyProduct(PRODUCT_ID_INVALID, UNPROCESSABLE_ENTITY)
				.jsonPath("$.path").isEqualTo("/product-business/" + PRODUCT_ID_INVALID)
				.jsonPath("$.message").isEqualTo("INVALID: " + PRODUCT_ID_INVALID);
	}

	private WebTestClient.BodyContentSpec getAndVerifyProduct(int productId, HttpStatus expectedStatus) {
		return client.get()
				.uri("/product-business/" + productId)
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectHeader().contentType(APPLICATION_JSON)
				.expectBody();
	}

	private void postAndVerifyProduct(ProductAggregate compositeProduct, HttpStatus expectedStatus) {
		client.post()
				.uri("/product-business")
				.body(just(compositeProduct), ProductAggregate.class)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus);
	}

	private void deleteAndVerifyProduct(int productId, HttpStatus expectedStatus) {
		client.delete()
				.uri("/product-business/" + productId)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus);
	}

}
