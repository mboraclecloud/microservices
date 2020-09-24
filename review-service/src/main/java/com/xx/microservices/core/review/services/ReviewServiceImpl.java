package com.xx.microservices.core.review.services;

import com.xx.api.core.review.Review;
import com.xx.api.core.review.ReviewService;
import com.xx.microservices.core.review.persistence.ReviewEntity;
import com.xx.microservices.core.review.persistence.ReviewRepository;
import com.xx.util.exceptions.InvalidInputException;
import com.xx.util.http.ServiceUtil;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static java.util.logging.Level.FINE;

@RestController
public class ReviewServiceImpl implements ReviewService {

    private static  final Logger LOG = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final ServiceUtil serviceUtil;
    private final ReviewRepository reviewRepository;
    private final ReviewMapper mapper;
    private final Scheduler scheduler;

    @Autowired
    public ReviewServiceImpl(ServiceUtil serviceUtil, ReviewRepository reviewRepository, ReviewMapper mapper, Scheduler scheduler) {
        this.serviceUtil = serviceUtil;
        this.reviewRepository = reviewRepository;
        this.mapper = mapper;
        this.scheduler = scheduler;
    }

    @Override
    public Review createReview(Review review) {
        try {
            ReviewEntity entity = mapper.apiToEntity(review);
            ReviewEntity newEntity = reviewRepository.save(entity);

            LOG.debug("createRecommendation: created a recommendation entity: {}/{}", review.getProductId(), review.getReviewId());
            return mapper.entityToApi(newEntity);

        } catch (DataIntegrityViolationException dive) {
            throw new InvalidInputException("Duplicate key, Product Id: " + review.getProductId() + ", Review Id:" + review.getReviewId());
        }
    }

    @Override
    public Flux<Review> getReviews(int productId) {
        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);

        LOG.info("Will get reviews for product with id={}", productId);

        return asyncFlux(() -> Flux.fromIterable(getByProductId(productId))).log(null, FINE);
    }

    protected List<Review> getByProductId(int productId) {

        List<ReviewEntity> entityList = reviewRepository.findByProductId(productId);
        List<Review> list = mapper.entityListToApiList(entityList);
        list.forEach(e -> e.setServiceAddress(serviceUtil.getServiceAddress()));

        LOG.debug("getReviews: response size: {}", list.size());

        return list;
    }

    @Override
    public void deleteReview(int productId) {
        LOG.debug("deleteReviews: tries to delete reviews for the product with productId: {}", productId);
        reviewRepository.deleteAll(reviewRepository.findByProductId(productId));
    }

    private <T> Flux<T> asyncFlux(Supplier<Publisher<T>> publisherSupplier) {
        return Flux.defer(publisherSupplier).subscribeOn(scheduler);
    }
}
