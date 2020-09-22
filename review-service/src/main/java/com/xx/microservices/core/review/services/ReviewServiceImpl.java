package com.xx.microservices.core.review.services;

import com.xx.api.core.review.Review;
import com.xx.api.core.review.ReviewService;
import com.xx.microservices.core.review.persistence.ReviewEntity;
import com.xx.microservices.core.review.persistence.ReviewRepository;
import com.xx.util.exceptions.InvalidInputException;
import com.xx.util.http.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
public class ReviewServiceImpl implements ReviewService {

    private static  final Logger LOG = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final ServiceUtil serviceUtil;
    private final ReviewRepository reviewRepository;
    private final ReviewMapper mapper;

    @Autowired
    public ReviewServiceImpl(ServiceUtil serviceUtil, ReviewRepository reviewRepository, ReviewMapper mapper) {
        this.serviceUtil = serviceUtil;
        this.reviewRepository = reviewRepository;
        this.mapper = mapper;
    }

    @Override
    public List<Review> getReviews(int productId) {
        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);
        List<ReviewEntity> listReviewEntity = reviewRepository.findByProductId(productId);
        List<Review> listReview = mapper.entityListToApiList(listReviewEntity);
        listReview.forEach(e -> e.setServiceAddress(serviceUtil.getServiceAddress()));
        LOG.debug("getReviews: response size: {}", listReview.size());
        return  listReview;
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
    public void deleteReview(int productId) {
        LOG.debug("deleteReviews: tries to delete reviews for the product with productId: {}", productId);
        reviewRepository.deleteAll(reviewRepository.findByProductId(productId));
    }
}
