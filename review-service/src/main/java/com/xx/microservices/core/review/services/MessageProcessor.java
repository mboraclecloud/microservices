package com.xx.microservices.core.review.services;


import com.xx.api.core.review.Review;
import com.xx.api.core.review.ReviewService;
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
    private final ReviewService reviewService;

    @Autowired
    public MessageProcessor(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @StreamListener(target = Sink.INPUT)
    public void process (Event<Integer, Review> event){
        LOG.info("Process message created at {}...",event.getEventCreatedAt());
        switch (event.getEventType()){
            case CREATE:
                Review review = event.getData();
                LOG.info("Create review with ID: {}", review.getReviewId());
                reviewService.createReview(review);
                break;
            case DELETE:
                int reviewId = event.getKey();
                LOG.info("Delete review with ID: {}", reviewId);
                reviewService.deleteReview(reviewId);
                break;
            default:
                LOG.warn("Invalid eventType: {}, expected a CREATE or DELETE event", event.getEventType());
              break;
        }
        LOG.info("Message processing done!");
    }
}
