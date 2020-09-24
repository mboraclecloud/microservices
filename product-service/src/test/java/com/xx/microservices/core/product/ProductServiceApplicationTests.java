package com.xx.microservices.core.product;

import com.xx.api.core.product.Product;
import com.xx.microservices.core.product.persistence.ProductRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.just;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment=RANDOM_PORT, properties = {"spring.data.mongodb.port: 0"})

public class ProductServiceApplicationTests {

	@Autowired
	private WebTestClient client;
	@Autowired
	private ProductRepository repository;

	@Before
	public void setupDb() {
		repository.deleteAll().block();
	}

	@Test
	public void getProductById() {

		int productId = 1;

		assertNull(repository.findByProductId(productId).block());
		assertEquals(0, (long)repository.count().block());

		postAndVerifyProduct(productId, OK);
		assertNotNull(repository.findByProductId(productId).block());
		assertEquals(1, (long)repository.count().block());

		getAndVerifyProduct(productId, OK)
				.jsonPath("$.productId").isEqualTo(productId);
	}

	@Test
	public void duplicateError() {

		int productId = 1;
		assertNull(repository.findByProductId(productId).block());

		postAndVerifyProduct(productId, OK);

		assertNotNull(repository.findByProductId(productId).block());

		postAndVerifyProduct(productId, UNPROCESSABLE_ENTITY)
				.jsonPath("$.path").isEqualTo("/product")
				.jsonPath("$.message").isEqualTo("Duplicate key, Product Id: " + productId);
	}

	@Test
	public void deleteProduct() {

		int productId = 1;

		postAndVerifyProduct(productId, OK);
		assertNotNull(repository.findByProductId(productId).block());

		deleteAndVerifyProduct(productId, OK);
		assertNull(repository.findByProductId(productId).block());

		deleteAndVerifyProduct(productId, OK);
	}

	@Test
	public void getProductInvalidParameterString() {

		getAndVerifyProduct("/no-integer", BAD_REQUEST)
				.jsonPath("$.path").isEqualTo("/product/no-integer");
//				.jsonPath("$.message").isEqualTo("Type mismatch.");
	}

	@Test
	public void getProductNotFound() {

		int productIdNotFound = 13;
		getAndVerifyProduct(productIdNotFound, NOT_FOUND)
				.jsonPath("$.path").isEqualTo("/product/" + productIdNotFound)
				.jsonPath("$.message").isEqualTo("No product found for productId: " + productIdNotFound);
	}

	@Test
	public void getProductInvalidParameterNegativeValue() {

		int productIdInvalid = -1;

		getAndVerifyProduct(productIdInvalid, UNPROCESSABLE_ENTITY)
				.jsonPath("$.path").isEqualTo("/product/" + productIdInvalid)
				.jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
	}

	private WebTestClient.BodyContentSpec getAndVerifyProduct(int productId, HttpStatus expectedStatus) {
		return getAndVerifyProduct("/" + productId, expectedStatus);
	}

	private WebTestClient.BodyContentSpec getAndVerifyProduct(String productIdPath, HttpStatus expectedStatus) {
		return client.get()
				.uri("/product" + productIdPath)
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectHeader().contentType(APPLICATION_JSON)
				.expectBody();
	}

	private WebTestClient.BodyContentSpec postAndVerifyProduct(int productId, HttpStatus expectedStatus) {
		Product product = new Product(productId, "Name " + productId, productId, "SA");
		return client.post()
				.uri("/product")
				.body(just(product), Product.class)
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectHeader().contentType(APPLICATION_JSON)
				.expectBody();
	}

	private WebTestClient.BodyContentSpec deleteAndVerifyProduct(int productId, HttpStatus expectedStatus) {
		return client.delete()
				.uri("/product/" + productId)
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectBody();
	}
}