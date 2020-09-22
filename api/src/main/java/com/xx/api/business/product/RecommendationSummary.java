package com.xx.api.business.product;

public class RecommendationSummary {

    private final int recommendationId;
    private final String author;
    private final String rate;
    private final String content;

    public RecommendationSummary() {
        this.recommendationId = 0;
        this.author = null;
        this.rate = null;
        this.content = null;
    }

    public RecommendationSummary(int recommendationId, String author, String rate, String content) {
        this.recommendationId = recommendationId;
        this.author = author;
        this.rate = rate;
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public int getRecommendationId() {
        return recommendationId;
    }

    public String getAuthor() {
        return author;
    }

    public String getRate() {
        return rate;
    }
}
