package com.xx.microservices.business.product;

import com.xx.microservices.business.product.services.ProductBusinessIntegration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.health.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.LinkedHashMap;

import static java.util.Collections.emptyList;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static springfox.documentation.builders.RequestHandlerSelectors.basePackage;
import static springfox.documentation.spi.DocumentationType.SWAGGER_2;

@SpringBootApplication
@ComponentScan("com.xx")
@EnableSwagger2
public class ProductBusinessServiceApplication {

	@Value("${api.common.version}")           String apiVersion;
	@Value("${api.common.title}")             String apiTitle;
	@Value("${api.common.description}")       String apiDescription;
	@Value("${api.common.termsOfServiceUrl}") String apiTermsOfServiceUrl;
	@Value("${api.common.license}")           String apiLicense;
	@Value("${api.common.licenseUrl}")        String apiLicenseUrl;
	@Value("${api.common.contact.name}")      String apiContactName;
	@Value("${api.common.contact.url}")       String apiContactUrl;
	@Value("${api.common.contact.email}")     String apiContactEmail;

	/**
	 * Will exposed on $HOST:$PORT/swagger-ui.html
	 *
	 * @return
	 */
	@Bean
	public Docket apiDocumentation() {

		return new Docket(SWAGGER_2)
				.select()
				.apis(basePackage("com.xx.microservices.business.product"))
				.paths(PathSelectors.any())
				.build()
				.globalResponseMessage(GET, emptyList())
				.apiInfo(new ApiInfo(
						apiTitle,
						apiDescription,
						apiVersion,
						apiTermsOfServiceUrl,
						new Contact(apiContactName, apiContactUrl, apiContactEmail),
						apiLicense,
						apiLicenseUrl,
						emptyList()
				));
	}

	@Bean
	RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Autowired
	HealthAggregator healthAggregator;

	@Autowired
	ProductBusinessIntegration integration;

	@Bean
	ReactiveHealthIndicator coreServices(){
		ReactiveHealthIndicatorRegistry registry = new
				DefaultReactiveHealthIndicatorRegistry(new LinkedHashMap<>());
		registry.register("product", () -> integration.getProductHealth());
		registry.register("recommendation", () ->
				integration.getRecommendationHealth());
		registry.register("review", () -> integration.getReviewHealth());
		return new CompositeReactiveHealthIndicator(healthAggregator,
				registry);
	}


	public static void main(String[] args) {
		SpringApplication.run(ProductBusinessServiceApplication.class, args);
	}

}
