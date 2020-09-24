package com.xx.microservices.core.product.services;

import com.xx.api.core.product.Product;
import com.xx.api.core.product.ProductService;
import com.xx.microservices.core.product.persistence.ProductEntity;
import com.xx.microservices.core.product.persistence.ProductRepository;
import com.xx.util.exceptions.InvalidInputException;
import com.xx.util.exceptions.NotFoundException;
import com.xx.util.http.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class ProductServiceImpl implements ProductService {

    private static final Logger LOG = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ServiceUtil serviceUtil;
    private final ProductRepository productRepository;
    private final ProductMapper mapper;

    @Autowired
    public ProductServiceImpl(ServiceUtil serviceUtil, ProductRepository productRepository, ProductMapper mapper) {
        this.serviceUtil = serviceUtil;
        this.productRepository = productRepository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Product> getProduct(int productId) {
        LOG.debug("/product return the found product for productId={}", productId);

        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);

        return productRepository.findByProductId(productId)
                .switchIfEmpty(Mono.error(new NotFoundException("No product found for productId: "+productId)))
                .log()
                .map(e -> mapper.entityToApi(e))
                .map(e -> {e.setServiceAddress(serviceUtil.getServiceAddress()); return e;});

    }

    @Override
    public Product createProduct(Product product) {

            if (product.getProductId() < 1) throw new InvalidInputException("Invalid productId: " + product.getProductId());

            ProductEntity entity = mapper.apiToEntity(product);
            Mono<Product> newEntity = productRepository.save(entity)
                    .onErrorMap(DuplicateKeyException.class, ex -> new InvalidInputException("Duplicate key, Product Id: " + product.getProductId()))
                    .log()
                    .map(e -> mapper.entityToApi(e));

            return newEntity.block();
    }

    @Override
    public void deleteProduct(int productId) {
        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);
        LOG.debug("deleteProduct: tries to delete an entity with productId: {}", productId);

        productRepository.findByProductId(productId)
                .log()
                .map(e -> productRepository.delete(e))
                .flatMap(e -> e)
                .block();

    }
}
