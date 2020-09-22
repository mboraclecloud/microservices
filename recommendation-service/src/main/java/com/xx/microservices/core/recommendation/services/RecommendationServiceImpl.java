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
    public List<Recommendation> getRecommendations(int productId) {
        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);

        List<RecommendationEntity> entityList = recommendationRepository.findByProductId(productId);
        List<Recommendation> list = mapper.entityListToApiList(entityList);
        list.forEach(e -> e.setServiceAddress(serviceUtil.getServiceAddress()));

        LOG.debug("getRecommendations: response size: {}", list.size());

        return list;
    }

    @Override
    public Recommendation createRecommendation(Recommendation recommendation) {
        try {
            RecommendationEntity entity = mapper.apiToEntity(recommendation);
            RecommendationEntity newEntity = recommendationRepository.save(entity);

            LOG.debug("createRecommendation: created a recommendation entity: {}/{}", recommendation.getProductId(), recommendation.getRecommendationId());
            return mapper.entityToApi(newEntity);

        } catch (DuplicateKeyException dke) {
            throw new InvalidInputException("Duplicate key, Product Id: " + recommendation.getProductId() + ", Recommendation Id:" + recommendation.getRecommendationId());
        }
    }


    @Override
    public void deleteRecommendation(int productId) {
        LOG.debug("deleteRecommendations: tries to delete recommendations for the product with productId: {}", productId);
        recommendationRepository.deleteAll(recommendationRepository.findByProductId(productId));
    }
}
