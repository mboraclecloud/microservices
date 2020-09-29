package com.xx.microservices.core.recommendation.services;


import com.xx.api.core.recommendation.Recommendation;
import com.xx.api.core.recommendation.RecommendationService;
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
    private final RecommendationService recommendationService;

    @Autowired
    public MessageProcessor(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @StreamListener(target = Sink.INPUT)
    public void process (Event<Integer, Recommendation> event){
        LOG.info("Process message created at {}...",event.getEventCreatedAt());
        switch (event.getEventType()){
            case CREATE:
                Recommendation recommendation = event.getData();
                LOG.info("Create recommendation with ID: {}", recommendation.getRecommendationId());
                recommendationService.createRecommendation(recommendation);
                break;
            case DELETE:
                int recommendationId = event.getKey();
                LOG.info("Delete recommendation with ID: {}", recommendationId);
                recommendationService.deleteRecommendation(recommendationId);
                break;
            default:
                LOG.warn("Invalid eventType: {}, expected a CREATE or DELETE event", event.getEventType());
              break;
        }
        LOG.info("Message processing done!");
    }
}
