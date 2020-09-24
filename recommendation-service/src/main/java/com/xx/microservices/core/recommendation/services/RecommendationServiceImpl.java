package com.xx.microservices.core.recommendation.services;

import com.xx.api.core.product.Product;
import com.xx.api.core.recommendation.Recommendation;
import com.xx.api.core.recommendation.RecommendationService;
import com.xx.microservices.core.recommendation.persistence.RecommendationEntity;
import com.xx.microservices.core.recommendation.persistence.RecommendationRepository;
import com.xx.util.exceptions.InvalidInputException;
import com.xx.util.http.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger LOG = LoggerFactory.getLogger(RecommendationServiceImpl.class);
    private final ServiceUtil serviceUtil;
    private final RecommendationRepository recommendationRepository;
    private final RecommendationMapper mapper;

    @Autowired
    public RecommendationServiceImpl(ServiceUtil serviceUtil, RecommendationRepository recommendationRepository, RecommendationMapper mapper) {
        this.serviceUtil = serviceUtil;
        this.recommendationRepository = recommendationRepository;
        this.mapper = mapper;
    }

    @Override
    public Flux<Recommendation> getRecommendations(int productId) {
        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);

        return  recommendationRepository.findByProductId(productId)
                .log()
                .map(e -> mapper.entityToApi(e))
                .map(e -> {e.setServiceAddress(serviceUtil.getServiceAddress()); return e;});

    }

    @Override
    public Recommendation createRecommendation(Recommendation recommendation) {

            RecommendationEntity entity = mapper.apiToEntity(recommendation);
            Mono<Recommendation> newEntity = recommendationRepository.save(entity)
                    .log()
                    .onErrorMap(DuplicateKeyException.class, ex -> new InvalidInputException("Duplicate key, Product Id: " + recommendation.getProductId() + "," +
                            " Recommendation Id:" + recommendation.getRecommendationId()))
                    .map(e -> mapper.entityToApi(e));

            LOG.debug("createRecommendation: created a recommendation entity: {}/{}", recommendation.getProductId(), recommendation.getRecommendationId());
            return newEntity.block();
    }


    @Override
    public void deleteRecommendation(int productId) {
        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);
        LOG.debug("deleteRecommendations: tries to delete recommendations for the product with productId: {}", productId);
        recommendationRepository.deleteAll(recommendationRepository.findByProductId(productId)).block();
    }
}
