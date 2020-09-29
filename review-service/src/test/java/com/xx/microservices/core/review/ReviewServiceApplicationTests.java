package com.xx.microservices.core.review;

import com.xx.api.core.review.Review;
import com.xx.api.event.Event;
import com.xx.microservices.core.review.persistence.ReviewRepository;
import com.xx.util.exceptions.InvalidInputException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.http.HttpStatus;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.xx.api.event.Event.Type.CREATE;
import static com.xx.api.event.Event.Type.DELETE;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.just;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment=RANDOM_PORT, properties = {
		"spring.datasource.url=jdbc:h2:mem:review-db"})
public class ReviewServiceApplicationTests {

	@Autowired
	private WebTestClient client;

	@Autowired
	private ReviewRepository repository;

	@Autowired
	private Sink channels;

	private AbstractMessageChannel input = null;

	@Before
	public void setupDb() {
		input = (AbstractMessageChannel) channels.input();
		repository.deleteAll();
	}
	@Test
	public void getReviewsByProductId() {
		setupDb();
		int productId = 1;

		assertEquals(0, repository.findByProductId(productId).size());

//		postAndVerifyReview(productId, 1, OK);
//		postAndVerifyReview(productId, 2, OK);
//		postAndVerifyReview(productId, 3, OK);

		sendCreateReviewEvent(productId, 1);
		sendCreateReviewEvent(productId, 2);
		sendCreateReviewEvent(productId, 3);

		assertEquals(3, repository.findByProductId(productId).size());

		getAndVerifyReviewsByProductId(productId, OK)
				.jsonPath("$.length()").isEqualTo(3)
				.jsonPath("$[2].productId").isEqualTo(productId)
				.jsonPath("$[2].reviewId").isEqualTo(3);
	}

	@Test
	public void duplicateError() {
		setupDb();
		int productId = 1;
		int reviewId = 1;

		assertEquals(0, repository.count());

//		postAndVerifyReview(productId, reviewId, OK)
//				.jsonPath("$.productId").isEqualTo(productId)
//				.jsonPath("$.reviewId").isEqualTo(reviewId);
		sendCreateReviewEvent(productId, reviewId);

		assertEquals(1, repository.count());

//		postAndVerifyReview(productId, reviewId, UNPROCESSABLE_ENTITY)
//				.jsonPath("$.path").isEqualTo("/review")
//				.jsonPath("$.message").isEqualTo("Duplicate key, Product Id: 1, Review Id:1");
		try{
			sendCreateReviewEvent(productId, reviewId);
			fail("Expected a MessagingException here!");
		} catch (MessagingException me){

			if (me.getCause() instanceof InvalidInputException)	{
				InvalidInputException iie = (InvalidInputException)me.getCause();
				assertEquals("Duplicate key, Product Id: 1, Review Id:1", iie.getMessage());
			} else {
				fail("Expected a InvalidInputException as the root cause!");
			}
		}
		assertEquals(1, repository.count());
	}

	@Test
	public void deleteReviews() {
		setupDb();
		int productId = 1;
		int reviewId = 1;

//		postAndVerifyReview(productId, reviewId, OK);
		sendCreateReviewEvent(productId, reviewId);
		assertEquals(1, repository.findByProductId(productId).size());

//		deleteAndVerifyReviewsByProductId(productId, OK);
		sendDeleteReviewEvent(productId);

		assertEquals(0, repository.findByProductId(productId).size());

//		deleteAndVerifyReviewsByProductId(productId, OK);
		sendDeleteReviewEvent(productId);
	}

	@Test
	public void getReviewsMissingParameter() {

		getAndVerifyReviewsByProductId("", BAD_REQUEST)
				.jsonPath("$.path").isEqualTo("/review");
//				.jsonPath("$.message").isEqualTo("Required int parameter 'productId' is not present");
	}

	@Test
	public void getReviewsInvalidParameter() {

		getAndVerifyReviewsByProductId("?productId=no-integer", BAD_REQUEST)
				.jsonPath("$.path").isEqualTo("/review");
//				.jsonPath("$.message").isEqualTo("Type mismatch.");
	}

	@Test
	public void getReviewsNotFound() {

		getAndVerifyReviewsByProductId("?productId=213", OK)
				.jsonPath("$.length()").isEqualTo(0);
	}

	@Test
	public void getReviewsInvalidParameterNegativeValue() {

		int productIdInvalid = -1;

		getAndVerifyReviewsByProductId("?productId=" + productIdInvalid, UNPROCESSABLE_ENTITY)
				.jsonPath("$.path").isEqualTo("/review")
				.jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
	}

	private WebTestClient.BodyContentSpec getAndVerifyReviewsByProductId(int productId, HttpStatus expectedStatus) {
		return getAndVerifyReviewsByProductId("?productId=" + productId, expectedStatus);
	}

	private WebTestClient.BodyContentSpec getAndVerifyReviewsByProductId(String productIdQuery, HttpStatus expectedStatus) {
		return client.get()
				.uri("/review" + productIdQuery)
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectHeader().contentType(APPLICATION_JSON)
				.expectBody();
	}

	private WebTestClient.BodyContentSpec postAndVerifyReview(int productId, int reviewId, HttpStatus expectedStatus) {
		Review review = new Review(productId, reviewId, "Author " + reviewId, "Subject " + reviewId, "Content " + reviewId, "SA");
		return client.post()
				.uri("/review")
				.body(just(review), Review.class)
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectHeader().contentType(APPLICATION_JSON)
				.expectBody();
	}

	private WebTestClient.BodyContentSpec deleteAndVerifyReviewsByProductId(int productId, HttpStatus expectedStatus) {
		return client.delete()
				.uri("/review/" + productId)
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectBody();
	}

	private void sendCreateReviewEvent(int productId, int reviewId){
		Review review = new Review(productId, reviewId, "author : "+ reviewId, "subject : "+ reviewId, "content : "+ reviewId, "SA");
		Event<Integer, Review> event = new Event(CREATE,reviewId, review);
		input.send(new GenericMessage<>(event));
	}

	private void sendDeleteReviewEvent(int productId){
		Event<Integer, Review> event = new Event(DELETE, productId, null);
		input.send(new GenericMessage<>(event));
	}
}
